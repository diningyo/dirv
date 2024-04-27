// See LICENSE for license details.

package peri.uart

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

  val m_txctrl = Module(new Ctrl(UartTx, durationCount))
  val m_rxctrl = Module(new Ctrl(UartRx, durationCount))

  io.uart.tx := m_txctrl.io.uart
  m_txctrl.io.reg <> io.r2c.tx

  m_rxctrl.io.uart := io.uart.rx
  m_rxctrl.io.reg <> io.r2c.rx
}

/**
  *  Uart control module for each direction
  * @param direction direction of peri.uart.
  * @param durationCount count cycle for peri.uart data per 1bit.
  */
class Ctrl(direction: UartDirection, durationCount: Int) extends Module {
  val io = IO(new Bundle {
    val uart = direction match {
      case UartTx => Output(UInt(1.W))
      case UartRx => Input(UInt(1.W))
    }
    val reg = direction match {
      case UartTx => Flipped(new FifoRdIO)
      case UartRx => Flipped(new FifoWrIO)
    }
  })

  val m_stm = Module(new CtrlStateMachine)
  val r_duration_ctr = RegInit(durationCount.U)
  val r_bit_idx = RegInit(0.U(3.W))

  // parameter
  val initDurationCount = direction match {
    case UartTx => 0
    case UartRx => durationCount / 2
  }

  // trigger for peri.uart request
  val w_start_req = direction match {
    case UartTx => !io.reg.asInstanceOf[FifoRdIO].empty
    case UartRx => !io.uart
  }

  val w_update_req = r_duration_ctr === (durationCount - 1).U
  val fin = m_stm.io.stop && w_update_req

  when (m_stm.io.idle ) {
    when (w_start_req) {
      r_duration_ctr := initDurationCount.U
    } .otherwise {
      r_duration_ctr := 0.U
    }
  } .otherwise {
    when (!w_update_req) {
      r_duration_ctr := r_duration_ctr + 1.U
    } .otherwise {
      r_duration_ctr := 0.U
    }
  }

  when (m_stm.io.data) {
    when (w_update_req) {
      r_bit_idx := r_bit_idx + 1.U
    }
  } .otherwise {
    r_bit_idx := 0.U
  }

  direction match {
    case UartTx =>
      val reg = io.reg.asInstanceOf[FifoRdIO]

      io.uart := MuxCase(1.U, Seq(
        m_stm.io.start -> 0.U,
        m_stm.io.data -> reg.data(r_bit_idx)
      ))

      reg.enable := m_stm.io.stop && w_update_req

    case UartRx =>
      val reg = io.reg.asInstanceOf[FifoWrIO]
      val rxData = RegInit(0.U)

      when (m_stm.io.idle && w_start_req) {
        rxData := 0.U
      } .elsewhen (m_stm.io.data) {
        when (w_update_req) {
          rxData := rxData | (io.uart << r_bit_idx).asUInt
        }
      }
      reg.enable := fin
      reg.data := rxData
  }

  // txStm <-> ctrl
  m_stm.io.start_req := w_start_req
  m_stm.io.data_req := m_stm.io.start && w_update_req
  m_stm.io.stop_req := m_stm.io.data && w_update_req && (r_bit_idx === 7.U)
  m_stm.io.fin := fin
}

/**
  * State machine for Uart control module
  */
class CtrlStateMachine extends Module {
  val io = IO(new Bundle {
    val start_req = Input(Bool())
    val data_req = Input(Bool())
    val stop_req = Input(Bool())
    val fin = Input(Bool())

    // state
    val idle = Output(Bool())
    val start = Output(Bool())
    val data = Output(Bool())
    val stop = Output(Bool())
  })

  val sIdle :: sStart :: sData :: sStop :: Nil = Enum(4)
  val r_stm = RegInit(sIdle)

  switch (r_stm) {
    is (sIdle) {
      when (io.start_req) {
        r_stm := sStart
      }
    }

    is (sStart) {
      when (io.data_req) {
        r_stm := sData
      }
    }

    is (sData) {
      when (io.stop_req) {
        r_stm := sStop
      }
    }

    is (sStop) {
      when (io.fin) {
        r_stm := sIdle
      }
    }
  }

  // output
  io.idle := r_stm === sIdle
  io.start := r_stm === sStart
  io.data := r_stm === sData
  io.stop := r_stm === sStop
}