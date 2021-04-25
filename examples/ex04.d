
// stores strings: 3 x 16 bytes of zero padded text

string[16] text


// start of the data definition
{
  // all texts will be expanded to 16 bytes and padded with 0x00
  // NOTE: the text will NOT have length stored as the leading short
  //       because the text length is specified in the type definition [16]
  text = "Hello world!"
  text = "Text 2"
  text = "This text is longer than 16 character and therefore will be trimmed. A compiler warning will be produced."
}
