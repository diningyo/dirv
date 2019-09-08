// See LICENSE for license details.

package peri.uart


import chisel3.iotesters._
import test.util.BaseTester

import scala.math.{floor, random}

/**
  * Unit tester for peri.uart.Fifo class
  * @param c dut instance
  */
class FifoUnitTester(c: Fifo) extends PeekPokeTester(c) {

  /**
    * Idle
    */
  def idle(): Unit = {
    poke(c.io.rd.enable, false)
    poke(c.io.wr.enable, false)
    step(1)
  }

  /**
    * Push data to Fifo
    * @param data data
    */
  def push(data: BigInt): Unit = {
    poke(c.io.wr.enable, true)
    poke(c.io.wr.data, data)
    step(1)
  }

  /**
    * Pop data from Fifo
    * @param exp expect value
    */
  def pop(exp: BigInt): Unit = {
    expect(c.io.rd.data, exp)
    poke(c.io.rd.enable, true)
    step(1)
  }

  /**
    * Push and Pop
    * @param data data
    * @param exp data
    */
  def pushAndPop(data: BigInt, exp: BigInt): Unit = {
    expect(c.io.rd.data, exp)
    poke(c.io.rd.enable, true)
    poke(c.io.wr.enable, true)
    poke(c.io.wr.data, data)
    step(1)
  }
}

/**
  *
  */
class FifoTester extends BaseTester {
  val dutName = "peri.uart.Fifo"
  val depth = 16

  it should "set a data into fifo when Host issues a command \"push\"" in {
    val outDir = dutName + "-fifo-push"
    val args = getArgs(Map(
      "--top-name" -> dutName,
      "--target-dir" -> s"test_run_dir/$outDir"
    ))

    Driver.execute(args, () => new Fifo(depth, true)) {
      c => new FifoUnitTester(c) {
        val setData = Range(0, 16).map(_ => floor(random * 256).toInt)

        expect(c.io.rd.empty, true)
        for ((data, idx) <- setData.zipWithIndex) {
          expect(c.io.dbg.get.wrptr, idx)
          expect(c.io.dbg.get.count, idx)
          push(data)
          expect(c.io.rd.empty, false)
          expect(c.io.rd.data, setData(0))
          expect(c.io.dbg.get.fifo(idx), data)
        }
        idle()
      }
    } should be (true)
  }

  it should "release a data from fifo when Host issues a command \"pop\"" in {
    val outDir = dutName + "-fifo-pop"
    val args = getArgs(Map(
      "--top-name" -> dutName,
      "--target-dir" -> s"test_run_dir/$outDir"
    ))

    Driver.execute(args, () => new Fifo(depth, true)) {
      c => new FifoUnitTester(c) {
        val setData = Range(0, 16).map(_ => floor(random * 256).toInt)

        // data set
        for (data <- setData) {
          push(data)
          expect(c.io.dbg.get.rdptr, 0)
        }
        idle()

        // pop
        for ((data, idx) <- setData.zipWithIndex) {
          expect(c.io.dbg.get.rdptr, idx)
          pop(data)
        }
        idle()
      }
    } should be (true)
  }

  it should "keep data count when Host issues command \"push\" and \"pop\" at the same time" in {
    val outDir = dutName + "-fifo-push-and-pop"
    val args = getArgs(Map(
      "--top-name" -> dutName,
      "--target-dir" -> s"test_run_dir/$outDir"
    ))

    Driver.execute(args, () => new Fifo(depth, true)) {
      c => new FifoUnitTester(c) {
        val txData = Range(0, 128).map(_ => floor(random * 256).toInt)
        push(txData(0))

        for ((data, exp) <- txData.tail.zip(txData)) {
          pushAndPop(data, exp)
          expect(c.io.dbg.get.count, 1)
        }
      }
    } should be (true)
  }

  it should "overwrap their pointer when pointer reaches fifo depth" in {
    val outDir = dutName + "-fifo-overwrap"
    val args = getArgs(Map(
      "--top-name" -> dutName,
      "--target-dir" -> s"test_run_dir/$outDir"
    ))

    Driver.execute(args, () => new Fifo(depth, true)) {
      c => new FifoUnitTester(c) {
        val txData = Range(0, 17).map(_ => floor(random * 256).toInt)

        for (data <- txData) {
          push(data)
        }
        expect(c.io.dbg.get.wrptr, 1)
        expect(c.io.dbg.get.count, 0)
      }
    } should be (true)
  }
}
