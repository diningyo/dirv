/*
// See LICENSE for license details.

package dirv

import chisel3._

/**
  * Run some programs with Dirv.
  */
object RunDirvProgram extends App {
  val programHexFile = ""
  implicit val cfg: Config = Config()
  Driver.execute(args, () => new SimDtm(programHexFile)) {
    c => new DirvUnitTester(c)
  }
}

/**
  * Run all Dirv's tests
  */
object RunRegression extends App {
  implicit val cfg: Config = Config()

  // TODO :: add execute whole tests
}
*/