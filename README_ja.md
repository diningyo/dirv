# dirv

Chiselで書いたRISC-Vのお試し実装

## 概要

- RV32I
- Machine mode Only
- User-Level ISA Version 2.2
- Privileged ISA Version 1.10
- 割り込みは未サポート
- 2ステージパイプライン　(Fetch - Decode/Execute/Memory/Write back)
- Interface Protocol - オリジナル
- Chiselで実装

## 動作に必要なもの

- Java 8.0 (Scalaのため)
- sbt
- verilator
- RV32Iのビルド環境（riscv-testsを実行する場合）

## 動かし方

本リポジトリのデータをクローン or ダウンロードしてください。

```bash
$ git clone https://github.com/diningyo/dirv.git
$ cd dirv
$ git submodule update --init --recursive
```

### RTLの生成

sbtシェル上から以下のコマンドを実行。

```scala
$ sbt
sbt:dirv> runMain Elaborate
```

### riscv-testsの実行

1. riscv-testsのビルド

```bash
$ cd src/test/resources/
$ patch -p0 < riscv-tests.patch 
$ cd riscv-tests
$ ./configure --with-xlen=32
$ make isa
$ cd ../../../
```

2. sbtシェル上で次のコマンドを実行

```scala
$ sbt
sbt:dirv> test
```

実行すると以下の様になります。


#### 波形を取得したい場合

```scala
sbt:dirv> testOnly dirv.DirvRV32ITester -D--generate-vcd-output=on
```

#### 特定のテストを実行したい場合

以下のいずれかで特定のテストを実行可能です。

```scala
sbt:dirv> testOnly dirv.DirvRV32ITester -- -z <テスト名>
sbt:dirv> testOnly dirv.DirvRV32ITester -- -z <テスト番号>
```

下記のログの以下の部分がテスト名/テスト番号に対応しています。

 - テスト名  : add
 - テスト番号: rv32ui-000 

```scala
[info] - must execute RISC-V instruction add        - [riscv-tests:rv32ui-000]
```

## TODO

- 3ステージ版/5ステージ版の作成
- 割り込みのサポート
- C-extensionのサポート
- M-extensionのサポート
- riscv-complianceテストの評価
- coremarkの評価
- FPGA実装