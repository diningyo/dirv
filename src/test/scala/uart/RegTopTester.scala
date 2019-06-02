
package uart

import scala.math.{floor, random}

import chisel3.iotesters._


class RegTopUnitTester(c: RegTop) extends PeekPokeTester(c) {

  def idle(cycle: Int = 1): Unit = {
    poke(c.io.regR.enable, false)
    poke(c.io.regW.enable, false)
    step(cycle)
  }

  /**
    * Register write
    * @param addr register address
    * @param data register write data
    */
  def hwrite(addr: Int, data: Int): Unit = {
    poke(c.io.regW.addr, addr)
    poke(c.io.regW.enable, true)
    poke(c.io.regW.strb, 0xf)
    poke(c.io.regW.data, data)
    step(1)
  }

  /**
    * Register read
    * @param addr register address
    * @param exp expect value for read register
    */
  def hread(addr: Int, exp: Int): Unit = {
    poke(c.io.regR.addr, addr)
    poke(c.io.regR.enable, true)
    step(1)
    expect(c.io.regR.dataValid, true)
    expect(c.io.regR.data, exp)
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

  it should "be able to write TxFifo register from Host" in {

    val outDir = dutName + "-txfifo"
    val args = getArgs(Map(
      "--top-name" -> dutName,
      "--target-dir" -> s"test_run_dir/$outDir"
    ))

    Driver.execute(args, () => new RegTop(true)) {
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

  it should "be asserted tx.empty when host write TxFifo register" in {

    val outDir = dutName + "-txfifo-txempty"
    val args = getArgs(Map(
      "--top-name" -> dutName,
      "--target-dir" -> s"test_run_dir/$outDir"
    ))

    Driver.execute(args, () => new RegTop(true)) {
      c => new RegTopUnitTester(c) {
        val txData = 0xff

        idle()
        expect(c.io.r2c.tx.empty, true)
        hwrite(RegInfo.txFifo, txData)
        expect(c.io.r2c.tx.empty, false)
      }
    } should be (true)
  }

  it should "be able to read Stat register from Host" in {
    fail
  }

  it should "be able to read Ctrl register from Host" in {
    fail
  }

  behavior of "RxFifo"
  it should "be written from CtrlHost" in {
    val outDir = dutName + "-rxfifo-write"
    val args = getArgs(Map(
      "--top-name" -> dutName,
      "--target-dir" -> s"test_run_dir/$outDir"
    ))

    Driver.execute(args, () => new RegTop(true)) {
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

  it should "be able to read RxFifo register from Host" in {

    val outDir = dutName + "-rxfifo-read"
    val args = getArgs(Map(
      "--top-name" -> dutName,
      "--target-dir" -> s"test_run_dir/$outDir"
    ))

    Driver.execute(args, () => new RegTop(true)) {
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
