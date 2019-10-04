#
# Makefile for sysuart
#

########################### User Setting Section #################################
## source files
# add Linker Script
ld_script := $(inc_dir)/link.ld

# add GAS source
asm_src := crt.S

# add C source
c_src := main.c

target := sysuart

########################### DO NOT EDIT BELLOW LINES #############################

user_defines += $(common_defines)

defines := $(addprefix -D, $(user_defines))

src_dir := $(dir $(lastword $(MAKEFILE_LIST)))

FLAGS = -Os
FLAGS_STR = "$(FLAGS)"

VPATH += $(src_dir) $(inc_dir)
incs += -I$(src_dir) -I$(inc_dir) -I$(bld_dir)

# compiler flags
CFLAGS := $(FLAGS) $(EXT_CFLAGS) \
-static -std=gnu99 -fno-common -fno-builtin-printf -fno-builtin-puts \
-Wa,-march=$(ARCH) -mabi=$(ABI) \
$(defines)

# linker flags
LDFLAGS := -nostartfiles -nostdlib -lc -lgcc -march=$(ARCH) -mabi=$(ABI)

# objects
c_objs := $(addprefix $(bld_dir)/,$(patsubst %.c, %.o, $(c_src)))
asm_objs := $(addprefix $(bld_dir)/,$(patsubst %.S, %.o, $(asm_src)))

default: all

all: $(bld_dir)/$(TARGETS).elf $(bld_dir)/$(TARGETS).hex $(bld_dir)/$(TARGETS).dump

# compile asm and c source
$(bld_dir)/%.o: %.S
	$(RISCV_GCC) $(CFLAGS) -D__ASSEMBLY__=1 -c $(incs) $< -o $@

$(bld_dir)/%.o: %.c
	$(RISCV_GCC) $(CFLAGS) -c $(incs) $< -o $@

# build
$(bld_dir)/$(target).elf: $(ld_script) $(c_objs) $(asm_objs)
	$(RISCV_GCC) -o $@ -T $^ $(LDFLAGS)

$(bld_dir)/$(target).dump: $(bld_dir)/$(target).elf
	$(RISCV_OBJDUMP) $^ > $@

$(bld_dir)/$(target).bin: $(bld_dir)/$(target).elf
	$(RISCV_OBJCOPY) -O binary $^ $@

$(bld_dir)/$(target).hex: $(bld_dir)/$(target).bin
	$(PYTHON3) $(BIN2HEX) --input $^ --output $@ --offset 0

$(bld_dir)/$(target).srec: $(bld_dir)/$(target).elf
	$(RISCV_OBJCOPY) -O srec $^ $@

clean:
	$(RM) $(rom_c_objs) $(rom_asm_objs) $(c_objs) $(asm_objs) $(bld_dir)/$(TARGETS).elf $(bld_dir)/$(TARGETS).hex $(bld_dir)/$(TARGETS).dump $(bld_dir)/$(target).srec $(bld_dir)/$(target).elf $(bld_dir)/$(target).dump $(bld_dir)/$(target).elf
