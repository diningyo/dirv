// See LICENSE for license details.

package peri.mem

import java.nio.channels.FileLockInterruptionException

import chisel3._
import chisel3.util._
import chisel3.core.{BlackBox, Bundle, IntParam, StringParam}

sealed trait RAMType
case object VerilogRAM extends RAMType
case object ChiselRAM extends RAMType


class RAM1RO1RWWrapper(ramType: RAMType, addrBits: Int,
                       dataBits: Int, strbBits: Int, initHexFile: String = ""
                      ) extends Module  {
  val io = IO(new Bundle {
    val a = Flipped(new RAMIO(RAMRO, addrBits, dataBits, hasRddv = true))
    val b = Flipped(new RAMIO(RAMRW, addrBits, dataBits, hasRddv = true))
  })

  ramType match {
    case VerilogRAM =>
      val m = Module(new RAM1RO1RW(addrBits, dataBits, strbBits, initHexFile))

      m.io.clk := clock

      m.io.addra := io.a.addr
      m.io.rena := io.a.rden.get
      io.a.rddata.get := m.io.qa
      io.a.rddv.get := RegNext(io.a.rden.get, false.B)

      m.io.addrb := io.b.addr
      m.io.renb := io.b.rden.get
      io.b.rddv.get := RegNext(io.b.rden.get, false.B)
      io.b.rddata.get := m.io.qb
      m.io.wenb := io.b.wren.get
      m.io.webb := io.b.wrstrb.get
      m.io.datab := io.b.wrdata.get

    case _ => assert(false, "Just now, verilog only.")
  }
}

class RAM1RO1RW(addrBits: Int, dataBits: Int, strbBits: Int, initHexFile: String)  extends BlackBox(
  Map(
    "p_ADDR_BITS" -> IntParam(addrBits),
    "p_DATA_BITS" -> IntParam(dataBits),
    "p_STRB_BITS" -> IntParam(strbBits),
    "p_INIT_HEX_FILE" -> StringParam(initHexFile)
  )) with HasBlackBoxResource {
  val io = IO(new Bundle{
    val clk = Input(Clock())

    // A port
    val addra = Input(UInt(addrBits.W))
    val rena = Input(Bool())
    val qa = Output(UInt(dataBits.W))

    // memory
    val addrb = Input(UInt(addrBits.W))
    val renb = Input(Bool())
    val qb = Output(UInt(dataBits.W))
    val wenb = Input(Bool())
    val webb = Input(UInt(strbBits.W))
    val datab = Input(UInt(dataBits.W))
  })
  setResource("/RAM1RO1RW.v")
}