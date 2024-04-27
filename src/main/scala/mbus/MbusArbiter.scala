// See LICENSE for license details.

package mbus

import chisel3._
import chisel3.util._
import _root_.circt.stage.ChiselStage

/**
  * Parameter class for MbusArbiter
  * @param ioAttr Mbus IO attribute.
  * @param masterInfos Master memory maps.
  * @param dataBits Data bits.
  */
case class MbusArbiterParams
(
  ioAttr: MbusIOAttr,
  masterInfos: Seq[(Int, Int)],
  dataBits: Int
) {
  val numOfMasters = masterInfos.length
  val startAddrs = masterInfos.map(_._1)
  val endAddrs = masterInfos.map(info => info._1 + info._2)
  val addrMap = startAddrs zip endAddrs
  val addrBits = log2Ceil(endAddrs.max)
}

/**
  * MbusArbiter I/O
  * @param p Instance of MbusArbiterParams
  */
class MbusArbiterIO(p: MbusArbiterParams) extends Bundle {
  /*
  val in = MixedVec(Seq(
    Flipped(MbusIO(p.ioAttr, p.addrBits, p.dataBits)),
    Flipped(MbusIO(p.ioAttr, p.addrBits + 4, p.dataBits))
  ))*/
  val in = Vec(p.numOfMasters, Flipped(MbusIO(p.ioAttr, p.addrBits, p.dataBits)))
  val out = MbusIO(p.ioAttr, p.addrBits, p.dataBits)
  override def cloneType: this.type =
    new MbusArbiterIO(p).asInstanceOf[this.type]
}

/**
  * MbusArbiter
  * @param p Paramter object for MbusArbiter (MbusArbiterParams).
  */
class MbusArbiter(p: MbusArbiterParams) extends Module {
  val io = IO(new MbusArbiterIO(p))

  val m_arb = Module(new RRArbiter(new MbusCmdIO(p.addrBits), p.numOfMasters))
  val m_out_slice = Module(new MbusSlice(p.ioAttr, p.addrBits, p.dataBits, false, false, false))
  val m_issued_q = Module(new Queue(UInt(log2Ceil(p.numOfMasters).W), 1, true, true))

  for ((io_in, arb_in) <- io.in zip m_arb.io.in) {
    arb_in <> io_in.c
  }

  m_issued_q.io.enq.valid := m_out_slice.io.in.c.valid
  m_issued_q.io.enq.bits := m_arb.io.chosen

  m_out_slice.io.in.w.get <> io.in(m_issued_q.io.deq.bits).w.get
  m_out_slice.io.in.w.get.valid := m_issued_q.io.deq.valid && io.in(m_issued_q.io.deq.bits).w.get.valid
  m_issued_q.io.deq.ready := m_out_slice.io.in.w.get.fire || m_out_slice.io.in.r.get.fire

  for ((io_in, idx) <- io.in.zipWithIndex) {
    val w_dport_sel = (m_issued_q.io.deq.bits === idx.U) && m_issued_q.io.deq.valid
    w_dport_sel.suggestName(s"w_dport_sel_$idx")
    io_in.r.get <> m_out_slice.io.in.r.get
    io_in.r.get.valid := w_dport_sel && m_out_slice.io.in.r.get.valid
    io_in.w.get.ready := w_dport_sel && m_out_slice.io.in.w.get.ready
  }

  m_out_slice.io.in.c <> m_arb.io.out
  io.out <> m_out_slice.io.out
}


object ElaborateMbusArb extends App {
  val p = MbusArbiterParams(MbusRW, Seq((1000, 256), (2000, 256)), 32)

  println(
    ChiselStage.emitSystemVerilog(
      gen = new MbusArbiter(p),
      firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info")
    )  
  )
}