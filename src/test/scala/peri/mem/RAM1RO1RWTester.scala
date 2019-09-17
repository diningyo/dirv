// See LICENSE for license details.

package peri.mem

import chisel3.iotesters._
import test.util.BaseTester


class RAM1RO1RWUnitTester(c: RAM1RO1RWWrapper) extends PeekPokeTester(c) {

  /**
    * Idle
    * @param cycle Number of cycle which stay in Idle
    */
  def idle(cycle: Int = 1): Unit = {
    poke(c.io.a.rden.get, false)
    poke(c.io.b.wren.get, false)
    poke(c.io.b.rden.get, false)
    step(cycle)
  }

  /**
    * Read request
    * @param addr register address
    */
  def read_req(port: RAMIO, addr: Int): Unit = {
    poke(port.addr, addr)
    poke(port.rden.get, true)
  }

  /**
    * Port A single read
    * @param addr register address
    * @param exp expect value for read register
    */
  def a_single_read(addr: Int, exp: Int): Unit = {
    read_req(c.io.a, addr)
    step(1)
    poke(c.io.a.rden.get, false)
    expect(c.io.a.rddata.get, exp)
  }

  /**
    * Port B single write
    * @param addr register address
    * @param data register write data
    */
  def b_single_write(addr: Int, strb: Int, data: Int): Unit = {
    poke(c.io.b.wren.get, true)
    poke(c.io.b.addr, addr)
    poke(c.io.b.wrstrb.get, strb)
    poke(c.io.b.wrdata.get, data)
    step(1)
    poke(c.io.b.wren.get, false)
  }

  /**
    * Port B read request
    * @param addr register address
    */
  def b_read_req(addr: Int): Unit = {
    poke(c.io.b.addr, addr)
    poke(c.io.b.rden.get, true)
  }

  /**
    * Port B single read
    * @param addr register address
    * @param exp expect value for read register
    */
  def b_single_read(addr: Int, exp: Int): Unit = {
    read_req(c.io.b, addr)
    step(1)
    poke(c.io.b.rden.get, false)
    expect(c.io.b.rddata.get, exp)
  }
}

class RAM1RO1RWTester extends BaseTester {

  val dutName = "RAM1RO1RW"

  behavior of dutName

  val basic_p = RAMParams(VerilogRAM, 128, 32)

  it should "be able to read valid data from memory when a.rden comes" in {

    val outDir = dutName + "-ram"
    val args = getArgs(Map(
      "--top-name" -> dutName,
      "--target-dir" -> s"test_run_dir/$outDir-000"
    ))

    Driver.execute(args, () => new RAM1RO1RWWrapper(basic_p)) {
      c => new RAM1RO1RWUnitTester(c) {

        idle()
        b_single_write(0x0, 0xf, 0xff)
        a_single_read(0x0, 0xff)
        step(10)
      }
    } should be (true)
  }

  it should "be able to read valid data from memory when b.rden comes" in {

    val outDir = dutName + "-ram"
    val args = getArgs(Map(
      "--top-name" -> dutName,
      "--target-dir" -> s"test_run_dir/$outDir-001"
    ))

    Driver.execute(args, () => new RAM1RO1RWWrapper(basic_p)) {
      c => new RAM1RO1RWUnitTester(c) {

        idle()
        b_single_write(0x0, 0xf, 0xff)
        b_single_read(0x0, 0xff)
        step(10)
      }
    } should be (true)
  }
}
