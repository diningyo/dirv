// See LICENSE for license details.

package mbus

import chisel3._
import chisel3.core.dontTouch
import chisel3.util._


/**
  * parameter class for MbusIC
  * @param ioAttr IO port access attribute.
  * @param slaveInfos Slave module information. It contains base address and size.
  * @param dataBits Data bus width.
  */
case class MbusICParams
(
  ioAttr: MbusIOAttr,
  masterInfos: Seq[(Int, Int)],
  slaveInfos: Seq[(Int, Int)],
  dataBits: Int
) {
  val numOfMasters = masterInfos.length
  val numOfSlaves = slaveInfos.length
  val addrBits = 32
  val decParams = Seq.fill(numOfMasters)(MbusDecoderParams(MbusRW, slaveInfos, dataBits))
  val arbParams = Seq.fill(numOfSlaves)(MbusArbiterParams(MbusRW, masterInfos, dataBits))
}
/**
  * MbusIC I/O
  * @param p Instance of MbusICParams
  */
class MbusICIO(p: MbusICParams) extends Bundle {
  val in = Vec(p.numOfMasters, Flipped(MbusIO(p.ioAttr, p.addrBits, p.dataBits)))
  val out = Vec(p.numOfSlaves, MbusIO(p.ioAttr, p.addrBits, p.dataBits))

  override def cloneType: this.type =
    new MbusICIO(p).asInstanceOf[this.type]
}

/**
  * Mbus Decoder
  * @param p
  */
//@chiselName
class MbusIC(p: MbusICParams) extends Module {

  val io = IO(new MbusICIO(p))

  val m_decs = p.decParams.map( dp => Module(new MbusDecoder(dp)))
  val m_arbs = p.arbParams.map( ap => Module(new MbusArbiter(ap)))

  // io.in(N) <-> dec(N).io.in
  for ((in , m_dec) <- io.in zip m_decs) {
    m_dec.io.in <> in
  }

  // dec.io.out(M) <-> arb.io.in(N)
  for ((m_dec, i) <- m_decs.zipWithIndex; (m_arb, j) <- m_arbs.zipWithIndex) {
    m_dec.io.out(j) <> m_arb.io.in(i)
  }

  // io.out(N) <-> arb.io.out(N)
  for ((out , arb) <- io.out zip m_arbs) {
    out <> arb.io.out
  }
}


object ElaborateMbusIC extends App {

  val infos = Seq(
    (0x0,   0x100),
    (0x100, 0x100),
    (0x1000, 0x100),
    (0x2000, 0x100)
  )

  val base_p = MbusICParams(MbusRW, infos, infos, 32)

  Driver.execute(args, () => new MbusIC(base_p))
}