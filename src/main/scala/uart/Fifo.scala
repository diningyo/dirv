// See LICENSE for license details.

package uart

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
  val rdPtr = Output(UInt(depthBits.W))
  val wrPtr = Output(UInt(depthBits.W))
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

  val fifo = RegInit(VecInit(Seq.fill(depth)(0.U(8.W))))
  val rdPtr = RegInit(0.U(depthBits.W))
  val wrPtr = RegInit(0.U(depthBits.W))
  val dataCount = RegInit(0.U((depthBits + 1).W))

  when(io.rst) {
    wrPtr := 0.U
  }.elsewhen(io.wr.enable) {
    fifo(wrPtr) := io.wr.data
    wrPtr := Mux(wrPtr === (depth - 1).U, 0.U, wrPtr + 1.U)
  }

  when(io.rst) {
    rdPtr := 0.U
  }.elsewhen(io.rd.enable) {
    rdPtr := Mux(rdPtr === (depth - 1).U, 0.U, rdPtr + 1.U)
  }

  when (io.rst) {
    dataCount := 0.U
  } .elsewhen (io.wr.enable && io.rd.enable) {
    dataCount := dataCount
  } .otherwise {
    when (io.wr.enable) {
      when (dataCount === depth.U) {
        dataCount := 0.U
      } .otherwise {
        dataCount := dataCount + 1.U
      }
    }
    when (io.rd.enable) {
      dataCount := dataCount - 1.U
    }
  }

  io.full := dataCount === depth.U
  io.rd.empty := dataCount === 0.U
  io.rd.data := fifo(rdPtr)

  // debug
  if (debug) {
    val dbg = io.dbg.get
    dbg.rdPtr := rdPtr
    dbg.wrPtr := wrPtr
    dbg.count := dataCount
    dbg.fifo := fifo
  }
}