// See LICENSE for license details.

import chisel3._

import dirv._

object Elaborate extends App {

  implicit val cfg: Config = Config()

  Driver.execute(
    Array("-tn=dirv", "-td=rtl/dirv"),
    () => new SimDtm(""))
}

object Help extends App {

  implicit val cfg: Config = Config()

  Driver.execute(
    Array("--help"),
    () => new SimDtm(""))
}


