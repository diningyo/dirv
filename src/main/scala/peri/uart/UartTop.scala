// See LICENSE for license details.

package peri.uart

import chisel3._
import mbus._

class UartTop(baudrate: Int, clockFreq: Int) extends Module {

  val sp = MbusSramBridgeParams(MbusRW, 4, 32)

  val io = IO(new Bundle {
    val mbus = Flipped(new MbusIO(sp.ioAttr, sp.addrBits, sp.dataBits))
    val uart= new UartIO
  })

  val m_brg = Module(new MbusSramBridge(MbusSramBridgeParams(MbusRW, 4, 32)))
  val m_reg = Module(new RegTop(sp.ramIOParams)())
  val m_ctrl = Module(new TxRxCtrl(baudrate, clockFreq))

  io.mbus <> m_brg.io.mbus
  io.uart <> m_ctrl.io.uart

  m_brg.io.sram <> m_reg.io.sram
  m_reg.io.r2c <> m_ctrl.io.r2c
}
