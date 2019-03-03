// See LICENSE for license details.

package dirv.pipeline

import chisel3._
import dirv.Config

class MpfrIO(addrBits: Int, dataBits:Int) extends Bundle {
  val addr = Input(UInt(addrBits.W))
  val data = Output(UInt(dataBits.W))
}

class Mpfr(implicit cfg: Config) extends Module with InstInfo {
  val io = IO(new Bundle {
    val rs1 = new MpfrIO(rs1Bits, cfg.dataBits)
    val rs2 = new MpfrIO(rs2Bits, cfg.dataBits)
    val wb = new Bundle {
      val en = Input(Bool())
      val addr = Input(UInt(rs1Bits.W))
      val data = Input(UInt(cfg.dataBits.W))
    }
    val fin = if (cfg.dbg) Some(Output(Bool())) else None
  })

  val xRegs = RegInit(VecInit(Seq.fill(cfg.arch.regNum-1)(0.U(cfg.addrBits.W))))

  io.rs1.data := Mux(io.rs1.addr === 0.U, 0.U, xRegs(io.rs1.addr-1.U))
  io.rs2.data := Mux(io.rs2.addr === 0.U, 0.U, xRegs(io.rs2.addr-1.U))

  when (io.wb.en) {
    xRegs(io.wb.addr-1.U) := io.wb.data
  }

  if (cfg.dbg) {
    io.fin.get := (xRegs(3) === 0x1.U)
  }
}
