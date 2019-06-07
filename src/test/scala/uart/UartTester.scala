// See LICENSE for license details.

package uart

import chisel3.iotesters._
import dirv.io.MemCmd

import scala.math.{floor, pow, random, round}

/**
  * Unit tester for TxRxCtrl module.
  * @param c dut module (instance of TxRXCtrl)
  * @param baudrate test duration count. this valuable is used for controlling uart signals.
  */
class UartUnitTester(c: Top, baudrate: Int, clockFreq: Int) extends PeekPokeTester(c) {

  val memAccLimit = 10
  val timeOutCycle = 1000
  val duration = round(clockFreq * pow(10, 6) / baudrate).toInt

  var i = 0

  def idle(): Unit = {
    poke(c.io.mem.valid, false)
    poke(c.io.mem.w.get.valid, false)
    poke(c.io.mem.r.get.ready, true)
  }

  def waitCmdRdy(): Unit = {
    i = 0
    while (peek(c.io.mem.ready) != 0) {
      step(1)
      i += 1
    }
  }

  def issueRdCmd(addr: Int): Unit = {
    poke(c.io.mem.cmd, MemCmd.rd)
    poke(c.io.mem.valid, true)
  }

  def waitR(exp: BigInt, cmp: Boolean): BigInt = {
    var i = 0
    poke(c.io.mem.r.get.ready, true)
    while ((peek(c.io.mem.r.get.valid) == 0) && (i < memAccLimit)) {
      step(1)
      i += 1
    }
    if (cmp) expect(c.io.mem.r.get.data, exp)
    peek(c.io.mem.r.get.data)
  }

  def issueWrCmd(addr: Int): Unit = {
    poke(c.io.mem.cmd, MemCmd.wr)
    poke(c.io.mem.valid, true)
  }

  def issueW(data: Int, strb: Int): Unit = {
    poke(c.io.mem.w.get.valid, true)
    poke(c.io.mem.w.get.data, data)
    poke(c.io.mem.w.get.strb, strb)
  }

  /**
    * Uart register write
    * @param addr destination address
    * @param data data to write
    */
  def writeReg(addr: Int, data: Int): Unit = {
    println(f"write(0x$addr%02x) : 0x$data%08x")
    issueWrCmd(addr)
    issueW(data, 0xf)
    while (((peek(c.io.mem.ready) == 0) || (peek(c.io.mem.w.get.ready) == 0)) && (i < memAccLimit)) {
      step(1)
      i += 1
    }
    step(1)
    poke(c.io.mem.valid, false)
    poke(c.io.mem.w.get.valid, false)
    step(1)
  }

  def readReg(addr: Int, exp: BigInt, cmp: Boolean = true): BigInt = {
    issueRdCmd(addr)
    step(1)
    poke(c.io.mem.r.get.valid, false)
    val data = waitR(exp, cmp)
    println(f"read (0x$addr%02x) : 0x$data%08x")
    data
  }

  /**
    * Uart send
    * @param data send data. And this value will be expect value.
    */
  def send(data: Int): Unit = {

    // send start bit
    poke(c.io.uart.rx, false)
    for (_ <- Range(0, duration)) {
      step(1)
    }

    // send bit data
    for (idx <- Range(0, 8)) {
      val rxBit = (data >> idx) & 0x1
      poke(c.io.uart.rx, rxBit)
      for (_ <- Range(0, duration)) {
        step(1)
      }
    }

    // send stop bits : TODO : change action to read stat and rxFifo
  }

  /**
    * Uart data receive
    * @param exp expect value
    */
  def receive(exp: Int): Unit = {

    // detect start
    while (peek(c.io.uart.tx) == 0x1) {
      step(1)
    }

    // shift half period
    for (_ <- Range(0, duration / 2)) {
      step(1)
    }

    expect(c.io.uart.tx, false, "detect bit must be low")

    for (idx <- Range(0, 8)) {
      val expTxBit = (exp >> idx) & 0x1
      for (_ <- Range(0, duration)) {
        step(1)
      }
      expect(c.io.uart.tx, expTxBit, s"don't match exp value bit($idx) : exp = $expTxBit")
    }

    // stop bits
    for (_ <- Range(0, duration)) {
      step(1)
    }

    // check stop bit value
    expect(c.io.uart.tx, true, s"stop bit must be high")
  }
}

