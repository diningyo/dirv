// See LICENSE for license details.

import chisel3._
import dirv.{Config, Dirv}
import peri.mem.MemModel


/**
  * Simulation environment top module
  * @param prgHexFile riscv-tests hex file path
  * @param cfg Dirv's configuration instance
  */
class SimDtm(prgHexFile: String)(implicit cfg: Config) extends Module {
  val io = IO(new Bundle {
    val fin = Output(Bool())
    val pc = Output(UInt(cfg.arch.xlen.W))
    val xregs = Output(Vec(cfg.arch.regNum, UInt(cfg.arch.xlen.W)))
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
  })

  // module instances
  val mem = Module(new MemModel(prgHexFile))
  val dut = Module(new Dirv)

  // connect mem and dut
  mem.io.clk := clock
  mem.io.rst := reset
  mem.io.imem <> dut.io.imem
  mem.io.dmem <> dut.io.dmem

  //
  // check riscv-tests finish condition
  //
  val finCondition = RegInit(false.B)
  val failInst = 0x00119193
  val fail = RegInit(false.B)
  val xregs = dut.io.dbg.get.xregs

  // gp register keeps riscv-tests number and it starts from 2.
  when (xregs(3) === 2.U) {
    finCondition := true.B
  }

  // if tests fail, instruction "slli gp, gp, 0x1" would execute.
  when (finCondition && (dut.io.dbg.get.inst === failInst.U)) {
    fail := true.B
  }

  // connect top I/O
  io.fin := (!fail) && (xregs(3) === 1.U)
  io.pc := dut.io.dbg.get.pc
  io.xregs := dut.io.dbg.get.xregs

  io.zero := xregs(0)
  io.ra := xregs(1)
  io.sp := xregs(2)
  io.gp := xregs(3)
  io.tp := xregs(4)
  io.t0 := xregs(5)
  io.t1 := xregs(6)
  io.t2 := xregs(7)
  io.s0 := xregs(8)
  io.s1 := xregs(9)
  io.a0 := xregs(10)
  io.a1 := xregs(11)
  io.a2 := xregs(12)
  io.a3 := xregs(13)
  io.a4 := xregs(14)
  io.a5 := xregs(15)
  io.a6 := xregs(16)
  io.a7 := xregs(17)
  io.s2 := xregs(18)
  io.s3 := xregs(19)
  io.s4 := xregs(20)
  io.s5 := xregs(21)
  io.s6 := xregs(22)
  io.s7 := xregs(23)
  io.s8 := xregs(24)
  io.s9 := xregs(25)
  io.s10 := xregs(26)
  io.s11 := xregs(27)
  io.t3 := xregs(28)
  io.t4 := xregs(29)
  io.t5 := xregs(30)
  io.t6 := xregs(31)
}
