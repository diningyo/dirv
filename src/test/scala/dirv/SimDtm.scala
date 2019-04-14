// See LICENSE for license details.

package dirv

import chisel3._

class SimDtm(prgHexFile: String)(implicit cfg: Config) extends Module {
  val io = IO(new Bundle {
    val fin = Output(Bool())
  })

  val mem = Module(new MemModel(prgHexFile))
  val dut = Module(new Dirv)

  //
  io.fin := dut.io.dbg.get.fin

  // connect mem and dut
  mem.io.clk := clock
  mem.io.rst := reset
  mem.io.imem <> dut.io.imem
  mem.io.dmem <> dut.io.dmem
}
