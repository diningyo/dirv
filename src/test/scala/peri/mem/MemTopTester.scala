// See LICENSE for license details.

package peri.mem

import chisel3.iotesters._
import dirv.io.{MemCmd, MemIO}
import test.util.BaseTester

/**
  * Unit test class for MbusSramBridge
  * @param c Instance of SimDTMMbusSramBridge
  */
class MemTopUnitTester(c: SimDTMMemTop) extends PeekPokeTester(c) {

  val imem = c.io.dut.imem
  val dmem = c.io.dut.dmem

  def idle(cycle: Int = 1): Unit = {
    poke(imem.valid, false)
    poke(imem.r.get.ready, true)
    poke(dmem.valid, false)
    poke(dmem.w.get.valid, false)
    poke(dmem.r.get.ready, true)
    step(cycle)
  }

  /**
    * Dmem write request
    * @param addr Address to write
    */
  def d_write_req(addr: Int): Unit = {
    poke(dmem.valid, true)
    poke(dmem.addr, addr)
    poke(dmem.cmd, MemCmd.wr)
  }

  /**
    * Dmem send write data
    * @param strb Valid byte lane
    * @param data Data to write
    */
  def d_write_data(strb: Int,  data: Int): Unit = {
    poke(dmem.w.get.valid, true)
    poke(dmem.w.get.strb, strb)
    poke(dmem.w.get.data, data)
  }

  /**
    * Dmem single write
    * @param addr Address to write
    * @param strb Valid byte lane
    * @param data register address
    */
  def single_write(addr: Int, strb: Int,  data: Int, wrDataLatency: Int = 0): Unit = {
    d_write_req(addr)
    if (wrDataLatency == 0) {
      d_write_data(strb, data)
    }

    var cmd_fire = BigInt(0)
    var w_fire = BigInt(0)
    var count = 0
    while ((cmd_fire != 1) || (w_fire != 1)) {
      if (count == wrDataLatency) {
        d_write_data(strb, data)
      }

      if ((peek(dmem.valid) & peek(dmem.ready)) == 1) {
        cmd_fire = 1
      }
      if ((peek(dmem.w.get.valid) & peek(dmem.w.get.ready)) == 1) {
        w_fire = 1
      }

      println(f"(cmd_ready, w_ready) = ($cmd_fire, $w_fire)")

      step(1)

      count += 1

      if (cmd_fire == 0x1) {
        poke(dmem.valid, false)
      }
      if (w_fire == 0x1) {
        poke(dmem.w.get.valid, false)
      }
    }
    step(1)
  }

  /**
    * MemIO read request
    * @param addr Address to read
    */
  def read_req(port: MemIO, addr: Int): Unit = {
    poke(port.valid, true)
    poke(port.addr, addr)
    poke(port.cmd, MemCmd.rd)
  }

  /**
    * MemIO single read
    * @param addr Address to write
    * @param exp expect value for read register
    */
  def single_read(port: MemIO, addr: Int, exp: Int, rdDataLatency: Int = 0): Unit = {
    read_req(port, addr)

    var cmd_ready = BigInt(0)
    while (cmd_ready != 1) {
      cmd_ready = peek(port.ready)

      // This check is for Zero sram read latency.
      if (rdDataLatency == 0) {
        expect(port.r.get.valid, true)
        expect(port.r.get.data, exp)
      } else {
        expect(port.r.get.valid, false)
      }

      step(1)

      if (cmd_ready == 0x1) {
        poke(port.valid, false)
      }
    }

    if (rdDataLatency != 0) {
      var r_valid = BigInt(0)
      var count = 1
      while (r_valid != 1) {
        if (count == rdDataLatency) {
          expect(port.r.get.valid, true)
          expect(port.r.get.data, exp)
        }

        r_valid = peek(port.r.get.valid)
        step(1)
        count += 1
      }
    }
  }
}

/**
  * Test class for MbusSramBridge
  */
class MemTopTester extends BaseTester {

  val dutName = "MemTopTester"

  behavior of dutName

  val timeoutCycle = 1000
  val base_p = MemTopParams(128, 32)

  it should "be able to convert Mbus write access to Sram write access" in {

    val outDir = dutName + "-000"
    val args = getArgs(Map(
      "--top-name" -> dutName,
      "--target-dir" -> s"test_run_dir/$outDir"
    ))

    Driver.execute(args, () => new SimDTMMemTop(base_p)(timeoutCycle)) {
      c => new MemTopUnitTester(c) {
        idle(10)
        single_write(0x1, 0xf, 0x12345678)
        idle(10)
      }
    } should be (true)
  }

  it should "wait for issuing Sram write, when Mbus write data doesn't come." in {

    val outDir = dutName + "-001"
    val args = getArgs(Map(
      "--top-name" -> dutName,
      "--target-dir" -> s"test_run_dir/$outDir"
    ))

    Driver.execute(args, () => new SimDTMMemTop(base_p)(timeoutCycle)) {
      c => new MemTopUnitTester(c) {
        for (delay <- 0 until 5) {
          idle(2)
          single_write(delay, 0xf, 0x12345678, delay)
          idle(2)
        }
      }
    } should be (true)
  }
}
