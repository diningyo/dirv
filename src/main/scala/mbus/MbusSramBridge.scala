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
  * Parameter class for MbusSramBridge
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
  val m_cmd_q = Queue(io.mbus.c, 1, pipe = true, flow = true)

  //
  // Mbus : Write data
  //
  val w_sram_wr_ready = WireInit(false.B)

  if (p.ioAttr != MbusRO) {
    val m_wr_q = Queue(io.mbus.w.get, 1, pipe = true, flow = true)

    val w_sram_wr_req = m_cmd_q.valid && (m_cmd_q.bits.cmd === MbusCmd.wr.U)
    w_sram_wr_ready := w_sram_wr_req && m_wr_q.valid
    m_wr_q.ready := w_sram_wr_ready

    io.sram.wren.get := w_sram_wr_ready
    io.sram.wrstrb.get := m_wr_q.bits.strb
    io.sram.wrdata.get := m_wr_q.bits.data
  }

  //
  // Mbus : Read data
  //
  val w_rd_enq = Wire(chiselTypeOf(io.mbus.r.get))
  w_rd_enq.valid := io.sram.rddv.get
  w_rd_enq.bits.data := io.sram.rddata.get
  w_rd_enq.bits.resp := MbusResp.ok.U

  val m_rd_q = Queue(w_rd_enq, 1, pipe = true, flow = true)
  m_rd_q.ready := io.mbus.r.get.ready

  val w_sram_read_req = m_cmd_q.valid && m_cmd_q.bits.cmd === MbusCmd.rd.U

  //
  // Queue : read connection
  //
  m_cmd_q.ready := w_sram_wr_ready || (w_sram_read_req && m_rd_q.ready)

  //
  // Mbus I/O
  //
  io.mbus.r.get.valid := m_rd_q.valid
  io.mbus.r.get.bits.data := m_rd_q.bits.data
  io.mbus.r.get.bits.resp := m_rd_q.bits.resp

  //
  // Sram I/O
  //
  io.sram.addr := m_cmd_q.bits.addr
  io.sram.rden.get := w_sram_read_req && m_rd_q.ready
}
