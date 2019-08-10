// See LICENSE for license details.

package peri.uart

import chisel3._
import chisel3.util._

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
  val txFifoFull = UInt(1.W)
  val txFifoEmpty = UInt(1.W)
  val rxFifoFull = UInt(1.W)
  val rxFifoValidData = UInt(1.W)

  def write(v: UInt): Unit = {
    parrityError := v(7)
    frameError := v(6)
    overrunError := v(5)
    intrEnabled := v(4)
    txFifoFull := v(3)
    txFifoEmpty := v(2)
    rxFifoFull := v(1)
    rxFifoValidData := v(0)
  }
  def read(): UInt = Cat(
    parrityError, frameError,
    overrunError, intrEnabled,
    txFifoFull, txFifoEmpty,
    rxFifoFull, rxFifoValidData
  )

}

class RegTop(debug: Boolean=false) extends Module {

  val io = IO(new Bundle {
    val regR = Flipped(new RegRdIO())
    val regW = Flipped(new RegWrIO())
    val r2c = new RegTop2CtrlIO()
    val dbg = if (debug) Some(new RegTopDebugIO) else None
  })

  val fifoDepth = 16

  val rxFifo = Module(new Fifo(fifoDepth))
  val txFifo = Module(new Fifo(fifoDepth))
  val ctrl = WireInit(0.U.asTypeOf(new CtrlReg))
  val stat = WireInit(0.U.asTypeOf(new StatReg))

  // write
  val rdselRxFifo = (io.regR.addr === RegInfo.rxFifo.U) && io.regR.enable
  val wrselTxFifo = (io.regW.addr === RegInfo.txFifo.U) && io.regW.enable
  val rdselStat = (io.regR.addr === RegInfo.stat.U) && io.regR.enable
  val wrselCtrl = (io.regW.addr === RegInfo.ctrl.U) && io.regW.enable


  // stat
  stat.txFifoEmpty := txFifo.io.rd.empty
  stat.txFifoFull := !txFifo.io.rd.empty
  stat.rxFifoFull := rxFifo.io.full
  stat.rxFifoValidData := !rxFifo.io.rd.empty

  // ctrl
  when (wrselCtrl) {
    ctrl.write(io.regW.data)
  }

  // read
  io.regR.dataValid := RegNext(rdselRxFifo || rdselStat, false.B)
  io.regR.data := RegNext(MuxCase(0.U, Seq(
    rdselRxFifo -> rxFifo.io.rd.data,
    rdselStat -> stat.read())), 0.U)

  io.r2c.tx <> txFifo.io.rd
  txFifo.io.rst := wrselCtrl & ctrl.rstTxFifo
  txFifo.io.wr.enable := wrselTxFifo
  txFifo.io.wr.data := io.regW.data

  io.r2c.rx <> rxFifo.io.wr
  rxFifo.io.rst := wrselCtrl & ctrl.rstTxFifo
  rxFifo.io.rd.enable := rdselRxFifo

  // debug
  if (debug) {
    val dbg = io.dbg.get
    dbg.rxFifo := rxFifo.io.rd.data
    dbg.txFifo := txFifo.io.wr.data
    dbg.stat := stat.read()
    dbg.ctrl := ctrl.read()
  }
}
