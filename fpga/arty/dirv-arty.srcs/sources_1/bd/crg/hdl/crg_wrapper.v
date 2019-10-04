//Copyright 1986-2018 Xilinx, Inc. All Rights Reserved.
//--------------------------------------------------------------------------------
//Tool Version: Vivado v.2018.3 (lin64) Build 2405991 Thu Dec  6 23:36:41 MST 2018
//Date        : Fri Oct  4 18:49:04 2019
//Host        : diningyo-pc running 64-bit Ubuntu 16.04.6 LTS
//Command     : generate_target crg_wrapper.bd
//Design      : crg_wrapper
//Purpose     : IP block netlist
//--------------------------------------------------------------------------------
`timescale 1 ps / 1 ps

module crg_wrapper
   (ext_clock_i,
    ext_cpu_reset_i,
    ext_por_i,
    sys_clock_o,
    sys_reset_o);
  input ext_clock_i;
  input ext_cpu_reset_i;
  input ext_por_i;
  output sys_clock_o;
  output [0:0]sys_reset_o;

  wire ext_clock_i;
  wire ext_cpu_reset_i;
  wire ext_por_i;
  wire sys_clock_o;
  wire [0:0]sys_reset_o;

  crg crg_i
       (.ext_clock_i(ext_clock_i),
        .ext_cpu_reset_i(ext_cpu_reset_i),
        .ext_por_i(ext_por_i),
        .sys_clock_o(sys_clock_o),
        .sys_reset_o(sys_reset_o));
endmodule
