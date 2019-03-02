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

  io := DontCare
}
