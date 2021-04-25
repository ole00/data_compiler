#!/bin/bash

FILE=$1
shift


java -jar ../gdc.jar -i=$FILE -o=$FILE.bin $*