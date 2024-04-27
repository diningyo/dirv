// See LICENSE for license details.

package dirv.pipeline

import chisel3._
import chisel3.util._
import dirv.Config
import mbus._
/**
  * Load store unit <-> Execution unit I/O
  * @param cfg dirv's configuration parameter.
  */
class Lsu2ExuIO(implicit val cfg: Config) extends Bundle {
  val stallReq = Output(Bool())
  val loadDataValid = Output(Bool())
  val loadData = Output(UInt(cfg.dataBits.W))
  val excWrMa = Output(new ExcMa())
  val excRdMa = Output(new ExcMa())
}


/**
  * Load store unit's I/O
  * @param cfg dirv's configuration parameter.
  */
class LsuIO(implicit val cfg: Config) extends Bundle {
  val exu2lsu = Flipped(new Exu2LsuIO)
  val lsu2exu = new Lsu2ExuIO
  val lsu2ext = MbusIO(cfg.dmemIOType, cfg.addrBits, cfg.dataBits)
}


/**
  * Load store unit
  * @param cfg dirv's configuration parameter.
  */
class Lsu(implicit cfg: Config) extends Module {

  val io = IO(new LsuIO())

  /**
    * Check data address is misaligned or not.
    * @param dataReq data request to memory
    * @param addr requested data address
    * @param size requested data size
    * @return if address is misaligned, return true.B
    */
  def setMaAddr(dataReq: Bool, addr: UInt, size: UInt): ExcMa = {
    val uaMsb = log2Ceil(cfg.arch.xlen / 8)
    val uaAddr = addr(uaMsb - 1, 0)

    val excReq = MuxCase(false.B, Seq(
      (size === MbusSize.half.U) -> uaAddr(0).asBool,
      (size === MbusSize.word.U) -> (uaAddr =/= "b00".U)
    ))

    val ret = Wire(new ExcMa())

    ret.excReq := excReq && dataReq
    ret.excAddr := addr

    ret
  }

  // alias
  val ext = io.lsu2ext
  val extR = ext.r.get
  val extW = ext.w.get

  val sIdle :: sCmdWait :: sRdataWait :: sWriteWait :: Nil = Enum(4)
  val fsm = RegInit(sIdle)

  val inst = io.exu2lsu.inst
  val size = Mux1H(Seq(
    (inst.sb || inst.lb || inst.lbu) -> MbusSize.byte.U,
    (inst.sh || inst.lh || inst.lhu) -> MbusSize.half.U,
    (inst.sw || inst.lw) -> MbusSize.word.U
  ))

  // make strobe signal
  val byteNum = cfg.arch.xlen / 8
  val uaMsb = log2Ceil(byteNum)
  val unaligedAddr = io.exu2lsu.memAddr(uaMsb - 1, 0)
  val strb = Mux1H(Seq(
    inst.sb -> Fill(0x1 << MbusSize.byte, 1.U),
    inst.sh -> Fill(0x1 << MbusSize.half, 1.U),
    inst.sw -> Fill(0x1 << MbusSize.word, 1.U)
  )) << unaligedAddr

  val wrdataVec = Wire(Vec(byteNum, UInt(8.W)))
  val loadData = Wire(Vec(byteNum, UInt(8.W)))

  // bits -> bytes
  (0 until byteNum).foreach(i => {
    loadData(i) := extR.bits.data(((i + 1) * 8) - 1, i * 8)
    wrdataVec(i) := io.exu2lsu.memWrdata(((i + 1) * 8) - 1, i * 8)
  })

  // write data
  val shiftNum = (unaligedAddr << 3.U).asUInt
  val wrdata = Mux1H(Seq(
    inst.sb -> io.exu2lsu.memWrdata(7, 0),
    inst.sh -> io.exu2lsu.memWrdata(15, 0),
    inst.sw -> io.exu2lsu.memWrdata
  )) << shiftNum

  // access done
  val rdDone = extR.valid && extR.ready
  val wrDone = extW.valid && extW.ready

  // fsm
  switch (fsm) {
    is (sIdle) {
      when (ext.c.valid) {
        when (ext.c.ready) {
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
      when (ext.c.valid && ext.c.ready) {
        when(inst.storeValid && !(extW.valid && extW.ready)) {
          fsm := sWriteWait
        } .elsewhen (inst.loadValid && !(extR.valid && extR.ready)) {
          fsm := sRdataWait
        }
      }
    }

    is (sRdataWait) {
      when (rdDone) {
        fsm := sIdle
      }
    }

    is (sWriteWait) {
      when(wrDone) {
        fsm := sIdle
      }
    }
  }

  // lsu -> exu
  val extAccessReq = !(io.lsu2exu.excRdMa.excReq || io.lsu2exu.excWrMa.excReq) && (inst.loadValid || inst.storeValid)
  io.lsu2exu.excRdMa := setMaAddr(inst.loadValid, io.exu2lsu.memAddr, size)
  io.lsu2exu.excWrMa := setMaAddr(inst.storeValid, io.exu2lsu.memAddr, size)
  io.lsu2exu.stallReq := ((extAccessReq && (fsm === sIdle)) && !wrDone) || ((fsm =/= sIdle) && !(wrDone || rdDone))
  io.lsu2exu.loadDataValid :=  extR.valid && extR.ready
  io.lsu2exu.loadData := Mux1H(Seq(
    (inst.lb || inst.lbu) -> Cat(Fill(24, inst.lb && loadData(unaligedAddr)(7)), loadData(unaligedAddr)),
    (inst.lh || inst.lhu) -> Cat(Fill(16, inst.lh && loadData(unaligedAddr + 1.U)(7)),
      Cat(loadData(unaligedAddr + 1.U), loadData(unaligedAddr))),
    inst.lw -> Cat(loadData.reverse)
  ))

  // lsu -> external
  ext.c.valid := extAccessReq && ((fsm === sIdle) || (fsm === sCmdWait))
  ext.c.bits.addr := io.exu2lsu.memAddr
  ext.c.bits.size := size
  ext.c.bits.cmd := Mux(inst.loadValid, MbusCmd.rd.U, MbusCmd.wr.U)
  extW.bits.data := wrdata
  extW.bits.strb := strb
  extW.valid := io.exu2lsu.inst.storeValid
  extR.ready := true.B
}
