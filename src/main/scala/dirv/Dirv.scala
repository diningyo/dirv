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
  //val

  ifu.io.ifu2ext <> io.imem
  ifu.io.ifu2idu <> idu.io.ifu2idu

  exu.io.inst2ext <> idu.io.inst2ext
  io.dmem <> exu.io.exu2ext

  if (cfg.dbg) {
    io.dbg.get.pc := 0x0.U
    io.dbg.get.fin := exu.io.fin.get
  }
}
