// See LICENSE for license details.

package mbus

import chisel3._
import chisel3.util._
import peri.mem.{RAMIO, RAMIOParams, RAMRO, RAMRW, RAMWO}

// TODO : move to mbus
sealed trait MbusIOAttr
case object MbusRO extends MbusIOAttr
case object MbusWO extends MbusIOAttr
case object MbusRW extends MbusIOAttr

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
  val ramIOAttr = ioAttr match {
    case MbusRO => RAMRO
    case MbusWO => RAMWO
    case MbusRW => RAMRW
  }
  val ramIOParams = RAMIOParams(ramIOAttr, addrBits, dataBits, hasRddv = true)
}

/**
  * MbusSramBridge I/O
  * @param p Instance of MbusSramBridgeParams
  */
class MbusSramBridgeIO(p: MbusSramBridgeParams) extends Bundle {
  val mbus = Flipped(MbusIO(p.ioAttr, p.addrBits, p.dataBits))
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
  // Mbus : Write data
  //
  val w_sram_wr_ready = WireInit(false.B)

  if (p.ioAttr != MbusRO) {
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

    val w_sram_wr_req = m_cmd_q.valid && (m_cmd_q.bits.cmd === MemCmd.wr.U)
    w_sram_wr_ready := w_sram_wr_req && m_wr_q.valid

    m_wr_q.ready := w_sram_wr_ready

    io.mbus.w.get.ready := w_mbus_wr.ready
    io.mbus.w.get.resp := w_mbus_wr.bits.resp

    io.sram.wren.get := m_cmd_q.fire() && (m_cmd_q.bits.cmd === MemCmd.wr.U)
    io.sram.wrstrb.get := m_wr_q.bits.strb
    io.sram.wrdata.get := m_wr_q.bits.data
  }

  //
  // Mbus : Read data
  //
  class RdData extends Bundle {
    val data = chiselTypeOf(io.mbus.r.get.data)
    val resp = chiselTypeOf(io.mbus.r.get.resp)
  }

  val w_sram_rd = Wire(Flipped(Decoupled(new RdData)))
  w_sram_rd.valid := io.sram.rddv.get
  w_sram_rd.bits.data := io.sram.rddata.get
  w_sram_rd.bits.resp := MemResp.ok.U

  val m_rd_q = Queue(w_sram_rd, 1, pipe = true, flow = true)
  m_rd_q.ready := io.mbus.r.get.ready

  val w_sram_read_req = m_cmd_q.valid && m_cmd_q.bits.cmd === MemCmd.rd.U

  //
  // Queue : read connection
  //
  m_cmd_q.ready := w_sram_wr_ready || w_sram_read_req

  //
  // Mbus I/O
  //
  io.mbus.ready := w_mbus_cmd.ready
  io.mbus.r.get.valid := m_rd_q.valid
  io.mbus.r.get.data := m_rd_q.bits.data
  io.mbus.r.get.resp := m_rd_q.bits.resp

  //
  // Sram I/O
  //
  io.sram.addr := m_cmd_q.bits.addr
  io.sram.rden.get := w_sram_read_req && m_rd_q.ready
}
