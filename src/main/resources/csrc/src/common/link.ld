/*
 * @file       link_ram.ld
 * @brief      Linker scrypt for Dirv firmware
 *
 *  Created on: 2019/09/25
 *      Author: diningyo
 */

OUTPUT_ARCH( "riscv" )
ENTRY(_start)

MEMORY {
  RAM (rwx) : ORIGIN = 0x00000000, LENGTH = 64K
}

DATA_SECTION_OFFSET = 0x8000; /* .data/.sdata/.tdata/.bss/.sbss/.tbss */
STACK_SIZE = 1024;             /* stack region size */

CL_SIZE = 32;

SECTIONS {

  /* code segment */
  .text.init ORIGIN(RAM) : {
    PROVIDE(__RAM_TOP__ = ADDR(.text.init));
    FILL(0);
    . = 0x100 - 12;
    RAM_EXIT = .;
    LONG(0x13);
    RAM_STOP = .;
    LONG(0x6F);
    LONG(-1);
    . = 0x100;
    *crt.o(.text .text.*)
  } >RAM

  .text : {
    *(.text .text.*)
    *(sc_test_section)
    . = ALIGN(CL_SIZE);
  } >RAM

  .rodata : {
    _gp = . + 0x800;
    *(.srodata.cst16) *(.srodata.cst8) *(.srodata.cst4) *(.srodata.cst2) *(.srodata*)
    . = ALIGN(16);
  } >RAM

  /* data segment */
  .data ORIGIN(RAM) + DATA_SECTION_OFFSET : {
    __data_load = .;
    PROVIDE(__RAM_DATA_START__ = LOADADDR(.data));
    PROVIDE(__RAM_DATA_START__ = ADDR(.data));
    *(.data .data.*)
    . = ALIGN(CL_SIZE);
  } >RAM

  .sdata : {
    *(.sdata .sdata.* .gnu.linkonce.s.*)
    . = ALIGN(CL_SIZE);
  } >RAM

  /* thread-local data segment */
  .tdata : {
    *(.tdata .tdata.*)
    . = ALIGN(CL_SIZE);
    PROVIDE(__RAM_DATA_END__ = .);
  } >RAM

  /* bss segment */
  .bss : {
    PROVIDE(__BSS_START__ = .);
    *(.bss .bss.*)
    . = ALIGN(CL_SIZE);
  } >RAM

  .sbss : {
    *(.sbss .sbss.* .gnu.linkonce.sb.*)
    *(.scommon)
  } >RAM

  .tbss : {
    *(.tbss .tbss.*)
    . = ALIGN(CL_SIZE);
    PROVIDE(__BSS_END__ = .);
  } >RAM

  _end = .;
  PROVIDE(__end = .);

  /* End of uninitalized data segement */

  .stack ORIGIN(RAM) + LENGTH(RAM) - STACK_SIZE : {
    PROVIDE(__STACK_START__ = .);
    . += STACK_SIZE;
    PROVIDE(__C_STACK_TOP__ = .);
    PROVIDE(__STACK_END__ = .);
  } >RAM

  /* firm will starts top address + 0x200*/
  __dirv_firm_start = ORIGIN(RAM) + 0x200;

  /DISCARD/ : {
    *(.eh_frame .eh_frame.*)
  }
}
