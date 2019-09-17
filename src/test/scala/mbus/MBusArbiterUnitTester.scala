// See LICENSE for license details.

package mbus

import chisel3.iotesters._
import test.util.BaseTester

import scala.util.Random

/**
  * Unit test class for MbusArbiter
  * @param c Instance of SimDTMMbusArbiter
  */
class MbusArbiterUnitTester(c: SimDTMMbusArbiter) extends PeekPokeTester(c) {

  val r = new Random(1)
  val in = c.io.dut.in
  val out = c.io.dut.out

  def idle(cycle: Int = 1): Unit = {
    for (idx <- c.p.addrMap.indices) {
      poke(in(idx).c.valid, false)
      poke(in(idx).w.get.valid, false)
      poke(in(idx).r.get.ready, true)
    }

    poke(c.io.dut.out.w.get.ready, true)
    step(cycle)
  }

  /**
    * MemIO write request
    * @param addr Address to write
    */
  def write_req(port: Int, addr: BigInt): Unit = {
    poke(in(port).c.valid, true)
    poke(in(port).c.bits.addr, addr)
    poke(in(port).c.bits.cmd, MbusCmd.wr)
    poke(in(port).c.bits.size, MbusSize.word)
  }

  /**
    * MemIO send write data
    * @param strb Valid byte lane
    * @param data Data to write
    */
  def write_data(port: Int, strb: Int,  data: BigInt): Unit = {
    poke(in(port).w.get.valid, true)
    poke(in(port).w.get.bits.data, data)
    poke(in(port).w.get.bits.strb, strb)
  }

  /**
    * MemIO single write
    * @param addr Address to write
    * @param strb Valid byte lane
    * @param data register address
    */
  def single_write(port: Int, addr: BigInt, strb: Int,  data: BigInt, wrDataLatency: Int = 0): Unit = {
    write_req(port, addr)
    if (wrDataLatency == 0) {
      write_data(port, strb, data)
    }
    step(1)
    poke(in(port).c.valid, false)

    if (wrDataLatency != 0) {
      step(wrDataLatency - 1)
      write_data(port, strb, data)
      step(1)
    }
    poke(in(port).w.get.valid, false)
    step(1)
  }

  /**
    * MemIO read request
    * @param addr Address to read
    */
  def read_req(addr: Int): Unit = {
  }

  /**
    * Sram return read data
    * @param data Data to read
    */
  def return_read_data(data: Int): Unit = {
  }

  /**
    * MemIO single read
    * @param addr Address to write
    * @param exp expect value for read register
    */
  def single_read(addr: Int, exp: Int, rdDataLatency: Int = 0): Unit = {
  }

  /**
    * MemIO single read ready
    * @param addr Address to write
    * @param exp expect value for read register
    */
  def single_read_ready(addr: Int, exp: Int, readyLatency: Int = 0): Unit = {
  }
}

/**
  * Test class for MbusArbiter
  */
class MbusArbiterTester extends BaseTester {

  val dutName = "MbusArbiter"

  behavior of dutName

  val timeoutCycle = 1000
  val base_p = MbusArbiterParams(
    MbusRW,
    Seq(
      (0x0,   0x100),
      (0x100, 0x100),
      (0x1000, 0x100)
    ), 32)

  it should "single write. [wr:000]" in {

    val outDir = dutName + "-000"
    val args = getArgs(Map(
      "--top-name" -> dutName,
      "--target-dir" -> s"test_run_dir/$outDir"
    ))

    Driver.execute(args, () => new SimDTMMbusArbiter(base_p)(timeoutCycle)) {
      c => new MbusArbiterUnitTester(c) {
        idle(10)

        for (delay <- 0 until 10) {
          for (idx <- 0 until base_p.numOfMasters) {
            val addr = intToUnsignedBigInt(r.nextInt())
            val data = intToUnsignedBigInt(r.nextInt())
            single_write(idx, addr, 0xf, data, delay)
          }
        }
        idle(10)

      }
    } should be (true)
  }
}
