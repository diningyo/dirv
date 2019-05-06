// See LICENSE for license details.

package dirv.pipeline

import chisel3._
import chisel3.util._
import dirv.Config


class Exu2IfuIO(implicit cfg: Config) extends Bundle {
  val updatePcReq = Output(Bool())
  val updatePc = Output(UInt(cfg.arch.xlen.W))
  val stopFetch = Output(Bool())

  override def cloneType: this.type = new Exu2IfuIO().asInstanceOf[this.type]

}

class Exu2LsuIO(implicit cfg: Config) extends Bundle {
  val memAddr = Output(UInt(cfg.addrBits.W))
  val memWrdata = Output(UInt(cfg.dataBits.W))
  val inst = Output(new InstRV32())

  override def cloneType: this.type = new Exu2LsuIO().asInstanceOf[this.type]
}

/**
  * Execution Unit
  * @param cfg dirv's configuration parameter.
  */
class Exu(implicit cfg: Config) extends Module{
  val io = IO(new Bundle {
    val idu2exu = Flipped(new Idu2ExuIO())
    val exu2ifu = new Exu2IfuIO()
    val exu2lsu = new Exu2LsuIO()
    val lsu2exu = Flipped(new Lsu2ExuIO)
    val inst = if (cfg.dbg) Some(Output(UInt(cfg.arch.xlen.W))) else None
    val pc = if (cfg.dbg) Some(Output(UInt(cfg.arch.xlen.W))) else None
    val xregs = if (cfg.dbg) Some(Output(Vec(cfg.arch.regNum, UInt(cfg.arch.xlen.W)))) else None
  })

  // Module Instance
  val alu = Module(new Alu)
  val mpfr = Module(new Mpfr)
  val csrf = Module(new Csrf)

  // Alias
  val instExe :: instMem :: instWb :: Nil = Seq.fill(3)(io.idu2exu.inst.bits)

  // PC
  val currPc = RegInit(cfg.initAddr.U)

  //
  val updatePcReq = Wire(Bool())
  val initPcSeq = Seq.fill(3)(RegInit(false.B))
  val initPc = !initPcSeq(2) && initPcSeq(1)

  when (!initPcSeq.reduce(_ && _)) {
    (Seq(true.B) ++ initPcSeq).zip(initPcSeq).foreach{case (c, n) => n := c}
  }

  updatePcReq := initPc

  when (io.idu2exu.inst.valid && io.idu2exu.inst.ready) {
    when (io.exu2ifu.updatePcReq) {
      currPc := io.exu2ifu.updatePc
    } .otherwise {
      currPc := currPc + 4.U
    }
  }

  // dependancy check
  /*
  val invalidRdWb = instWb.rd === 0.U
  val depRs1Rd = (instExe.rs1 === instWb.rd) && invalidRdWb
  val depRs2Rd = (instExe.rs2 === instWb.rd) && invalidRdWb
  */
  // branch control
  val condBranchValid = Wire(Bool())

  condBranchValid := MuxCase(false.B, Seq(
    instExe.beq -> (mpfr.io.rs1.data === mpfr.io.rs2.data),
    instExe.bne -> (mpfr.io.rs1.data =/= mpfr.io.rs2.data),
    instExe.blt -> (mpfr.io.rs1.data.asSInt() < mpfr.io.rs2.data.asSInt()),
    instExe.bge -> (mpfr.io.rs1.data.asSInt() >= mpfr.io.rs2.data.asSInt()),
    instExe.bltu -> (mpfr.io.rs1.data < mpfr.io.rs2.data),
    instExe.bgeu -> (mpfr.io.rs1.data >= mpfr.io.rs2.data)
  ))


  // illegal
  val illegal = instExe.illegal || (!instExe.valid) && io.idu2exu.inst.valid
  val excDataMaReq = io.lsu2exu.excWrMa.excReq || io.lsu2exu.excRdMa.excReq
  val excReq = illegal || excDataMaReq || instExe.ecall || instExe.ebreak

  val jmpPcReq = instExe.ebreak || instExe.jal || instExe.jalr ||
    condBranchValid || instExe.mret || instExe.wfi || excReq
  val jmpPc = Mux1H(Seq(
    excReq -> csrf.io.mtvec,
    (instExe.mret || instExe.wfi) -> csrf.io.mepc,
    instExe.jal -> (currPc + instExe.immJ),
    instExe.jalr -> ((mpfr.io.rs1.data + instExe.immI) & (~1.U(cfg.arch.xlen.W)).asUInt()),
    condBranchValid -> (currPc + instExe.immB)
  ))(cfg.addrBits - 1, 0)

  // connect Mpfr
  mpfr.io.rs1.addr := instExe.rs1
  mpfr.io.rs2.addr := instExe.rs2

  // data to Alu
  val aluRs1Data = Wire(UInt(cfg.dataBits.W))
  val aluRs2Data = Wire(UInt(cfg.dataBits.W))

  aluRs1Data := mpfr.io.rs1.data
  aluRs2Data := mpfr.io.rs2.data

  alu.io.inst := instExe
  alu.io.pc := currPc
  alu.io.rs1 := aluRs1Data
  alu.io.rs2 := aluRs2Data

  // csr
  csrf.io.inst := instWb
  csrf.io.invalidWb := false.B
  csrf.io.excRdMa := io.lsu2exu.excRdMa
  csrf.io.excWrMa := io.lsu2exu.excWrMa
  csrf.io.excMepcWren := false.B
  csrf.io.excPc := currPc
  csrf.io.excCode := 0.U
  csrf.io.trapOccured := false.B
  csrf.io.trapAddr := 0.U
  csrf.io.wrData := alu.io.result

  // mem

  // wb
  mpfr.io.rd.en := (!illegal) && (instWb.aluValid || instWb.csrValid ||
    io.lsu2exu.loadDataValid || instWb.jal || instWb.jalr)
  mpfr.io.rd.addr := instWb.rd
  mpfr.io.rd.data := MuxCase(0.U, Seq(
    instWb.loadValid -> io.lsu2exu.loadData,
    (instWb.aluValid || instWb.jal || instWb.jalr) -> alu.io.result,
    instWb.csrValid -> csrf.io.rdData
  ))

  //
  io.idu2exu.inst.ready := !io.lsu2exu.stallReq

  //
  io.exu2ifu.updatePcReq := jmpPcReq || updatePcReq
  io.exu2ifu.updatePc := jmpPc
  io.exu2ifu.stopFetch := false.B

  //
  io.exu2lsu.memAddr := alu.io.result
  io.exu2lsu.memWrdata := mpfr.io.rs2.data
  io.exu2lsu.inst := instMem

  // debug
  if (cfg.dbg) {
    io.pc.get := currPc
    io.inst.get := instExe.rawData.get
    io.xregs.get := mpfr.io.xregs.get
  }
}
