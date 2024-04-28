// See LICENSE for license details.

package dirv.pipeline

import chisel3._
import chisel3.util.{Cat, Fill, Irrevocable}
import dirv.Config

/**
  * Trait for RV32I bitmap
  */
trait InstInfoRV32 {
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
  * RISC-V RV32 instructions
  *  -> Just now, xlen == 32 is only supported
  * @param xlen xlen
  * @param cfg dirv's configuration parameter.
  */
class InstRV32(xlen: Int = 32)(implicit val cfg: Config)
  extends Bundle with InstInfoRV32 {

  val rawData = if (cfg.dbg) Some(UInt(xlen.W)) else None
  val funct7 = UInt(funct7Bits.W)
  val rs2 = UInt(rs2Bits.W)
  val rs1 = UInt(rs1Bits.W)
  val funct3 = UInt(funct3Bits.W)
  val rd = UInt(rdBits.W)
  val opcode = UInt(opCodeBits.W)
  val illegal = Bool()
  val valid = Bool()
  val illegalOpcode = Bool()

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
  val lb = Bool()
  val lh = Bool()
  val lw = Bool()
  val lbu = Bool()
  val lhu = Bool()

  // R-alu
  val arith = Bool()
  val aluReg = Bool()
  val add = Bool()
  val sub = Bool()
  val sll = Bool()
  val slt = Bool()
  val sltu = Bool()
  val xor = Bool()
  val srl = Bool()
  val sra = Bool()
  val or = Bool()
  val and = Bool()
  val aluValid = Bool()

  // I-op-imm instructions
  val aluImm = Bool()
  val addi = Bool()
  val slti = Bool()
  val sltiu = Bool()
  val andi = Bool()
  val ori = Bool()
  val xori = Bool()

  // shift
  val slli = Bool()
  val srli = Bool()
  val srai = Bool()
  val shamt = UInt(5.W)
  val shiftValid = Bool()
  val illegalShamt = Bool()

  // Store
  val storeValid = Bool()
  val sb = Bool()
  val sh = Bool()
  val sw = Bool()

  // Branch
  val bValid = Bool()
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

  val throughAlu = Bool()

  /**
    * Instruction Decode
    */
  def decode(dataValid: Bool, data: UInt): Unit = {
    funct7 := data(funct7Msb, funct7Lsb)
    rs2 := data(rs2Msb, rs2Lsb)
    rs1 := data(rs1Msb, rs1Lsb)
    funct3 := data(funct3Msb, funct3Lsb)
    rd := data(rdMsb, rdLsb)
    opcode := data(opCodeMsb, opCodeLsb)

    jalr := opcode === "b1100111".U
    fenceValid := opcode === "b0001111".U
    // only decode to avoid illegal instruction -> equal to NOP
    fence := (funct3 === "b000".U) && fenceValid
    fenceI := (funct3 === "b001".U) && fenceValid

    loadInstDecode()
    systemInstDecode()
    excDecode()
    aluInstDecode()
    branchDecode()
    jumpDecode()
    uiDecode()

    throughAlu := lui
    valid := csrValid || aluValid || jalr || loadValid || excValid ||
      fenceValid || jValid || storeValid || lui || auipc || bValid
    illegalOpcode := !valid && dataValid
    illegal := illegalShamt || illegalOpcode

    // Debug
    if (cfg.dbg) rawData.get := data
  }

  def loadInstDecode(): Unit = {
    loadValid := opcode === "b0000011".U
    lb := (funct3 === "b000".U) && loadValid
    lh := (funct3 === "b001".U) && loadValid
    lw := (funct3 === "b010".U) && loadValid
    lbu := (funct3 === "b100".U) && loadValid
    lhu := (funct3 === "b101".U) && loadValid

    storeValid := opcode === "b0100011".U
    sb := (funct3 === "b000".U) && storeValid
    sh := (funct3 === "b001".U) && storeValid
    sw := (funct3 === "b010".U) && storeValid
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

  def aluInstDecode(): Unit = {
    // register
    aluReg := opcode === "b0110011".U
    add := (funct3 === "b000".U) && aluReg && (!arith)
    sub := (funct3 === "b000".U) && aluReg && arith
    sll := (funct3 === "b001".U) && aluReg
    slt := (funct3 === "b010".U) && aluReg
    sltu := (funct3 === "b011".U) && aluReg
    xor := (funct3 === "b100".U) && aluReg
    srl := (funct3 === "b101".U) && aluReg && (!arith)
    sra := (funct3 === "b101".U) && aluReg && arith
    or := (funct3 === "b110".U) && aluReg
    and := (funct3 === "b111".U) && aluReg

    // immediate
    aluImm := opcode === "b0010011".U
    addi := (funct3 === "b000".U) && aluImm
    slti := (funct3 === "b010".U) && aluImm
    sltiu := (funct3 === "b011".U) && aluImm
    xori := (funct3 === "b100".U) && aluImm
    ori := (funct3 === "b110".U) && aluImm
    andi := (funct3 === "b111".U) && aluImm
    aluValid := aluImm || aluReg || auipc || lui || shiftValid
    shiftInstDecode()
  }

  def shiftInstDecode(): Unit = {
    shiftValid := slli || srli || srai
    illegalShamt := !((funct7 === "b0000000".U) || (funct7 === "b0100000".U)) && shiftValid
    slli := (funct3 === "b001".U) && aluImm
    srli := (funct3 === "b101".U) && aluImm && (!arith)
    srai := (funct3 === "b101".U) && aluImm && arith
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

  def isCsrWr: Bool = csrrw || csrrwi
  def isCsrRd: Bool = csrrs || csrrsi
  def isCsrClr: Bool = csrrc || csrrci

  def immI: UInt = Cat(Fill(20, funct7(6)), funct7, rs2)
  def immS: UInt = Cat(Fill(20, funct7(6)), funct7, rd)
  def immB: UInt = Cat(Fill(21, funct7(6)), rd(0), funct7(5, 0), rd(4, 1), 0.U(1.W))
  def immU: UInt = Cat(funct7, rs2, rs1, funct3, Fill(12, 0.U))
  def immJ: UInt = {
    Cat(Fill(13, funct7(6)), rs1, funct3, rs2(0), funct7, rs2(4, 1), 0x0.U(1.W))
  }
}

/**
  *
  * @param cfg dirv's configuration parameter.
  */
class Idu2ExuIO(implicit val cfg: Config) extends Bundle {
  val inst = Irrevocable(new InstRV32())
}



/**
  * Class for IDU I/O
  * @param cfg dirv's configuration parameter.
  */
class IduIO(implicit val cfg: Config) extends Bundle {
  val ifu2idu = Flipped(new Ifu2IduIO())
  val idu2exu = new Idu2ExuIO()
}

/**
  * Instruction Decode Unit
  * @param cfg dirv's configuration parameter.
  */
class Idu(implicit cfg: Config) extends Module with InstInfoRV32 {
  val io = IO(new IduIO())

  val inst = Wire(new InstRV32())

  inst.decode(io.ifu2idu.valid && io.ifu2idu.ready, io.ifu2idu.inst)

  //
  io.ifu2idu.ready := io.idu2exu.inst.ready

  io.idu2exu.inst.bits := inst
  io.idu2exu.inst.valid := io.ifu2idu.valid

  /*
  if (cfg.arch == RV32E)
  val codeIsCExt = subOpCode =/= "b000".U
  */
}
