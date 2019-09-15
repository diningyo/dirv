// See LICENSE for license details.

package mbus

import chisel3._
import chisel3.core.dontTouch
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
//@chiselName
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
    (start.U <= dst_addr) && (dst_addr < end.U)
  }

  val io = IO(new MbusDecoderIO(p))

  val m_in_slice = Module(new MbusSlice(p.ioAttr, p.addrBits, p.dataBits,
    cSlice = false, rSlice = false, wSlice = false))

  m_in_slice.io.in <> io.in

  for (((out_port, info), idx) <- (io.out zip p.addrMap).zipWithIndex) {
    out_port.c <> m_in_slice.io.out.c

    val w_port_sel = checkAddress(m_in_slice.io.out.c.bits.addr, info)
    w_port_sel.suggestName(s"w_port_sel_${idx}")

    out_port.c.valid := m_in_slice.io.out.c.valid && w_port_sel

    out_port.w.get <> m_in_slice.io.out.w.get

    val r_wr_sel = RegNext(w_port_sel, false.B)
    r_wr_sel.suggestName(s"r_wr_sel_${idx}")

    when (!w_port_sel && m_in_slice.io.out.w.get.fire()) {
      r_wr_sel := false.B
    }

    val w_wr_valid = dontTouch(w_port_sel || r_wr_sel)
    w_wr_valid.suggestName(s"w_wr_valid_${idx}")

    out_port.w.get.valid := w_wr_valid

    // out(N).r.get.ready reflects io.in.r.get.ready
    out_port.r.get.ready := m_in_slice.io.out.r.get.ready
  }

  // read
  val w_rd_valid = io.out.map(_.r.get.valid)
  val w_rd_data = io.out.map(_.r.get.bits)
  m_in_slice.io.out.r.get.valid := w_rd_valid.reduce(_ || _)

  val w_dummy = 0.U.asTypeOf(chiselTypeOf(io.in.r.get.bits))
  m_in_slice.io.out.r.get.bits := MuxCase(w_dummy, w_rd_valid zip w_rd_data)
}
