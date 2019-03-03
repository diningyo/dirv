// See LICENSE for license details.

package dirv.pipeline

import chisel3._
import chisel3.util.Queue
import dirv.Config
import dirv.io.{MemCmd, MemIO}

class Ifu(implicit cfg: Config) extends Module {
  val io = IO(new Bundle {
    // external - ifu
    val ifu2ext = MemIO(cfg.imemIOType, cfg.addrBits, cfg.dataBits)
    val ifu2idu = Flipped(MemIO(cfg.imemIOType, cfg.addrBits, cfg.dataBits))
  })

  io.ifu2ext.req := true.B
  io.ifu2ext.cmd := MemCmd.rd

  val cmdBuf = RegInit(cfg.initAddr.U(cfg.dataBits.W))
  val hasCmd = RegInit(false.B)

  when (io.ifu2ext.r.get.rddv) {
    hasCmd := true.B
    cmdBuf := io.ifu2ext.r.get.data
  } .otherwise {
    hasCmd := false.B
  }

  io.ifu2idu.r.get.data := cmdBuf

  val cmdAddr = RegInit(0.U(cfg.dataBits.W))

  when (io.ifu2ext.r.get.rddv ) {
    cmdAddr := cmdAddr + 4.U
  }

  io.ifu2ext.addr := cmdAddr

  io.ifu2idu <> io.ifu2ext

}
