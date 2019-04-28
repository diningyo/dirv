// See LICENSE for license details.

package dirv

import chisel3._
import dirv.io.DirvIO
import dirv.pipeline._

class Dirv(implicit cfg: Config) extends Module {
  val io = IO(new DirvIO())

  val ifu = Module(new Ifu)
  val idu = Module(new Idu)
  val exu = Module(new Exu)
  val lsu = Module(new Lsu)

  //val

  // tmp
  io.imem <> ifu.io.ifu2ext
  ifu.io.ifu2idu <> idu.io.ifu2idu
  ifu.io.exu2ifu <> exu.io.exu2ifu

  exu.io.idu2exu <> idu.io.idu2exu

  lsu.io.exu2lsu <> exu.io.exu2lsu
  io.dmem <> lsu.io.lsu2ext

  if (cfg.dbg) {
    io.dbg.get.inst := exu.io.inst.get
    io.dbg.get.pc := exu.io.pc.get
    io.dbg.get.xregs := exu.io.xregs.get
  }
}
