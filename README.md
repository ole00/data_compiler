# data_compiler
Compiles generic data elements expressed in text scripts into a binary format.

**Why binary data?**

* simpler to parse: actually no need to parse -just read the data into structures
* when you don't need to parse the data (otherwise stored in text files like .json or 
  .xml) then you don't need the parsing libraries which take memory space. 
  Important for embedded projects.
* when you don't need to parse you get higher speeds while reading the data
* when you don't need to parse then your CPU can do other things (handling ISR etc.)
  That's important for embedded projects.
* binary data require smaller storage space - can fit to MCU or inside a small flash chip etc.
* parsing and data transfer of binary data consumes less energy - important for battery
  operated devices. 

**Why data compiler?**

Sure, you can use a hex editor and cobble some data in it - and that's fine for small projects
or one-off data files that you'll never touch again. But what if you need to maintain the data in git?
What if the data are more complex so hacking binary in hex editor becomes a chore? What if
you are actually not the owner of the data, and you have other team members contributing to the 
contents of the data - they may not be able to modify data, or they can accidentally break
the binary data by hacking them in hex editor. In such cases the data compiler comes handy: the data
can ne expressed in a text format (with C-like syntax) and then compiled into binaries during build time
of your project.

**Features**

* C-like syntax
* preprocessor #include and #ifdef #else #endif, symbols passed via command line
* basic data types: byte, short, int, long, float, double
* support for arrays of data types
* support for variable length strings and constant length strings
* support for defining structures with default auto-filled/initialised values
* big-endian / little-endian support (via command line parameter during compile time)
* support for constants
* support for element counting
* support for embedding files

**Give me some example**

This is the simplest Hello World script:
<pre>
string name
{
  name = "Hello World!"
}
</pre>
The binary output is like this:
<pre>
00000000  00 0c 48 65 6c 6c 6f 20  57 6f 72 6c 64 21        |..Hello World!|
</pre>
The script defines a field called 'name' of type 'string'. Then the text is saved
as a variable-length string: first 2 bytes is the length of the string, then
the UTF-8 byte representation of the string follows. 

See 'examples' directory for more examples.


**Building the data compiler**

You'll need a GNU make and a java compiler. Ensure your java compiler (javac) can be exeecuted.
Change your PATH environment variable andpoint to the javac location if not.
Like this: 
<pre>
export PATH=/opt/java-8-openjdk-amd64/bin:$PATH
</pre>

Then run 'make' to build the data compiler. It will produce gdc.jar file in the project's
root directory - that's it.
To compile the examples run 'make examples' - the example files will be compiled in ot ./tmp
directory. Also a text files with a hex dump will be produced for your convenience.

**Running the compiler**

The complier is a java program, therefore you'll need at least JRE of version 1.6 (older versions may
work as well). Compile your data like that:
<pre>
java -jar gdc.jar -i=[input_script.d] -o=[output_binary.bin] [options]
</pre>

If you don't pass any parameters, then the list of options will be printed.


**Basic syntax rules**

* only one data element on each row, the exception is the array element, where the contents of the 
  array can be put on the same line
* use curly braces to start and end the data section
* curly braces for data section must be on separate text lines (no mixing curly braces & data on
  a single line)
* use '{#}' to insert the data section counter (by default 16 bit integer) which counts and stores
  the number of elements existing on the same hierarchy level within the script
* use '{ MyStructure' to specify that the section must adhere to certain
  structure format (ensures the same number of fields and the same order of fields within the struct)
* define your elements: type name [optional-value]. For example 'int MAGIC 0x102030' defines a 
  constant called MAGIC of type int (32 bits) with value 0x102030.
* define your arrays: the same way as other variables. Array elements are spearated by space.
  For example 'short[] coords' defines a data type called 'coords' that will contain a variable-length
  array of 16bit integers. To fill the array and to save data use 'coords = 100 200 300 400' which will
  save the number ofthe eleements in the array as 16 bit integer, then the individual elements.
* define your struct: 
<pre>
struct MyStruct {
  byte id
  string label ""
}
{
  { MyStruct
    id = 1
    // the label is auto filled by an empty string
  }
}
</pre>

* use '//' for comments
* use #ifdef [SYMBOL] #else #endif for conditional data compilation
* use -DSYMBOL or -DCONST_VAL=100 as a parameter to inject values or preprocessor
  symbols during compilation time.
  
