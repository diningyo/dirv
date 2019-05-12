# dirv
This is My first trial project for designing RISC-V in Chisel

## Description 

- RV32I
- Machine mode Only
- User-Level ISA Version 2.2
- Privileged ISA Version 1.10
- NOT support interrupts.
- 2-stage pipelines (Fetch - Decode/Execute/Memory/Write back)
- Interface Protocol - original interface which uses ready-valid mechanism.
- Written in Chisel

## Demo

Just now, this risc-v implementation is only passed [riscv-tests](https://github.com/riscv/riscv-tests).

```scala
$ sbt
sbt:dirv> test
[info] DirvRV32ITester:
[info] Dirv
[info] - must execute RISC-V instruction add        - [riscv-tests:rv32ui-000]
[info] - must execute RISC-V instruction addi       - [riscv-tests:rv32ui-001]
[info] - must execute RISC-V instruction and        - [riscv-tests:rv32ui-002]
[info] - must execute RISC-V instruction andi       - [riscv-tests:rv32ui-003]
[info] - must execute RISC-V instruction auipc      - [riscv-tests:rv32ui-004]
[info] - must execute RISC-V instruction beq        - [riscv-tests:rv32ui-005]
[info] - must execute RISC-V instruction bge        - [riscv-tests:rv32ui-006]
[info] - must execute RISC-V instruction bgeu       - [riscv-tests:rv32ui-007]
[info] - must execute RISC-V instruction blt        - [riscv-tests:rv32ui-008]
[info] - must execute RISC-V instruction bltu       - [riscv-tests:rv32ui-009]
[info] - must execute RISC-V instruction bne        - [riscv-tests:rv32ui-010]
[info] - must execute RISC-V instruction fence_i    - [riscv-tests:rv32ui-011]
[info] - must execute RISC-V instruction jal        - [riscv-tests:rv32ui-012]
[info] - must execute RISC-V instruction jalr       - [riscv-tests:rv32ui-013]
[info] - must execute RISC-V instruction lb         - [riscv-tests:rv32ui-014]
[info] - must execute RISC-V instruction lbu        - [riscv-tests:rv32ui-015]
[info] - must execute RISC-V instruction lh         - [riscv-tests:rv32ui-016]
[info] - must execute RISC-V instruction lhu        - [riscv-tests:rv32ui-017]
[info] - must execute RISC-V instruction lui        - [riscv-tests:rv32ui-018]
[info] - must execute RISC-V instruction lw         - [riscv-tests:rv32ui-019]
[info] - must execute RISC-V instruction or         - [riscv-tests:rv32ui-020]
[info] - must execute RISC-V instruction ori        - [riscv-tests:rv32ui-021]
[info] - must execute RISC-V instruction sb         - [riscv-tests:rv32ui-022]
[info] - must execute RISC-V instruction sh         - [riscv-tests:rv32ui-023]
[info] - must execute RISC-V instruction simple     - [riscv-tests:rv32ui-024]
[info] - must execute RISC-V instruction sll        - [riscv-tests:rv32ui-025]
[info] - must execute RISC-V instruction slli       - [riscv-tests:rv32ui-026]
[info] - must execute RISC-V instruction slt        - [riscv-tests:rv32ui-027]
[info] - must execute RISC-V instruction slti       - [riscv-tests:rv32ui-028]
[info] - must execute RISC-V instruction sltiu      - [riscv-tests:rv32ui-029]
[info] - must execute RISC-V instruction sltu       - [riscv-tests:rv32ui-030]
[info] - must execute RISC-V instruction sra        - [riscv-tests:rv32ui-031]
[info] - must execute RISC-V instruction srai       - [riscv-tests:rv32ui-032]
[info] - must execute RISC-V instruction srl        - [riscv-tests:rv32ui-033]
[info] - must execute RISC-V instruction srli       - [riscv-tests:rv32ui-034]
[info] - must execute RISC-V instruction sub        - [riscv-tests:rv32ui-035]
[info] - must execute RISC-V instruction sw         - [riscv-tests:rv32ui-036]
[info] - must execute RISC-V instruction xor        - [riscv-tests:rv32ui-037]
[info] - must execute RISC-V instruction xori       - [riscv-tests:rv32ui-038]
[info] - must execute RISC-V instruction breakpoint - [riscv-tests:rv32mi-000]
[info] - must execute RISC-V instruction csr        - [riscv-tests:rv32mi-001]
[info] - must execute RISC-V instruction illegal    - [riscv-tests:rv32mi-002]
[info] - must execute RISC-V instruction ma_addr    - [riscv-tests:rv32mi-003]
[info] - must execute RISC-V instruction ma_fetch   - [riscv-tests:rv32mi-004]
[info] - must execute RISC-V instruction mcsr       - [riscv-tests:rv32mi-005]
[info] - must execute RISC-V instruction sbreak     - [riscv-tests:rv32mi-006]
[info] - must execute RISC-V instruction scall      - [riscv-tests:rv32mi-007]
[info] - must execute RISC-V instruction shamt      - [riscv-tests:rv32mi-008]
[info] ScalaTest
[info] Run completed in 1 minute, 40 seconds.
[info] Total number of tests run: 48
[info] Suites: completed 1, aborted 0
[info] Tests: succeeded 48, failed 0, canceled 0, ignored 0, pending 0
[info] All tests passed.
[info] Passed: Total 48, Failed 0, Errors 0, Passed 48
```

## Requirement

- Java 8.0 (for Scala)
- sbt
- verilator
- Build environment for RV32I (If you want to run riscv-test suite)

## Usage

Clone this repository or download to directory which you wanted.

```bash
$ git clone https://github.com/diningyo/dirv.git
$ git submodule update --init --recursive
$ cd dirv
$ sbt
```

### Generate Verilog-HDL RTL

run follow command on sbt-shell

```scala
sbt:dirv> runMain Elaborate
```

### Run riscv-tests

1. build riscv-tests

```bash
$ cd src/test/resources/
$ patch -p0 < riscv-tests.patch 
$ cd riscv-tests
$ ./configure --with-xlen=32
$ make isa
```

2. run test command on sbt-shell

```scala
$ sbt
sbt:dirv> test
```

#### To dump waveform

```scala
sbt:dirv> testOnly dirv.DirvRV32ITester -D--generate-vcd-output=on
```

#### To run specific tests

You can run specific test follow commands.

```scala
sbt:dirv> testOnly dirv.DirvRV32ITester -- -z <test_name>
sbt:dirv> testOnly dirv.DirvRV32ITester -- -z <test_no>
```

The test_name and test_No correspond with a part of bellow logs.

The log is:

> \[info\] - must execute RISC-V instruction add        - \[riscv-tests:rv32ui-000\]

And test_name and test_no are:

 - test_name : add
 - test_no : rv32ui-000

## TODO

- Implement 3-stage/5-stage pipelines.
- Support Interrupts.
- Support C-extension.
- Support M-extension.
- Evaluate riscv-compliance test suite.
- Evaluate coremark benchmark test suite.
