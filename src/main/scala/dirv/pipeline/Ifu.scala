// See LICENSE for license details.

package dirv.pipeline

import chisel3._
import chisel3.util._
import dirv.Config
import mbus._


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
  val excInstMa = Output(new ExcMa())

  override def cloneType: this.type = new Ifu2exuIO().asInstanceOf[this.type]
}

/**
  *
  * @param cfg dirv's configuration parameter.
  */
class IfuIO(implicit cfg: Config) extends Bundle {
  val ifu2ext = MbusIO(cfg.imemIOType, cfg.addrBits, cfg.dataBits)
  val ifu2idu = new Ifu2IduIO()
 // val ifu2exu = new Ifu2exuIO()
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

  val qInstFlush = io.exu2ifu.updatePcReq
  val qInstWren = Wire(Bool())
  val qInstRden = Wire(Bool())
  val qInstRdPtr = RegInit(0.U(1.W))
  val qInstWrPtr = RegInit(0.U(1.W))
  val qInstDataNum = RegInit(0.U(2.W))
  val qInstHasData = Wire(Bool())
  val qInst = RegInit(VecInit(Seq.fill(2)(0.U(cfg.dataBits.W))))

  val imemAddrReg = RegInit(cfg.initAddr.U)
  val fsm = RegInit(sIdle)

  when (qInstWren) {
    when (io.exu2ifu.updatePcReq) {
      imemAddrReg := io.exu2ifu.updatePc + 4.U
    } .otherwise {
      imemAddrReg := imemAddrReg + 4.U
    }
  }

  // Instruction FIFO
  val qInstIsFull = Wire(Bool())
  qInstWren := io.ifu2ext.r.get.valid && (!qInstIsFull)
  qInstRden := io.ifu2idu.valid && io.ifu2idu.ready

  when (qInstFlush) {
    qInst(0) := dirv.Constants.nop
  } .elsewhen (qInstWren) {
    qInst(qInstWrPtr) := io.ifu2ext.r.get.data
  }

  when (qInstFlush) {
    qInstRdPtr := 0.U
    qInstWrPtr := 0.U
  } .otherwise {
    when (qInstWren) {
      qInstWrPtr := qInstWrPtr + 1.U
    }
    when (qInstRden) {
      qInstRdPtr := qInstRdPtr + 1.U
    }
  }

  when (qInstFlush) {
    qInstDataNum := 0.U
  } .elsewhen (qInstRden && qInstRden) {
    qInstDataNum := qInstDataNum
  } .elsewhen (qInstRden) {
    qInstDataNum := qInstDataNum - 1.U
  } .elsewhen (qInstWren) {
    qInstDataNum := qInstDataNum + 1.U
  }

  qInstIsFull := (qInstDataNum === 1.U) && !qInstRden
  qInstHasData := qInstDataNum =/= 0.U

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

  // External <-> IF
  io.ifu2ext.valid := (fsm === sFetch)
  io.ifu2ext.addr := Mux(io.exu2ifu.updatePcReq, io.exu2ifu.updatePc, imemAddrReg)
  io.ifu2ext.cmd := MbusCmd.rd.U
  io.ifu2ext.size := MbusSize.word.U
  io.ifu2ext.r.get.ready := !qInstIsFull

  // IFU <-> IDU
  io.ifu2idu.valid := qInstHasData
  io.ifu2idu.inst := qInst(qInstRdPtr)
}
