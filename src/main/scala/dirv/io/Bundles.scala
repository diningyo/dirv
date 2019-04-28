// See LICENSE for license details.

package dirv.io

import chisel3._
import dirv.{Config, defaultConfig}

/**
  * Enumeration for Memory IO type
  */
sealed trait MemIOType
case object MemRIO extends MemIOType
case object MemWIO extends MemIOType
case object MemRWIO extends MemIOType

/**
  * Memory access command
  */
object MemCmd {
  val bits = 1
  val rd = 0x0
  val wr = 0x1
}

/**
  * Memory access size
  */
object MemSize {
  val bits = 2
  val byte = 0x0
  val half = 0x1
  val word = 0x2
}

/**
  * Memory access response
  */
object MemResp {
  val bits = 1
  val ok = 0x0
  val err = 0x1
}

/**
  * Memory base I/O
  * @param addrBits address bit width
  */
class MemBaseIO(addrBits: Int) extends Bundle {
  val addr = Output(UInt(addrBits.W))
  val cmd = Output(UInt(MemCmd.bits.W))
  val size = Output(UInt(MemSize.bits.W))
  val valid = Output(Bool())
  val ready = Input(Bool())

  override def cloneType: MemBaseIO.this.type =
    new MemBaseIO(addrBits).asInstanceOf[this.type]
}

/**
  * Memory read data I/O
  * @param dataBits data bit width
  */
class MemR(dataBits: Int) extends Bundle {
  val resp = Input(UInt(MemResp.bits.W))
  val data = Input(UInt(dataBits.W))
  val valid = Input(Bool())
  val ready = Output(Bool())

  override def cloneType: MemR.this.type =
    new MemR(dataBits).asInstanceOf[this.type]
}

/**
  * Memory write data I/O
  * @param dataBits data bit width
  */
class MemW(dataBits: Int) extends Bundle {
  val strb = Output(UInt((dataBits / 8).W))
  val data = Output(UInt(dataBits.W))
  val resp = Input(UInt(MemResp.bits.W))
  val valid = Output(Bool())
  val ready = Input(Bool())

  override def cloneType: MemW.this.type =
    new MemW(dataBits).asInstanceOf[this.type]
}

/**
  * Dirv's memory I/O
  * @param ioType memory io type
  * @param addrBits address bit width
  * @param dataBits data bit width
  */
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

/**
  * Companion object for MemIO class
  */
object MemIO {
  def apply(ioType: MemIOType, addrBits: Int, dataBits: Int): MemIO = new MemIO(ioType, addrBits, dataBits)
  def apply(ioType: MemIOType): MemIO = apply(ioType, defaultConfig.addrBits, defaultConfig.dataBits)
}

/**
  * Debug I/O
  * @param cfg Dirv's configuration object
  */
class DbgIO(implicit cfg: Config) extends Bundle {
  val pc = Output(UInt(cfg.addrBits.W))
  val inst = Output(UInt(cfg.dataBits.W))
  val xregs = Output(Vec(cfg.arch.regNum, UInt(cfg.arch.xlen.W)))

  override def cloneType(): DbgIO.this.type = new DbgIO().asInstanceOf[this.type]
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
  val imem = MemIO(cfg.imemIOType, cfg.addrBits, cfg.dataBits)
  val dmem = MemIO(cfg.dmemIOType, cfg.addrBits, cfg.dataBits)
  val dbg = if (cfg.dbg) Some(new DbgIO) else None

  override def cloneType: DirvIO.this.type =
    new DirvIO().asInstanceOf[this.type]
}
