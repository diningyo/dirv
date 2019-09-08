// See LICENSE for license details.

package peri.uart

import chisel3._
import mbus._

class UartTop(bardrate: Int, clockFreq: Int) extends Module {

  val sp = MbusSramBridgeParams(MbusRW, 4, 32)

  val io = IO(new Bundle {
    val mbus = Flipped(new MbusIO(sp.ioAttr, sp.addrBits, sp.dataBits))
    val uart= new UartIO
  })

  val memBrg = Module(new MbusSramBridge(MbusSramBridgeParams(MbusRW, 4, 32)))
  val regTop = Module(new RegTop(sp.ramIOParams)())
  val ctrl = Module(new TxRxCtrl(bardrate, clockFreq))

  io.mbus <> memBrg.io.mbus
  io.uart <> ctrl.io.uart

  memBrg.io.sram <> regTop.io.sram
  regTop.io.r2c <> ctrl.io.r2c
}