class UartTester extends BaseTester {

  import RegInfo._

  val dutName = "uart-Top"

  behavior of dutName

  var baudrate0: Int = 500000
  var clockFreq0: Int = 1

  it should "send with uart tx when host writes TxFiFo register. [uart-tx-000]" in {
    val outDir = dutName + "_uart-tx-000"
    val args = getArgs(Map(
      "--top-name" -> dutName,
      "--target-dir" -> s"test_run_dir/$outDir"
    ))

    Driver.execute(args, () => new Top(baudrate0, clockFreq0)) {
      c => new UartUnitTester(c, baudrate0, clockFreq0) {

        val txData = Range(0, 10000).map(_ => floor(random * 256).toInt)

        for (data <- txData) {
          while ((readReg(stat, 0x4, cmp=false) & 0x4) == 0x0) {
            step(1)
          }
          readReg(stat, 0x4) // TxFifoEmpty
          writeReg(txFifo, data)
          receive(data)
        }
      }
    } should be (true)
  }

  it should "negate TxEmpty bit and assert TxFifoFull bit in Stat register when uart.Top send data. [uart-tx-001]" in {
    val outDir = dutName + "_uart-tx-001"
    val args = getArgs(Map(
      "--top-name" -> dutName,
      "--target-dir" -> s"test_run_dir/$outDir"
    ))

    Driver.execute(args, () => new Top(baudrate0, clockFreq0)) {
      c => new UartUnitTester(c, baudrate0, clockFreq0) {

        val txData = Range(0, 10000).map(_ => floor(random * 256).toInt)

        for (data <- txData) {
          while ((readReg(stat, 0x4, cmp=false) & 0x4) == 0x0) {
            step(1)
          }
          readReg(stat, 0x4) // TxFifoEmpty
          writeReg(txFifo, data)
          readReg(stat, 0x8) // TxFifoFull
        }
      }
    } should be (true)
  }

  var baudrate1: Int = 9600
  var clockFreq1: Int = 100

  it should "send with uart tx when host writes TxFiFo register. [uart-tx-100]" in {
    val outDir = dutName + "_uart-tx-100"
    val args = getArgs(Map(
      "--top-name" -> dutName,
      "--target-dir" -> s"test_run_dir/$outDir"
    ))

    Driver.execute(args, () => new Top(baudrate1, clockFreq1)) {
      c => new UartUnitTester(c, baudrate1, clockFreq1) {

        val txData = Range(0, 10).map(_ => floor(random * 256).toInt)

        for (data <- txData) {
          while ((readReg(stat, 0x4, cmp=false) & 0x4) == 0x0) {
            step(1)
          }
          readReg(stat, 0x4) // TxFifoEmpty
          writeReg(txFifo, data)
          receive(data)
        }
      }
    } should be (true)
  }

  it should "negate TxEmpty bit and assert TxFifoFull bit in Stat register when uart.Top send data. [uart-tx-101]" in {
    val outDir = dutName + "_uart-tx-101"
    val args = getArgs(Map(
      "--top-name" -> dutName,
      "--target-dir" -> s"test_run_dir/$outDir"
    ))

    Driver.execute(args, () => new Top(baudrate1, clockFreq1)) {
      c => new UartUnitTester(c, baudrate1, clockFreq1) {

        val txData = Range(0, 10).map(_ => floor(random * 256).toInt)

        for (data <- txData) {
          while ((readReg(stat, 0x4, cmp=false) & 0x4) == 0x0) {
            step(1)
          }
          readReg(stat, 0x4) // TxFifoEmpty
          writeReg(txFifo, data)
          readReg(stat, 0x8) // TxFifoFull
        }
      }
    } should be (true)
  }
}
