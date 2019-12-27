// See LICENSE for license details.

import chisel3._
import dirv.Config
import peri.uart.UartIO
import test.util._


/**
  *
  * @param limit Maximum cycles of simulation.
  * @param abortEn True if simulation will finish, when timeout is occurred.
  * @param prgHexFile
  * @param cfg
  */
class SimDTMSysUart(
  limit: Int,
  abortEn: Boolean = true
)(prgHexFile: String)
(baudrate: Int, clockFreq: Int)
(implicit cfg: Config) extends BaseSimDTM(limit, abortEn) {
  val io = IO(new Bundle with BaseSimDTMIO {
    val dut = new Bundle {
      val fin = Output(Bool())
      val uart = new UartIO()
      val pc = Output(UInt(cfg.arch.xlen.W))
      val zero = Output(UInt(cfg.arch.xlen.W))
      val ra = Output(UInt(cfg.arch.xlen.W))
      val sp = Output(UInt(cfg.arch.xlen.W))
      val gp = Output(UInt(cfg.arch.xlen.W))
      val tp = Output(UInt(cfg.arch.xlen.W))
      val t0 = Output(UInt(cfg.arch.xlen.W))
      val t1 = Output(UInt(cfg.arch.xlen.W))
      val t2 = Output(UInt(cfg.arch.xlen.W))
      val s0 = Output(UInt(cfg.arch.xlen.W))
      val s1 = Output(UInt(cfg.arch.xlen.W))
      val a0 = Output(UInt(cfg.arch.xlen.W))
      val a1 = Output(UInt(cfg.arch.xlen.W))
      val a2 = Output(UInt(cfg.arch.xlen.W))
      val a3 = Output(UInt(cfg.arch.xlen.W))
      val a4 = Output(UInt(cfg.arch.xlen.W))
      val a5 = Output(UInt(cfg.arch.xlen.W))
      val a6 = Output(UInt(cfg.arch.xlen.W))
      val a7 = Output(UInt(cfg.arch.xlen.W))
      val s2 = Output(UInt(cfg.arch.xlen.W))
      val s3 = Output(UInt(cfg.arch.xlen.W))
      val s4 = Output(UInt(cfg.arch.xlen.W))
      val s5 = Output(UInt(cfg.arch.xlen.W))
      val s6 = Output(UInt(cfg.arch.xlen.W))
      val s7 = Output(UInt(cfg.arch.xlen.W))
      val s8 = Output(UInt(cfg.arch.xlen.W))
      val s9 = Output(UInt(cfg.arch.xlen.W))
      val s10 = Output(UInt(cfg.arch.xlen.W))
      val s11 = Output(UInt(cfg.arch.xlen.W))
      val t3 = Output(UInt(cfg.arch.xlen.W))
      val t4 = Output(UInt(cfg.arch.xlen.W))
      val t5 = Output(UInt(cfg.arch.xlen.W))
      val t6 = Output(UInt(cfg.arch.xlen.W))
    }
  })

  val dut = Module(new SysUart(prgHexFile)(baudrate, clockFreq))

  io.dut <> dut.io

  connect(false.B)
}
