
package peri.uart

import scala.math.{floor, random}
import chisel3.iotesters._
import mbus.{MbusRW, MbusSramBridgeParams}
import test.util.BaseTester


class RegTopUnitTester(c: RegTop) extends PeekPokeTester(c) {

  def idle(cycle: Int = 1): Unit = {
    poke(c.io.sram.wren.get, false)
    poke(c.io.sram.rden.get, false)
    step(cycle)
  }

  /**
    * Register write
    * @param addr register address
    * @param data register write data
    */
  def hwrite(addr: Int, data: Int): Unit = {
    poke(c.io.sram.addr, addr)
    poke(c.io.sram.wren.get, true)
    poke(c.io.sram.wrdata.get, data)
    step(1)
  }

  /**
    * Register read
    * @param addr register address
    * @param exp expect value for read register
    */
  def hread(addr: Int, exp: Int): Unit = {
    poke(c.io.sram.addr, addr)
    poke(c.io.sram.rden.get, true)
    step(1)
    poke(c.io.sram.rden.get, false)
    expect(c.io.sram.rddv.get, true)
    expect(c.io.sram.rddata.get, exp)
  }

  /**
    * RxFifo write from Ctrl
    * @param data register write data
    */
  def uwrite(data: Int): Unit = {
    poke(c.io.r2c.rx.enable, true)
    poke(c.io.r2c.rx.data, data)
    step(1)
    poke(c.io.r2c.rx.enable, false)
    step(1)
  }

  /**
    * RxFifo write from Ctrl
    */
  def txfifoAck(): Unit = {
    poke(c.io.r2c.tx.enable, true)
    step(1)
  }
}


class RegTopTester extends BaseTester {

  val dutName = "RegTop"

  behavior of dutName

  val sp = MbusSramBridgeParams(MbusRW, 4, 32)

  it should s"be able to write TxFifo register from Host [$dutName-001]" in {

    val outDir = dutName + "-txfifo"
    val args = getArgs(Map(
      "--top-name" -> dutName,
      "--target-dir" -> s"test_run_dir/$outDir"
    ))

    Driver.execute(args, () => new RegTop(sp.ramIOParams)(true)) {
      c => new RegTopUnitTester(c) {
        val txData = Range(0, 10).map(_ => floor(random * 256).toInt)

        idle()
        for (d <- txData) {
          hwrite(RegInfo.txFifo, d)
          expect(c.io.dbg.get.txFifo, d)
        }
      }
    } should be (true)
  }

  it should s"be asserted tx.empty when host write TxFifo register [$dutName-002]" in {

    val outDir = dutName + "-txfifo-txempty"
    val args = getArgs(Map(
      "--top-name" -> dutName,
      "--target-dir" -> s"test_run_dir/$outDir"
    ))

    Driver.execute(args, () => new RegTop(sp.ramIOParams)(true)) {
      c => new RegTopUnitTester(c) {
        val txData = 0xff

        idle()
        expect(c.io.r2c.tx.empty, true)
        hwrite(RegInfo.txFifo, txData)
        expect(c.io.r2c.tx.empty, false)
      }
    } should be (true)
  }

  it should s"be able to read Stat register from Host [$dutName-003]" in {
    val outDir = dutName + "-stat"
    val args = getArgs(Map(
      "--top-name" -> dutName,
      "--target-dir" -> s"test_run_dir/$outDir"
    ))

    Driver.execute(args, () => new RegTop(sp.ramIOParams)(true)) {
      c => new RegTopUnitTester(c) {
        val txData = 0xff

        idle()
        hwrite(RegInfo.stat, 0xff)
        idle()
        hread(RegInfo.stat, 0x4)
        idle()
        step(5)
      }
    } should be (true)
  }

  ignore should s"be able to read Ctrl register from Host [$dutName-004]" in {
  }

  behavior of "RxFifo"

  it should s"be written from CtrlHost [$dutName-101]" in {
    val outDir = dutName + "-rxfifo-write"
    val args = getArgs(Map(
      "--top-name" -> dutName,
      "--target-dir" -> s"test_run_dir/$outDir"
    ))

    Driver.execute(args, () => new RegTop(sp.ramIOParams)(true)) {
      c => new RegTopUnitTester(c) {
        val txData = Range(0, 10).map(_ => floor(random * 256).toInt)

        idle()
        for (d <- txData) {
          uwrite(d)
          expect(c.io.dbg.get.rxFifo, txData(0))
        }
      }
    } should be (true)
  }

  it should s"be able to read RxFifo register from Host [$dutName-102]" in {

    val outDir = dutName + "-rxfifo-read"
    val args = getArgs(Map(
      "--top-name" -> dutName,
      "--target-dir" -> s"test_run_dir/$outDir"
    ))

    Driver.execute(args, () => new RegTop(sp.ramIOParams)(true)) {
      c => new RegTopUnitTester(c) {
        val txData = Range(0, 10).map(_ => floor(random * 256).toInt)

        idle()
        for (d <- txData) {
          hwrite(RegInfo.txFifo, d)
          expect(c.io.dbg.get.txFifo, d)
        }
      }
    } should be (true)
  }
}
