// See LICENSE for license details.

package dirv.pipeline

import chisel3._
import chisel3.util.Cat
import dirv.Config

class Alu(implicit cfg: Config) extends Module {
  val io = IO(new Bundle {
    val inst = Flipped(new Inst())
    val rs1 = Input(UInt(cfg.dataBits.W))
    val rs2 = Input(UInt(cfg.dataBits.W))
    val aluOut = Output(UInt(cfg.dataBits.W))
  })

  def regInst(): UInt = "b011".U
  def immInst(): UInt = "b001".U

  val subOpCode = io.inst.opCode(6, 4)
  val isImm = io.inst.funct7(5)
  val imm = Cat(io.inst.funct7, io.inst.rs2)
  val addOrSub = (regInst === subOpCode) && isImm

  val op1 = io.rs1
  val op2 = Mux(immInst === subOpCode, imm, io.rs2)

  io.aluOut := op1 + op2
}
