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

  val m_cmd_q = Module(new Queue(chiselTypeOf(io.c.bits), 1, true, true))
  val m_issued_q = Module(new Queue(chiselTypeOf(io.c.bits), 1, true, true))
  val m_wr_q = Module(new Queue(chiselTypeOf(io.w.get.bits), 1, true, true))
  val m_rd_q = Module(new Queue(chiselTypeOf(io.r.get.bits), 1, true, true))

  val w_rand_data = m_glfsr.io.out

  m_cmd_q.io.enq.valid := true.B
  m_cmd_q.io.enq.bits.addr := 0.U
  m_cmd_q.io.enq.bits.size := 0.U
  m_cmd_q.io.enq.bits.cmd := w_rand_data(0)

  m_issued_q.io.enq.valid := m_cmd_q.io.deq.fire()
  m_issued_q.io.enq.bits := m_cmd_q.io.deq.bits

  when (m_issued_q.io.deq.bits.cmd === MbusCmd.rd.U) {
    m_issued_q.io.deq.ready := m_wr_q.io.deq.valid
  } .otherwise {
    m_issued_q.io.deq.ready := m_rd_q.io.deq.valid
  }

  m_wr_q.io.enq.valid := true.B
  m_wr_q.io.enq.bits.data := 0.U

  m_rd_q.io.enq.valid := true.B
  m_rd_q.io.enq.bits.data := 0.U

  m_cmd_q.io.enq.valid := true.B
  m_cmd_q.io.enq.bits.addr := 0.U
  m_cmd_q.io.enq.bits.size := 0.U
  m_cmd_q.io.enq.bits.cmd := 0.U


  io.c <> m_cmd_q.io.deq
  io.w.get <> m_wr_q.io.deq
  io.r.get <> m_rd_q.io.deq
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
