// See LICENSE for license details.

package dirv

import dirv.io.{MemRIO, MemRWIO}

case object Consts {
  val rv32XLen = 32
  val rviRegNum = 32
  val rveRegNum = 16
}

// arch
sealed abstract trait RVArch {
  def xLen(): Int
  def regNum(): Int
}
case object RV32I extends RVArch {
  override def xLen(): Int = Consts.rv32XLen
  override def regNum(): Int = Consts.rviRegNum
}
case object RV32E extends RVArch {
  override def xLen(): Int = Consts.rv32XLen
  override def regNum(): Int = Consts.rveRegNum
}

case object defaultConfig {
  val initAddr = BigInt("00010000", 16)
  val rv32Bits = 32
  val addrBits = 32
  val dataBits = 32
}

case class Config(
    arch: RVArch = RV32I,
    initAddr: BigInt = defaultConfig.initAddr,
    addrBits: Int = defaultConfig.addrBits,
    dataBits: Int = defaultConfig.dataBits,
    dbg: Boolean = true
) {
  val imemIOType = MemRIO
  val dmemIOType = MemRWIO
}


