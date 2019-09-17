// See LICENSE for license details.

package mbus

import chisel3._
import chisel3.util._

/**
  * Mbus access command
  */
object MbusCmd {
  val bits = 1
  val rd = 0x0
  val wr = 0x1
}

/**
  * Mbus access size
  */
object MbusSize {
  val bits = 2
  val byte = 0x0
  val half = 0x1
  val word = 0x2
}

/**
  * Mbus access response
  */
object MbusResp {
  val bits = 1
  val ok = 0x0
  val err = 0x1
}

/**
  * Memory base I/O
  * @param addrBits address bit width
  */
class MbusCmdIO(addrBits: Int) extends Bundle {
  val addr = UInt(addrBits.W)
  val cmd = UInt(MbusCmd.bits.W)
  val size = UInt(MbusSize.bits.W)

  override def cloneType: MbusCmdIO.this.type =
    new MbusCmdIO(addrBits).asInstanceOf[this.type]
}

object MbusCmdIO {
  def apply(addrBits: Int): DecoupledIO[MbusCmdIO] =
    DecoupledIO(new MbusCmdIO(addrBits))
}

/**
  * Memory read data I/O
  * @param dataBits data bit width
  */
class MbusRIO(dataBits: Int) extends Bundle {
  val resp = Input(UInt(MbusResp.bits.W))
  val data = Input(UInt(dataBits.W))

  override def cloneType: MbusRIO.this.type =
    new MbusRIO(dataBits).asInstanceOf[this.type]
}

object MbusRIO {
  def apply(dataBits: Int): DecoupledIO[MbusRIO] =
    Flipped(DecoupledIO(new MbusRIO(dataBits)))
}

/**
  * Memory write data I/O
  * @param dataBits data bit width
  */
class MbusWIO(dataBits: Int) extends Bundle {
  val strb = UInt((dataBits / 8).W)
  val data = UInt(dataBits.W)
  //val resp = Input(UInt(MbusResp.bits.W))

  override def cloneType: MbusWIO.this.type =
    new MbusWIO(dataBits).asInstanceOf[this.type]
}

object MbusWIO {
  def apply(dataBits: Int): DecoupledIO[MbusWIO] =
    DecoupledIO(new MbusWIO(dataBits))
}


/**
  * Dirv's memory I/O
  * @param ioType memory io type
  * @param addrBits address bit width
  * @param dataBits data bit width
  */
class MbusIO(ioType: MbusIOAttr, addrBits: Int, dataBits: Int) extends Bundle {
  val c = MbusCmdIO(addrBits)

  val w = ioType match {
    case MbusRO => None
    case _ => Some(MbusWIO(dataBits))
  }
  val r = ioType match {
    case MbusWO => None
    case _ => Some(MbusRIO(dataBits))
  }

  override def cloneType: MbusIO.this.type =
    new MbusIO(ioType, addrBits, dataBits).asInstanceOf[this.type]
}

/**
  * Companion object for MemIO class
  */
object MbusIO {
  def apply(ioType: MbusIOAttr, addrBits: Int, dataBits: Int): MbusIO =
    new MbusIO(ioType, addrBits, dataBits)
}
