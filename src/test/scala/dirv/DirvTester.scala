// See LICENSE for license details.

package dirv

import chisel3.iotesters.{ChiselFlatSpec, Driver}
import org.scalatest.{BeforeAndAfterAllConfigMap, ConfigMap, ParallelTestExecution}


/**
  * Object for parameter of riscv-tests
  */
object RiscvTestsParams {

  /**
    * riscv-tests/isa/rv32ui
    */
  val rv32uiTestList: Seq[((String, String), Int)] = Map(
    "add"        -> "rv32ui-p-add.hex",
    "addi"       -> "rv32ui-p-addi.hex",
    "and"        -> "rv32ui-p-and.hex",
    "andi"       -> "rv32ui-p-andi.hex",
    "auipc"      -> "rv32ui-p-auipc.hex",
    "beq"        -> "rv32ui-p-beq.hex",
    "bge"        -> "rv32ui-p-bge.hex",
    "bgeu"       -> "rv32ui-p-bgeu.hex",
    "blt"        -> "rv32ui-p-blt.hex",
    "bltu"       -> "rv32ui-p-bltu.hex",
    "bne"        -> "rv32ui-p-bne.hex",
    "fence_i"    -> "rv32ui-p-fence_i.hex",
    "jal"        -> "rv32ui-p-jal.hex",
    "jalr"       -> "rv32ui-p-jalr.hex",
    "lb"         -> "rv32ui-p-lb.hex",
    "lbu"        -> "rv32ui-p-lbu.hex",
    "lh"         -> "rv32ui-p-lh.hex",
    "lhu"        -> "rv32ui-p-lhu.hex",
    "lui"        -> "rv32ui-p-lui.hex",
    "lw"         -> "rv32ui-p-lw.hex",
    "or"         -> "rv32ui-p-or.hex",
    "ori"        -> "rv32ui-p-ori.hex",
    "sb"         -> "rv32ui-p-sb.hex",
    "sh"         -> "rv32ui-p-sh.hex",
    "simple"     -> "rv32ui-p-simple.hex",
    "sll"        -> "rv32ui-p-sll.hex",
    "slli"       -> "rv32ui-p-slli.hex",
    "slt"        -> "rv32ui-p-slt.hex",
    "slti"       -> "rv32ui-p-slti.hex",
    "sltiu"      -> "rv32ui-p-sltiu.hex",
    "sltu"       -> "rv32ui-p-sltu.hex",
    "sra"        -> "rv32ui-p-sra.hex",
    "srai"       -> "rv32ui-p-srai.hex",
    "srl"        -> "rv32ui-p-srl.hex",
    "srli"       -> "rv32ui-p-srli.hex",
    "sub"        -> "rv32ui-p-sub.hex",
    "sw"         -> "rv32ui-p-sw.hex",
    "xor"        -> "rv32ui-p-xor.hex",
    "xori"       -> "rv32ui-p-xori.hex"
  ).toSeq.sortBy(_._1).zipWithIndex

  /**
    * riscv-tests/isa/rv32mi
    */
  val rv32miTestList: Seq[((String, String), Int)] = Map(
    "breakpoint" -> "rv32mi-p-breakpoint.hex",
    "csr"        -> "rv32mi-p-csr.hex",
    "illegal"    -> "rv32mi-p-illegal.hex",
    "ma_addr"    -> "rv32mi-p-ma_addr.hex",
    "ma_fetch"   -> "rv32mi-p-ma_fetch.hex",
    "mcsr"       -> "rv32mi-p-mcsr.hex",
    "sbreak"     -> "rv32mi-p-sbreak.hex",
    "scall"      -> "rv32mi-p-scall.hex",
    "shamt"      -> "rv32mi-p-shamt.hex"
  ).toSeq.sortBy(_._1).zipWithIndex

