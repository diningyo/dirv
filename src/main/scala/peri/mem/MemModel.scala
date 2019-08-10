// See LICENSE for license details.

package peri.mem

import chisel3._
import chisel3.experimental.StringParam
import chisel3.util.HasBlackBoxResource
import dirv.Config
import dirv.io.MemIO

class MemModel(testFile: String)(implicit cfg: Config) extends BlackBox(
  Map("p_TEST_HEX_FILE" -> StringParam(testFile))) with HasBlackBoxResource {
  val io = IO(new Bundle {
    val clk = Input(Clock())
    val rst = Input(Bool())
    val imem = Flipped(MemIO(cfg.imemIOType, cfg.addrBits, cfg.dataBits))
    val dmem = Flipped(MemIO(cfg.dmemIOType, cfg.addrBits, cfg.dataBits))
  })

  setResource("/MemModel.v")
}
