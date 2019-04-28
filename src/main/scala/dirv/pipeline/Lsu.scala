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

  io.lsu2ext.valid := false.B
  io.lsu2ext.cmd := MemCmd.rd.U
  io.lsu2ext.addr := io.exu2lsu.memAddr
  io.lsu2ext.w.get.data := 0xdeafbeafL.U
  io.lsu2ext.w.get.strb := 0xf.U
  io.lsu2ext.r.get.ready := true.B
  io.lsu2ext.size := Mux1H(Seq(
    (inst.sb || inst.lb || inst.lbu) -> MemSize.byte.U,
    (inst.sh || inst.lh || inst.lhu) -> MemSize.half.U,
    (inst.sw || inst.lw) -> MemSize.byte.U
  ))
  io.lsu2ext.w.get.valid := io.exu2lsu.inst.storeValid
}
