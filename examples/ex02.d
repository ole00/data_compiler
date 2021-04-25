
// ./compile.sh ex02.d -le -DUSE_SIXTY_FOUR -DTHE_VALUE=128

// stores 6 x 16 bit integer: 1, 0xABCD ,  3 (number of elements), 32, 64 (or 65), 128
// NOTE: if USE_SIXTY_FOUR is not defined then value 65 is written
// NOTE: THE_VALUE constant is not defined, therefore must be provided on command line
//       as a Define (-DTHE_VALUE=value)

short value  //the 'value' will be written as 16bit number

short VERSION 1
short MAGIC 0xABCD // produces a compilation warning because it is out of range of signed 16 bit number


// start of the data definition
{
	VERSION
	MAGIC

	{#} //number of elements on this level
	{
		value = 32
	}

#ifdef USE_SIXTY_FOUR
	{
		value = 64
	}
#else
	{
		value = 65
	}
#endif

	{
		value = THE_VALUE
	}

}
