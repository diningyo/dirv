// See LICENSE for license details.

package mbus

import chisel3._
import chisel3.util._
import test.util._

/**
  * Testbench top module for MbusSramBridge
  * @param limit Maximum cycles of simulation.
  * @param abortEn True if simulation will finish, when timeout is occurred.
  */
class SimDTMMbusSramBridge()
(
  limit: Int,
  abortEn: Boolean = true
) extends BaseSimDTM(limit, abortEn) {
  val io = IO(new Bundle with BaseSimDTMIO)

  connect(false.B)
}
