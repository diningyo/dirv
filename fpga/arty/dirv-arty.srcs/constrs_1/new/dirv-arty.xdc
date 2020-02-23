# Pin assign
set_property PACKAGE_PIN E3 [get_ports ext_clock_i]
set_property PACKAGE_PIN D9 [get_ports ext_cpu_reset_i]
set_property PACKAGE_PIN B8 [get_ports ext_por_i]
set_property PACKAGE_PIN H5 [get_ports fin]
set_property PACKAGE_PIN A9 [get_ports uart_rx]
set_property PACKAGE_PIN D10 [get_ports uart_tx]

# IO Standard
set_property IOSTANDARD LVCMOS33 [get_ports ext_cpu_reset_i]
set_property IOSTANDARD LVCMOS33 [get_ports ext_por_i]
set_property IOSTANDARD LVCMOS33 [get_ports fin]
set_property IOSTANDARD LVCMOS33 [get_ports ext_clock_i]
set_property IOSTANDARD LVCMOS33 [get_ports uart_tx]
set_property IOSTANDARD LVCMOS33 [get_ports uart_rx]

# Clock
create_clock -period 10.000 -name ext_clk_i -waveform {0.000 5.000} -add [get_ports ext_clock_i]
create_clock -period 20.000 -name VIRTUAL_clk_out1_crg_clk_wiz_0_0 -waveform {0.000 10.000}

# Input/Output delays
set_input_delay -clock [get_clocks ext_clk_i] -min -add_delay 1.000 [get_ports ext_cpu_reset_i]
set_input_delay -clock [get_clocks ext_clk_i] -max -add_delay 6.000 [get_ports ext_cpu_reset_i]

set_input_delay -clock [get_clocks VIRTUAL_clk_out1_crg_clk_wiz_0_0] -min -add_delay 1.000 [get_ports uart_rx]
set_input_delay -clock [get_clocks VIRTUAL_clk_out1_crg_clk_wiz_0_0] -max -add_delay 6.000 [get_ports uart_rx]

set_output_delay -clock [get_clocks VIRTUAL_clk_out1_crg_clk_wiz_0_0] -min -add_delay 1.000 [get_ports uart_tx]
set_output_delay -clock [get_clocks VIRTUAL_clk_out1_crg_clk_wiz_0_0] -max -add_delay 6.000 [get_ports uart_tx]

set_output_delay -clock [get_clocks VIRTUAL_clk_out1_crg_clk_wiz_0_0] -min -add_delay 1.000 [get_ports fin]
set_output_delay -clock [get_clocks VIRTUAL_clk_out1_crg_clk_wiz_0_0] -max -add_delay 6.000 [get_ports fin]

# CFGBVS
set_property CONFIG_VOLTAGE 3.3 [current_design]
set_property CFGBVS VCCO [current_design]

