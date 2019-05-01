// See LICENSE for license details.

package dirv.pipeline

import chisel3._
import chisel3.util._
import dirv.Config
import dirv.io.{MemCmd, MemIO, MemSize}

/**
  * Load store unit <-> Execution unit I/O
  * @param cfg dirv's configuration parameter.
  */
class Lsu2ExuIO(implicit val cfg: Config) extends Bundle {
  val stallReq = Output(Bool())
  val loadDataVaid = Output(Bool())
  val loadData = Output(UInt(cfg.dataBits.W))
}


/**
  * Load store unit's I/O
  * @param cfg dirv's configuration parameter.
  */
class LsuIO(implicit val cfg: Config) extends Bundle {
  val exu2lsu = Flipped(new Exu2LsuIO)
  val lsu2exu = new Lsu2ExuIO
  val lsu2ext = MemIO(cfg.dmemIOType, cfg.addrBits, cfg.dataBits)
}


/**
  * Load store unit
  * @param cfg dirv's configuration parameter.
  */
class Lsu(implicit cfg: Config) extends Module {
  val io = IO(new LsuIO())

  // alias
  val ext = io.lsu2ext
  val extR = ext.r.get
  val extW = ext.w.get

  val sIdle :: sCmdWait :: sRdataWait :: sWriteWait :: Nil = Enum(4)
  val fsm = RegInit(sIdle)

  val inst = io.exu2lsu.inst
  val size = Mux1H(Seq(
    (inst.sb || inst.lb || inst.lbu) -> MemSize.byte.U,
    (inst.sh || inst.lh || inst.lhu) -> MemSize.half.U,
    (inst.sw || inst.lw) -> MemSize.byte.U
  ))

  val uaMsb = log2Ceil(cfg.arch.xlen / 8)
  val unaligedAddr = io.exu2lsu.memAddr(uaMsb - 1, 0)
  val strb = Mux1H(Seq(
    inst.sb -> Fill(0x1 << MemSize.byte, 1.U),
    inst.sh -> Fill(0x1 << MemSize.half, 1.U),
    inst.sw -> Fill(0x1 << MemSize.word, 1.U)
  )) << unaligedAddr

  // fsm
  switch (fsm) {
    is (sIdle) {
      when (ext.valid) {
        when (ext.ready) {
          when(inst.storeValid && !(extW.valid && extW.ready)) {
            fsm := sWriteWait
          } .elsewhen (inst.loadValid && !(extR.valid && extR.ready)) {
            fsm := sRdataWait
          }
        } .otherwise {
          fsm := sCmdWait
        }
      }
    }

    is (sCmdWait) {
      when (ext.valid && ext.ready) {
        when(inst.storeValid && !(extW.valid && extW.ready)) {
          fsm := sWriteWait
        } .elsewhen (inst.loadValid && !(extR.valid && extR.ready)) {
          fsm := sRdataWait
        }
      }
    }

    is (sRdataWait) {
      when (extR.valid && extR.ready) {
        fsm := sIdle
      }
    }

    is (sWriteWait) {
      when(extW.valid && extW.ready) {
        fsm := sIdle
      }
    }
  }

  // lsu -> exu
  io.lsu2exu.stallReq := (fsm =/= sIdle)
  io.lsu2exu.loadDataVaid := extR.valid && extR.ready
  io.lsu2exu.loadData := extR.data

  // lsu -> external
  ext.valid := inst.loadValid || inst.storeValid
  ext.addr := io.exu2lsu.memAddr
  ext.size := size
  ext.cmd := Mux(inst.loadValid, MemCmd.rd.U, MemCmd.wr.U)
  extW.data := 0xdeafbeafL.U
  extW.strb := strb
  extW.valid := io.exu2lsu.inst.storeValid
  extR.ready := true.B
}
