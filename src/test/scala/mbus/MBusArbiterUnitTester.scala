// See LICENSE for license details.

package mbus

import chisel3.iotesters._
import test.util.BaseTester

/**
  * Unit test class for MbusArbiter
  * @param c Instance of SimDTMMbusArbiter
  */
class MbusArbiterUnitTester(c: SimDTMMbusArbiter) extends PeekPokeTester(c) {

  val in = c.io.dut.in
  val out = c.io.dut.out

  def idle(cycle: Int = 1): Unit = {
    step(cycle)
  }

  /**
    * MemIO write request
    * @param addr Address to write
    */
  def write_req(addr: Int): Unit = {
  }

  /**
    * MemIO send write data
    * @param strb Valid byte lane
    * @param data Data to write
    */
  def write_data(strb: Int,  data: Int): Unit = {
  }

  /**
    * MemIO single write
    * @param addr Address to write
    * @param strb Valid byte lane
    * @param data register address
    */
  def single_write(addr: Int, strb: Int,  data: Int, wrDataLatency: Int = 0): Unit = {
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

  it should "write" in {

    val outDir = dutName + "-000"
    val args = getArgs(Map(
      "--top-name" -> dutName,
      "--target-dir" -> s"test_run_dir/$outDir"
    ))

    Driver.execute(args, () => new SimDTMMbusArbiter(base_p)(timeoutCycle)) {
      c => new MbusArbiterUnitTester(c) {
        idle(10)

        for (idx <- 0 until base_p.numOfMasters) {
          poke(c.io.dut.in(idx).c.valid, true)
          poke(c.io.dut.in(idx).c.bits.addr, idx)
          poke(c.io.dut.in(idx).c.bits.cmd, MbusCmd.wr)
          poke(c.io.dut.in(idx).c.bits.size, MbusSize.word)
          poke(c.io.dut.in(idx).c.valid, true)
          poke(c.io.dut.in(idx).w.get.bits.data, idx)
          poke(c.io.dut.in(idx).w.get.bits.strb, 0xf)
        }
        step(1)
        for (idx <- 0 until base_p.numOfMasters) {
          poke(c.io.dut.in(idx).c.valid, false)
          poke(c.io.dut.in(idx).c.valid, false)
        }
        step(1)
        idle(10)

      }
    } should be (true)
  }

  it should "read and data will return same cycle" in {

    val outDir = dutName + "-100"
    val args = getArgs(Map(
      "--top-name" -> dutName,
      "--target-dir" -> s"test_run_dir/$outDir"
    ))

    Driver.execute(args, () => new SimDTMMbusArbiter(base_p)(timeoutCycle)) {
      c => new MbusArbiterUnitTester(c) {
        idle(10)

        for (idx <- 0 until base_p.numOfMasters) {
          poke(c.io.dut.in(idx).c.valid, true)
          poke(c.io.dut.in(idx).c.bits.addr, idx)
          poke(c.io.dut.in(idx).c.bits.cmd, MbusCmd.rd)
          poke(c.io.dut.in(idx).c.bits.size, MbusSize.word)
          poke(c.io.dut.in(idx).c.valid, true)
          poke(c.io.dut.in(idx).r.get.ready, true)
        }
        poke(c.io.dut.out.r.get.valid, true)
        poke(c.io.dut.out.r.get.bits.data, 0x12345678)
        step(1)
        poke(c.io.dut.out.r.get.valid, false)
        step(1)
        idle(10)

      }
    } should be (true)
  }

  it should "read and data will return next cycle." in {

    val outDir = dutName + "-101"
    val args = getArgs(Map(
      "--top-name" -> dutName,
      "--target-dir" -> s"test_run_dir/$outDir"
    ))

    Driver.execute(args, () => new SimDTMMbusArbiter(base_p)(timeoutCycle)) {
      c => new MbusArbiterUnitTester(c) {
        idle(10)

        for (idx <- 0 until base_p.numOfMasters) {
          poke(c.io.dut.in(idx).c.valid, true)
          poke(c.io.dut.in(idx).c.bits.addr, idx)
          poke(c.io.dut.in(idx).c.bits.cmd, MbusCmd.rd)
          poke(c.io.dut.in(idx).c.bits.size, MbusSize.word)
          poke(c.io.dut.in(idx).c.valid, true)
          poke(c.io.dut.in(idx).r.get.ready, true)
        }
        poke(c.io.dut.out.c.ready, true)
        step(1)
        poke(c.io.dut.out.r.get.valid, true)
        poke(c.io.dut.out.r.get.bits.data, 0x12345678)
        for (idx <- 0 until base_p.numOfMasters) {
          poke(c.io.dut.in(idx).c.valid, false)
          poke(c.io.dut.in(idx).c.valid, false)
        }
        step(1)
        poke(c.io.dut.out.r.get.valid, false)
        idle(10)

      }
    } should be (true)
  }
}
