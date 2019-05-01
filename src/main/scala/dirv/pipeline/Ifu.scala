// See LICENSE for license details.

package dirv.pipeline

import chisel3._
import chisel3.util._
import dirv.Config
import dirv.io.{MemCmd, MemIO, MemSize}


/**
  *
  * @param cfg dirv's configuration parameter.
  */
class Ifu2IduIO(implicit cfg: Config) extends Bundle {
  val valid = Output(Bool())
  val ready = Input(Bool())
  val inst = Output(UInt(cfg.arch.xlen.W))

  override def cloneType: this.type = new Ifu2IduIO().asInstanceOf[this.type]
}

/**
  *
  * @param cfg dirv's configuration parameter.
  */
class Ifu2exuIO(implicit cfg: Config) extends Bundle {
  val valid = Output(Bool())

  override def cloneType: this.type = new Ifu2IduIO().asInstanceOf[this.type]
}

/**
  *
  * @param cfg dirv's configuration parameter.
  */
class IfuIO(implicit cfg: Config) extends Bundle {
  val ifu2ext = MemIO(cfg.imemIOType, cfg.addrBits, cfg.dataBits)
  val ifu2idu = new Ifu2IduIO()
  val exu2ifu = Flipped(new Exu2IfuIO())

  override def cloneType: this.type = new IfuIO().asInstanceOf[this.type]
}


/**
  *
  * @param cfg dirv's configuration parameter.
  */
class Ifu(implicit cfg: Config) extends Module {
  val io = IO(new IfuIO())

  val sIdle :: sFetch :: Nil = Enum(2)

  val hasCmd = RegInit(false.B)
  val active = RegInit(false.B)
  val fetchValid = Wire(Bool())
  val fetchInst = Wire(UInt(cfg.dataBits.W))
  val fetchInstBuf = RegInit(0.U(cfg.dataBits.W))

  val imemAddrReg = RegInit(cfg.initAddr.U)
  val fsm = RegInit(sIdle)

  //
  active := true.B

  when (io.ifu2ext.valid && io.ifu2ext.ready) {
    when (io.exu2ifu.updatePcReq) {
      imemAddrReg := io.exu2ifu.updatePc + 4.U
    } .otherwise {
      imemAddrReg := imemAddrReg + 4.U
    }
  }

  // fetch
  fetchValid := io.ifu2ext.r.get.valid && io.ifu2ext.r.get.ready
  fetchInst := Mux(fetchValid, io.ifu2ext.r.get.data, fetchInstBuf)

  when (io.ifu2idu.ready) {
    fetchInstBuf := io.ifu2ext.r.get.data
  }

  // state
  switch (fsm) {
    is (sIdle) {
      when (io.exu2ifu.updatePcReq & !io.exu2ifu.stopFetch) {
        fsm := sFetch
      }
    }
    is (sFetch) {
      when (io.exu2ifu.stopFetch) {
        fsm := sIdle
      }
    }
  }

  //
  //
  //

  // External <-> IFU
  io.ifu2ext.valid := fsm === sFetch
  io.ifu2ext.addr := Mux(io.exu2ifu.updatePcReq, io.exu2ifu.updatePc, imemAddrReg)
  io.ifu2ext.cmd := MemCmd.rd.U
  io.ifu2ext.size := MemSize.word.U
  io.ifu2ext.r.get.ready := io.ifu2idu.ready

  // IFU <-> IDU
  io.ifu2idu.valid := fetchValid
  io.ifu2idu.inst := Mux(fetchValid, fetchInst, fetchInstBuf)
}
