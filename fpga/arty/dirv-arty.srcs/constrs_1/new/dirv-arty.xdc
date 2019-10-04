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
create_clock -period 10.000 -name ext_clk_i                        -waveform {0.000 5.000} -add [get_ports ext_clock_i]
create_clock -period 20.000 -name VIRTUAL_clk_out1_crg_clk_wiz_0_0 -waveform {0.000 10.000}

# Input/Output delays
set_input_delay -clock [get_clocks ext_clk_i] -min 1.000 -add_delay [get_ports ext_cpu_reset_i]
set_input_delay -clock [get_clocks ext_clk_i] -max 6.000 -add_delay [get_ports ext_cpu_reset_i]

set_input_delay -clock [get_clocks VIRTUAL_clk_out1_crg_clk_wiz_0_0] -min -add_delay 1.000 [get_ports uart_rx]
set_input_delay -clock [get_clocks VIRTUAL_clk_out1_crg_clk_wiz_0_0] -max -add_delay 6.000 [get_ports uart_rx]

set_output_delay -clock [get_clocks VIRTUAL_clk_out1_crg_clk_wiz_0_0] -min -add_delay 1.000 [get_ports uart_tx]
set_output_delay -clock [get_clocks VIRTUAL_clk_out1_crg_clk_wiz_0_0] -max -add_delay 6.000 [get_ports uart_tx]

set_output_delay -clock [get_clocks VIRTUAL_clk_out1_crg_clk_wiz_0_0] -min -add_delay 1.000 [get_ports fin]
set_output_delay -clock [get_clocks VIRTUAL_clk_out1_crg_clk_wiz_0_0] -max -add_delay 6.000 [get_ports fin]

# CFGBVS
set_property CONFIG_VOLTAGE 3.3 [current_design]
set_property CFGBVS VCCO [current_design]
