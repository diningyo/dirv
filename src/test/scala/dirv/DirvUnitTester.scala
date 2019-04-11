// See LICENSE for license details.

package dirv

import scala.util.control.Breaks

import chisel3.iotesters.PeekPokeTester

/**
  * Environment for Dirv's riscv-tests
  * @param c  SimDtm(This module includes Dirv and Memory)
  */
class DirvUnitTester(c: SimDtm) extends PeekPokeTester(c) {

  val timeoutCycle = 1000
  val b = Breaks

  b.breakable {
    reset()
    step(1)

    for (_ <- 0 until timeoutCycle) {
      if (peek(c.io.fin) == 0x1) {
        println("c.io.fin becomes high. **TEST SUCCEEDED**")
        b.break
      }
      step(1)
    }
  }
  step(5)
}
