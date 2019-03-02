// See LICENSE for license details.

package dirv

import chisel3._
import chisel3.iotesters.PeekPokeTester

class SimDtm(implicit cfg: Config) extends Module {
  val io = IO(new Bundle {

  })

  val a = "/home/diningyo/prj/risc-v/dirv/test_run_dir/dirv.DirvMain457984422/test.hex"
  val mem = Module(new MemModel(a))
  val dut = Module(new Dirv)

  // Temp.
  io := DontCare
  dut.io := DontCare

  // connect mem and dut
  mem.io.clk := clock
  mem.io.rst := reset
  mem.io.imem <> dut.io.imem
  mem.io.dmem <> dut.io.dmem
}
