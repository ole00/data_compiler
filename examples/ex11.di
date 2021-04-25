
// this file contains a few constant and a struct definition

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

