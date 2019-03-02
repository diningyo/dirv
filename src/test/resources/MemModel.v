module MemModel
    #(
      parameter p_TEST_HEX_FILE = "test.hex"
      )
    (
      input clk
     ,input rst
     );

    reg [7:0] mem[0:1024*256]; // temp. 256Kbytes

    initial begin
        $readmemh(p_TEST_HEX_FILE, mem);
    end

endmodule // MemModel
