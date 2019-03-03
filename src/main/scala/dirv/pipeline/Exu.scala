// See LICENSE for license details.

package dirv.pipeline

import chisel3._
import dirv.Config
import dirv.io.{MemCmd, MemIO}

class Exu(implicit cfg: Config) extends Module{
  val io = IO(new Bundle {
    val inst2ext = Flipped(new Inst())
    val exu2ext = MemIO(cfg.dmemIOType, cfg.addrBits, cfg.dataBits)
    val fin = if (cfg.dbg) Some(Output(Bool())) else None
  })

  // Module Instance
  val alu = Module(new Alu)
  val mpfr = Module(new Mpfr)
  val csrf = Module(new Csrf)
  val lsu = Module(new Lsu)

  // dummy IO connection


  // connect Mpfr
  mpfr.io.rs1.addr := io.inst2ext.rs1
  mpfr.io.rs2.addr := io.inst2ext.rs2

  // data to Alu
  val aluRs1Data = Wire(UInt(cfg.dataBits.W))
  val aluRs2Data = Wire(UInt(cfg.dataBits.W))

  aluRs1Data := mpfr.io.rs1.data
  aluRs2Data := mpfr.io.rs2.data

  io.inst2ext <> alu.io.inst
  alu.io.rs1 := aluRs1Data
  alu.io.rs2 := aluRs2Data

  // mem
  io.exu2ext.req := true.B
  io.exu2ext.cmd := MemCmd.rd
  io.exu2ext.addr := alu.io.aluOut
  io.exu2ext.w.get.data := 0xdeafbeafL.U
  io.exu2ext.w.get.strb := 0xf.U


  // wb
  mpfr.io.wb.en := true.B
  mpfr.io.wb.addr := io.inst2ext.rd
  mpfr.io.wb.data := alu.io.aluOut

  // debug
  if (cfg.dbg) {
    io.fin.get := mpfr.io.fin.get
  }
}
