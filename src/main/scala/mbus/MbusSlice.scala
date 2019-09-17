// See LICENSE for license details.

package mbus

import chisel3._
import chisel3.util._

class MbusSlice
(
  ioAttr: MbusIOAttr,
  addrBits: Int,
  dataBits: Int,
  cSlice: Boolean,
  rSlice: Boolean,
  wSlice: Boolean
) extends Module {
  val io = IO(new Bundle {
    val in = Flipped(MbusIO(ioAttr, addrBits, dataBits))
    val out = MbusIO(ioAttr, addrBits, dataBits)
  })

  val m_cmd_q = Queue(io.in.c, 1, !cSlice, !cSlice)
  io.out.c <> m_cmd_q

  if (ioAttr != MbusWO) {
    val m_rd_q = Queue(io.out.r.get, 1, !rSlice, !rSlice)
    m_rd_q.suggestName("m_rd_q")
    io.in.r.get <> m_rd_q
  }

  if (ioAttr != MbusRO) {
    val m_wr_q = Queue(io.in.w.get, 1, !wSlice, !wSlice)
    m_wr_q.suggestName("m_wr_q")
    io.out.w.get <> m_wr_q
  }
}