  /**
    * riscv-tests/isa/rv32ua
    */
  val rv32uaTestList: Seq[((String, String), Int)] = Map(
    "amoadd_w"   -> "rv32ua-p-amoadd_w.hex",
    "amoand_w"   -> "rv32ua-p-amoand_w.hex",
    "amomax_w"   -> "rv32ua-p-amomax_w.hex",
    "amomaxu_w"  -> "rv32ua-p-amomaxu_w.hex",
    "amomin_w"   -> "rv32ua-p-amomin_w.hex",
    "amominu_w"  -> "rv32ua-p-amominu_w.hex",
    "amoor_w"    -> "rv32ua-p-amoor_w.hex",
    "amoswap_w"  -> "rv32ua-p-amoswap_w.hex",
    "amoxor_w"   -> "rv32ua-p-amoxor_w.hex",
    "lrsc"       -> "rv32ua-p-lrsc.hex"
  ).toSeq.sortBy(_._1).zipWithIndex

  /**
    * riscv-tests/isa/rv32uc
    */
  val rv32ucTestList: Seq[((String, String), Int)] = Map(
    "rvc"        -> "rv32uc-p-rvc.hex"
  ).toSeq.sortBy(_._1).zipWithIndex

  /**
    * riscv-tests/isa/rv32um
    */
  val rv32umTestList: Seq[((String, String), Int)] = Map(
    "div"        -> "rv32um-p-div.hex",
    "divu"       -> "rv32um-p-divu.hex",
    "mul"        -> "rv32um-p-mul.hex",
    "mulh"       -> "rv32um-p-mulh.hex",
    "mulhsu"     -> "rv32um-p-mulhsu.hex",
    "mulhu"      -> "rv32um-p-mulhu.hex",
    "rem"        -> "rv32um-p-rem.hex",
    "remu"       -> "rv32um-p-remu.hex"
  ).toSeq.sortBy(_._1).zipWithIndex
}


/**
  * Base test module for Dirv
  */
abstract class DirvBaseTester extends ChiselFlatSpec with BeforeAndAfterAllConfigMap  {

  val defaultArgs = scala.collection.mutable.Map(
    "--generate-vcd-output" -> "off",
    "--backend-name" -> "verilator",
    "--is-verbose" -> false
  )

  val isaTestDir = "src/test/resources/riscv-tests/isa/"

  /**
    * Get program arguments from ConfigMap
    * @param configMap ScalaTest ConfigMap object
    */
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

  def dutName: String = "Dirv"

  /**
    * Run riscv-tests
    * @param category riscv-tests category name (rv32ui/rv32mi etc..)
    * @param testInfo test information.
    *                 ((instruction name, test hex filepath), seq No.)
    * @param cfg Dirv configuration object.
    */
  def runRiscvTests(category: String,
                    testInfo: ((String, String), Int))
                   (implicit cfg: Config): Unit = {

    val ((instruction, testFile), seqNo) = testInfo

    val testFilePath = isaTestDir + testFile

    it must f"execute RISC-V instruction $instruction%-10s - [riscv-tests:$category%s-$seqNo%03d]" in {

      val args = getArgs(Map(
        "--top-name" -> instruction,
        "--target-dir" -> s"test_run_dir/isa/$instruction"
      ))

      Driver.execute(args, () => new SimDtm(testFilePath)) {
        c => new DirvUnitTester(c)
      } should be (true)
    }
  }
}

/**
  * Test module for RV32I
  */
//class DirvRV32ITester extends DirvBaseTester with ParallelTestExecution {
class DirvRV32ITester extends DirvBaseTester {

  behavior of dutName

  implicit val cfg: Config = Config()

  val testList = Map(
    "rv32ui" -> RiscvTestsParams.rv32uiTestList,
    "rv32mi" -> RiscvTestsParams.rv32miTestList
  )

  for ((subTestGroup, subTestList) <- testList; test <- subTestList) {
    runRiscvTests(subTestGroup, test)
  }
}
