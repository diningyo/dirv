// See LICENSE for license details.

package dirv.pipeline

import chisel3._
import chisel3.util.{Cat, Fill, Mux1H, MuxCase}
import dirv.Config

/**
  * Object for Control and Status Register parameters
  */
case object CSR {
  // Machine Information Registers
  val mvendorid = 0xf11
  val marchid = 0xf12
  val mimpid = 0xf13
  val mhartid = 0xf14
  // Machine Trap Setup
  val mstatus = 0x300
  val misa = 0x301
  val medeleg = 0x302
  val mideleg = 0x303
  val mie = 0x304
  val mtvec = 0x305
  val mtounteren = 0x306
  // Machine Trap Handling
  val mscratch = 0x340
  val mepc = 0x341
  val mcause = 0x342
  val mtval = 0x343
  val mip = 0x344
  // Machine Protection Translation

  val csrRegAddrs = Seq(
    mvendorid, marchid, mimpid, mhartid,
    mtvec, mepc, mtval, mcause, mip, mscratch, mie, misa, mstatus
  ).sorted

  val csrRegMaps = csrRegAddrs.zipWithIndex.toMap
}

/**
  * Super class for CSR register
  */
abstract class BaseReg extends Bundle {
  /**
    * Write register
    * @param wrData data to write register
    */
  def write(wrData: UInt): Unit = Unit

  /**
    * Read register
    * @return register value
    */
  def read: UInt = 0.U

  /**
    * Initalize register
    * @return this class's instance after initializing
    */
  def init: this.type = this
}

/**
  * Class for both readable and writable register which has only one field.
  * @param bits field bit width
  */
class NbitsRegRW(bits: Int) extends BaseReg {
  val reg = UInt(bits.W)

  override def write(wrData: UInt): Unit = reg := wrData(bits - 1, 0)
  override def read: UInt = reg
  override def cloneType: NbitsRegRW.this.type = new NbitsRegRW(bits).asInstanceOf[this.type]
}

/**
  * Class for read only register which has only one field.
  * @param bits field bit width
  */
class NbitsRegRO(bits: Int) extends BaseReg {
  val reg = UInt(bits.W)

  override def read: UInt = reg
  override def cloneType: NbitsRegRO.this.type = new NbitsRegRO(bits).asInstanceOf[this.type]
}

/**
  * Machine Status Register
  */
class Mstatus extends BaseReg {
  val mie = Bool()
  val mpie = Bool()
  val mpp = UInt(2.W)

  override def write(wrData: UInt): Unit = {
    mpie := wrData(7)
    mie := wrData(3)
  }

  override def read: UInt = Cat(0x0.U(19.W), mpp, 0x0.U(3.W), mpie, 0x0.U(3.W), mie, 0x0.U(3.W))
}

/**
  * Machine ISA Register
  * @param xlen XLEN Value.
  *             when init method calls, this value will convert misa's mxl field value and set it.
  * @param ext Machine ISA Register Extension field value
  */
class Misa(val xlen: Int = 32, val ext: Int = 0x100) extends BaseReg {
  val mxl = UInt(2.W)
  val extensions = UInt(26.W)

  override def init: this.type = {
    mxl := (xlen match {
      case 32 => 0x1.U
      case 64 => 0x2.U
      case 128 => 0x3.U
    })

    extensions := ext.U
    this
  }

  override def read: UInt = Cat(mxl, Fill(xlen - 28, 0x0.U), extensions)
}

/**
  * Machine Trap-Vector Base-Address Register
  * @param xlen XLEN Value.
  */
class Mtvec(val xlen: Int = 32) extends BaseReg {
  val modeBits = 2
  val base = UInt((xlen - modeBits).W)
  val mode = UInt(modeBits.W)

  /**
    * Write register
    * @param wrData data to write register
    */
  override def write(wrData: UInt): Unit = {
    base := wrData(xlen - 1, modeBits)
    mode := wrData(modeBits - 1, 0)
  }

  /**
    * Read register
    * @return register value
    */
  override def read: UInt = Cat(base, mode)

  /**
    * Get trap-handler base address
    * @return trap-handler base address
    */
  def getBase: UInt = Cat(base, 0.U(2.W))
}

