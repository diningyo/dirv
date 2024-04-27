// See LICENSE for license details.

package peri.mem

import math.pow

import chisel3._
import chisel3.experimental.{IntParam, StringParam}
import chisel3.util.{log2Ceil, HasBlackBoxResource}

sealed trait RAMType
case object VerilogRAM extends RAMType
case object ChiselRAM extends RAMType

/**
  *
  * @param ramType RAM type.
  * @param numOfMemBytes number of Memory bytes.
  * @param dataBits Data bit width.
  * @param initHexFile File path of Hex data file for initializing memory.
  */
case class RAMParams
(
  ramType: RAMType,
  numOfMemBytes: Int,
  dataBits: Int,
  initHexFile: String = ""
) {
  require(dataBits % 8 == 0, "dataBits must be multiply of 8")
  val strbBits = dataBits / 8
  val addrBits = log2Ceil(numOfMemBytes / strbBits)
  val numOfMemRows = numOfMemBytes / strbBits // convert byte to number of row
  val portAParams = RAMIOParams(RAMRO, addrBits, dataBits, hasRddv = true)
  val portBParams = RAMIOParams(RAMRW, addrBits, dataBits, hasRddv = true)
}

class RAM1RO1RWWrapper(p: RAMParams) extends Module {
  val io = IO(new Bundle {
    val a = Flipped(new RAMIO(p.portAParams))
    val b = Flipped(new RAMIO(p.portBParams))
  })

  p.ramType match {
    case VerilogRAM =>
      val m = Module(new RAM1RO1RW(p))

      m.io.clk := clock

      // TODO : modify address conversion.
      m.io.addra := io.a.addr(p.addrBits - 1, 2)
      m.io.rena := io.a.rden.get
      io.a.rddata.get := m.io.qa
      io.a.rddv.get := RegNext(io.a.rden.get, false.B)
      // TODO : modify address conversion.
      m.io.addrb := io.b.addr(p.addrBits - 1, 2)
      m.io.renb := io.b.rden.get
      io.b.rddv.get := RegNext(io.b.rden.get, false.B)
      io.b.rddata.get := m.io.qb
      m.io.wenb := io.b.wren.get
      m.io.webb := io.b.wrstrb.get
      m.io.datab := io.b.wrdata.get

    case _ => assert(cond = false, "Just now, verilog only.")
  }
}

/**
  * RAM1RO1RW
  * Dual port RAM which one port is READ ONLY and the other is READ and WRITE.
  * @param p parameter for Memory
  */
class RAM1RO1RW(p: RAMParams) extends BlackBox(
  Map(
    "p_ADDR_BITS" -> IntParam(p.addrBits),
    "p_DATA_BITS" -> IntParam(p.dataBits),
    "p_STRB_BITS" -> IntParam(p.strbBits),
    "p_MEM_ROW_NUM" -> IntParam(p.numOfMemRows),
    "p_INIT_HEX_FILE" -> StringParam(p.initHexFile)
  )) with HasBlackBoxResource {
  val io = IO(new Bundle{

    println(s"p_INIT_HEX_FILE = ${p.initHexFile}")

    val clk = Input(Clock())

    // A port
    val addra = Input(UInt(p.addrBits.W))
    val rena = Input(Bool())
    val qa = Output(UInt(p.dataBits.W))

    // memory
    val addrb = Input(UInt(p.addrBits.W))
    val renb = Input(Bool())
    val qb = Output(UInt(p.dataBits.W))
    val wenb = Input(Bool())
    val webb = Input(UInt(p.strbBits.W))
    val datab = Input(UInt(p.dataBits.W))
  })
  addResource("/RAM1RO1RW.v")
}