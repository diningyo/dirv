#------------------------------------------------------------------------------
# Makefile for dirv
#------------------------------------------------------------------------------

## Common Settings
# please set risv tools path
ifdef ($(RISCV),)
$(error Plese set environment valuable RISCV)
endif
export PATH=/usr/bin:/bin:$(RISCV)/bin:$PATH

# Parameters
export ARCH := rv32i
export ABI  ?= ilp32

# Paths
export root_dir := $(shell pwd)
export inc_dir := $(root_dir)/src/common
export bld_dir := $(root_dir)/build

# Environment
export CROSS_PREFIX ?= riscv32-unknown-elf-
export RISCV_GCC ?= $(CROSS_PREFIX)gcc
export RISCV_OBJDUMP ?= $(CROSS_PREFIX)objdump -D
export RISCV_OBJCOPY ?= $(CROSS_PREFIX)objcopy
export RISCV_READELF ?= $(CROSS_PREFIX)readelf -s
export BIN2HEX ?= $(root_dir)/../../../test/resources/bin2hex.py
export PYTHON ?= python3

ifdef $(TEST)
export TARGETS := $(TEST)
else
export TARGETS := sysuart
endif

# Common Defines
common_defines :=

# Targets
.PHONY: all

default: all

all: clean $(TARGETS)

$(bld_dir):
	mkdir -p $(bld_dir)

# Target project
# Below targets must be located under $(root_dir)/src directory.
$(TARGETS): | $(bld_dir)
	$(MAKE) -C $(root_dir)/src/$(TARGETS) EXT_CFLAGS="$(EXT_CFLAGS)" common_defines="$(common_defines)"

clean:
	$(MAKE) -C $(root_dir)/src/$(TARGETS) clean
