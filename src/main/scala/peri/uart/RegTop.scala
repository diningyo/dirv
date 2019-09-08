// See LICENSE for license details.

package peri.uart

import chisel3._
import chisel3.util._
import peri.mem.{RAMIO, RAMIOParams}

object RegInfo {
  val rxFifo = 0x0 // 受信FIFO
  val txFifo = 0x4 // 送信FIFO
  val stat = 0x8   // ステータス
  val ctrl = 0xc   // 制御
}

/**
  * Debug Interface for RegTop
  */
class RegTopDebugIO extends Bundle {
  val rxFifo = Output(UInt(8.W))
  val txFifo = Output(UInt(8.W))
  val stat = Output(UInt(8.W))
  val ctrl = Output(UInt(8.W))
}



class RegTop2CtrlIO extends Bundle {
  val tx = new FifoRdIO
  val rx = new FifoWrIO
}

abstract class UartReg extends Bundle {
  def write(v: UInt)
  def read(): UInt
}

class FifoReg extends UartReg {
  val data = UInt(8.W)
  def write(v: UInt): Unit = data := v(7, 0)
  def read(): UInt = data
}

class CtrlReg extends UartReg {
  val enableIntr = UInt(1.W)
  val rstRxFifo = UInt(1.W)
  val rstTxFifo = UInt(1.W)
  def write(v: UInt): Unit = {
    enableIntr := v(4)
    rstRxFifo := v(1)
    rstTxFifo := v(0)
  }

  def read(): UInt = Cat(enableIntr, 0.U(1.W), rstRxFifo, rstTxFifo)
}

class StatReg extends UartReg {
  val parrityError = UInt(1.W)
  val frameError = UInt(1.W)
  val overrunError = UInt(1.W)
  val intrEnabled = UInt(1.W)
  val txfifo_full = UInt(1.W)
  val txfifo_empty = UInt(1.W)
  val rxfifo_full = UInt(1.W)
  val rxfifo_valid = UInt(1.W)

  def write(v: UInt): Unit = {
    parrityError := v(7)
    frameError := v(6)
    overrunError := v(5)
    intrEnabled := v(4)
    txfifo_full := v(3)
    txfifo_empty := v(2)
    rxfifo_full := v(1)
    rxfifo_valid := v(0)
  }
  def read(): UInt = Cat(
    parrityError, frameError,
    overrunError, intrEnabled,
    txfifo_full, txfifo_empty,
    rxfifo_full, rxfifo_valid
  )

}

class RegTop(p: RAMIOParams)(debug: Boolean = false) extends Module {

  val io = IO(new Bundle {
    val sram = Flipped(new RAMIO(p))
    val r2c = new RegTop2CtrlIO()
    val dbg = if (debug) Some(new RegTopDebugIO) else None
  })

  val fifoDepth = 16

  val m_rx_fifo = Module(new Fifo(fifoDepth))
  val m_tx_fifo = Module(new Fifo(fifoDepth))
  val bw_ctrl = WireInit(0.U.asTypeOf(new CtrlReg))
  val bw_stat = WireInit(0.U.asTypeOf(new StatReg))

  // write
  val w_rdsel_rxfifo = (io.sram.addr === RegInfo.rxFifo.U) && io.sram.rden.get
  val w_rdsel_txfifo = (io.sram.addr === RegInfo.txFifo.U) && io.sram.wren.get
  val w_rdsel_stat = (io.sram.addr === RegInfo.stat.U) && io.sram.rden.get
  val w_wrsel_ctrl = (io.sram.addr === RegInfo.ctrl.U) && io.sram.wren.get


  // stat
  bw_stat.txfifo_empty := m_tx_fifo.io.rd.empty
  bw_stat.txfifo_full := !m_tx_fifo.io.rd.empty
  bw_stat.rxfifo_full := m_rx_fifo.io.full
  bw_stat.rxfifo_valid := !m_rx_fifo.io.rd.empty

  // ctrl
  when (w_wrsel_ctrl) {
    bw_ctrl.write(io.sram.wrdata.get)
  }

  // read
  io.sram.rddv.get := RegNext(w_rdsel_rxfifo || w_rdsel_stat, false.B)
  io.sram.rddata.get := RegNext(MuxCase(0.U, Seq(
    w_rdsel_rxfifo -> m_rx_fifo.io.rd.data,
    w_rdsel_stat -> bw_stat.read())), 0.U)

  io.r2c.tx <> m_tx_fifo.io.rd
  m_tx_fifo.io.rst := w_wrsel_ctrl & bw_ctrl.rstTxFifo
  m_tx_fifo.io.wr.enable := w_rdsel_txfifo
  m_tx_fifo.io.wr.data := io.sram.wrdata.get

  io.r2c.rx <> m_rx_fifo.io.wr
  m_rx_fifo.io.rst := w_wrsel_ctrl & bw_ctrl.rstTxFifo
  m_rx_fifo.io.rd.enable := w_rdsel_rxfifo

  // debug
  if (debug) {
    val dbg = io.dbg.get
    dbg.rxFifo := m_rx_fifo.io.rd.data
    dbg.txFifo := m_tx_fifo.io.wr.data
    dbg.stat := bw_stat.read()
    dbg.ctrl := bw_ctrl.read()
  }
}
