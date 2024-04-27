// See LICENSE for license details.

package mbus

import chisel3._
import chisel3.util._


case class MbusPortParams
(
  startAddr: Int,
  size: Int,
  slice: (Boolean, Boolean, Boolean)
)

/**
  * Parameter class for MbusDecoder
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
}

/**
  * Mbus Decoder
  * @param p Instance of MbusDecoderParams
  */
//@chiselName
class MbusDecoder(p: MbusDecoderParams) extends Module {

  /**
    * Check destination address is in range or not.
    * @param dst_addr Input destination address.
    * @param addrMap Port address range.
    * @return True if dst_addr is in the range of A and B.
    */
  def checkAddress(dst_addr: UInt, addrMap: (Int, Int)): Bool = {
    val start = addrMap._1
    val end = addrMap._2
    (start.U <= dst_addr) && (dst_addr < end.U)
  }

  p.addrMap.foreach {
    case (s, e) => println(f"(start, end) = ($s%x, $e%x)")
  }

  val io = IO(new MbusDecoderIO(p))

  val m_in_slice = Module(new MbusSlice(p.ioAttr, p.addrBits, p.dataBits,
    cSlice = false, rSlice = false, wSlice = false))

  val w_port_sels = Seq.fill(p.addrMap.length)(Wire(Bool()))

  m_in_slice.io.in <> io.in

  for (((out_port, info), idx) <- (io.out zip p.addrMap).zipWithIndex) {
    out_port.c <> m_in_slice.io.out.c

    val w_port_sel = checkAddress(m_in_slice.io.out.c.bits.addr, info)
    w_port_sel.suggestName(s"w_port_sel_$idx")

    w_port_sels(idx) := w_port_sel

    val w_wr_req = w_port_sel && m_in_slice.io.out.c.valid && (m_in_slice.io.out.c.bits.cmd === MbusCmd.wr.U)
    w_wr_req.suggestName(s"w_wr_req_$idx")

    out_port.c.valid := m_in_slice.io.out.c.valid && w_port_sel

    out_port.w.get <> m_in_slice.io.out.w.get

    val r_wr_sel = RegNext(w_wr_req, false.B)
    r_wr_sel.suggestName(s"r_wr_sel_$idx")

    when (!w_wr_req && m_in_slice.io.out.w.get.fire) {
      r_wr_sel := false.B
    }

    val w_wr_valid = dontTouch(w_wr_req || r_wr_sel)
    w_wr_valid.suggestName(s"w_wr_valid_$idx")

    out_port.w.get.valid := Mux(w_wr_valid, m_in_slice.io.out.w.get.valid, false.B)

    // out(N).r.get.ready reflects m_in_slice.io.out.r.get.ready
    out_port.r.get.ready := m_in_slice.io.out.r.get.ready
  }

  // write ready control
  val w_chosen = PriorityEncoder(w_port_sels)
  val m_chosen_q = Module(new Queue(UInt(log2Ceil(p.addrMap.length).W), 1, true, true))

  m_chosen_q.io.enq.valid := m_in_slice.io.out.c.fire
  m_chosen_q.io.enq.bits := w_chosen
  m_chosen_q.io.deq.ready := io.out(m_chosen_q.io.deq.bits).w.get.fire
  m_in_slice.io.out.w.get.ready := io.out(m_chosen_q.io.deq.bits).w.get.ready

  // read
  val w_rd_valid = io.out.map(_.r.get.valid)
  val w_rd_data = io.out.map(_.r.get.bits)
  m_in_slice.io.out.r.get.valid := w_rd_valid.reduce(_ || _)

  val w_dummy = 0.U.asTypeOf(chiselTypeOf(io.in.r.get.bits))
  m_in_slice.io.out.r.get.bits := MuxCase(w_dummy, w_rd_valid zip w_rd_data)
}
