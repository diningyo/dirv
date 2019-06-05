// See LICENSE for license details.

package uart

import chisel3._
import chisel3.util._

import dirv.io._

class RegRdIO() extends Bundle {
  val addr = Output(UInt(4.W))
  val enable = Output(Bool())
  val dataValid = Input(Bool())
  val data = Input(UInt(8.W))
}

class RegWrIO() extends Bundle {
  val addr = Output(UInt(4.W))
  val enable =  Output(Bool())
  val strb = Output(UInt(4.W))
  val data = Output(UInt(8.W))
}

class Mem2Sram extends Module {
  val io = IO(new Bundle {
    val mem = Flipped(new MemIO(MemRWIO, 32, 32))
    val regR = new RegRdIO()
    val regW = new RegWrIO()
  })

  val mem = io.mem
  val memR = mem.r.get
  val memW = mem.w.get

  val memRvalid = RegInit(false.B)
  val memRdata = RegInit(0.U(32.W))

  val regAddr = RegInit(0.U(32.W))
  val regSize = RegInit(0.U(MemCmd.bits.W))
  val regRead = mem.cmd === MemCmd.rd.U
  val regWrite = mem.cmd === MemCmd.wr.U

  val regWstrb = RegInit(0.U(4.W))
  val regWdata = RegInit(0.U(32.W))
  val regWvalid = RegInit(false.B)

  val sIdle :: sRead :: sWrite :: Nil = Enum(3)
  val stm = RegInit(sIdle)

  switch (stm) {
    is (sIdle) {
      when (mem.valid) {
        stm := Mux1H(Seq(
          regWrite -> sWrite,
          regRead -> sRead
        ))
      }
    }

    is (sWrite) {
      when (regWvalid) {
        stm := sIdle
      }
    }

    is (sRead) {
      when (memRvalid && memR.ready) {
        stm := sIdle
      }
    }
  }

  when (stm === sIdle) {
    when(mem.valid) {
      regAddr := mem.addr
      regSize := mem.size
    }
  }

  val cmdReady = RegInit(true.B)

  when (stm === sIdle) {
    when (mem.valid) {
      cmdReady := false.B
    } .otherwise {
      cmdReady := true.B
    }
  }

  when (stm === sRead) {
    when (memRvalid && memR.ready) {
      memRvalid := false.B
      memRdata := io.regR.data
    } .elsewhen (io.regR.dataValid) {
      memRvalid := true.B
      memRdata := io.regR.data
    }
  }

  when (memW.valid) {
    regWstrb := memW.strb
    regWdata := memW.data
    regWvalid := memW.valid
  }

  io.regW.addr := regAddr
  io.regW.enable := regWvalid && (stm === sWrite)
  io.regW.strb := memW.strb
  io.regW.data := regWdata

  io.regR.addr := regAddr
  io.regR.enable := regRead

  mem.ready := true.B

  memW.resp := MemResp.ok.U
  memW.ready := true.B

  memR.valid := memRvalid
  memR.data := memRdata
  memR.resp := MemResp.ok.U

}
