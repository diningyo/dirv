// See LICENSE for license details.

package dirv.pipeline

import chisel3._
import chisel3.util.{Cat, Mux1H, MuxCase}
import dirv.Config


/**
  * ALU I/O
  * @param cfg dirv's configuration parameter.
  */
class AluIO(implicit cfg: Config) extends Bundle {
  val inst = Input(new InstRV32())
  val pc = Input(UInt(cfg.dataBits.W))
  val rs1 = Input(UInt(cfg.dataBits.W))
  val rs2 = Input(UInt(cfg.dataBits.W))
  val result = Output(UInt(cfg.dataBits.W))
}


/**
  * ALU
  * @param cfg dirv's configuration parameter.
  */
class Alu(implicit cfg: Config) extends Module {
  val io = IO(new AluIO)

  val inst = io.inst
  val rs1 = MuxCase(io.rs1, Seq(
    inst.csrUimmValid -> inst.rs1,
    inst.auipc -> io.pc,
    (inst.jal || inst.jalr) -> (io.pc + 4.U)
  ))
  val rs2 = MuxCase(io.rs2, Seq(
    (inst.csrValid || inst.jal || inst.jalr) -> 0.U,
    inst.storeValid -> inst.immS,
    (inst.slli || inst.srli || inst.srai) -> inst.shamt,
    (inst.aluImm || inst.loadValid) -> inst.immI,
    inst.auipc -> inst.immU
  ))
  val aluThrough = inst.auipc || inst.loadValid || inst.storeValid || inst.jal || inst.jalr ||
    inst.csrrs || inst.csrrc || inst.csrrw || inst.csrrsi || inst.csrrci || inst.csrrwi
  val add = inst.addi || inst.add
  val shamt = Mux(inst.slli || inst.srli || inst.srai, inst.shamt, rs2(inst.shamt.getWidth - 1, 0))

  val rv32iAlu = scala.collection.mutable.Seq(
    (add || aluThrough) -> (rs1 + rs2),
    (inst.slti || inst.slt) -> (rs1.asSInt < rs2.asSInt).asUInt,
    (inst.sltiu || inst.sltu) -> (rs1 < rs2),
    inst.sub -> (rs1 - rs2),
    (inst.andi || inst.and) -> (rs1 & rs2),
    (inst.ori || inst.or) -> (rs1 | rs2),
    (inst.xori || inst.xor) -> (rs1 ^ rs2),
    (inst.slli || inst.sll) -> (rs1 << shamt)(cfg.arch.xlen - 1, 0),
    (inst.srli || inst.srl) -> (rs1 >> shamt),
    (inst.srai || inst.sra) -> (rs1.asSInt >> shamt).asUInt,
    inst.lui -> inst.immU
  )

  val alu = rv32iAlu

  val aluResult = Mux1H(alu)

  io.result := aluResult
}
