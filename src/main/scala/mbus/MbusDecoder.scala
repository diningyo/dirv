// See LICENSE for license details.

package mbus

import chisel3._
import chisel3.internal.naming.chiselName
import chisel3.util._


case class MbusPortParams
(
  startAddr: Int,
  size: Int,
  slice: (Boolean, Boolean, Boolean)
)

/**
  * parameter class for MbusDecoder
  * @param ioAttr IO port access attribute.
  * @param slaveInfos Slave module information. It contains base address and size.
  * @param dataBits Data bus width.
  */
case class MbusDecoderParams
(
  ioAttr: MbusIOAttr,
  slaveInfos: Seq[(Int, Int)],
  dataBits: Int
) {
  val startAddrs = slaveInfos.map(_._1)
  val endAddrs = slaveInfos.map(info => info._1 + info._2)
  val numOfSlaves = slaveInfos.length
  val addrMap = startAddrs zip endAddrs
  val addrBits = log2Ceil(endAddrs.max)
}
/**
  * MbusDecoder I/O
  * @param p Instance of MbusDecoderParams
  */
class MbusDecoderIO(p: MbusDecoderParams) extends Bundle {
  val in = Flipped(MbusIO(p.ioAttr, p.addrBits, p.dataBits))
  val out = Vec(p.numOfSlaves, MbusIO(p.ioAttr, p.addrBits, p.dataBits))

  override def cloneType: this.type =
    new MbusDecoderIO(p).asInstanceOf[this.type]
}

/**
  * Mbus Decoder
  * @param p
  */
@chiselName
class MbusDecoder(p: MbusDecoderParams) extends Module {

  /**
    * Check destination address is in range or not.
    * @param dst_addr Input destination address.
    * @param addrMap Port address range.
    * @return
    */
  def checkAddress(dst_addr: UInt, addrMap: (Int, Int)): Bool = {
    val start = addrMap._1
    val end = addrMap._2
    (start.U <= dst_addr) && (dst_addr <= end.U)
  }

  val io = IO(new MbusDecoderIO(p))

  val m_cmd_q = Queue(io.in.c, 1, true, true)
  val m_rd_q = Queue(io.out(0).r.get, 1, true, true)
  val m_wr_q = Queue(io.in.w.get, 1, true, true)

  io := DontCare

  for ((out_port, info) <- io.out zip p.addrMap) {
    out_port.c <> m_cmd_q
    out_port.w.get <> m_wr_q

    val w_port_sel = checkAddress(m_cmd_q.bits.addr, info)
    out_port.c.valid := m_cmd_q.valid && w_port_sel

    val r_wr_sel = RegNext(w_port_sel, false.B)

    when (!w_port_sel && m_wr_q.fire()) {
      r_wr_sel := false.B
    }

    val w_wr_valid = w_port_sel || r_wr_sel

    out_port.w.get.valid := w_wr_valid
  }

  m_rd_q.ready := io.in.r.get.ready

  // read
  val w_rd_valid = io.out.map(_.r.get.valid)
  val w_rd_data = io.out.map(_.r.get.bits)
  io.in.r.get.valid := w_rd_valid.reduce(_ || _)

  val w_dummy = 0.U.asTypeOf(chiselTypeOf(io.in.r.get.bits))
  io.in.r.get.bits := MuxCase(w_dummy, w_rd_valid zip w_rd_data)
}
