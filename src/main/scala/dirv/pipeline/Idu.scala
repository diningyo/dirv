// See LICENSE for license details.

package dirv.pipeline

import chisel3._
import chisel3.util.{Cat, Fill, MuxCase}
import dirv.Config
import dirv.io.{MemCmd, MemIO, MemSize}

trait InstInfo {
  val funct7Msb = 31
  val funct7Lsb = 25
  val funct7Bits = funct7Msb - funct7Lsb + 1
  val rs2Msb = 24
  val rs2Lsb = 20
  val rs2Bits = rs2Msb - rs2Lsb + 1
  val rs1Msb = 19
  val rs1Lsb = 15
  val rs1Bits = rs1Msb - rs1Lsb + 1
  val funct3Msb = 14
  val funct3Lsb = 12
  val funct3Bits = funct3Msb - funct3Lsb + 1
  val rdMsb = 11
  val rdLsb = 7
  val rdBits = rdMsb - rdLsb + 1
  val opCodeMsb = 6
  val opCodeLsb = 0
  val opCodeBits = opCodeMsb - opCodeLsb + 1
}

/**
  * RISC-V instruction
  *  -> Just now, xlen == 32 is only supported
  * @param xlen xlen
  * @param dbg debug opiton. if true, added instruction raw data field.
  */
class Inst(xlen: Int = 32, dbg: Boolean = false) extends Bundle with InstInfo {
  val rawData = if (dbg) Some(UInt(xlen.W)) else None
  val funct7 = UInt(funct7Bits.W)
  val rs2 = UInt(rs2Bits.W)
  val rs1 = UInt(rs1Bits.W)
  val funct3 = UInt(funct3Bits.W)
  val rd = UInt(rdBits.W)
  val opcode = UInt(opCodeBits.W)
  val illegal = Bool()

  // instruction
  // I-system instructions
  val system = Bool()
  val csrrw = Bool()
  val csrrs = Bool()
  val csrrc = Bool()
  val csrrwi = Bool()
  val csrrsi = Bool()
  val csrrci = Bool()
  val csrUimmValid = Bool()
  val csrValid = Bool()

  // I-op-imm instructions
  val opImm = Bool()
  val addi = Bool()
  val slti = Bool()
  val sltiu = Bool()
  val andi = Bool()
  val ori = Bool()
  val xori = Bool()
  val aluValid = Bool()

  // fence
  val fenceValid = Bool()
  val fence = Bool()
  val fenceI = Bool()

  // I-opp jump
  val jalr = Bool()

  // I-op exception
  val excValid = Bool()
  val mret = Bool()
  val ecall = Bool()
  val ebreak = Bool()
  val wfi = Bool()

  // I-op load
  val loadValid = Bool()
  val size = UInt(2.W)
  val lb = Bool()
  val lh = Bool()
  val lw = Bool()
  val lbu = Bool()
  val lhu = Bool()

  // shift
  val slli = Bool()
  val srli = Bool()
  val srai = Bool()
  val shamt = UInt(5.W)
  val shiftValid = Bool()
  val illegalShamt = Bool()
  val arith = Bool()

  // Store
  val valid = Bool()
  val sb = Bool()
  val sh = Bool()
  val sw = Bool()

  // Branch
  val bValid = Bool()
  val branch = Bool()
  val beq = Bool() // branch equal
  val bne = Bool() // branch not equal
  val blt = Bool() // branch less than
  val bge = Bool() // branch greater than or equal
  val bltu = Bool() // branch less than unsigned
  val bgeu = Bool() // branch greater than or equal unsigned

  // U instructions
  val lui = Bool() // load upper immediate
  val auipc = Bool() // add upper immediate to pc

  // J instrcutions
  val jal = Bool()
  val jValid = Bool()

  /**
    * Instruction Decode
    */
  def decode(data: UInt): Unit = {
    funct7 := data(funct3Msb, funct7Lsb)
    rs2 := data(rs2Msb, rs2Lsb)
    rs1 := data(rs1Msb, rs1Lsb)
    funct3 := data(funct3Msb, funct3Lsb)
    rd := data(rdMsb, rdLsb)
    opcode := data(opCodeMsb, opCodeLsb)

    loadInstDecode()

    // system
    systemInstDecode()

    // execption
    excDecode()

    // opImm
    opImmInstDecode()

    jalr := opcode === "b1100111".U
    fenceValid := opcode === "b0001111".U
    // only decode to avoid illegal instruction -> equal to NOP
    fence := (funct3 === "b000".U) && fenceValid
    fenceI := (funct3 === "b001".U) && fenceValid

    valid := csrValid || aluValid || jalr || loadValid || excValid || fenceValid
    illegal := illegalShamt

    // Debug
    if (dbg) rawData.get := data
  }

  def loadInstDecode(): Unit = {
    loadValid := opcode === "b0000011".U
    lb := (funct3 === "b000".U) && loadValid
    lh := (funct3 === "b001".U) && loadValid
    lw := (funct3 === "b010".U) && loadValid
    lbu := (funct3 === "b100".U) && loadValid
    lhu := (funct3 === "b101".U) && loadValid

    valid := opcode === "b0100011".U
    sb := (funct3 === "b000".U) && valid
    sh := (funct3 === "b001".U) && valid
    sw := (funct3 === "b010".U) && valid

    size := MuxCase(MemSize.byte.U, Seq(
      sh -> MemSize.half.U,
      sw -> MemSize.word.U
    ))

    size := MuxCase(MemSize.byte.U, Seq(
      (lh || lhu) -> MemSize.half.U,
      lw -> MemSize.word.U
    ))
  }

