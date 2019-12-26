module RAM1RO1RW
    #(
      parameter p_ADDR_BITS  = 32
     ,parameter p_DATA_BITS  = 32
     ,parameter p_STRB_BITS  = p_DATA_BITS / 8
     ,parameter p_MEM_ROW_NUM = 0
     ,parameter p_INIT_HEX_FILE = "test.hex"
    )
    (
    // external
     input                        clk

    // memory
    ,input [p_ADDR_BITS-1:0]      addra
    ,output reg [p_DATA_BITS-1:0] qa
    ,input                        rena

    // memory
    ,input [p_ADDR_BITS-1:0]      addrb
    ,output reg [p_DATA_BITS-1:0] qb
    ,input                        renb
    ,input                        wenb
    ,input [p_STRB_BITS-1:0]      webb
    ,input [p_DATA_BITS-1:0]      datab
    );

    int i;

    reg [p_DATA_BITS-1:0] mem[0:p_MEM_ROW_NUM-1];

    initial begin
        if (p_INIT_HEX_FILE != "") begin
            $readmemh(p_INIT_HEX_FILE, mem);
        end
    end

    // imem side
    always @(posedge clk) begin
        if (rena) begin
            qa <= mem[addra];
        end
    end

    // dmem side
    always @(posedge clk) begin
        // read
        if (renb) begin
            qb <= mem[addrb];
        end

        // write
        if (wenb) begin
            for (i = 0; i < 4; i++) begin
                if (webb[i]) begin
                    mem[addrb][i*8 +: 8] <= datab[i*8 +: 8];
                end
            end
        end
    end

endmodule // data_ram