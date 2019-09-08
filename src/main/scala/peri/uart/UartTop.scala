// See LICENSE for license details.

package peri.uart

import chisel3._
import mbus._

class UartTop(bardrate: Int, clockFreq: Int) extends Module {
  val io = IO(new Bundle {
    val mem = Flipped(new MbusIO(MbusRW, 4, 32))
    val uart= new UartIO
  })

  val memBrg = Module(new Mem2Sram)
  val regTop = Module(new RegTop)
  val ctrl = Module(new TxRxCtrl(bardrate, clockFreq))

  io.mem <> memBrg.io.mem
  io.uart <> ctrl.io.uart

  memBrg.io.regR <> regTop.io.regR
  memBrg.io.regW <> regTop.io.regW
  regTop.io.r2c <> ctrl.io.r2c
}
