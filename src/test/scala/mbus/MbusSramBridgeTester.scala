// See LICENSE for license details.

package mbus

import chisel3.iotesters._

import test.util.BaseTester

/**
  * Unit test class for MbusSramBridge
  * @param c Instance of SimDTMMbusSramBridge
  */
class MbusSramBridgeUnitTester(c: SimDTMMbusSramBridge) extends PeekPokeTester(c) {

}

/**
  * Test class for MbusSramBridge
  */
class MbusSramBridgeTester extends BaseTester {

  val dutName = "MbusSramBridge"

  behavior of dutName

  val timeoutCycle = 1000

  it should "" in {

    val outDir = dutName + "-000"
    val args = getArgs(Map(
      "--top-name" -> dutName,
      "--target-dir" -> s"test_run_dir/$outDir"
    ))

    Driver.execute(args, () => new SimDTMMbusSramBridge()(timeoutCycle)) {
      c => new MbusSramBridgeUnitTester(c) {
        fail
      }
    } should be (true)
  }
}
