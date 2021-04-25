
// stores 2 strings: each string contains 2 bytes of the length, then the UTF-8 bytes

// eaxmaple of splitting the string to multiple lines
string text


// start of the data definition
{
  // The text can be split to multiple lines - use backslash at the end of the line to concatenate.
  // Note that the space character needs to be explicitly added between words.
  text = "Hello" \
         " text world!"
  
  // Or this can be used as well. 
  // Note that a space character will be automatically added in between concatenated lines. 
  text = "Hello \
text \
world!"
}
