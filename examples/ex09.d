
// variable number of elements with deeper levels, using a structre

byte ID_APPLE 1
byte ID_LEMON 4 

// This structure has some default values, which - if not provided -
// will be auto-filled. The 'value' must be defined, because it has no
// default value.
struct Dataset {
	short property 32
	int value
	string[16] label "<no label>"
}

// start of the data definition
{

  {#} // number of top level elements (2 in this case)
 
  // top level element no. 1
  {
    ID_APPLE  //stores a byte with value 1
    {#} // number of elements in this section (3 in this case)
    { Dataset
      property = 34
      value = 100
      // 'label' is auto-filled
    }

    { Dataset
      // 'property' is autofilled
      value = 101
      // 'label' is auto-filled
    }

    { Dataset
      // 'property' is autofilled
      value = 102
      // the order of fileds defined in the 'struct Dataset' definition,
      // must match this definition as well
      label = "label-1.3"
    }
  }

  // top level element no. 2
  {
     ID_LEMON //stores a byte with value 4
     {#} // number of elements in this section (1 in this case)
     { Dataset
        property = 34
        value = 200
        label = "label-2.1"
     }
  }
}
