// See LICENSE for license details.

package dirv.pipeline

import chisel3._
import chisel3.util._
import dirv.Config
import dirv.io.{MemCmd, MemIO, MemSize}

/**
  *
  * @param cfg dirv's configuration parameter.
  */
class LsuIO(implicit val cfg: Config) extends Bundle {
  val exu2lsu = Flipped(new Exu2LsuIO())
  val lsu2ext = MemIO(cfg.dmemIOType, cfg.addrBits, cfg.dataBits)
}


/**
  * Load store unit
  * @param cfg dirv's configuration parameter.
  */
class Lsu(implicit cfg: Config) extends Module {
  val io = IO(new LsuIO())

  val inst = io.exu2lsu.inst
  val size = Mux1H(Seq(
    (inst.sb || inst.lb || inst.lbu) -> MemSize.byte.U,
    (inst.sh || inst.lh || inst.lhu) -> MemSize.half.U,
    (inst.sw || inst.lw) -> MemSize.byte.U
  ))

  val uaMsb = log2Ceil(cfg.arch.xlen / 8)
  val unaligedAddr = io.exu2lsu.memAddr(uaMsb - 1, 0)
  val strb = Mux1H(Seq(
    inst.sb -> Fill(0x1 << MemSize.byte, 1.U),
    inst.sh -> Fill(0x1 << MemSize.half, 1.U),
    inst.sw -> Fill(0x1 << MemSize.word, 1.U)
  )) << unaligedAddr

  io.lsu2ext.valid := inst.loadValid || inst.storeValid
  io.lsu2ext.cmd := Mux(inst.loadValid, MemCmd.rd.U, MemCmd.wr.U)
  io.lsu2ext.addr := io.exu2lsu.memAddr
  io.lsu2ext.w.get.data := 0xdeafbeafL.U
  io.lsu2ext.w.get.strb := strb
  io.lsu2ext.r.get.ready := true.B
  io.lsu2ext.size := size
  io.lsu2ext.w.get.valid := io.exu2lsu.inst.storeValid
}
