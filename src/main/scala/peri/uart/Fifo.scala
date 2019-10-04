// See LICENSE for license details.

package peri.uart

import chisel3._
import chisel3.util._

/**
  * Fifo read I/O
  */
class FifoRdIO extends Bundle {
  val enable = Input(Bool())
  val empty = Output(Bool())
  val data = Output(UInt(8.W))
}

/**
  * Fifo write I/O
  */
class FifoWrIO extends Bundle {
  val enable = Input(Bool())
  val data = Input(UInt(8.W))
}

/**
  * Fifo debug I/O
  * @param depth fifo depth
  */
class dbgFifoIO(depth: Int) extends Bundle {
  val depthBits = log2Ceil(depth)
  val rdptr = Output(UInt(depthBits.W))
  val wrptr = Output(UInt(depthBits.W))
  val count = Output(UInt((depthBits + 1).W))
  val fifo = Output(Vec(depth, UInt(32.W)))
}


/**
  * Fifo I/O
  * @param depth fifo depth
  * @param debug debug option, if true I/O has debug ports
  */
class FifoIO(depth: Int=16, debug: Boolean=false) extends Bundle {
  val wr = new FifoWrIO()
  val rd = new FifoRdIO()
  val rst = Input(Bool())
  val full = Output(Bool())
  val dbg = if (debug) Some(new dbgFifoIO(depth)) else None

  override def cloneType: this.type = new FifoIO(depth, debug).asInstanceOf[this.type]
}

/**
  * Fifo for Uart
  * @param depth fifo depth
  * @param debug debug option, if true I/O has debug ports
  */
class Fifo(depth: Int=16, debug: Boolean=false) extends Module {
  val io = IO(new FifoIO(depth, debug))

  // parameter
  val depthBits = log2Ceil(depth)

  val r_fifo = RegInit(VecInit(Seq.fill(depth)(0.U(8.W))))
  val r_rdptr = RegInit(0.U(depthBits.W))
  val r_wrptr = RegInit(0.U(depthBits.W))
  val r_data_ctr = RegInit(0.U((depthBits + 1).W))

  when(io.rst) {
    r_wrptr := 0.U
  }.elsewhen(io.wr.enable) {
    r_fifo(r_wrptr) := io.wr.data
    r_wrptr := Mux(r_wrptr === (depth - 1).U, 0.U, r_wrptr + 1.U)
  }

  when(io.rst) {
    r_rdptr := 0.U
  }.elsewhen(io.rd.enable) {
    r_rdptr := Mux(r_rdptr === (depth - 1).U, 0.U, r_rdptr + 1.U)
  }

  when (io.rst) {
    r_data_ctr := 0.U
  } .elsewhen (io.wr.enable && io.rd.enable) {
    r_data_ctr := r_data_ctr
  } .otherwise {
    when (io.wr.enable) {
      when (r_data_ctr === depth.U) {
        r_data_ctr := 0.U
      } .otherwise {
        r_data_ctr := r_data_ctr + 1.U
      }
    }
    when (io.rd.enable) {
      r_data_ctr := r_data_ctr - 1.U
    }
  }

  io.full := r_data_ctr === depth.U
  io.rd.empty := r_data_ctr === 0.U
  io.rd.data := r_fifo(r_rdptr)

  // debug
  if (debug) {
    val dbg = io.dbg.get
    dbg.rdptr := r_rdptr
    dbg.wrptr := r_wrptr
    dbg.count := r_data_ctr
    dbg.fifo := r_fifo
  }
}