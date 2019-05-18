// See LICENSE for license details.

import chisel3.iotesters._
import scala.util.control.Breaks

/**
  * Environment for Dirv's riscv-tests
  * @param c  SimDtm(This module includes Dirv and Memory)
  */
class DirvUnitTester(c: SimDtm) extends PeekPokeTester(c) {

  val timeoutCycle = 1000
  val b = Breaks
  val regNames = Seq(
    /*"zero",*/
    "ra",  "sp",  "gp", "tp", "t0", "t1", "s0", "s1",
    "a0",  "a1",  "a2", "a3", "a4", "a5", "a6", "a7",
    "s2",  "s3",  "s4", "s5", "s6", "s7", "s8", "s9",
    "s10", "s11", "t3", "t4", "t5", "t6"
  ).zipWithIndex

  def info(): Unit = {
    val xregsInfo = regNames.map {
      case (regName, idx) => f"$regName: 0x${peek(c.io.xregs(idx + 1))}%08x | "
    }.reduce(_ + _)

    println(
        f"pc: ${peek(c.io.pc)}%08x | " +
        f"inst: ${peek(c.io.pc)}%08x | " +
        xregsInfo)
  }

  b.breakable {
    reset()
    step(1)

    for (_ <- 0 until timeoutCycle) {
      info()
      if (peek(c.io.fin) == 0x1) {
        println("c.io.fin becomes high. **TEST SUCCEEDED**")
        b.break
      }
      step(1)
    }
  }
  expect(c.io.fin, true)
  step(5)
}
