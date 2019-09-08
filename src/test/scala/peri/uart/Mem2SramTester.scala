// See LICENSE for license details.

package peri.uart

import scala.math.{floor, random}
import chisel3.iotesters._
import mbus._
import test.util.BaseTester


class Mem2SramUnitTester(c: Mem2Sram) extends PeekPokeTester(c) {

  def idle(cycle: Int = 1): Unit = {
    poke(c.io.regR.enable, false)
    poke(c.io.regW.enable, false)
    step(cycle)
  }

  /**
    * MemIO write
    * @param addr register address
    * @param data register write data
    */
  def write(addr: Int, data: Int): Unit = {
    poke(c.io.mem.valid, true)
    poke(c.io.mem.addr, addr)
    poke(c.io.mem.cmd, MemCmd.wr)
    poke(c.io.mem.size, MemSize.word)
    poke(c.io.mem.w.get.valid, true)
    poke(c.io.mem.w.get.data, data)
    step(1)
    poke(c.io.mem.valid, false)
    poke(c.io.mem.w.get.valid, false)

    expect(c.io.regW.enable, true)
    expect(c.io.regW.addr, addr)
    expect(c.io.regW.data, data)
    expect(c.io.regR.enable, false)
    step(1)
    expect(c.io.regW.enable, false)
    expect(c.io.regW.addr, addr)
    expect(c.io.regW.data, data)
  }

  /**
    * MemIO read
    * @param addr register address
    * @param exp expect value for read register
    */
  def read(addr: Int, exp: Int): Unit = {
    poke(c.io.regR.addr, addr)
    poke(c.io.regR.enable, true)
    step(1)
    expect(c.io.regR.dataValid, true)
    expect(c.io.regR.data, exp)
  }
}


class Mem2SramTester extends BaseTester {

  val dutName = "Mem2Sram"

  behavior of dutName

  it should "convert MemIO.write access to sram write" in {

    val outDir = dutName + "-txfifo"
    val args = getArgs(Map(
      "--top-name" -> dutName,
      "--target-dir" -> s"test_run_dir/$outDir"
    ))

    Driver.execute(args, () => new Mem2Sram) {
      c => new Mem2SramUnitTester(c) {
        //val txData = Range(0, 10).map(_ => floor(random * 256).toInt)

        idle()
        write(0x0, 0xff)
      }
    } should be (true)
  }
}
