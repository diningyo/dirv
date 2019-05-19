// See LICENSE for license details.

package dirv

import chisel3._
import chisel3.util.log2Ceil
import dirv.io.{MemRIO, MemRWIO}

case object Consts {
  val rv32Xlen = 32
  val rviRegNum = 32
  val rveRegNum = 16
  val nop = 0x4033.U(32.W)
}

// arch
sealed trait RVArch {
  def xlen: Int
  def regNum: Int
  def mpfrBits: Int = log2Ceil(regNum)
}
case object RV32I extends RVArch {
  override def xlen: Int = Consts.rv32Xlen
  override def regNum: Int = Consts.rviRegNum
}
case object RV32E extends RVArch {
  override def xlen: Int = Consts.rv32Xlen
  override def regNum: Int = Consts.rveRegNum
}

case object defaultConfig {
  val initAddr = BigInt("00001000", 16)
  val addrBits = 32
  val dataBits = 32
  val vendorId = BigInt("1", 16)
  val archId = BigInt("0", 16)
  val impId = BigInt("0", 16)
  val hartId = BigInt("0", 16)
}

case class Config(
    arch: RVArch = RV32I,
    initAddr: BigInt = defaultConfig.initAddr,
    addrBits: Int = defaultConfig.addrBits,
    dataBits: Int = defaultConfig.dataBits,
    vendorId: BigInt = defaultConfig.vendorId,
    archId: BigInt = defaultConfig.archId,
    impId: BigInt = defaultConfig.impId,
    hartId: BigInt = defaultConfig.hartId,
    dbg: Boolean = true
) {
  val imemIOType = MemRIO
  val dmemIOType = MemRWIO
}


