// See LICENSE for license details.

package mbus

import chisel3._
import test.util._

/**
  * Testbench top module for MbusDecoder
  * @param limit Maximum cycles of simulation.
  * @param abortEn True if simulation will finish, when timeout is occurred.
  */
class SimDTMMbusDecoder(val p: MbusDecoderParams)
(
  limit: Int,
  abortEn: Boolean = true
) extends Module {

  val dut = Module(new MbusDecoder(p))
  val wdt = Module(new WDT(limit, abortEn))

  val io = IO(new Bundle {
    val dut_io = chiselTypeOf(dut.io)
    val wdt_io = chiselTypeOf(wdt.io)
  })

  io.dut_io <> dut.io
  io.wdt_io <> wdt.io
}
