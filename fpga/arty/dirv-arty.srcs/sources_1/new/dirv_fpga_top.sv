`timescale 1ns / 1ps
//////////////////////////////////////////////////////////////////////////////////
// Company:
// Engineer:
//
// Create Date: 2019/09/01 22:05:34
// Design Name:
// Module Name: dirv_fpga_top
// Project Name:
// Target Devices:
// Tool Versions:
// Description:
//
// Dependencies:
//
// Revision:
// Revision 0.01 - File Created
// Additional Comments:
//
//////////////////////////////////////////////////////////////////////////////////


module dirv_fpga_top
    (
     input      ext_clock_i
    ,input      ext_por_i
    ,input      ext_cpu_reset_i
    ,output reg fin
    ,output     uart_tx
    ,input      uart_rx
    );

    localparam lp_RESET_CYC = 16;

    // for POR
    reg [lp_RESET_CYC-1:0] r_ext_por_i_sync;
    reg                    r_ext_por_i;

    // for ext_cpu_reset
    reg [lp_RESET_CYC-1:0] r_ext_cpu_reset_i_sync;
    reg                    r_ext_cpu_reset_i;

    // for SysUart
    wire                   w_sys_clock;
    wire                   w_sys_reset;

    generate
        genvar i;
        for (i = 0; i < lp_RESET_CYC; i++) begin
            always @(posedge ext_clock_i) begin
                if (i == 0) begin
                    r_ext_por_i_sync[i] <= ext_por_i;
                    r_ext_cpu_reset_i_sync[i] <= ext_cpu_reset_i | ext_por_i;
                end
                else begin
                    r_ext_por_i_sync[i] <= r_ext_por_i_sync[i-1];
                    r_ext_cpu_reset_i_sync[i] <= r_ext_cpu_reset_i_sync[i-1];
                end
            end
        end
    endgenerate

    always @(posedge ext_clock_i) begin
        r_ext_por_i <= &r_ext_por_i_sync;
        r_ext_cpu_reset_i <= !(&r_ext_cpu_reset_i_sync);
    end

    // finish
    always @(posedge w_sys_clock) begin
        if (w_sys_reset) begin
            fin <= 1'b0;
        end
        else if (w_fin) begin
            fin <= 1'b1;
        end
    end

    // debug
    (* dont_touch = "true" *) wire [31:0] w_io_pc;
    (* dont_touch = "true" *) wire [31:0] w_io_zero;
    (* dont_touch = "true" *) wire [31:0] w_io_ra;
    (* dont_touch = "true" *) wire [31:0] w_io_sp;
    (* dont_touch = "true" *) wire [31:0] w_io_gp;
    (* dont_touch = "true" *) wire [31:0] w_io_tp;
    (* dont_touch = "true" *) wire [31:0] w_io_t0;
    (* dont_touch = "true" *) wire [31:0] w_io_t1;
    (* dont_touch = "true" *) wire [31:0] w_io_t2;
    (* dont_touch = "true" *) wire [31:0] w_io_s0;
    (* dont_touch = "true" *) wire [31:0] w_io_s1;
    (* dont_touch = "true" *) wire [31:0] w_io_a0;
    (* dont_touch = "true" *) wire [31:0] w_io_a1;
    (* dont_touch = "true" *) wire [31:0] w_io_a2;
    (* dont_touch = "true" *) wire [31:0] w_io_a3;
    (* dont_touch = "true" *) wire [31:0] w_io_a4;
    (* dont_touch = "true" *) wire [31:0] w_io_a5;
    (* dont_touch = "true" *) wire [31:0] w_io_a6;
    (* dont_touch = "true" *) wire [31:0] w_io_a7;
    (* dont_touch = "true" *) wire [31:0] w_io_s2;
    (* dont_touch = "true" *) wire [31:0] w_io_s3;
    (* dont_touch = "true" *) wire [31:0] w_io_s4;
    (* dont_touch = "true" *) wire [31:0] w_io_s5;
    (* dont_touch = "true" *) wire [31:0] w_io_s6;
    (* dont_touch = "true" *) wire [31:0] w_io_s7;
    (* dont_touch = "true" *) wire [31:0] w_io_s8;
    (* dont_touch = "true" *) wire [31:0] w_io_s9;
    (* dont_touch = "true" *) wire [31:0] w_io_s10;
    (* dont_touch = "true" *) wire [31:0] w_io_s11;
    (* dont_touch = "true" *) wire [31:0] w_io_t3;
    (* dont_touch = "true" *) wire [31:0] w_io_t4;
    (* dont_touch = "true" *) wire [31:0] w_io_t5;
    (* dont_touch = "true" *) wire [31:0] w_io_t6;

    SysUart mSysUart
        (
          .clock       (w_sys_clock )
         ,.reset       (w_sys_reset )
         ,.io_fin      (w_fin       )
         ,.io_uart_tx  (uart_tx     )
         ,.io_uart_rx  (uart_rx     )
         ,.io_pc       (w_io_pc     )
         ,.io_zero     (w_io_zero   )
         ,.io_ra       (w_io_ra     )
         ,.io_sp       (w_io_sp     )
         ,.io_gp       (w_io_gp     )
         ,.io_tp       (w_io_tp     )
         ,.io_t0       (w_io_t0     )
         ,.io_t1       (w_io_t1     )
         ,.io_t2       (w_io_t2     )
         ,.io_s0       (w_io_s0     )
         ,.io_s1       (w_io_s1     )
         ,.io_a0       (w_io_a0     )
         ,.io_a1       (w_io_a1     )
         ,.io_a2       (w_io_a2     )
         ,.io_a3       (w_io_a3     )
         ,.io_a4       (w_io_a4     )
         ,.io_a5       (w_io_a5     )
         ,.io_a6       (w_io_a6     )
         ,.io_a7       (w_io_a7     )
         ,.io_s2       (w_io_s2     )
         ,.io_s3       (w_io_s3     )
         ,.io_s4       (w_io_s4     )
         ,.io_s5       (w_io_s5     )
         ,.io_s6       (w_io_s6     )
         ,.io_s7       (w_io_s7     )
         ,.io_s8       (w_io_s8     )
         ,.io_s9       (w_io_s9     )
         ,.io_s10      (w_io_s10    )
         ,.io_s11      (w_io_s11    )
         ,.io_t3       (w_io_t3     )
         ,.io_t4       (w_io_t4     )
         ,.io_t5       (w_io_t5     )
         ,.io_t6       (w_io_t6     )
         );

    crg_wrapper m_crg_wrapper
        (
          .ext_clock_i     (ext_clock_i       )
         ,.ext_cpu_reset_i (r_ext_cpu_reset_i )
         ,.ext_por_i       (r_ext_por_i       )
         ,.sys_clock_o     (w_sys_clock       )
         ,.sys_reset_o     (w_sys_reset       )
         );

endmodule
