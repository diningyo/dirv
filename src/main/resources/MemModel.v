module MemCtrl
    #(
       parameter p_ADDR_BITS  = 32
      ,parameter p_DATA_BITS  = 32
      ,parameter p_STRB_BITS  = p_DATA_BITS / 8
      ,parameter p_PORT_NAME  = ""
      ,parameter p_IF_TYPE    = 1'b1
      )
    (
     // external
     input                    clk
    ,input                    rst
    ,input [p_ADDR_BITS-1:0]  mem_addr
    ,input                    mem_cmd
    ,input [1:0]              mem_size
    ,input                    mem_valid
    ,output                   mem_ready
    ,input                    mem_r_ready
    ,output                   mem_r_valid
    ,output [p_DATA_BITS-1:0] mem_r_data
    ,output                   mem_r_resp
    ,input                    mem_w_valid
    ,output                   mem_w_ready
    ,input [p_STRB_BITS-1:0]  mem_w_strb
    ,input [p_DATA_BITS-1:0]  mem_w_data
    ,output                   mem_w_resp

    // memory
    ,output [p_ADDR_BITS-1:0] addr
    ,output [p_DATA_BITS-1:0] rddata
    ,output                   wren
    ,output [p_STRB_BITS-1:0] wrstrb
    ,output [p_DATA_BITS-1:0] wrdata
    );

    //
    // local parameters
    //
    localparam lps_CMD = 1'b0;
    localparam lps_DATA = 1'b1;

    //
    reg r_fsm;

    // mem side
    wire [p_ADDR_BITS-1:0]    w_addr;
    wire                      w_read;
    reg                       r_read;
    reg                       r_write;
    reg                       r_cmd_active;
    reg [p_ADDR_BITS-1:0]     r_mem_addr;
    reg                       r_mem_ready;
    reg                       r_mem_r_ready;
    reg                       r_mem_w_ready;
    wire                      mem_read;
    wire                      mem_write;
    reg                       r_rvalid;
    reg [p_DATA_BITS-1:0]     r_rddata;
    wire                      rden;

    // control
    assign mem_read   = (mem_cmd == 1'b0);
    assign mem_write  = (mem_cmd == 1'b1);

    // external
    assign mem_ready = (r_fsm == lps_CMD) ? 1'b1 : (mem_r_ready && mem_w_ready);
    assign mem_r_valid = (p_IF_TYPE) ? r_rvalid : rden;
    assign mem_r_data = rddata;
    assign mem_w_ready = 1'b1;

    assign w_addr = (mem_read) ? r_mem_addr : mem_addr;
    assign addr = {w_addr[p_ADDR_BITS-1:2], 2'b00};
    assign rden = w_read || r_read;
    assign wren  = mem_write && mem_valid && mem_ready;
    assign wrdata = mem_w_data;
    assign wrstrb = mem_w_strb;

    assign w_read = (mem_read && mem_valid && mem_ready);


    always @(posedge clk or negedge rst) begin
        if (rst) begin
            r_cmd_active <= 1'b0;
        end
        else if (w_read || r_read) begin
            if (mem_r_valid && mem_r_ready) begin
                r_cmd_active <= 1'b0;
                r_read <= 1'b0;
            end
            else begin
                r_cmd_active <= 1'b1;
                r_read <= 1'b1;
            end
        end
        else if (wren) begin
            if (mem_w_valid && mem_w_ready) begin
                r_cmd_active <= 1'b1;
                r_write      <= 1'b1;
            end
            else begin
                r_cmd_active <= 1'b0;
                r_write      <= 1'b0;
            end
        end
    end

    always @(posedge clk or negedge rst) begin
        if (rst) begin
            r_mem_addr <= 'h0;
        end
        else if (w_read || wren) begin
            r_mem_addr <= mem_addr;
        end
    end

    // read
    always @(posedge clk or posedge rst) begin
        if (rst) begin
            r_rvalid <= 1'b0;
            r_rddata <= 'h0;
        end
        else if (!w_read && (r_fsm == lps_DATA && mem_r_ready)) begin
            r_rvalid <= 1'b0;
        end
        else if (rden)begin
            r_rvalid <= rden;
            r_rddata <= rddata;
        end
    end

    //
    always @(posedge clk or negedge rst) begin
        if (rst) begin
            r_fsm <= lps_CMD;
        end
        else begin
            case (r_fsm)
                lps_CMD : begin
                    if (mem_valid && mem_ready) begin
                        if (mem_w_valid && mem_w_ready) begin
                            r_fsm <= lps_CMD;
                        end
                        else begin
                            r_fsm <= lps_DATA;
                        end
                    end
                end

                lps_DATA : begin
                    if ((mem_r_valid && mem_r_ready) ||
                        (mem_w_valid && mem_w_ready)) begin
                        r_fsm <= lps_DATA;
                    end
                    else begin
                        r_fsm <= lps_CMD;
                    end
                end
            endcase
        end
    end

endmodule : MemCtrl


module MemModel
    #(
        parameter p_ADDR_BITS = 32
        ,parameter p_DATA_BITS = 32
        ,parameter p_STRB_BITS = p_DATA_BITS / 8
        ,parameter p_TEST_HEX_FILE = "test.hex"
    )
    (
        input                    clk
        ,input                    rst
    // imem
    ,input [p_ADDR_BITS-1:0]  imem_addr
    ,input                    imem_cmd
    ,input [1:0]              imem_size
    ,input                    imem_valid
    ,output                   imem_ready
    ,output                   imem_r_valid
    ,output                   imem_r_ready
    ,output [p_DATA_BITS-1:0] imem_r_data
    ,output                   imem_r_resp

    // dmem
    ,input [p_ADDR_BITS-1:0]  dmem_addr
    ,input                    dmem_cmd
    ,input [1:0]              dmem_size
    ,input                    dmem_valid
    ,output                   dmem_ready
    ,input                    dmem_r_ready
    ,output                   dmem_r_valid
    ,output [p_DATA_BITS-1:0] dmem_r_data
    ,output                   dmem_r_resp
    ,input                    dmem_w_valid
    ,output                   dmem_w_ready
    ,input [p_STRB_BITS-1:0]  dmem_w_strb
    ,input [p_DATA_BITS-1:0]  dmem_w_data
    ,output                   dmem_w_resp
    );

    parameter p_IMEM = 1'b0;
    parameter p_DMEM = 1'b1;

    wire [p_ADDR_BITS-1:0] i_addr;
    wire [p_DATA_BITS-1:0] i_rddata;
    wire                   i_wren;
    wire [p_STRB_BITS-1:0] i_wrstrb;
    wire [p_DATA_BITS-1:0] i_wrdata;

    wire [p_ADDR_BITS-1:0] d_addr;
    wire [p_DATA_BITS-1:0] d_rddata;
    wire                   d_wren;
    wire [p_STRB_BITS-1:0] d_wrstrb;
    wire [p_DATA_BITS-1:0] d_wrdata;

    //
    // Module Instance
    //
    MemCtrl
    #(
       .p_ADDR_BITS (p_ADDR_BITS )
      ,.p_DATA_BITS (p_DATA_BITS )
      ,.p_STRB_BITS (p_STRB_BITS )
      ,.p_PORT_NAME ("imem"      )
    )
    imemCtrl
    (
      .clk         (clk          )
     ,.rst         (rst          )
     ,.mem_addr    (imem_addr    )
     ,.mem_cmd     (imem_cmd     )
     ,.mem_size    (imem_size    )
     ,.mem_valid   (imem_valid   )
     ,.mem_ready   (imem_ready   )
     ,.mem_r_ready (imem_r_ready )
     ,.mem_r_valid (imem_r_valid )
     ,.mem_r_data  (imem_r_data  )
     ,.mem_r_resp  (imem_r_resp  )
     ,.mem_w_valid (1'b0         )
     ,.mem_w_ready (             )
     ,.mem_w_strb  ('h0          )
     ,.mem_w_data  ('h0          )
     ,.mem_w_resp  (             )

     // memory
     ,.addr        (i_addr       )
     ,.rddata      (i_rddata     )
     ,.wren        (             )
     ,.wrstrb      (             )
     ,.wrdata      (             )
    );

    MemCtrl
    #(
       .p_ADDR_BITS (p_ADDR_BITS )
      ,.p_DATA_BITS (p_DATA_BITS )
      ,.p_STRB_BITS (p_STRB_BITS )
      ,.p_PORT_NAME ("dmem"      )
    )
    dmemCtrl
    (
      .clk         (clk          )
     ,.rst         (rst          )
     ,.mem_addr    (dmem_addr    )
     ,.mem_cmd     (dmem_cmd     )
     ,.mem_size    (dmem_size    )
     ,.mem_valid   (dmem_valid   )
     ,.mem_ready   (dmem_ready   )
     ,.mem_r_ready (dmem_r_ready )
     ,.mem_r_valid (dmem_r_valid )
     ,.mem_r_data  (dmem_r_data  )
     ,.mem_r_resp  (dmem_r_resp  )
     ,.mem_w_valid (dmem_w_valid )
     ,.mem_w_ready (dmem_w_ready )
     ,.mem_w_strb  (dmem_w_strb  )
     ,.mem_w_data  (dmem_w_data  )
     ,.mem_w_resp  (dmem_w_resp  )

     // memory
     ,.addr        (d_addr       )
     ,.rddata      (d_rddata     )
     ,.wren        (d_wren       )
     ,.wrstrb      (d_wrstrb     )
     ,.wrdata      (d_wrdata     )
    );

`ifdef d_XILINX
    blk_mem_gen_0 blk_mem_gen_0
        (
          .clka   (clk                    )
         ,.wea    (1'b0                   )
         ,.addra  (i_addr                 )
         ,.dina   ('b0                    )
         ,.douta  (i_rddata               )
         ,.clkb   (clk                    )
         ,.web    ({4{d_wren}} & d_wrstrb )
         ,.addrb  (d_addr                 )
         ,.dinb   (d_wrdata               )
         ,.doutb  (d_rddata               )
         );
`else
    data_ram
        #(
           .p_ADDR_BITS     (p_ADDR_BITS     )
          ,.p_DATA_BITS     (p_DATA_BITS     )
          ,.p_STRB_BITS     (p_STRB_BITS     )
          ,.p_TEST_HEX_FILE (p_TEST_HEX_FILE )
        )
    m_data_ram
        (
          // external
          .clk      (clk      )
         ,.rst      (rst      )

         ,.addr_0   (i_addr   )
         ,.rddata_0 (i_rddata )
         ,.en_0     (1'b0     )
         ,.wren_0   (1'b0     )
         ,.wrstrb_0 ('h0      )
         ,.wrdata_0 ('h0      )

         ,.addr_1   (d_addr   )
         ,.rddata_1 (d_rddata )
         ,.en_1     (d_wren   )
         ,.wren_1   (d_wren   )
         ,.wrstrb_1 (d_wrstrb )
         ,.wrdata_1 (d_wrdata )
        );
`endif

    // log
    /*
    always @(posedge clk) begin
        if (i_rden)
            $display("[%t][imem](addr, rddata) = (0x%08x, 0x%08x)", $time, i_addr, i_rddata);
        if (d_rden)
            $display("[%t][dmem](addr, rddata) = (0x%08x, 0x%08x)", $time, d_addr, d_rddata);
    end
    */


endmodule : MemModel

module data_ram
    #(
       parameter p_ADDR_BITS  = 32
      ,parameter p_DATA_BITS  = 32
      ,parameter p_STRB_BITS  = p_DATA_BITS / 8
      ,parameter p_TEST_HEX_FILE = "test.hex"
      )
    (
    // external
     input                    clk
    ,input                    rst

    // memory
    ,input [p_ADDR_BITS-1:0]  addr_0
    ,output [p_DATA_BITS-1:0] rddata_0
    ,input                    en_0
    ,input                    wren_0
    ,input [p_STRB_BITS-1:0]  wrstrb_0
    ,input [p_DATA_BITS-1:0]  wrdata_0

    // memory
    ,input [p_ADDR_BITS-1:0]  addr_1
    ,output [p_DATA_BITS-1:0] rddata_1
    ,input                    en_1
    ,input                    wren_1
    ,input [p_STRB_BITS-1:0]  wrstrb_1
    ,input [p_DATA_BITS-1:0]  wrdata_1
    );

    reg [7:0] mem[0:1024*64]; // temp. 256Kbytes

    initial begin
        $readmemh(p_TEST_HEX_FILE, mem);
    end

    function [p_DATA_BITS-1:0] read
        (
            input [p_ADDR_BITS-1:0] addr
        );

        reg [p_ADDR_BITS-1:0] rddata;
        rddata = {mem[addr+3], mem[addr+2], mem[addr+1], mem[addr]};
        read = rddata;
    endfunction : read // read

    // imem side
    assign rddata_0 = read(addr_0);

    // dmem side
    assign rddata_1  = read(addr_1);

    // write
    always @(posedge clk) begin
        if (en_1) begin
            if (wren_1) begin
                if (wrstrb_1[0]) begin
                    mem[addr_1] <= wrdata_1[7:0];
                end
                if (wrstrb_1[1]) begin
                    mem[addr_1 + 1] <= wrdata_1[15:8];
                end
                if (wrstrb_1[2]) begin
                    mem[addr_1 + 2] <= wrdata_1[24:16];
                end
                if (wrstrb_1[3]) begin
                    mem[addr_1 + 3] <= wrdata_1[31:24];
                end
            end
        end
    end

endmodule : data_ram
