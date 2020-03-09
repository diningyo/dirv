// See LICENSE for license details.

package mbus

import chisel3._
import chisel3.util._
import chisel3.util.random.MaxPeriodGaloisLFSR
import test.util._

class Xtor(ioType: MbusIOAttr, addrBits: Int, dataBits: Int)(randBits: Int, seed: Int) extends Module {
  val io = MbusIO(ioType, addrBits, dataBits)

  val m_glfsr = Module(new MaxPeriodGaloisLFSR(randBits, Some(1)))

  val r_rand_valid = RegInit(false.B)

  r_rand_valid := true.B

  m_glfsr.io.seed.valid := r_rand_valid
  m_glfsr.io.seed.bits := VecInit(seed.U.asBools())

  val w_rand_data = m_glfsr.io.out

  io.c.valid := true.B
  io.c.bits := true.B

}

/**
  * Testbench top module for MbusIC
  * @param limit Maximum cycles of simulation.
  * @param abortEn True if simulation will finish, when timeout is occurred.
  */
class SimDTMMbusIC(val p: MbusICParams)
                  (
  limit: Int,
  abortEn: Boolean = true
) extends BaseSimDTM(limit, abortEn) {
  val io = IO(new Bundle with BaseSimDTMIO {
    val dut = new MbusICIO(p)
  })

  val dut = Module(new MbusIC(p))

  io.dut <> dut.io

  connect(false.B)
}
