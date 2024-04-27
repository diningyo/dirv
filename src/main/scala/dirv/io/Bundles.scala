// See LICENSE for license details.

package dirv.io

import chisel3._
import dirv._
import mbus._


/**
  * Debug I/O
  * @param cfg Dirv's configuration object
  */
class DbgIO(implicit cfg: Config) extends Bundle {
  val pc = Output(UInt(cfg.addrBits.W))
  val inst = Output(UInt(cfg.dataBits.W))
  val xregs = Output(Vec(cfg.arch.regNum, UInt(cfg.arch.xlen.W)))
}

/**
  * Companion object for DbgIO class
  */
object DbgIO {
  def apply(implicit cfg: dirv.Config): DbgIO = new DbgIO()
}

/**
  * Dirv top I/O class
  * @param cfg Dirv's configuration object
  */
class DirvIO(implicit cfg: dirv.Config) extends Bundle {
  val imem = MbusIO(cfg.imemIOType, cfg.addrBits, cfg.dataBits)
  val dmem = MbusIO(cfg.dmemIOType, cfg.addrBits, cfg.dataBits)
  val dbg = if (cfg.dbg) Some(new DbgIO) else None
}
