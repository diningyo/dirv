// See LICENSE for license details.

package uart

import chisel3.iotesters._

import scala.math.{pow, round}

/**
  * Unit tester for TxRxCtrl module.
  * @param c dut module (instance of TxRXCtrl)
  * @param baudrate test duration count. this valuable is used for controlling uart signals.
  */
class UartUnitTester(c: Top, baudrate: Int, clockFreq: Int) extends PeekPokeTester(c) {

  val timeOutCycle = 1000
  val duration = round(clockFreq * pow(10, 6) / baudrate).toInt

}

class UartTester extends BaseTester {
  val dutName = "uart.Top"

}
