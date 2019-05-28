// See README.md for license details.

package uart

import scala.util.control.Breaks

import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}
import org.scalatest.{BeforeAndAfterAllConfigMap, ConfigMap}

/**
  * Uartのベース環境
  */
abstract class BaseTester extends ChiselFlatSpec with BeforeAndAfterAllConfigMap  {

  val defaultArgs = scala.collection.mutable.Map(
    "--generate-vcd-output" -> "on",
    "--backend-name" -> "treadle",
    "--is-verbose" -> false
  )

  override def beforeAll(configMap: ConfigMap): Unit = {
    if (configMap.get("--backend-name").isDefined) {
      defaultArgs("--backend-name") = configMap.get("--backend-name").fold("")(_.toString)
    }
    if (configMap.get("--generate-vcd-output").isDefined) {
      defaultArgs("--generate-vcd-output") = configMap.get("--generate-vcd-output").fold("")(_.toString)
    }
    if (configMap.get("--is-verbose").isDefined) {
      defaultArgs("--is-verbose") = true
    }
  }

  def getArgs(optArgs: Map[String, Any]): Array[String] = {
    val argsMap = defaultArgs ++ optArgs
    argsMap.map {
      case (key: String, value: String) => s"$key=$value"
      case (key: String, value: Boolean) => if (value) key else ""
    }.toArray
  }

  def dutName: String
}