set_property BITSTREAM.GENERAL.COMPRESS TRUE [current_design]
set_property BITSTREAM.CONFIG.CONFIGRATE 33 [current_design]
set_property CONFIG_MODE SPIx4 [current_design]
create_debug_core u_ila_0 ila
set_property ALL_PROBE_SAME_MU true [get_debug_cores u_ila_0]
set_property ALL_PROBE_SAME_MU_CNT 1 [get_debug_cores u_ila_0]
set_property C_ADV_TRIGGER false [get_debug_cores u_ila_0]
set_property C_DATA_DEPTH 1024 [get_debug_cores u_ila_0]
set_property C_EN_STRG_QUAL false [get_debug_cores u_ila_0]
set_property C_INPUT_PIPE_STAGES 0 [get_debug_cores u_ila_0]
set_property C_TRIGIN_EN false [get_debug_cores u_ila_0]
set_property C_TRIGOUT_EN false [get_debug_cores u_ila_0]
set_property port_width 1 [get_debug_ports u_ila_0/clk]
connect_debug_port u_ila_0/clk [get_nets [list m_crg_wrapper/crg_i/dirv_clock_gen/inst/clk_out1]]
set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe0]
set_property port_width 12 [get_debug_ports u_ila_0/probe0]
connect_debug_port u_ila_0/probe0 [get_nets [list {mSysUart/m_mem/m_ram_io_a_addr[2]} {mSysUart/m_mem/m_ram_io_a_addr[3]} {mSysUart/m_mem/m_ram_io_a_addr[4]} {mSysUart/m_mem/m_ram_io_a_addr[5]} {mSysUart/m_mem/m_ram_io_a_addr[6]} {mSysUart/m_mem/m_ram_io_a_addr[7]} {mSysUart/m_mem/m_ram_io_a_addr[8]} {mSysUart/m_mem/m_ram_io_a_addr[9]} {mSysUart/m_mem/m_ram_io_a_addr[10]} {mSysUart/m_mem/m_ram_io_a_addr[11]} {mSysUart/m_mem/m_ram_io_a_addr[12]} {mSysUart/m_mem/m_ram_io_a_addr[13]}]]
create_debug_port u_ila_0 probe
set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe1]
set_property port_width 32 [get_debug_ports u_ila_0/probe1]
connect_debug_port u_ila_0/probe1 [get_nets [list {mSysUart/m_mem/m_ram_io_a_rddata[0]} {mSysUart/m_mem/m_ram_io_a_rddata[1]} {mSysUart/m_mem/m_ram_io_a_rddata[2]} {mSysUart/m_mem/m_ram_io_a_rddata[3]} {mSysUart/m_mem/m_ram_io_a_rddata[4]} {mSysUart/m_mem/m_ram_io_a_rddata[5]} {mSysUart/m_mem/m_ram_io_a_rddata[6]} {mSysUart/m_mem/m_ram_io_a_rddata[7]} {mSysUart/m_mem/m_ram_io_a_rddata[8]} {mSysUart/m_mem/m_ram_io_a_rddata[9]} {mSysUart/m_mem/m_ram_io_a_rddata[10]} {mSysUart/m_mem/m_ram_io_a_rddata[11]} {mSysUart/m_mem/m_ram_io_a_rddata[12]} {mSysUart/m_mem/m_ram_io_a_rddata[13]} {mSysUart/m_mem/m_ram_io_a_rddata[14]} {mSysUart/m_mem/m_ram_io_a_rddata[15]} {mSysUart/m_mem/m_ram_io_a_rddata[16]} {mSysUart/m_mem/m_ram_io_a_rddata[17]} {mSysUart/m_mem/m_ram_io_a_rddata[18]} {mSysUart/m_mem/m_ram_io_a_rddata[19]} {mSysUart/m_mem/m_ram_io_a_rddata[20]} {mSysUart/m_mem/m_ram_io_a_rddata[21]} {mSysUart/m_mem/m_ram_io_a_rddata[22]} {mSysUart/m_mem/m_ram_io_a_rddata[23]} {mSysUart/m_mem/m_ram_io_a_rddata[24]} {mSysUart/m_mem/m_ram_io_a_rddata[25]} {mSysUart/m_mem/m_ram_io_a_rddata[26]} {mSysUart/m_mem/m_ram_io_a_rddata[27]} {mSysUart/m_mem/m_ram_io_a_rddata[28]} {mSysUart/m_mem/m_ram_io_a_rddata[29]} {mSysUart/m_mem/m_ram_io_a_rddata[30]} {mSysUart/m_mem/m_ram_io_a_rddata[31]}]]
create_debug_port u_ila_0 probe
set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe2]
set_property port_width 1 [get_debug_ports u_ila_0/probe2]
connect_debug_port u_ila_0/probe2 [get_nets [list ext_cpu_reset_i_IBUF]]
create_debug_port u_ila_0 probe
set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe3]
set_property port_width 1 [get_debug_ports u_ila_0/probe3]
connect_debug_port u_ila_0/probe3 [get_nets [list uart_tx_OBUF]]
create_debug_port u_ila_0 probe
set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe4]
set_property port_width 1 [get_debug_ports u_ila_0/probe4]
connect_debug_port u_ila_0/probe4 [get_nets [list w_sys_reset]]
create_debug_port u_ila_0 probe
set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe5]
set_property port_width 1 [get_debug_ports u_ila_0/probe5]
connect_debug_port u_ila_0/probe5 [get_nets [list mSysUart/m_dirv/m_mem_io_imem_c_valid]]
create_debug_port u_ila_0 probe
set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe6]
set_property port_width 1 [get_debug_ports u_ila_0/probe6]
connect_debug_port u_ila_0/probe6 [get_nets [list mSysUart/m_dirv/m_mem_io_imem_r_ready]]
create_debug_port u_ila_0 probe
set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe7]
set_property port_width 1 [get_debug_ports u_ila_0/probe7]
connect_debug_port u_ila_0/probe7 [get_nets [list mSysUart/m_dirv/m_ram_io_a_rddv]]
create_debug_port u_ila_0 probe
set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe8]
set_property port_width 1 [get_debug_ports u_ila_0/probe8]
connect_debug_port u_ila_0/probe8 [get_nets [list mSysUart/m_dirv/m_ram_io_a_rden]]
set_property C_CLK_INPUT_FREQ_HZ 300000000 [get_debug_cores dbg_hub]
set_property C_ENABLE_CLK_DIVIDER false [get_debug_cores dbg_hub]
set_property C_USER_SCAN_CHAIN 1 [get_debug_cores dbg_hub]
connect_debug_port dbg_hub/clk [get_nets w_sys_clock]
