// See LICENSE for license details.

import chiseltest._
import chiseltest.iotesters.PeekPokeTester
import chiseltest.VerilatorBackendAnnotation

import dirv.Config
import test.util.BaseTester

import scala.math.{pow, round}
import scala.util.Random

/**
  * Unit test class for MbusIC
  * @param c Instance of SimDTMMbusIC
  */
class SysUartUnitTester(c: SimDTMSysUart)
                       (baudrate: Int, clockFreq: Int) extends PeekPokeTester(c) {

  val r = new Random(1)
  val duration = round(clockFreq * pow(10, 6) / baudrate).toInt
  val uart = c.io.dut_io.uart

  /**
    * Uart data receive
    * @param exp expect value
    */
  def receive(exp: Int): Unit = {

    // detect start
    while (peek(uart.tx) == 0x1) {
      step(1)
    }

    // shift half period
    for (_ <- Range(0, duration / 2)) {
      step(1)
    }

    expect(uart.tx, false, "detect bit must be low")

    for (idx <- Range(0, 8)) {
      val expTxBit = (exp >> idx) & 0x1
      for (_ <- Range(0, duration)) {
        step(1)
      }

      expect(uart.tx, expTxBit, s"don't match exp value bit($idx) : exp = $expTxBit")
    }

    // stop bits
    for (_ <- Range(0, duration)) {
      step(1)
    }

    // check stop bit value
    expect(uart.tx, true, s"stop bit must be high")
  }
}

/**
  * Test class for MbusIC
  */
class SysUartTester extends BaseTester {

  val dutName = "SysUart"

  behavior of dutName

  val timeoutCycle = 1000000


  implicit val cfg = Config(initAddr = BigInt("200", 16))
  val file = "./src/main/resources/csrc/build/sysuart.hex"

  it should "be able to transfer write data, when Master issue write command. [wr:000]" in {

    val baudrate = 9600
    val clockFreq = 50

    val outDir = dutName + "-000"
    val args = getArgs(Map(
      "--top-name" -> dutName,
      "--target-dir" -> s"test_run_dir/$outDir"
    ))

    val expValues = "Hello, World!\r\n".map(_.toByte)

    println(expValues)

    test(new SimDTMSysUart(timeoutCycle)(file)(baudrate, clockFreq)).
      withAnnotations(Seq(VerilatorBackendAnnotation)).
      runPeekPoke(new SysUartUnitTester(_)(baudrate, clockFreq) {
        for (expValue <- expValues) {
          receive(expValue)
        }
      })
  }
}
