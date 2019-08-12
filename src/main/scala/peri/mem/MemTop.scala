// See LICENSE for license details.

package peri.mem

import chisel3._
import dirv.io.{MemIO, MemRIO, MemRWIO}
import mbus.{MbusSramBridge, MbusSramBridgeParams, ROMbusIO, RWMbusIO}

/**
  * parameter for MemTop
  * @param numOfMemBytes number of Memory bytes.
  * @param dataBits Data bit width.
  * @param initHexFile File path of Hex data file for initializing memory.
  */
case class MemTopParams
(
  numOfMemBytes: Int,
  dataBits: Int,
  initHexFile: String = ""
) {
  val ramParams = RAMParams(VerilogRAM, numOfMemBytes, dataBits)
  val iBrgParams = MbusSramBridgeParams(ROMbusIO, ramParams.addrBits, ramParams.dataBits)
  val dBrgParams = MbusSramBridgeParams(RWMbusIO, ramParams.addrBits, ramParams.dataBits)
}

/**
  * I/O class for MemTop
  * @param p Instance of MemTopParams
  */
class MemTopIO(p: MemTopParams) extends Bundle {
  val rp = p.ramParams
  val imem = Flipped(MemIO(MemRIO, rp.addrBits, rp.dataBits))
  val dmem = Flipped(MemIO(MemRWIO, rp.addrBits, rp.dataBits))

  override def cloneType: this.type = new MemTopIO(p).asInstanceOf[this.type]
}

class MemTop(p: MemTopParams) extends Module {
  val io = IO(new MemTopIO(p))

  val m_ram = Module(new RAM1RO1RWWrapper(p.ramParams))

  val m_imem_brg = Module(new MbusSramBridge(p.iBrgParams))
  val m_dmem_brg = Module(new MbusSramBridge(p.dBrgParams))

  m_imem_brg.io.mbus <> io.imem
  m_dmem_brg.io.mbus <> io.dmem

  m_ram.io.a <> m_imem_brg.io.sram
  m_ram.io.b <> m_dmem_brg.io.sram
}
