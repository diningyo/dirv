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
    ,input                    imem_req
    ,output                   imem_resp
    ,output                   imem_r_rddv
    ,output [p_DATA_BITS-1:0] imem_r_data

    // dmem
    ,input [p_ADDR_BITS-1:0]  dmem_addr
    ,input                    dmem_cmd
    ,input                    dmem_req
    ,output                   dmem_resp
    ,output                   dmem_r_rddv
    ,output [p_DATA_BITS-1:0] dmem_r_data
    ,output                   dmem_w_ack
    ,input [p_STRB_BITS-1:0]  dmem_w_strb
    ,input [p_DATA_BITS-1:0]  dmem_w_data
    );

    reg [7:0] mem[0:1024*256]; // temp. 256Kbytes

    initial begin
        $readmemh(p_TEST_HEX_FILE, mem);
    end

    function [p_DATA_BITS-1:0] read
        (
            input [p_ADDR_BITS-3:0] addr
        );
        read = {mem[addr+3], mem[addr+2], mem[addr+1], mem[addr]};
    endfunction // read

    // imem side
    assign imem_resp = 1'b0;
    assign imem_r_rddv = imem_req;
    assign imem_r_data = (imem_cmd == 1'b0) ? read(imem_addr[p_ADDR_BITS-1:2]) : $random;


    // dmem side
    wire [p_ADDR_BITS-3:0]       dmem_wdaddr;
    wire dmem_read;
    wire dmem_write;

    assign dmem_read = (dmem_cmd == 1'b0);
    assign dmem_write = (dmem_cmd == 1'b1);
    assign dmem_wdaddr = dmem_addr[p_ADDR_BITS-1:2];

    assign dmem_resp = 1'b0;
    assign dmem_r_rddv = dmem_req && dmem_read;
    assign dmem_r_data  = (dmem_read) ? read(dmem_wdaddr) : $random;

    assign dmem_w_ack = dmem_write;

    always @(posedge clk or posedge rst) begin
        if (dmem_req && dmem_write) begin
            if (dmem_w_strb[0]) begin
                mem[dmem_wdaddr] <= dmem_w_data[7:0];
            end
            if (dmem_w_strb[1]) begin
                mem[dmem_wdaddr] <= dmem_w_data[15:8];
            end
            if (dmem_w_strb[2]) begin
                mem[dmem_wdaddr] <= dmem_w_data[24:16];
            end
            if (dmem_w_strb[3]) begin
                mem[dmem_wdaddr] <= dmem_w_data[31:24];
            end
        end
    end

endmodule // MemModel
