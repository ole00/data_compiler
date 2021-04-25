
// variable number of elements with deeper levels 

byte ID_APPLE 1
byte ID_PEAR 2
byte ID_ORANGE 3
byte ID_LEMON 4 


short property
int value


// start of the data definition
{

  {#} // number of top level elements (2 in this case)
 
  // top level element no. 1
  {
    ID_APPLE  //stores a byte with value 1
    {#} // number of elements in this section (3 in this case)
    {
      property = 32
      value = 100
    }

    {
      property = 32
      value = 101
    }

    {
      property = 32
      value = 100
    }
  }

  // top level element no. 2
  {
     ID_LEMON //stores a byte with value 4
     {#} // number of elements in this section (1 in this case)
     {
        property = 34
        value = 200
     }
  }
}
