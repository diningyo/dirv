// See LICENSE for license details.

package test.util

import chisel3._

/**
  * Signals of BaseSimDTM
  */
trait BaseSimDTMIO extends Bundle {
  val timeout = Output(Bool())
  val finish = Output(Bool())
}

/**
  * Base testbench module for simulation
  * @param limit Maximum cycles of simulation.
  * @param abortEn True if simulation will finish, when timeout is occurred.
  */
abstract class BaseSimDTM(limit: Int, abortEn: Boolean = true)
  extends Module {
  val io: BaseSimDTMIO
  val wdt = Module(new WDT(limit, abortEn))

  def connect(finish: Bool): Unit = {
    io.finish := finish
    io.timeout := wdt.io.timeout
  }
}
