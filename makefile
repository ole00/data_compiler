
SRC_DIR := src/ole/gdc
FILES := \
	${SRC_DIR}/DataCompiler.java \
	${SRC_DIR}/DataElement.java \
	${SRC_DIR}/DataStruct.java \
	${SRC_DIR}/LineItem.java \
	${SRC_DIR}/UnicodeEscapes.java \
	${SRC_DIR}/Utils.java \


EX_DIR := examples
EXAMPLES := \
	${EX_DIR}/ex01.d \
	${EX_DIR}/ex02.d \
	${EX_DIR}/ex03.d \
	${EX_DIR}/ex04.d \
	${EX_DIR}/ex05.d \
	${EX_DIR}/ex06.d \
	${EX_DIR}/ex07.d \
	${EX_DIR}/ex08.d \
	${EX_DIR}/ex09.d \
	${EX_DIR}/ex10.d \
	${EX_DIR}/ex11.d \


EXAMPLES_BIN := $(addsuffix .bin, $(EXAMPLES))
DC_FLAGS := -le -DTHE_VALUE=20

all: gdc.jar

gdc.jar : compile package

set_tmp:
	@mkdir -p tmp

compile:
	rm -rf tmp
	mkdir -p tmp
	javac -target 1.6 -source 1.6 -d tmp ${FILES}

package:
	@rm -f gdc.jar
	@printf "Main-Class: ole.gdc.DataCompiler\n" >/tmp/gdc-mf
	jar  cfm gdc.jar /tmp/gdc-mf -C tmp .
	@rm /tmp/gdc-mf

examples: gdc.jar set_tmp $(EXAMPLES_BIN)

# implicit rule to compile .bin files out of .d files
%.bin : %
	@echo "[dc] : $<"
	@java -jar gdc.jar -i=$< -o=tmp/$(basename $(<F)).bin $(DC_FLAGS)
#	produce hexdump of the binary file for convenience
	@hexdump -C -v tmp/$(basename $(<F)).bin > tmp/$(basename $(<F)).txt