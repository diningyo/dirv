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
}

/**
  *
  * @param cfg dirv's configuration parameter.
  */
class Ifu2exuIO(implicit cfg: Config) extends Bundle {
  val excInstMa = Output(new ExcMa())
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
}


/**
  *
  * @param cfg dirv's configuration parameter.
  */
class Ifu(implicit cfg: Config) extends Module {
  val io = IO(new IfuIO())

  val sIdle :: sFetch :: Nil = Enum(2)

  //
  val initPcSeq = Seq.fill(3)(RegInit(false.B))
  val initPc = !initPcSeq(2) && initPcSeq(1)

  when (!initPcSeq.reduce(_ && _)) {
    (Seq(true.B) ++ initPcSeq).zip(initPcSeq).foreach{case (c, n) => n := c}
  }

  val qIssuedCtr = RegInit(0.U(2.W))

  when (io.ifu2ext.c.fire && io.ifu2ext.r.get.fire) {
    qIssuedCtr := qIssuedCtr
  } .elsewhen(io.ifu2ext.c.fire) {
    qIssuedCtr := qIssuedCtr + 1.U
  } .elsewhen(io.ifu2ext.r.get.fire) {
    qIssuedCtr := qIssuedCtr - 1.U
  }

  val qInstFlush = io.exu2ifu.updatePcReq
  val qInstWren = Wire(Bool())
  val qInstRden = Wire(Bool())
  val qInstRdPtr = RegInit(0.U(1.W))
  val qInstWrPtr = RegInit(0.U(1.W))
  val qInstDataNum = RegInit(0.U(2.W))
  val qInstHasData = Wire(Bool())
  val qInst = RegInit(VecInit(Seq.fill(2)(0.U(cfg.dataBits.W))))

  val qInvalidReq = RegInit(0.U(2.W))

  val imemAddrReg = RegInit(cfg.initAddr.U)
  val fsm = RegInit(sIdle)

  when (io.exu2ifu.updatePcReq) {
    imemAddrReg := io.exu2ifu.updatePc
  } .otherwise {
    when (io.ifu2ext.c.fire) {
      imemAddrReg := imemAddrReg + 4.U
    }
  }

  // for prevending invalid instruction puts qInst.
  when (io.exu2ifu.updatePcReq) {
    qInvalidReq := qIssuedCtr
  } .elsewhen((qInvalidReq =/= 0.U) && io.ifu2ext.r.get.fire) {
    qInvalidReq := qInvalidReq - 1.U
  }

  // Instruction FIFO
  val qInstIsFull = Wire(Bool())
  qInstWren := io.ifu2ext.r.get.valid && (qInvalidReq === 0.U) && (!qInstIsFull)
  qInstRden := io.ifu2idu.valid && io.ifu2idu.ready

  when (qInstFlush) {
    qInst(0) := dirv.Constants.nop
  } .elsewhen (qInstWren) {
    qInst(qInstWrPtr) := io.ifu2ext.r.get.bits.data
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
  } .elsewhen (qInstRden && qInstWren) {
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
      when ((initPc || io.exu2ifu.updatePcReq) & !io.exu2ifu.stopFetch) {
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
  io.ifu2ext.c.valid := (fsm === sFetch)
  io.ifu2ext.c.bits.addr := imemAddrReg
  io.ifu2ext.c.bits.cmd := MbusCmd.rd.U
  io.ifu2ext.c.bits.size := MbusSize.word.U
  io.ifu2ext.r.get.ready := !qInstIsFull

  // IFU <-> IDU
  io.ifu2idu.valid := qInstHasData
  io.ifu2idu.inst := qInst(qInstRdPtr)
}
