// See LICENSE for license details.

package mbus

import chisel3._
import test.util._

/**
  * Testbench top module for MbusSramBridge
  * @param limit Maximum cycles of simulation.
  * @param abortEn True if simulation will finish, when timeout is occurred.
  */
class SimDTMMbusSramBridge(p: MbusSramBridgeParams)
(
  limit: Int,
  abortEn: Boolean = true
) extends BaseSimDTM(limit, abortEn) {
  val io = IO(new Bundle with BaseSimDTMIO {
    val dut = new MbusSramBridgeIO(p)
  })

  val dut = Module(new MbusSramBridge(p))

  io.dut <> dut.io

  connect(false.B)
}
