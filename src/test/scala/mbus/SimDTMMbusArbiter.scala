// See LICENSE for license details.

package mbus

import chisel3._
import test.util._

/**
  * Testbench top module for MbusArbiter
  * @param limit Maximum cycles of simulation.
  * @param abortEn True if simulation will finish, when timeout is occurred.
  */
class SimDTMMbusArbiter(p: MbusArbiterParams)
                       (
  limit: Int,
  abortEn: Boolean = true
) extends BaseSimDTM(limit, abortEn) {
  val io = IO(new Bundle with BaseSimDTMIO {
    val dut = new MbusArbiterIO(p)
  })

  val dut = Module(new MbusArbiter(p))

  io.dut <> dut.io

  connect(false.B)
}