  def systemInstDecode(): Unit = {
    system := (opcode === "b1110011".U)
    csrrw := (funct3 === "b001".U) && system
    csrrs := (funct3 === "b010".U) && system
    csrrc := (funct3 === "b011".U) && system
    csrrwi := (funct3 === "b101".U) && system
    csrrsi := (funct3 === "b110".U) && system
    csrrci := (funct3 === "b111".U) && system
    csrUimmValid := csrrwi || csrrsi || csrrci
    csrValid := system && (funct3 =/= "b000".U)
  }

  def excDecode(): Unit = {
    excValid := opcode === "b1110011".U && (funct3 === "b000".U)
    wfi := excValid && rs2(2)
    mret := excValid && rs2(1)
    ecall := excValid && (rs2(1, 0) === "b00".U)
    ebreak := excValid && (rs2(1, 0) === "b01".U)
  }

  def opImmInstDecode(): Unit = {
    opImm := opcode === "b0010011".U
    addi := (funct3 === "b000".U) && opImm
    slti := (funct3 === "b010".U) && opImm
    sltiu := (funct3 === "b011".U) && opImm
    xori := (funct3 === "b100".U) && opImm
    ori := (funct3 === "b110".U) && opImm
    andi := (funct3 === "b111".U) && opImm
    aluValid := opImm
    shiftInstDecode()
  }

  def shiftInstDecode(): Unit = {
    shiftValid := slli || srli || srai
    illegalShamt := !((funct7(11, 5) === "b0000000".U) || (funct7(11, 5) === "b0100000".U)) && shiftValid
    slli := (funct3 === "b001".U) && opImm
    srli := (funct3 === "b101".U) && opImm && (!arith)
    srai := (funct3 === "b101".U) && opImm && arith
    arith := funct7(5)
    shamt := rs2(4, 0)
  }

  def branchDecode(): Unit = {
    bValid  := opcode === "b1100011".U
    beq := (funct3 === "b000".U) && bValid
    bne := (funct3 === "b001".U) && bValid
    blt := (funct3 === "b100".U) && bValid
    bge := (funct3 === "b101".U) && bValid
    bltu := (funct3 === "b110".U) && bValid
    bgeu := (funct3 === "b111".U) && bValid
  }

  def uiDecode(): Unit = {
    lui := opcode === "b0110111".U
    auipc := opcode === "b0010111".U
  }

  def jumpDecode(): Unit = {

    jal := opcode === "b1101111".U
    jValid := jal
  }

  def getCsr: UInt = Cat(funct7, rs2)
  def getAluImm: UInt = Cat(Fill(20, funct7(6)), funct7, rs2)

  def isCsrWr: Bool = csrrw || csrrwi
  def isCsrRd: Bool = csrrs || csrrsi
  def isCsrClr: Bool = csrrc || csrrci

  def immS: UInt = Cat(Fill(20, funct7(6)), funct7, rd)
  def immB: UInt = Cat(Fill(21, funct7(6)), rd(0), funct7(5, 0), rd(4, 1), 0.U(1.W))
  def immU: UInt = Cat(funct7, rs2, Fill(12, 0.U))
  def immJ: UInt = {
    Cat(Fill(13, funct7(6)), rs1, funct3, rs2(0), funct7, rs2(4, 1), 0x0.U(1.W))
  }
}

class Idu(implicit cfg: Config) extends Module with InstInfo {
  val io = IO(new Bundle {
    val ifu2idu = MemIO(cfg.imemIOType, cfg.addrBits, cfg.dataBits)
    val inst2ext = Output(new Inst())
    val pc = if (cfg.dbg) Some(Output(UInt(cfg.addrBits.W))) else None
  })

  val pcReg = RegInit(cfg.initAddr.U)

  pcReg := pcReg + 4.U

  io.ifu2idu.cmd := MemCmd.rd.U
  io.ifu2idu.addr := pcReg
  io.ifu2idu.req := true.B

  val currCmd = Mux(io.ifu2idu.r.get.rddv, io.ifu2idu.r.get.data, 0.U)

  io.inst2ext.opcode := currCmd(opCodeMsb, opCodeLsb)
  io.inst2ext.rd := currCmd(rdMsb, rdLsb)
  io.inst2ext.funct3 := currCmd(funct3Msb, funct3Lsb)
  io.inst2ext.rs1 := currCmd(rs1Msb, rs1Lsb)
  io.inst2ext.rs2 := currCmd(rs2Msb, rs2Lsb)
  io.inst2ext.funct7 := currCmd(funct7Msb, funct7Lsb)

  // debug
  if (cfg.dbg) {
    io.pc.get := pcReg
  }

  /*
  if (cfg.arch == RV32E)
  val codeIsCExt = subOpCode =/= "b000".U
  */
}
