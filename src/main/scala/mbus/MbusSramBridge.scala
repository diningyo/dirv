// See LICENSE for license details.

package mbus

import chisel3._
import dirv.io._
import peri.mem.{RAMIO, RAMIOParams, RAMRO, RAMRW, RAMWO}

// TODO : move to mbus
sealed trait MbusIOAttr
case object ROMbusIO extends MbusIOAttr
case object WOMbusIO extends MbusIOAttr
case object RWMbusIO extends MbusIOAttr

/**
  * parameter class for MbusSramBridge
  * @param ioAttr IO port access attribute.
  * @param addrBits Address bus width.
  * @param dataBits Data bus width.
  */
case class MbusSramBridgeParams
(
  ioAttr: MbusIOAttr,
  addrBits: Int,
  dataBits: Int
) {

  val memIOAttr = ioAttr match {
    case ROMbusIO => MemRIO
    case WOMbusIO => MemWIO
    case RWMbusIO => MemRWIO
  }

  val ramIOAttr = ioAttr match {
    case ROMbusIO => RAMRO
    case WOMbusIO => RAMWO
    case RWMbusIO => RAMRW
  }
  val ramIOParams = RAMIOParams(ramIOAttr, addrBits, dataBits, hasRddv = true)
}

/**
  * MbusSramBridge I/O
  * @param p Instance of MbusSramBridgeParams
  */
class MbusSramBridgeIO(p: MbusSramBridgeParams) extends Bundle {
  val mbus = Flipped(MemIO(p.memIOAttr, p.addrBits, p.dataBits))
  val sram = new RAMIO(p.ramIOParams)

  override def cloneType: this.type =
    new MbusSramBridgeIO(p).asInstanceOf[this.type]
}

/**
  * MbusSramBridge
  * @param p Instance of MbusSramBridgeParams
  */
class MbusSramBridge(p: MbusSramBridgeParams) extends Module {
  val io = IO(new MbusSramBridgeIO(p))

  io := DontCare
}
