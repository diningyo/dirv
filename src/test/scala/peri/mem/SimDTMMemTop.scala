// See LICENSE for license details.

package peri.mem

import chisel3._
import test.util._

/**
  * Testbench top module for MbusSramBridge
  * @param limit Maximum cycles of simulation.
  * @param abortEn True if simulation will finish, when timeout is occurred.
  */
class SimDTMMemTop(p: MemTopParams)
(
  limit: Int,
  abortEn: Boolean = true
) extends BaseSimDTM(limit, abortEn) {
  val io = IO(new Bundle with BaseSimDTMIO {
    val dut = new MemTopIO(p)
  })

  val dut = Module(new MemTop(p))

  io.dut <> dut.io

  connect(false.B)
}
