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
  val wr = 0x0
  val rd = 0x1
}

object MemResp {
  val len = 1
  val ok = 0x0
  val err = 0x1
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
  val data = Output(UInt(dataBits.W))
  val rddv = Input(Bool())

  override def cloneType: MemR.this.type =
    new MemR(dataBits).asInstanceOf[this.type]
}

class MemW(dataBits: Int) extends Bundle {
  val data = Output(UInt(dataBits.W))
  val ack = Input(Bool())

  override def cloneType: MemW.this.type =
    new MemW(dataBits).asInstanceOf[this.type]
}

class MemIO(ioType: MemIOType, addrBits: Int, dataBits: Int) extends MemBaseIO(addrBits) {
  val w = ioType match {
    case _: MemRIO.type => None
    case _: MemWIO.type => Some(new MemW(dataBits))
    case _: MemRWIO.type => Some(new MemW(dataBits))
  }
  val r = ioType match {
    case _: MemRIO.type => Some(new MemR(dataBits))
    case _: MemWIO.type => None
    case _: MemRWIO.type => Some(new MemR(dataBits))
  }

  override def cloneType: MemIO.this.type =
    new MemIO(ioType, addrBits, dataBits).asInstanceOf[this.type]
}

object MemIO {
  def apply(ioType: MemIOType, addrBits: Int, dataBits: Int): MemIO = new MemIO(ioType, addrBits, dataBits)
  def apply(ioType: MemIOType): MemIO = apply(ioType, defaultConfig.addrBits, defaultConfig.dataBits)
}


class DbgIO(implicit  cfg: Config) extends Bundle {
  val pc = UInt(cfg.addrBits.W)
}

object DbgIO {
  def apply(implicit cfg: dirv.Config): DbgIO = new DbgIO()
}

class DirvIO(implicit cfg: dirv.Config) extends Bundle {
  val imem = MemIO(cfg.imemIOType, cfg.addrBits, cfg.dataBits)
  val dmem = MemIO(cfg.dmemIOType, cfg.addrBits, cfg.dataBits)
  val dbg = DbgIO

  override def cloneType: DirvIO.this.type =
    new DirvIO().asInstanceOf[this.type]
}
