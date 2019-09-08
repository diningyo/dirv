// See LICENSE for license details.

package mbus

import chisel3._

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
class MbusRIO(dataBits: Int) extends Bundle {
  val resp = Input(UInt(MemResp.bits.W))
  val data = Input(UInt(dataBits.W))
  val valid = Input(Bool())
  val ready = Output(Bool())

  override def cloneType: MbusRIO.this.type =
    new MbusRIO(dataBits).asInstanceOf[this.type]
}

/**
  * Memory write data I/O
  * @param dataBits data bit width
  */
class MbusWIO(dataBits: Int) extends Bundle {
  val strb = Output(UInt((dataBits / 8).W))
  val data = Output(UInt(dataBits.W))
  val resp = Input(UInt(MemResp.bits.W))
  val valid = Output(Bool())
  val ready = Input(Bool())

  override def cloneType: MbusWIO.this.type =
    new MbusWIO(dataBits).asInstanceOf[this.type]
}

/**
  * Dirv's memory I/O
  * @param ioType memory io type
  * @param addrBits address bit width
  * @param dataBits data bit width
  */
class MbusIO(ioType: MbusIOAttr, addrBits: Int, dataBits: Int) extends MemBaseIO(addrBits) {
  val w = ioType match {
    case MbusRO => None
    case _ => Some(new MbusWIO(dataBits))
  }
  val r = ioType match {
    case MbusWO => None
    case _ => Some(new MbusRIO(dataBits))
  }

  override def cloneType: MbusIO.this.type =
    new MbusIO(ioType, addrBits, dataBits).asInstanceOf[this.type]
}

/**
  * Companion object for MemIO class
  */
object MbusIO {
  def apply(ioType: MbusIOAttr, addrBits: Int, dataBits: Int): MbusIO = new MbusIO(ioType, addrBits, dataBits)
}
