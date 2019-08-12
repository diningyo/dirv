// See LICENSE for license details.

package peri.mem

import chisel3._

sealed trait RAMAttr
case object RAMRO extends RAMAttr
case object RAMWO extends RAMAttr
case object RAMRW extends RAMAttr

case class RAMIOParams
(
  attr: RAMAttr,
  addrBits: Int,
  dataBits: Int,
  hasRddv: Boolean
)

class RAMIO(p: RAMIOParams) extends Bundle {
  val addr = Output(UInt(p.addrBits.W))
  val rden = if (p.attr != RAMWO) Some(Output(Bool())) else None
  val rddv = if ((p.attr != RAMWO) && p.hasRddv) Some(Input(Bool())) else None
  val rddata = if (p.attr != RAMWO) Some(Input(UInt(p.dataBits.W))) else None
  val wren = if (p.attr != RAMRO) Some(Output(Bool())) else None
  val wrstrb = if (p.attr != RAMRO) Some(Output(UInt((p.dataBits / 8).W))) else None
  val wrdata = if (p.attr != RAMRO) Some(Output(UInt(p.dataBits.W))) else None

  override def cloneType: this.type =
    new RAMIO(p).asInstanceOf[this.type]
}
