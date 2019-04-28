// See LICENSE for license details.

package dirv.pipeline

import chisel3._
import chisel3.util.{Cat, Mux1H, MuxCase}
import dirv.Config

/**
  * ALU
  * @param cfg dirv's configuration parameter.
  */
class Alu(implicit cfg: Config) extends Module {
  val io = IO(new Bundle {
    val inst = Input(new InstRV32())
    val pc = Input(UInt(cfg.dataBits.W))
    val rs1 = Input(UInt(cfg.dataBits.W))
    val rs2 = Input(UInt(cfg.dataBits.W))
    val result = Output(UInt(cfg.dataBits.W))
  })

  val inst = io.inst
  val rs1 = Mux(inst.auipc, io.pc, io.rs1)
  val rs2 = MuxCase(io.rs2, Seq(
    io.inst.aluImm -> inst.getAluImm,
    io.inst.auipc -> inst.immU
  ))
  val shamt = io.inst.shamt

  val rv32iAlu = Seq(
    (inst.addi || inst.add || inst.auipc) -> (rs1 + rs2),
    (inst.slti || inst.slt) -> (rs1.asSInt() < rs2.asSInt()).asUInt(),
    (inst.sltiu || inst.sltu) -> (rs1 < rs2),
    inst.sub -> (rs1 - rs2),
    (inst.andi || inst.and) -> (rs1 & rs2),
    (inst.ori || inst.or) -> (rs1 | rs2),
    (inst.xori || inst.xor) -> (rs1 ^ rs2),
    (inst.slli || inst.sll) -> (rs1 << shamt)(cfg.arch.xlen - 1, 0),
    (inst.srli || inst.srl) -> (rs1 >> shamt),
    (inst.srai || inst.sra) -> (rs1.asSInt() >> shamt).asUInt(),
    inst.lui -> inst.immU
  )

  val alu = rv32iAlu

  val aluResult = Mux1H(alu)

  io.result := aluResult
}
