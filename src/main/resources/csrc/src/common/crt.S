/*
 * @file       crt.S
 * @brief      CRT for Dirv firmware
 *
 *  Created on: 2019/09/25
 *      Author: diningyo
 */

#ifndef TMR_INT_HANDLER
#define TMR_INT_HANDLER timer_int_handler
#endif

#ifndef EXT_INT_HANDLER
#define EXT_INT_HANDLER ext_int_handler
#endif

#define MSTATUS_MIE         0x00000008

#define REGBYTES 4

    .globl _start
    .globl main
    .globl trap_entry
    .globl handle_trap
    .globl dirv_exit
    .weak trap_entry, handle_trap, dirv_exit

    .text
    .org (0x40*3) // skip user/supervisor/hypervisor trap entries
    .align 6

// 0x1c0
machine_trap_entry:
    .skip 0x20

vec_machine_timer:
    j    vec_machine_tmr_handler

    .skip 0x8

vec_machine_ext:
    j    vec_machine_ext_handler

    .align 6

_start:
    # clear bss
    la      a1, __BSS_START__
    la      a2, __BSS_END__
    j       4f
3:  sw      zero, 0(a1)
    add     a1, a1, 4
4:  bne     a1, a2, 3b
    auipc   gp, %hi(_gp)
    addi    gp, gp, %lo(_gp)
    la      sp, __C_STACK_TOP__

    // TODO : Enable if dirv support interrupts.
    // enable global interrrupt
    //csrs    mstatus, MSTATUS_MIE

    // vectored mode
    la      t0, __RAM_TOP__
    addi    t0, t0, 0x1C0  // set RAM machine handler base address
    ori     t0, t0, 0x1    // set vectored mode
    csrw    mtvec, t0

    li      a0, 0
    li      a1, 0
    jal     main
    j       dirv_exit

trap_entry:
    addi sp, sp, -272

    sw x1, 1*REGBYTES(sp)
    sw x2, 2*REGBYTES(sp)
    sw x3, 3*REGBYTES(sp)
    sw x4, 4*REGBYTES(sp)
    sw x5, 5*REGBYTES(sp)
    sw x6, 6*REGBYTES(sp)
    sw x7, 7*REGBYTES(sp)
    sw x8, 8*REGBYTES(sp)
    sw x9, 9*REGBYTES(sp)
    sw x10, 10*REGBYTES(sp)
    sw x11, 11*REGBYTES(sp)
    sw x12, 12*REGBYTES(sp)
    sw x13, 13*REGBYTES(sp)
    sw x14, 14*REGBYTES(sp)
    sw x15, 15*REGBYTES(sp)
    sw x16, 16*REGBYTES(sp)
    sw x17, 17*REGBYTES(sp)
    sw x18, 18*REGBYTES(sp)
    sw x19, 19*REGBYTES(sp)
    sw x20, 20*REGBYTES(sp)
    sw x21, 21*REGBYTES(sp)
    sw x22, 22*REGBYTES(sp)
    sw x23, 23*REGBYTES(sp)
    sw x24, 24*REGBYTES(sp)
    sw x25, 25*REGBYTES(sp)
    sw x26, 26*REGBYTES(sp)
    sw x27, 27*REGBYTES(sp)
    sw x28, 28*REGBYTES(sp)
    sw x29, 29*REGBYTES(sp)
    sw x30, 30*REGBYTES(sp)
    sw x31, 31*REGBYTES(sp)

    csrr a0, mcause
    csrr a1, mepc
    mv a2, sp
    jal handle_trap

    lw x1, 1*REGBYTES(sp)
    lw x2, 2*REGBYTES(sp)
    lw x3, 3*REGBYTES(sp)
    lw x4, 4*REGBYTES(sp)
    lw x5, 5*REGBYTES(sp)
    lw x6, 6*REGBYTES(sp)
    lw x7, 7*REGBYTES(sp)
    lw x8, 8*REGBYTES(sp)
    lw x9, 9*REGBYTES(sp)
    lw x10, 10*REGBYTES(sp)
    lw x11, 11*REGBYTES(sp)
    lw x12, 12*REGBYTES(sp)
    lw x13, 13*REGBYTES(sp)
    lw x14, 14*REGBYTES(sp)
    lw x15, 15*REGBYTES(sp)
    lw x16, 16*REGBYTES(sp)
    lw x17, 17*REGBYTES(sp)
    lw x18, 18*REGBYTES(sp)
    lw x19, 19*REGBYTES(sp)
    lw x20, 20*REGBYTES(sp)
    lw x21, 21*REGBYTES(sp)
    lw x22, 22*REGBYTES(sp)
    lw x23, 23*REGBYTES(sp)
    lw x24, 24*REGBYTES(sp)
    lw x25, 25*REGBYTES(sp)
    lw x26, 26*REGBYTES(sp)
    lw x27, 27*REGBYTES(sp)
    lw x28, 28*REGBYTES(sp)
    lw x29, 29*REGBYTES(sp)
    lw x30, 30*REGBYTES(sp)
    lw x31, 31*REGBYTES(sp)

    addi sp, sp, 272
    mret

vec_machine_tmr_handler:
#ifdef USE_TIMER_HANDLER
    call TMR_INT_HANDLER
    mret
#endif

vec_machine_ext_handler:
#ifdef USE_EXT_HANDLER
    call EXT_INT_HANDLER
    mret
#endif

handle_trap:
dirv_exit:
    j RAM_EXIT

// end of crt.S