/**
  * Machine Exception Program Counter
  */
class Mepc extends BaseReg {
  val config = 0
  val reg = UInt(31.W)

  /**
    * write resiger
    * @param wrData data to write register
    */
  override def write(wrData: UInt): Unit = {
    reg := wrData(31, 1)
  }

  override def read: UInt = Cat(reg, 0.U(1.W))
}

/**
  * Machine Cause Register
  * @param xlen XLEN Value.
  */
class Mcause(val xlen: Int = 32) extends BaseReg {
  val interrupt = Bool()
  val exceptionCode = UInt((xlen - 1).W)

  /**
    * Write Register
    * @param wrData data to write register
    */
  override def write(wrData: UInt): Unit = {
    interrupt := wrData(31)
    exceptionCode := wrData(30, 0)
  }

  /**
    * Read Register
    * @return register value
    */
  override def read: UInt = Cat(interrupt, exceptionCode)
}


/**
  * Machine Interrupt-pending Register
  */
class Mip extends BaseReg {
  val meip = Bool()
  val seip = Bool()
  val ueip = Bool()
  val mtip = Bool()
  val stip = Bool()
  val utip = Bool()
  val msip = Bool()
  val ssip = Bool()
  val usip = Bool()

  /**
    * Write register
    * @param wrData data to write register
    */
  override def write(wrData: UInt): Unit = {
    meip := wrData(11)
    seip := wrData(9)
    ueip := wrData(8)
    mtip := wrData(7)
    stip := wrData(5)
    utip := wrData(4)
    msip := wrData(3)
    ssip := wrData(1)
    usip := wrData(0)
  }

  /**
    * Read register
    * @return register value
    */
  override def read: UInt = Cat(
    0.U(20.W),
    meip, 0.U(1.W), seip, ueip,
    mtip, 0.U(1.W), stip, utip,
    msip, 0.U(1.W), ssip, usip)
}

/**
  * Machine Interrupt-enable Register
  */
class Mie extends BaseReg {
  val meie = Bool()
  val seie = Bool()
  val ueie = Bool()
  val mtie = Bool()
  val stie = Bool()
  val utie = Bool()
  val msie = Bool()
  val ssie = Bool()
  val usie = Bool()

  /**
    * Write register
    * @param wrData data to write register
    */
  override def write(wrData: UInt): Unit = {
    meie := wrData(11)
    seie := wrData(9)
    ueie := wrData(8)
    mtie := wrData(7)
    stie := wrData(5)
    utie := wrData(4)
    msie := wrData(3)
    ssie := wrData(1)
    usie := wrData(0)
  }

  /**
    * Read register
    * @return register value
    */
  override def read: UInt = Cat(
    0.U(20.W),
    meie, 0.U(1.W), seie, ueie,
    mtie, 0.U(1.W), stie, utie,
    msie, 0.U(1.W), ssie, usie)
}


/**
  * Class for CSR block I/O
  * @param xlen XLEN Value.
  * @param csrBits Csr block's address bit width.
  */
class CsrIO(val xlen: Int = 32, val csrBits: Int = 12)
           (implicit val cfg: Config) extends Bundle {
  val inst = Input(new InstRV32())
  val rdData = Output(UInt(xlen.W))
  val wrData = Input(UInt(xlen.W))
  val invalidWb = Input(Bool())
  //val excOccured = Input(Bool())
  val excCode = Input(UInt((xlen - 1).W))
  val excMepcWren = Input(Bool())
  val excPc = Input(UInt(xlen.W))
  val trapOccured = Input(Bool())
  val trapAddr = Input(UInt(xlen.W))
  val mtvec = Output(UInt(xlen.W))
  val mepc = Output(UInt(xlen.W))
}

/**
  * RISC-V Contorl and Status Registers block.
  * @param cfg dirv's configuration parameter.
  */
class Csrf(implicit cfg: Config) extends Module {

  val io = IO(new CsrIO)

  val inst = io.inst

