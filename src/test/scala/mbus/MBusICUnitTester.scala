// See LICENSE for license details.

package mbus

import chiseltest._
import chiseltest.iotesters.PeekPokeTester
import chiseltest.VerilatorBackendAnnotation
import test.util.{IntToBigInt, BaseTester}

import scala.util.Random

/**
  * Unit test class for MbusIC
  * @param c Instance of SimDTMMbusIC
  */
class MbusICUnitTester(c: SimDTMMbusIC) extends PeekPokeTester(c) with IntToBigInt {

  val r = new Random(1)
  val in = c.io.dut_io.in


  val out = c.io.dut_io.out

  def idle(cycle: Int = 1): Unit = {
    for (i <- c.p.masterInfos.indices) {
      poke(in(i).c.valid, false)
      poke(in(i).w.get.valid, false)
      poke(in(i).r.get.ready, true)
    }
    for (i <- c.p.slaveInfos.indices) {
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
  def write_req(port: Int, addr: Int): Unit = {
    poke(in(port).c.valid, true)
    poke(in(port).c.bits.addr, addr)
    poke(in(port).c.bits.cmd, MbusCmd.wr)
  }

  /**
    * MemIO send write data
    * @param strb Valid byte lane
    * @param data Data to write
    */
  def write_data(port: Int, strb: Int,  data: BigInt): Unit = {
    poke(in(port).w.get.valid, true)
    poke(in(port).w.get.bits.strb, strb)
    poke(in(port).w.get.bits.data, data)
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
  def read_req(port: Int, addr: Int): Unit = {
    poke(in(port).c.valid, true)
    poke(in(port).c.bits.addr, addr)
    poke(in(port).c.bits.cmd, MbusCmd.rd)
  }

  /**
    * Sram return read data
    * @param data Data to read
    */
  def return_read_data(port: Int, data: BigInt): Unit = {
    poke(out(port).r.get.valid, true)
    poke(out(port).r.get.bits.data, data)
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
  * Test class for MbusIC
  */
class MbusICTester extends BaseTester {

  val dutName = "MbusIC"

  behavior of dutName

  val timeoutCycle = 1000
  val base_p = MbusICParams(
    MbusRW,
    Seq(
      (0x0,   0x100),
      (0x100, 0x100),
      (0x1000, 0x100),
      (0x2000, 0x100)
    ),
    Seq(
      (0x0,   0x100),
      (0x100, 0x100),
      (0x1000, 0x100)
    ), 32)

  it should "be able to transfer write data, when Master issue write command. [wr:000]" in {

    val outDir = dutName + "-000"
    val args = getArgs(Map(
      "--top-name" -> dutName,
      "--target-dir" -> s"test_run_dir/$outDir"
    ))

    test(new SimDTMMbusIC(base_p)(timeoutCycle)).
      withAnnotations(Seq(VerilatorBackendAnnotation)).
      runPeekPoke(new MbusICUnitTester(_) {
        for (i <- dut.p.masterInfos.indices) {
          write_req(i, 100)
          step(1)
          idle(1)
        }
      })
  }
}
