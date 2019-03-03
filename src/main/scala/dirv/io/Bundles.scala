// See LICENSE for license details.

package dirv.io

import chisel3._
import dirv.{defaultConfig, Config}

sealed abstract trait MemIOType
case object MemRIO extends MemIOType
case object MemWIO extends MemIOType
case object MemRWIO extends MemIOType


object MemCmd {
  val len = 1
  val wr = 0x0.U
  val rd = 0x1.U
}

object MemResp {
  val len = 1
  val ok = 0x0.U
  val err = 0x1.U
}

class MemBaseIO(addrBits: Int) extends Bundle {
  val addr = Output(UInt(addrBits.W))
  val cmd = Output(UInt(MemCmd.len.W))
  val resp = Input(UInt(MemResp.len.W))
  val req = Output(Bool())

  override def cloneType: MemBaseIO.this.type =
    new MemBaseIO(addrBits).asInstanceOf[this.type]
}

class MemR(dataBits: Int) extends Bundle {
  val data = Input(UInt(dataBits.W))
  val rddv = Input(Bool())

  override def cloneType: MemR.this.type =
    new MemR(dataBits).asInstanceOf[this.type]
}

class MemW(dataBits: Int) extends Bundle {
  val strb = Output(UInt((dataBits / 8).W))
  val data = Output(UInt(dataBits.W))
  val ack = Input(Bool())

  override def cloneType: MemW.this.type =
    new MemW(dataBits).asInstanceOf[this.type]
}

class MemIO(ioType: MemIOType, addrBits: Int, dataBits: Int) extends MemBaseIO(addrBits) {
  val w = ioType match {
    case MemRIO => None
    case MemWIO => Some(new MemW(dataBits))
    case MemRWIO => Some(new MemW(dataBits))
  }
  val r = ioType match {
    case MemRIO => Some(new MemR(dataBits))
    case MemWIO => None
    case MemRWIO => Some(new MemR(dataBits))
  }

  override def cloneType: MemIO.this.type =
    new MemIO(ioType, addrBits, dataBits).asInstanceOf[this.type]
}

object MemIO {
  def apply(ioType: MemIOType, addrBits: Int, dataBits: Int): MemIO = new MemIO(ioType, addrBits, dataBits)
  def apply(ioType: MemIOType): MemIO = apply(ioType, defaultConfig.addrBits, defaultConfig.dataBits)
}


class DbgIO(implicit  cfg: Config) extends Bundle {
  val pc = Output(UInt(cfg.addrBits.W))
  val fin = Output(Bool())
}

object DbgIO {
  def apply(implicit cfg: dirv.Config): DbgIO = new DbgIO()
}

class DirvIO(implicit cfg: dirv.Config) extends Bundle {
  val imem = MemIO(cfg.imemIOType, cfg.addrBits, cfg.dataBits)
  val dmem = MemIO(cfg.dmemIOType, cfg.addrBits, cfg.dataBits)
  val dbg = if (cfg.dbg) Some(new DbgIO) else None

  override def cloneType: DirvIO.this.type =
    new DirvIO().asInstanceOf[this.type]
}
