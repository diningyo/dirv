// See LICENSE for license details.

package mbus

import chisel3.iotesters._
import test.util.BaseTester

/**
  * Unit test class for MbusDecoder
  * @param c Instance of SimDTMMbusDecoder
  */
class MbusDecoderUnitTester(c: SimDTMMbusDecoder) extends PeekPokeTester(c) {

  val in = c.io.dut.in
  val in_c = in.c
  val in_r = in.r.get
  val in_w = in.w.get

  val out = c.io.dut.out

  def idle(cycle: Int = 1): Unit = {
    poke(in_c.valid, false)
    poke(in_w.valid, false)
    poke(in_r.ready, true)
    for (i <- 0 until c.p.addrMap.length) {
      poke(out(i).c.ready, true)
      poke(out(i).w.get.ready, true)
      poke(out(i).r.get.valid, false)
    }
    step(cycle)
  }

  /**
    * MemIO write request
    * @param addr Address to write
    */
  def write_req(addr: Int): Unit = {
    poke(in_c.valid, true)
    poke(in_c.bits.addr, addr)
    poke(in_c.bits.cmd, MbusCmd.wr)
  }

  /**
    * MemIO send write data
    * @param strb Valid byte lane
    * @param data Data to write
    */
  def write_data(strb: Int,  data: Int): Unit = {
    poke(in_w.valid, true)
    poke(in_w.bits.strb, strb)
    poke(in_w.bits.data, data)
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
    poke(in_c.valid, true)
    poke(in_c.bits.addr, addr)
    poke(in_c.bits.cmd, MbusCmd.rd)
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
  * Test class for MbusDecoder
  */
class MbusDecoderTester extends BaseTester {

  val dutName = "MbusDecoder"

  behavior of dutName

  val timeoutCycle = 1000
  val base_p = MbusDecoderParams(
    MbusRW,
    Seq(
      (0x0,   0x100),
      (0x100, 0x100),
      (0x1000, 0x100)
    ), 32)

  it should "be able to choose right output port by cmd.bits.addr, when Master issue write command." in {

    val outDir = dutName + "-000"
    val args = getArgs(Map(
      "--top-name" -> dutName,
      "--target-dir" -> s"test_run_dir/$outDir"
    ))

    Driver.execute(args, () => new SimDTMMbusDecoder(base_p)(timeoutCycle)) {
      c => new MbusDecoderUnitTester(c) {

        for (((addr, _), idx) <- base_p.addrMap.zipWithIndex) {
          idle(10)
          write_req(addr)
          step(1)
          idle(10)
        }
      }
    } should be (true)
  }

  it should "be able to choose right output port by cmd.bits.addr, when Master issue read command." in {

    val outDir = dutName + "-100"
    val args = getArgs(Map(
      "--top-name" -> dutName,
      "--target-dir" -> s"test_run_dir/$outDir"
    ))

    Driver.execute(args, () => new SimDTMMbusDecoder(base_p)(timeoutCycle)) {
      c => new MbusDecoderUnitTester(c) {
        for ((addr, _) <- base_p.addrMap) {
          idle(10)
          read_req(addr)
          step(1)
          idle(10)
        }
      }
    } should be (true)
  }
}
