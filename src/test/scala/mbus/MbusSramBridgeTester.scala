// See LICENSE for license details.

package mbus

import chisel3.iotesters._
import dirv.io.MemCmd
import test.util.BaseTester

/**
  * Unit test class for MbusSramBridge
  * @param c Instance of SimDTMMbusSramBridge
  */
class MbusSramBridgeUnitTester(c: SimDTMMbusSramBridge) extends PeekPokeTester(c) {

  val mbus = c.io.dut.mbus
  val sram = c.io.dut.sram

  def idle(cycle: Int = 1): Unit = {
    poke(mbus.valid, false)
    poke(mbus.w.get.valid, false)
    poke(mbus.r.get.ready, true)
    poke(sram.rddv.get, false)
    step(cycle)
  }

  /**
    * MemIO write request
    * @param addr Address to write
    */
  def write_req(addr: Int): Unit = {
    poke(mbus.valid, true)
    poke(mbus.addr, addr)
    poke(mbus.cmd, MemCmd.wr)
  }

  /**
    * MemIO send write data
    * @param strb Valid byte lane
    * @param data Data to write
    */
  def write_data(strb: Int,  data: Int): Unit = {
    poke(mbus.w.get.valid, true)
    poke(mbus.w.get.strb, strb)
    poke(mbus.w.get.data, data)
  }

  /**
    * MemIO single write
    * @param addr Address to write
    * @param strb Valid byte lane
    * @param data register address
    */
  def single_write(addr: Int, strb: Int,  data: Int, wrDataLatency: Int = 0): Unit = {
    write_req(addr)
    if (wrDataLatency == 0) {
      write_data(strb, data)
    }

    var cmd_fire = BigInt(0)
    var w_fire = BigInt(0)
    var count = 0
    while ((cmd_fire != 1) || (w_fire != 1)) {
      if (count == wrDataLatency) {
        write_data(strb, data)
      }

      if ((peek(mbus.valid) & peek(mbus.ready)) == 1) {
        cmd_fire = 1
      }
      if ((peek(mbus.w.get.valid) & peek(mbus.w.get.ready)) == 1) {
        w_fire = 1
      }

      println(f"(cmd_ready, w_ready) = ($cmd_fire, $w_fire)")

      if (cmd_fire == 1 && w_fire == 1) {
        expect(sram.addr, addr)
        expect(sram.wren.get, true)
        expect(sram.wrstrb.get, strb)
        expect(sram.wrdata.get, data)
      }

      step(1)

      count += 1

      if (cmd_fire == 0x1) {
        poke(mbus.valid, false)
      }
      if (w_fire == 0x1) {
        poke(mbus.w.get.valid, false)
      }
    }
    step(1)
  }

  /**
    * MemIO read request
    * @param addr Address to read
    */
  def read_req(addr: Int): Unit = {
    poke(mbus.valid, true)
    poke(mbus.addr, addr)
    poke(mbus.cmd, MemCmd.rd)
  }

  /**
    * Sram return read data
    * @param data Data to read
    */
  def return_read_data(data: Int): Unit = {
    poke(sram.rddv.get, true)
    poke(sram.rddata.get, data)
  }

  /**
    * MemIO single read
    * @param addr Address to write
    * @param exp expect value for read register
    */
  def single_read(addr: Int, exp: Int): Unit = {
    read_req(addr)

    var cmd_ready = BigInt(0)
    while (cmd_ready != 1) {
      cmd_ready = peek(mbus.ready)

      if (cmd_ready == 1) {
        expect(sram.addr, addr)
        expect(sram.rden.get, true)
      }

      // This check is for Zero sram read latency.
      if (peek(sram.rddv.get) == 1) {
        expect(mbus.r.get.valid, true)
        expect(mbus.r.get.data, exp)
      }

      step(1)

      if (cmd_ready == 0x1) {
        poke(mbus.valid, false)
      }
    }

    var r_valid = BigInt(0)
    while (r_valid != 1) {
      r_valid = peek(mbus.r.get.valid)

      // This check is for Zero sram read latency.
      if (peek(sram.rddv.get) == 1) {
        expect(mbus.r.get.valid, true)
        expect(mbus.r.get.data, exp)
      }

      step(1)
    }
  }
}

/**
  * Test class for MbusSramBridge
  */
class MbusSramBridgeTester extends BaseTester {

  val dutName = "MbusSramBridge"

  behavior of dutName

  val timeoutCycle = 1000
  val base_p = MbusSramBridgeParams(RWMbusIO, 32, 32)

  it should "be able to convert Mbus write access to Sram write access" in {

    val outDir = dutName + "-000"
    val args = getArgs(Map(
      "--top-name" -> dutName,
      "--target-dir" -> s"test_run_dir/$outDir"
    ))

    Driver.execute(args, () => new SimDTMMbusSramBridge(base_p)(timeoutCycle)) {
      c => new MbusSramBridgeUnitTester(c) {
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

    Driver.execute(args, () => new SimDTMMbusSramBridge(base_p)(timeoutCycle)) {
      c => new MbusSramBridgeUnitTester(c) {
        idle(10)
        single_write(0x1, 0xf, 0x12345678, 1)
        idle(10)

      }
    } should be (true)
  }

  it should "be able to convert Mbus read access to Sram read access" +
    "and return read data from Sram port." in {

    val outDir = dutName + "-100"
    val args = getArgs(Map(
      "--top-name" -> dutName,
      "--target-dir" -> s"test_run_dir/$outDir"
    ))

    Driver.execute(args, () => new SimDTMMbusSramBridge(base_p)(timeoutCycle)) {
      c => new MbusSramBridgeUnitTester(c) {
        idle(10)
        single_write(0x1, 0xf, 0x12345678)
        single_read(0x1, 0x12345678)
        idle(10)

      }
    } should be (true)
  }
}
