// See LICENSE for license details.

package dirv.pipeline

import chisel3._
import chisel3.util.{BitPat, ListLookup}
import dirv.{Config, RV32E}
import dirv.io.{MemCmd, MemIO}

trait InstInfo {
  val opCodeLsb = 0
  val opCodeMsb = 6
  val opCodeBits = opCodeMsb - opCodeLsb + 1
  val rdLsb = 7
  val rdMsb = 11
  val rdBits = rdMsb - rdLsb + 1
  val funct3Lsb = 12
  val funct3Msb = 14
  val funct3Bits = funct3Msb - funct3Lsb + 1
  val rs1Lsb = 15
  val rs1Msb = 19
  val rs1Bits = rs1Msb - rs1Lsb + 1
  val rs2Lsb = 20
  val rs2Msb = 24
  val rs2Bits = rs2Msb - rs2Lsb + 1
  val funct7Lsb = 25
  val funct7Msb = 31
  val funct7Bits = funct7Msb - funct7Lsb + 1
}

class Inst extends Bundle with InstInfo {
  val opCode = Output(UInt(opCodeBits.W))
  val rd = Output(UInt(rdBits.W))
  val funct3 = Output(UInt(funct3Bits.W))
  val rs1 = Output(UInt(rs1Bits.W))
  val rs2 = Output(UInt(rs2Bits.W))
  val funct7 = Output(UInt(funct7Bits.W))
}

class Idu(implicit cfg: Config) extends Module with InstInfo {
  val io = IO(new Bundle {
    val ifu2idu = MemIO(cfg.imemIOType, cfg.addrBits, cfg.dataBits)
    val inst2ext = new Inst()
  })


  io.ifu2idu.cmd := MemCmd.rd
  io.ifu2idu.addr := 0x0.U
  io.ifu2idu.req := true.B

  val currCmd = Mux(io.ifu2idu.r.get.rddv, io.ifu2idu.r.get.data, 0.U)

  io.inst2ext.opCode := currCmd(opCodeMsb, opCodeLsb)
  io.inst2ext.rd := currCmd(rdMsb, rdLsb)
  io.inst2ext.funct3 := currCmd(funct3Msb, funct3Lsb)
  io.inst2ext.rs1 := currCmd(rs1Msb, rs1Lsb)
  io.inst2ext.rs2 := currCmd(rs2Msb, rs2Lsb)
  io.inst2ext.funct7 := currCmd(funct7Msb, funct7Lsb)

  /*
  if (cfg.arch == RV32E)
  val codeIsCExt = subOpCode =/= "b000".U
  */
}
