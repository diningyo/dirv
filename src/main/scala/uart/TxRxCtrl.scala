
package uart

import scala.math.{pow, round}

import chisel3._
import chisel3.util._

/**
  * Enumeration for Uart direction
  */
sealed trait UartDirection
case object UartTx extends UartDirection
case object UartRx extends UartDirection

/**
  *　Interface for Uart
  */
class UartIO extends Bundle {
  val tx = Output(UInt(1.W))
  val rx = Input(UInt(1.W))
}

/**
  * Uart control top module
  * @param baudrate target baudrate
  * @param clockFreq clock frequency(MHz)
  */
class TxRxCtrl(baudrate: Int=9600,
               clockFreq: Int=100) extends Module {
  val io = IO(new Bundle {
    val uart = new UartIO
    val r2c = Flipped(new RegTop2CtrlIO())
  })

  val durationCount = round(clockFreq * pow(10, 6) / baudrate).toInt

  val txCtrl = Module(new Ctrl(UartTx, durationCount))
  val rxCtrl = Module(new Ctrl(UartRx, durationCount))

  io.uart.tx := txCtrl.io.uart
  txCtrl.io.reg <> io.r2c.tx

  rxCtrl.io.uart := io.uart.rx
  rxCtrl.io.reg <> io.r2c.rx
}

/**
  *  Uart control module for each direction
  * @param direction direction of uart.
  * @param durationCount count cycle for uart data per 1bit.
  */
class Ctrl(direction: UartDirection, durationCount: Int) extends Module {
  val io = IO(new Bundle {
    val uart = direction match {
      case UartTx => Output(UInt(1.W))
      case UartRx => Input(UInt(1.W))
    }
    val reg = direction match {
      case UartTx => Flipped(new TxFifoIO)
      case UartRx => Flipped(new RxFifoIO)
    }
  })

  val stm = Module(new StateMachine)
  val durationCounter = RegInit(durationCount.U)
  val bitIdx = RegInit(0.U(3.W))

  // parameter
  val initDurationCount = (direction match {
    case UartTx => 0
    case UartRx => durationCount / 2
  }).U

  // trigger for uart request
  val startReq = direction match {
    case UartTx => !io.reg.asInstanceOf[TxFifoIO].empty
    case UartRx => !io.uart
  }

  // Uart Rx received data
  val rxData = direction match {
    case UartTx => None
    case UartRx => Some(RegInit(0.U))
  }

  val updateReq = durationCounter === (durationCount - 1).U

  when (stm.io.idle ) {
    when (startReq) {
      durationCounter := initDurationCount
    } .otherwise {
      durationCounter := 0.U
    }
  } .otherwise {
    when (!updateReq) {
      durationCounter := durationCounter + 1.U
    } .otherwise {
      durationCounter := 0.U
    }
  }

  when (stm.io.data) {
    when (updateReq) {
      bitIdx := bitIdx + 1.U
    }
  } .otherwise {
    bitIdx := 0.U
  }

  direction match {

    case UartTx =>
      val reg = io.reg.asInstanceOf[TxFifoIO]

      io.uart := MuxCase(1.U, Seq(
        stm.io.start -> 0.U,
        stm.io.data -> reg.data(bitIdx)
      ))

      reg.rden := stm.io.stop && updateReq

    case UartRx =>
      val reg = io.reg.asInstanceOf[RxFifoIO]

      when (stm.io.data) {
        when (updateReq) {
          rxData.get := rxData.get | (io.uart << bitIdx).asUInt()
        }
      } .otherwise {
        rxData.get := 0.U
      }
      reg.wren := stm.io.stopReq
      reg.data := rxData.get
  }
  // txStm <-> ctrl
  stm.io.startReq := startReq
  stm.io.dataReq := stm.io.start && updateReq
  stm.io.stopReq := stm.io.data && updateReq && (bitIdx === 7.U)
  stm.io.fin := stm.io.stop && updateReq
}

/**
  * State machine for Uart control module
  */
class StateMachine extends Module {
  val io = IO(new Bundle {
    val startReq = Input(Bool())
    val dataReq = Input(Bool())
    val stopReq = Input(Bool())
    val fin = Input(Bool())

    // state
    val idle = Output(Bool())
    val start = Output(Bool())
    val data = Output(Bool())
    val stop = Output(Bool())
  })

  val sIdle :: sStart :: sData :: sStop :: Nil = Enum(4)
  val stm = RegInit(sIdle)

  switch (stm) {
    is (sIdle) {
      when (io.startReq) {
        stm := sStart
      }
    }

    is (sStart) {
      when (io.dataReq) {
        stm := sData
      }
    }

    is (sData) {
      when (io.stopReq) {
        stm := sStop
      }
    }

    is (sStop) {
      when (io.fin) {
        stm := sIdle
      }
    }
  }

  // output
  io.idle := stm === sIdle
  io.start := stm === sStart
  io.data := stm === sData
  io.stop := stm === sStop
}