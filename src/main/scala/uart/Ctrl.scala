
package uart

import chisel3._
import chisel3.util._


class UartIO extends Bundle {
  val tx = Output(UInt(1.W))
  val rx = Input(UInt(1.W))
}

class Ctrl extends Module {
  val io = IO(new Bundle {
    val uart = new UartIO
    val tx = Flipped(ValidIO(UInt(8.W)))
    val rx = ValidIO(UInt(8.W))
  })

  val baudrateCount = 100 // TODO : ちゃんと周波数に合わせる

  // Tx
  val txStm = Module(new StateMachine)
  val txDurationCounter = RegInit(0.U(32.W))
  val txBits = RegInit(0.U(3.W))

  when (!txStm.io.idle) {
    when (txDurationCounter < baudrateCount.U) {
      txDurationCounter := txDurationCounter + 1.U
    } .otherwise {
      txDurationCounter := 0.U
    }
  }

  val txUpdateReq = txDurationCounter === baudrateCount.U

  when (txStm.io.data) {
    when (txUpdateReq) {
      txBits := txBits + 1.U
    }
  } .otherwise {
    txBits := 0.U
  }

  io.uart.tx := MuxCase(1.U, Seq(
    txStm.io.start -> 0.U,
    txStm.io.data -> io.tx.bits(txBits)
  ))

  // txStm <-> ctrl
  txStm.io.startReq := io.tx.valid
  txStm.io.dataReq := txStm.io.start && txUpdateReq
  txStm.io.stopReq := txStm.io.data && txUpdateReq && (txBits === 7.U)
  txStm.io.fin := txStm.io.stop && txUpdateReq

  // Rx
  val rxStm = Module(new StateMachine)
  val rxDurationCounter = RegInit(0.U(32.W))
  val rxBits = RegInit(0.U(3.W))
  val rxData = RegInit(0.U)

  when (rxStm.io.idle ) {
    when (!io.uart.rx) {
      rxDurationCounter := (baudrateCount / 2).U
    } .otherwise {
      rxDurationCounter := 0.U
    }
  } .otherwise {
    when (rxDurationCounter < baudrateCount.U) {
      rxDurationCounter := rxDurationCounter + 1.U
    } .otherwise {
      rxDurationCounter := 0.U
    }
  }

  val rxUpdateReq = rxDurationCounter === baudrateCount.U

  when (rxStm.io.data) {
    when (rxUpdateReq) {
      rxBits := rxBits + 1.U
    }
  } .otherwise {
    rxBits := 0.U
  }

  when (rxStm.io.data) {
    when (rxUpdateReq) {
      rxData := rxData | (io.uart.rx << rxBits).asUInt()
    }
  } .otherwise {
    rxData := 0.U
  }

  // rxStm <-> ctrl
  rxStm.io.startReq := rxStm.io.idle && !io.uart.rx
  rxStm.io.dataReq := rxStm.io.start && rxUpdateReq
  rxStm.io.stopReq := rxStm.io.data && rxUpdateReq && (rxBits === 7.U)
  rxStm.io.fin := rxStm.io.stop && rxUpdateReq

  // top <-> ctrl
  io.rx.valid := rxStm.io.stopReq
  io.rx.bits := rxData

}

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