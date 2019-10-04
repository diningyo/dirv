// See LICENSE for license details.

import chisel3.iotesters._
import dirv.Config
import test.util.BaseTester

import scala.util.Random

/**
  * Unit test class for MbusIC
  * @param c Instance of SimDTMMbusIC
  */
class SysUartUnitTester(c: SimDTMSysUart) extends PeekPokeTester(c) {

  val r = new Random(1)
}

/**
  * Test class for MbusIC
  */
class SysUartTester extends BaseTester {

  val dutName = "SysUart"

  behavior of dutName

  val timeoutCycle = 100000


  implicit val cfg = Config(initAddr = BigInt("240", 16))
  val file = "/home/diningyo/prj/risc-v/dirv/src/main/resources/csrc/build/sysuart.hex"

  it should "be able to transfer write data, when Master issue write command. [wr:000]" in {

    val outDir = dutName + "-000"
    val args = getArgs(Map(
      "--top-name" -> dutName,
      "--target-dir" -> s"test_run_dir/$outDir"
    ))

    Driver.execute(args, () => new SimDTMSysUart(timeoutCycle)(file)) {
      c => new SysUartUnitTester(c) {
        step(timeoutCycle)
        fail
      }
    } should be (true)
  }
}
