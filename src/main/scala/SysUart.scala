// See LICENSE for license details.

import java.nio.file.Paths

import chisel3._
import chisel3.stage.ChiselStage
import dirv.{Config, Dirv}
import mbus.{MbusIC, MbusICParams, MbusRW}
import peri.mem.{MemTop, MemTopParams}
import peri.uart.{UartIO, UartTop}


/**
  * Simulation environment top module
  * Uart baudrate is 9600 @ 50MHz
  * @param prgHexFile riscv-tests hex file path
  * @param cfg Dirv's configuration instance
  */
class SysUart(prgHexFile: String)(baudrate: Int, clockFreq: Int)(implicit cfg: Config) extends Module {
  val io = IO(new Bundle {
    val fin = Output(Bool())
    val uart = new UartIO()
    val pc = Output(UInt(cfg.arch.xlen.W))
    val zero = Output(UInt(cfg.arch.xlen.W))
    val ra = Output(UInt(cfg.arch.xlen.W))
    val sp = Output(UInt(cfg.arch.xlen.W))
    val gp = Output(UInt(cfg.arch.xlen.W))
    val tp = Output(UInt(cfg.arch.xlen.W))
    val t0 = Output(UInt(cfg.arch.xlen.W))
    val t1 = Output(UInt(cfg.arch.xlen.W))
    val t2 = Output(UInt(cfg.arch.xlen.W))
    val s0 = Output(UInt(cfg.arch.xlen.W))
    val s1 = Output(UInt(cfg.arch.xlen.W))
    val a0 = Output(UInt(cfg.arch.xlen.W))
    val a1 = Output(UInt(cfg.arch.xlen.W))
    val a2 = Output(UInt(cfg.arch.xlen.W))
    val a3 = Output(UInt(cfg.arch.xlen.W))
    val a4 = Output(UInt(cfg.arch.xlen.W))
    val a5 = Output(UInt(cfg.arch.xlen.W))
    val a6 = Output(UInt(cfg.arch.xlen.W))
    val a7 = Output(UInt(cfg.arch.xlen.W))
    val s2 = Output(UInt(cfg.arch.xlen.W))
    val s3 = Output(UInt(cfg.arch.xlen.W))
    val s4 = Output(UInt(cfg.arch.xlen.W))
    val s5 = Output(UInt(cfg.arch.xlen.W))
    val s6 = Output(UInt(cfg.arch.xlen.W))
    val s7 = Output(UInt(cfg.arch.xlen.W))
    val s8 = Output(UInt(cfg.arch.xlen.W))
    val s9 = Output(UInt(cfg.arch.xlen.W))
    val s10 = Output(UInt(cfg.arch.xlen.W))
    val s11 = Output(UInt(cfg.arch.xlen.W))
    val t3 = Output(UInt(cfg.arch.xlen.W))
    val t4 = Output(UInt(cfg.arch.xlen.W))
    val t5 = Output(UInt(cfg.arch.xlen.W))
    val t6 = Output(UInt(cfg.arch.xlen.W))
  })

  // Tmp.
  // 0x0000 - 0x7fff : Memory
  // 0x8000 - 0x8100 : Uart
  val base_p = MbusICParams(
    MbusRW,
    Seq(
      (0x0,    0x8000),
      (0x8000, 0x100)
    ),
    Seq(
      (0x0,    0x8000), // MemTop (32KBytes)
      (0x8000, 0x8100)  // Uart
    ), 32)

  val mp = MemTopParams(64 * 1024, 32, prgHexFile)

  // module instances
  val m_mbus_ic = Module(new MbusIC(base_p))
  m_mbus_ic.io := DontCare
  val m_mem = Module(new MemTop(mp))
  val m_uart = Module(new UartTop(baudrate, clockFreq))
  val m_dirv = Module(new Dirv())

  // connect mem and dut
  m_mem.io.imem <> m_dirv.io.imem

  m_mbus_ic.io.in(0) <> m_dirv.io.dmem
  m_mem.io.dmem <> m_mbus_ic.io.out(0)
  m_uart.io.mbus <> m_mbus_ic.io.out(1)
  io.uart <> m_uart.io.uart

  //
  // check riscv-tests finish condition
  //
  val xregs = m_dirv.io.dbg.get.xregs

  // connect top I/O
  io.fin := false.B
  io.pc := m_dirv.io.dbg.get.pc
  io.zero := xregs(0)
  io.ra := xregs(1)
  io.sp := xregs(2)
  io.gp := xregs(3)
  io.tp := xregs(4)
  io.t0 := xregs(5)
  io.t1 := xregs(6)
  io.t2 := xregs(7)
  io.s0 := xregs(8)
  io.s1 := xregs(9)
  io.a0 := xregs(10)
  io.a1 := xregs(11)
  io.a2 := xregs(12)
  io.a3 := xregs(13)
  io.a4 := xregs(14)
  io.a5 := xregs(15)
  io.a6 := xregs(16)
  io.a7 := xregs(17)
  io.s2 := xregs(18)
  io.s3 := xregs(19)
  io.s4 := xregs(20)
  io.s5 := xregs(21)
  io.s6 := xregs(22)
  io.s7 := xregs(23)
  io.s8 := xregs(24)
  io.s9 := xregs(25)
  io.s10 := xregs(26)
  io.s11 := xregs(27)
  io.t3 := xregs(28)
  io.t4 := xregs(29)
  io.t5 := xregs(30)
  io.t6 := xregs(31)
}

/**
  * Create SysUart RTL
  */
object ElaborateSysUart extends App {

  implicit val cfg = Config(initAddr = BigInt("200", 16))
  val baudrate = 9600
  val clockFreq = 50
  val file = Paths.get(args(0))

  println(
    ChiselStage.emitSystemVerilog(
      gen = new SysUart(file.toAbsolutePath.toString)(baudrate, clockFreq)
    )
  )
}
