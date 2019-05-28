
package uart

import scala.math.{pow, round, floor, random}

import chisel3.iotesters._

/**
  * Unit tester for TxRxCtrl module.
  * @param c dut module (instance of TxRXCtrl)
  * @param baudrate test duration count. this valuable is used for controlling uart signals.
  */
class TxRxCtrlUnitTester(c: TxRxCtrl, baudrate: Int, clockFreq: Int) extends PeekPokeTester(c) {

  val timeOutCycle = 1000
  val duration = round(clockFreq * pow(10, 6) / baudrate).toInt

  println(s"duration = $duration")

  /**
    * Idle
    */
  def idle(): Unit = {
    // send start bit
    for (_ <- Range(0, duration)) {
      poke(c.io.uart.rx, true)
      step(1)
    }
  }

  /**
    * Uart send
    * @param data send data. And this value will be expect value.
    */
  def send(data: Int): Unit = {

    // send start bit
    poke(c.io.uart.rx, false)
    for (_ <- Range(0, duration)) {
      step(1)
    }

    // send bit data
    for (idx <- Range(0, 8)) {
      val rxBit = (data >> idx) & 0x1
      poke(c.io.uart.rx, rxBit)
      for (_ <- Range(0, duration)) {
        step(1)
      }
    }

    // send stop bits
    poke(c.io.uart.rx, true)
    for (_ <- Range(0, duration)) {
      if (peek(c.io.r2c.rx.wren) == 0x1) {
        expect(c.io.r2c.rx.data, data)
      }
      step(1)
    }
  }

  /**
    * Uart data receive
    * @param exp expect value
    */
  def receive(exp: Int): Unit = {

    // detect start
    while (peek(c.io.uart.tx) == 0x1) {
      step(1)
    }

    // shift half period
    for (_ <- Range(0, duration / 2)) {
      step(1)
    }

    expect(c.io.uart.tx, false, "detect bit must be low")

    for (idx <- Range(0, 8)) {
      val expTxBit = (exp >> idx) & 0x1
      for (_ <- Range(0, duration)) {
        step(1)
      }
      expect(c.io.uart.tx, expTxBit, s"don't match exp value bit($idx) : exp = $expTxBit")
    }

    // stop bits
    for (_ <- Range(0, duration)) {
      step(1)
    }

    // check stop bit value
    expect(c.io.uart.tx, true, s"stop bit must be high")
  }
}

/**
  * Test class for TxRxCtrl module
  */
class TxRxCtrlTester extends BaseTester {

  val dutName = "ctrl"

  behavior of dutName

  // 周波数が1MHzの場合設定値上は666666がMaxだが
  // 実際には500000で動くことになる(四捨五入でduration=2になるから）
  // 論理上のbaurateの上限は周波数の1/2
  val baudrate: Int = 500000
  val clockFreq: Int = 1

  it should "send when io.tx.valid is high. [uart-tx]" in {
    val outDir = dutName + "-tx"
    val args = getArgs(Map(
      "--top-name" -> dutName,
      "--target-dir" -> s"test_run_dir/$outDir"
    ))

    Driver.execute(args, () => new TxRxCtrl(baudrate, clockFreq)) {
      c => new TxRxCtrlUnitTester(c, baudrate, clockFreq) {
        val txData = Range(0, 100).map(_ => floor(random * 256).toInt)
        poke(c.io.r2c.tx.rden, true)

        for (d <- txData) {
          poke(c.io.r2c.tx.data, d)
          step(1)
          receive(d)
        }
      }
    } should be (true)
  }

  it should "receive when io.uart.rx.valid is low. [uart-rx]" in {
    val outDir = dutName + "-rx"
    val args = getArgs(Map(
      "--top-name" -> dutName,
      "--target-dir" -> s"test_run_dir/$outDir"
    ))

    Driver.execute(args, () => new TxRxCtrl(baudrate, clockFreq)) {
      c => new TxRxCtrlUnitTester(c, baudrate, clockFreq) {
        val rxData = Range(0, 100).map(_ => floor(random * 256).toInt)

        idle()
        for (d <- rxData) {
          send(d)
        }
      }
    } should be (true)
  }
}