  // CSR registers
  val mvendorid = WireInit(cfg.vendorId.U.asTypeOf(new NbitsRegRO(cfg.arch.xlen)))
  val marchid = WireInit(cfg.archId.U.asTypeOf(new NbitsRegRO(cfg.arch.xlen)))
  val mimpid = WireInit(cfg.impId.U.asTypeOf(new NbitsRegRO(cfg.arch.xlen)))
  val mhartid = WireInit(cfg.hartId.U.asTypeOf(new NbitsRegRO(cfg.arch.xlen)))
  val mscratch = RegInit(0.U.asTypeOf(new NbitsRegRW(cfg.arch.xlen)))
  val misa = Wire(new Misa()).init
  val mie = RegInit(0.U.asTypeOf(new Mip))
  val mip = RegInit(0.U.asTypeOf(new Mie))
  val mstatus = RegInit(0.U.asTypeOf(new Mstatus))
  val mcause = RegInit(0.U.asTypeOf(new Mcause))
  val mtval = RegInit(0.U.asTypeOf(new NbitsRegRW(cfg.arch.xlen)))
  val mepc = RegInit(0.U.asTypeOf(new Mepc))
  val mtvec = RegInit(0.U.asTypeOf(new Mtvec))

  // connect CSR regs I/F
  val wrData = Mux(inst.csrUimmValid, inst.rs1, io.wrData)
  val rdData = Wire(UInt(cfg.arch.xlen.W))

  val rden = inst.csrValid
  val wren = inst.csrValid && (!io.invalidWb)
  val csrWrData = MuxCase(wrData, Seq(
    (inst.csrrc || inst.csrrci) -> (rdData & (~wrData).asUInt()),
    (inst.csrrs || inst.csrrsi) -> (rdData | wrData)
  ))

  // exception
  val excOccured = inst.illegalShamt || inst.ebreak // || misalignedInstAddr || misalignedDataAddr
  val excCode = Wire(UInt(cfg.arch.xlen.W))

  excCode := Mux1H(Seq(
    //misalignedInstAddr -> 0.U,
    inst.illegalShamt -> 2.U,
    //(inst.illegalShamt || illegalOpcode) -> 2.U,
    inst.ebreak -> 3.U
    //io.core.misalignedRdAddr -> 4.U,
    //io.core.misalignedWrAddr -> 6.U
  ))

  val excMepcWren = inst.illegalShamt || inst.ebreak //|| misalignedDataAddr || invalidWrBack

  // trap value
  /*
  csr.io.trapOccured := misalignedDataAddr || misalignedInstAddr
  csr.io.trapAddr := Mux1H(Seq(
    io.core.misalignedWrAddr -> io.core.dmemWrAddr,
    io.core.misalignedRdAddr -> io.core.dmemRdAddr,
    misalignedInstAddr -> ifuCurrPc
  ))
  */

  mstatus.mpp := "b11".U

  when (excOccured) {
    mcause.write(excCode)
  }

  when (excMepcWren) {
    mepc.write(io.excPc)
  }

  when (io.trapOccured) {
    mtval.write(io.trapAddr)
  }

  // address decode
  val csrSelBits = Cat(CSR.csrRegAddrs.reverse.map(_.asUInt() === inst.getCsr))

  // read
  val csrRegs = Seq(
    CSR.mstatus -> mstatus,
    CSR.misa -> misa,
    CSR.mie -> mie,
    CSR.mtvec -> mtvec,
    CSR.mepc -> mepc,
    CSR.mcause -> mcause,
    CSR.mip -> mip,
    CSR.mscratch -> mscratch,
    CSR.marchid -> marchid,
    CSR.mimpid -> mimpid,
    CSR.mtval -> mtval,
    CSR.mvendorid -> mvendorid,
    CSR.mhartid -> mhartid
  ).sortBy(_._1)

  // write register
  csrRegs.foreach {
    case (addr, reg) =>
      when (wren && csrSelBits(CSR.csrRegMaps(addr))) {
        reg.write(csrWrData)
      }
  }

  rdData := MuxCase(0.U, csrRegs.zipWithIndex.map {
    case(reg, selBits) => (csrSelBits(selBits), reg._2.read)
  })


  // io
  io.rdData := rdData
  io.mtvec := mtvec.getBase
  io.mepc := mepc.read
}
