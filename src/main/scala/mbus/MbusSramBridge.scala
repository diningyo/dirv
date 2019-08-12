// See LICENSE for license details.

package mbus

import java.nio.channels.FileLockInterruptionException

import chisel3._
import chisel3.util._
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

  //
  // Mbus : Command
  //
  class CmdData extends Bundle {
    val cmd = chiselTypeOf(io.mbus.cmd)
    val addr = chiselTypeOf(io.mbus.addr)
    val size = chiselTypeOf(io.mbus.size)
  }

  val w_mbus_cmd = Wire(Flipped(Decoupled(new CmdData)))
  w_mbus_cmd.valid := io.mbus.valid
  w_mbus_cmd.bits.cmd := io.mbus.cmd
  w_mbus_cmd.bits.addr := io.mbus.addr
  w_mbus_cmd.bits.size := io.mbus.size

  val m_cmd_q = Queue(w_mbus_cmd, 1, pipe = true, flow = true)

  //
  // Write data
  //
  class WrData extends Bundle {
    val strb = chiselTypeOf(io.mbus.w.get.strb)
    val data = chiselTypeOf(io.mbus.w.get.data)
    val resp = chiselTypeOf(io.mbus.w.get.resp)
  }

  val w_mbus_wr = Wire(Flipped(Decoupled(new WrData)))
  w_mbus_wr.valid := io.mbus.w.get.valid
  w_mbus_wr.bits.strb := io.mbus.w.get.strb
  w_mbus_wr.bits.data := io.mbus.w.get.data
  w_mbus_wr.bits.resp := MemResp.ok.U

  val m_wr_q = Queue(w_mbus_wr, 1, pipe = true, flow = true)

  val w_sram_wr_ready = m_cmd_q.valid && m_wr_q.valid

  m_cmd_q.ready := w_sram_wr_ready
  m_wr_q.ready := w_sram_wr_ready

  io.mbus.ready := w_mbus_cmd.ready
  io.mbus.w.get.ready := w_mbus_wr.ready

  io.sram.addr := m_cmd_q.bits.addr
  io.sram.wren.get := m_cmd_q.fire()
  io.sram.wrstrb.get := m_wr_q.bits.strb
  io.sram.wrdata.get := m_wr_q.bits.data
}
