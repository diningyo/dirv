// See LICENSE for license details.

package test.util

import chiseltest._
import org.scalatest.{BeforeAndAfterAllConfigMap, ConfigMap}
import org.scalatest.flatspec.AnyFlatSpec

trait IntToBigInt {
  /**
    * Convert an Int to unsigned (effectively 32-bit) BigInt
    * @param x  number to be converted
    * @return
    */
  def intToUnsignedBigInt(x: Int): BigInt = (BigInt(x >>> 1) << 1) | BigInt(x & 1)

  /**
    * Convert an Int to unsigned (effectively 64-bit) BigInt
    * @param x long to be converted
    * @return
    */
  def longToUnsignedBigInt(x: Long): BigInt = (BigInt(x >>> 1) << 1) | BigInt(x & 1)
}


/**
  * Base test module for Dirv
  */
abstract class BaseTester extends AnyFlatSpec with ChiselScalatestTester  {

  val defaultArgs = scala.collection.mutable.Map(
    "--generate-vcd-output" -> "on",
    "--backend-name" -> "verilator",
    "--is-verbose" -> false
  )

  /**
    * Get program arguments from ConfigMap
    * @param configMap ScalaTest ConfigMap object
    */
  //override def beforeAll(configMap: ConfigMap): Unit = {
  //  if (configMap.get("--backend-name").isDefined) {
  //    defaultArgs("--backend-name") = configMap.get("--backend-name").fold("")(_.toString)
  //  }
  //  if (configMap.get("--generate-vcd-output").isDefined) {
  //    defaultArgs("--generate-vcd-output") = configMap.get("--generate-vcd-output").fold("")(_.toString)
  //  }
  //  if (configMap.get("--is-verbose").isDefined) {
  //    defaultArgs("--is-verbose") = true
  //  }
  //}

  /**
    * Get argument for Driver.execute.
    * @param optArgs option arguments.
    * @return Driver.execute arguments as a Array.
    */
  def getArgs(optArgs: Map[String, Any]): Array[String] = {
    val argsMap = defaultArgs ++ optArgs
    argsMap.map {
      case (key: String, value: String) => s"$key=$value"
      case (key: String, value: Boolean) => if (value) key else ""
    }.toArray
  }

  /**
    * Dut module name
    * @return module name for testing
    */
  def dutName: String
}

/*
/**
  * MemBus Tester
  */
abstract class MemBusTester extends MemBusTester {
  def dutName: String = "MemBus"
}
*/