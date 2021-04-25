
// stores  strings: number of element (3), then the UTF-8 bytes of each text (3x 16)

string[16] text


// start of the data definition
{

  // write the number of elements (3 in this case)
  {#}

  // all texts will be expanded to 16 bytes and padded with 0x00
  // NOTE: the text will NOT have length stored as the leading short
  {
    text = "Hello world!"
  }

  {
    text = "Text 2"
  }

  {
    text = "The End."
  }


}
