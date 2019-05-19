// See LICENSE for license details.

import chisel3._

import dirv.{Config, Dirv}


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

  // gp register keeps riscv-tests number and it starts from 2.
  when (io.xregs(3) === 2.U) {
    finCondition := true.B
  }

  // if tests fail, instruction "slli gp, gp, 0x1" would execute.
  when (finCondition && (dut.io.dbg.get.inst === failInst.U)) {
    fail := true.B
  }

  // connect top I/O
  io.fin := (!fail) && (io.xregs(3) === 1.U)
  io.pc := dut.io.dbg.get.pc
  io.xregs := dut.io.dbg.get.xregs
}
