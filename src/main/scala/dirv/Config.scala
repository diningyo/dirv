// See LICENSE for license details.

package dirv

import dirv.io.{MemRIO, MemRWIO}

case object defaultConfig {
  val addrBits = 32
  val dataBits = 32
  val xLen = 32
}

case class Config(
    addrBits: Int = defaultConfig.addrBits,
    dataBits: Int = defaultConfig.dataBits,
    xLen: Int = defaultConfig.xLen,
    dbg: Boolean = true
) {
  val imemIOType = MemRIO
  val dmemIOType = MemRWIO
}


