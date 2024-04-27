// See LICENSE for license details.

package mbus

import scala.util.Random
import chiseltest._
import chiseltest.iotesters.PeekPokeTester
import test.util.{IntToBigInt, BaseTester}

/**
  * Unit test class for MbusDecoder
  * @param c Instance of SimDTMMbusDecoder
  */
class MbusDecoderUnitTester(c: SimDTMMbusDecoder) extends PeekPokeTester(c) with IntToBigInt {

  val r = new Random(1)
  val in = c.io.dut_io.in
  val in_c = in.c
  val in_r = in.r.get
  val in_w = in.w.get

  val out = c.io.dut_io.out

  def idle(cycle: Int = 1): Unit = {
    poke(in_c.valid, false)
    poke(in_w.valid, false)
    poke(in_r.ready, true)
    for (i <- 0 until c.p.addrMap.length) {
      poke(out(i).c.ready, true)
      poke(out(i).w.get.ready, false)
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
  def write_data(strb: Int,  data: BigInt): Unit = {
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

  it should "be able to transfer write data, when Master issue write command. [wr:000]" in {

    val outDir = dutName + "-000"
    val args = getArgs(Map(
      "--top-name" -> dutName,
      "--target-dir" -> s"test_run_dir/$outDir"
    ))

    test(new SimDTMMbusDecoder(base_p)(timeoutCycle)).
      withAnnotations(Seq(VerilatorBackendAnnotation)).
      runPeekPoke(new MbusDecoderUnitTester(_) {

        for (((addr, _), dst_port) <- base_p.addrMap.zipWithIndex) {
          val wrData = r.nextInt()
          idle(10)
          write_req(addr)
          write_data(0xf, wrData)
          for (i <- base_p.addrMap.indices) {
            val ready = if (i == dst_port) true else false
            poke(out(i).w.get.ready, ready)
          }
          expect(dut.io.dut_io.in.w.get.ready, true)
          expect(out(dst_port).c.valid, true)
          expect(out(dst_port).w.get.valid, true)
          expect(out(dst_port).w.get.bits.data, wrData)
          expect(out(dst_port).w.get.bits.strb, 0xf)
          step(1)
          idle(10)
        }
      })
  }

  it should "be able to transfer write data, when write data is delayed. [wr:001]" in {

    val outDir = dutName + "-001"
    val args = getArgs(Map(
      "--top-name" -> dutName,
      "--target-dir" -> s"test_run_dir/$outDir"
    ))

    test(new SimDTMMbusDecoder(base_p)(timeoutCycle)).
      withAnnotations(Seq(VerilatorBackendAnnotation)).
      runPeekPoke(new MbusDecoderUnitTester(_) {

        for (delay <- 1 until 10) {
          for (((addr, _), dst_port) <- base_p.addrMap.zipWithIndex) {
            val wrData = intToUnsignedBigInt(r.nextInt())
            idle(10)
            write_req(addr)
            expect(out(dst_port).c.valid, true)
            expect(out(dst_port).w.get.valid, false)
            step(1)
            poke(in.c.valid, false)
            step(delay - 1)
            write_data(0xf, wrData)
            expect(out(dst_port).w.get.valid, true)
            expect(out(dst_port).w.get.bits.data, wrData)
            expect(out(dst_port).w.get.bits.strb, 0xf)
            step(1)
            idle(10)
          }
        }
      })
  }

  it should "be able to choose right output port by cmd.bits.addr, " +
    "when Master issue write command. [wr:002]" in {

    val outDir = dutName + "-002"
    val args = getArgs(Map(
      "--top-name" -> dutName,
      "--target-dir" -> s"test_run_dir/$outDir"
    ))

    test(new SimDTMMbusDecoder(base_p)(timeoutCycle)).
      withAnnotations(Seq(VerilatorBackendAnnotation)).
      runPeekPoke(new MbusDecoderUnitTester(_) {

        for (((addr, _), dst_port) <- base_p.addrMap.zipWithIndex) {
          idle(10)
          write_req(addr)
          for (chk_port <- 0 until base_p.addrMap.length) {
            if (dst_port == chk_port) {
              expect(out(chk_port).c.valid, true)
            } else {
              expect(out(chk_port).c.valid, false)
            }
            expect(out(chk_port).c.bits.addr, addr)
            expect(out(chk_port).c.bits.cmd, MbusCmd.wr)
          }
          step(1)
          idle(10)
        }
      })
  }

  it should "be able to transfer write data, when read data is delayed. [rd:100]" in {

    val outDir = dutName + "-100"
    val args = getArgs(Map(
      "--top-name" -> dutName,
      "--target-dir" -> s"test_run_dir/$outDir"
    ))

    test(new SimDTMMbusDecoder(base_p)(timeoutCycle)).
      withAnnotations(Seq(VerilatorBackendAnnotation)).
      runPeekPoke(new MbusDecoderUnitTester(_) {
        for (((addr, _), dst_port) <- base_p.addrMap.zipWithIndex) {
          val rdData = intToUnsignedBigInt(r.nextInt())

          idle(10)
          read_req(addr)
          return_read_data(dst_port, rdData)
          expect(in.r.get.valid, true)
          expect(in.r.get.bits.data, rdData)

          // When command is read, out(N).w.get.valid is never asserted.
          for (out_port <- 0 until base_p.addrMap.length) {
            expect(out(out_port).w.get.valid, false)
          }
          step(1)
          idle(10)
        }
      })
  }

  it should "be able to choose right output port by cmd.bits.addr, when Master issue read command. [rd:101]" in {

    val outDir = dutName + "-101"
    val args = getArgs(Map(
      "--top-name" -> dutName,
      "--target-dir" -> s"test_run_dir/$outDir"
    ))

    test(new SimDTMMbusDecoder(base_p)(timeoutCycle)).
      withAnnotations(Seq(VerilatorBackendAnnotation)).
      runPeekPoke(new MbusDecoderUnitTester(_) {

        for (delay <- 1 until 10) {
          for (((addr, _), dst_port) <- base_p.addrMap.zipWithIndex) {
            val rdData = intToUnsignedBigInt(r.nextInt())

            idle(10)
            read_req(addr)
            expect(in.r.get.valid, false)
            step(1)
            poke(in.c.valid, false)
            step(delay)
            return_read_data(dst_port, rdData)
            expect(in.r.get.valid, true)
            expect(in.r.get.bits.data, rdData)

            // When command is read, out(N).w.get.valid is never asserted.
            for (out_port <- 0 until base_p.addrMap.length) {
              expect(out(out_port).w.get.valid, false)
            }
            step(1)
            idle(10)
          }
        }
      })
  }

  it should "be able to choose right output port by cmd.bits.addr, " +
    "when Master issue read command. [rd:102]" in {

    val outDir = dutName + "-102"
    val args = getArgs(Map(
      "--top-name" -> dutName,
      "--target-dir" -> s"test_run_dir/$outDir"
    ))

    test(new SimDTMMbusDecoder(base_p)(timeoutCycle)).
      withAnnotations(Seq(VerilatorBackendAnnotation)).
      runPeekPoke(new MbusDecoderUnitTester(_) {
        for (((addr, _), dst_port) <- base_p.addrMap.zipWithIndex) {
          idle(10)
          read_req(addr)
          for (chk_port <- 0 until base_p.addrMap.length) {
            if (dst_port == chk_port) {
              expect(out(chk_port).c.valid, true)
            } else {
              expect(out(chk_port).c.valid, false)
            }
            expect(out(chk_port).c.bits.addr, addr)
            expect(out(chk_port).c.bits.cmd, MbusCmd.rd)
          }
          step(1)
          idle(10)
        }
      })
  }
}
