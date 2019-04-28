// See LICENSE for license details.

package dirv.pipeline

import chisel3._
import dirv.Config


/**
  * Register Source I/O
  * @param addrBits address width
  * @param dataBits data width
  */
class RsIO(addrBits: Int, dataBits:Int) extends Bundle {
  val addr = Input(UInt(addrBits.W))
  val data = Output(UInt(dataBits.W))

  override def cloneType: RsIO.this.type = new RsIO(addrBits, dataBits).asInstanceOf[this.type]
}

/**
  * File Register I/O
  * @param cfg
  */
class MpfrIO(implicit val cfg: Config) extends Bundle {
  val rs1 = new RsIO(cfg.arch.mpfrBits, cfg.arch.xlen)
  val rs2 = new RsIO(cfg.arch.mpfrBits, cfg.arch.xlen)
  val rd = new Bundle {
    val en = Input(Bool())
    val addr = Input(UInt(cfg.arch.mpfrBits.W))
    val data = Input(UInt(cfg.dataBits.W))
  }
  val xregs = if (cfg.dbg) Some(Output(Vec(cfg.arch.regNum, UInt(cfg.arch.xlen.W)))) else None

  override def cloneType: MpfrIO.this.type = new MpfrIO().asInstanceOf[this.type]
}

/**
  * File Register
  * @param cfg
  */
class Mpfr(implicit cfg: Config) extends Module with InstInfoRV32 {
  val io = IO(new MpfrIO())

  val xRegs = RegInit(VecInit(Seq.fill(cfg.arch.regNum)(0.U(cfg.arch.xlen.W))))

  io.rs1.data := xRegs(io.rs1.addr)
  io.rs2.data := xRegs(io.rs2.addr)

  when (io.rd.en && (io.rd.addr =/= 0.U)) {
    xRegs(io.rd.addr) := io.rd.data
  }

  if (cfg.dbg) {
    io.xregs.get := xRegs
  }
}
