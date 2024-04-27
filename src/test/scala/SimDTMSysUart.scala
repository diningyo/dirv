// See LICENSE for license details.

import chisel3._
import dirv.Config
import peri.uart.UartIO
import test.util._


/**
  *
  * @param limit Maximum cycles of simulation.
  * @param abortEn True if simulation will finish, when timeout is occurred.
  * @param prgHexFile
  * @param cfg
  */
class SimDTMSysUart(
  limit: Int,
  abortEn: Boolean = true
)(prgHexFile: String)
(baudrate: Int, clockFreq: Int)
(implicit cfg: Config) extends Module {

  val dut = Module(new SysUart(prgHexFile)(baudrate, clockFreq))
  val wdt = Module(new WDT(limit, abortEn))

  val io = IO(new Bundle {
    val dut_io = chiselTypeOf(dut.io)
    val wdt_io = chiselTypeOf(wdt.io)
    })

  io.dut_io <> dut.io
  io.wdt_io <> wdt.io
}
