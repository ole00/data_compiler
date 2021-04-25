/* 
DataElement.java 
Copyright (C) 2007-2011 Marek Olejnik

This file is part of the Generic Data Compiler

Generic Data Compiler is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2, or (at your option)
any later version.

Generic Data Compiler is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
General Public License for more details.

*/

package ole.gdc;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;


public class DataElement {
	public static final int TYPE_AUX = 0;
	public static final int TYPE_BYTE = 1;
	public static final int TYPE_SHORT = 2;
	public static final int TYPE_INT = 4;
	public static final int TYPE_LONG = 8;
	public static final int TYPE_FILE = 9;
	public static final int TYPE_FLOAT = 10;
	public static final int TYPE_DOUBLE = 11;

	public static final int TYPE_BYTE_ARRAY = 32;
	public static final int TYPE_SHORT_ARRAY = 33;
	public static final int TYPE_INT_ARRAY = 34;
	public static final int TYPE_LONG_ARRAY = 35;
	public static final int TYPE_FLOAT_ARRAY = 36;
	public static final int TYPE_DOUBLE_ARRAY = 37;

	public static final int TYPE_STRUCT = 64;

	public static final int TYPE_STRING  = 128;

	private static final String MSG_INCOPATIBLE_TYPE = "incompatible type";
	
	private static final byte[] ZERO_ARRAY = new byte[1024];

	String name;
	int type;
	int arrayLen;  //string array length
	String value;	//for constants
	boolean checkedForExpression;	//true if value was checked for expression and that expression was resolved
	LineItem sourceLine;

	private static ByteArrayOutputStream leBos = new ByteArrayOutputStream(8);
	private static DataOutputStream leDout = new DataOutputStream(leBos);


	public DataElement(LineItem li, HashMap constants) {
		this.sourceLine = li;
		String line = li.line;
		StringTokenizer st = new StringTokenizer(line, " \t"); //space or tabulator
		int size = st.countTokens();
		if (size < 2) {
			throw new IllegalArgumentException("create data element=" + line);
		}

		String data = checkArrayName(st.nextToken());
		name = st.nextToken();
		
		//constant
		if (size == 3) {
			value = st.nextToken().trim();
		} else
		//string usually contain spaces and tabs -> keep the value intact
		if (size > 3) {
			value = st.nextToken().trim();
			int tokenIndex = line.indexOf(value);
			value = line.substring(tokenIndex).trim();
		}

		if (data.equals("byte")) {
			type = TYPE_BYTE;
		} else
		if (data.equals("short")) {
			type = TYPE_SHORT;
		} else
		if (data.equals("int")) {
			type = TYPE_INT;
		} else
		if (data.equals("long")) {
			type = TYPE_LONG;
		} else
		if (data.equals("string")) {
			type = TYPE_STRING;
		} else
		if (data.equals("float")) {
			type = TYPE_FLOAT;
		} else
		if (data.equals("double")) {
			type = TYPE_DOUBLE;
		} else
		if (data.equals("byteArray") || data.equals("byte[]")) {
			type = TYPE_BYTE_ARRAY;
		} else
		if (data.equals("shortArray") || data.equals("short[]")) {
			type = TYPE_SHORT_ARRAY;
		} else
		if (data.equals("intArray") || data.equals("int[]")) {
			type = TYPE_INT_ARRAY;
		} else
		if (data.equals("longArray") || data.equals("long[]")) {
			type = TYPE_LONG_ARRAY;
		} else
		if (data.equals("floatArray") || data.equals("float[]")) {
			type = TYPE_FLOAT_ARRAY;
		} else
		if (data.equals("doubleArray") || data.equals("double[]")) {
			type = TYPE_DOUBLE_ARRAY;
		} else
		if (data.equals("struct")) {
			type = TYPE_STRUCT;
		} else
		if (data.equalsIgnoreCase("file")) {
			type = TYPE_FILE;
		} else
		if (data.equals("=") && value != null) {
			type = guessType(value, constants);
		}
		else {
			throw new IllegalArgumentException("unknown element type=" + data);
		}
	}

	private String checkArrayName(String name) {
		if (!name.endsWith("]") || name.contains("[]")) {
			return name;
		}
		int index = name.indexOf("[");
		if (index < 1) {
			return name;
		}
		arrayLen = Integer.decode(name.substring(index + 1, name.length() - 1));
		//System.out.println("name=" + name.substring(0, index) + " arrlen=" + arrayLen);
		return name.substring(0, index);
	}
	
	public DataElement(String name, int type, LineItem li) {
		sourceLine = li;
		this.name = name;
		this.type = type;
	}

	public DataElement(String name, String value, int type, LineItem li) {
		sourceLine = li;
		this.name = name;
		this.type = type;
		this.value = value;
	}

	public DataElement(String name, String value, LineItem li, HashMap constants) {
		sourceLine = li;
		this.name = name;
		this.value = value;
		type = guessType(value, constants);
	}
	
	public int guessType(String val, HashMap constants) {
		if (val == null) {
			return TYPE_AUX;
		}
		if (isDecimalNumber(val)) {
			try {
				Float.parseFloat(val);
				return TYPE_FLOAT;
			} catch (Exception e) {
			}
			try {
				Double.parseDouble(val);
				return TYPE_DOUBLE;
			} catch (Exception e) {
			}
		}

		if (isIntegerNumber(val)) {
			Long l = Long.decode(val);
			if (l >= Byte.MIN_VALUE && l <= Byte.MAX_VALUE ) {
				return TYPE_BYTE;
			}
			if (l >= Short.MIN_VALUE && l <= Short.MAX_VALUE) {
				return TYPE_SHORT;
			}
			if (l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE) {
				return TYPE_INT;
			}
			return TYPE_LONG;
		}

		if (constants != null) {
			DataElement de = (DataElement) constants.get(val);
			if (de != null) {
				if (!de.checkedForExpression) {
					de.resolveExpressions(constants);
				}
				return de.type;
			}
		}

		return TYPE_STRING;
	}
	
	private boolean isIntegerNumber(String val) {
		char[] ca = val.toCharArray();
		for (int i = 0; i < ca.length; i++) {
			final char c = ca[i];
			if (
				(!(c == 'x' || c == 'X' || c == 'a' || c == 'A' || c=='b'|| c == 'B' || 
					c=='c' || c=='C' || c == 'd' || c == 'D' || c == 'e' || c == 'E' ||
					c=='f' || c=='F')
				) &&
				(c < '0' ||  c > '9')

			) {
				return false;
			}
		}
		return true;
	}

	private boolean isDecimalNumber(String val) {
		char[] ca = val.toCharArray();
		boolean foundDecimalPoint = false;
		for (int i = 0; i < ca.length; i++) {
			final char c = ca[i];
			//not a number AND not a decimal point
			if ((c < '0' ||  c > '9') && !(c == '.')) {
				return false;
			}

			if (c == '.') {
				// 2 decimal points found!
				if (foundDecimalPoint) {
					return false;
				}
				foundDecimalPoint = true;
			}
		}
		return foundDecimalPoint;
	}
	
	public void store(DataOutputStream dout, String data, HashMap constants, String parentPath, LineItem li) throws Exception {
		store(dout, data, type, constants, parentPath, true, li);
	}

	public boolean isConstant() {
		if (type == TYPE_STRUCT) {
			DataStruct struct = (DataStruct) DataStruct.structs.get(value);
			if (struct == null) {
				return false;
			}
			return struct.isConstant();
		} else {
			return value != null;
		}
	}
	
	public String getValue() {
		return value;
	}

	public int getElementCount(String data, int dataType, HashMap constants) {
		data = data.trim();
		switch (dataType) {
			case TYPE_BYTE: {
				DataElement de = (DataElement)constants.get(data);
				int elementType  = TYPE_BYTE; // by default treat each element as byte
				if (de != null) {
					data = de.value;
					elementType = de.type;
				}
				if (elementType ==  TYPE_BYTE) {
					return 1;
				} else
				if (elementType == TYPE_SHORT) {
					return 2;
				} else
				if (elementType == TYPE_INT) {
					return 4;
				} else
				if (elementType == TYPE_LONG) {
					return 8;
				}
			} break;
			case TYPE_SHORT: {
				DataElement de = (DataElement)constants.get(data);
				int elementType  = TYPE_SHORT; // by default treat each element as short
				if (de != null) {
					data = de.value;
					elementType = de.type;
				}
				if (elementType == TYPE_BYTE || elementType == TYPE_SHORT) {
					return 1;
				} else
				if (elementType == TYPE_INT){
					return 2;
				} else
				if (elementType == TYPE_LONG) {
					return 4;
				}
			} break;
			case TYPE_INT: {
				DataElement de = (DataElement)constants.get(data);
				int elementType  = TYPE_INT; // by default treat each element as int 
				if (de != null) {
					data = de.value;
					elementType = de.type;
				}
				if (elementType == TYPE_BYTE || elementType == TYPE_SHORT || elementType == TYPE_INT) {
					return 1;
				} else 
				if (elementType == TYPE_LONG){
					return 2;
				}
			} break;
		}
		return 1;
	}

	private void throwWarning(String dataType, String value) {
		String msg = MSG_INCOPATIBLE_TYPE + "=" + dataType + " value=" + value;
		if (DataCompiler.useWarningsAsErrors) {
			throw new IllegalArgumentException(msg);
		} else {
			System.out.println("warning: " + msg + ", line=" + DataCompiler.lineNumber);
		}
	}

	static final void writeShort(DataOutputStream dout, short i) throws IOException {
		if (DataCompiler.littleEndian) {
			int s = i & 0xFFFF;
			dout.write(s & 0xFF);
			dout.write(s >> 8);
		} else {
			dout.writeShort(i);
		}
	}

	static final void writeInt(DataOutputStream dout, int i) throws IOException {
		if (DataCompiler.littleEndian) {
			long s = i & 0xFFFFFFFF;
			dout.write((int)(s & 0xFF));
			dout.write((int)((s >> 8) & 0xFF));
			dout.write((int)((s >> 16) & 0xFF));
			dout.write((int)((s >> 24) & 0xFF));
		} else {
			dout.writeInt(i);
		}
	}

	private static final void writeLong(DataOutputStream dout, long i) throws IOException {
		if (DataCompiler.littleEndian) {
			dout.write((int)(i & 0xFF));
			dout.write((int)((i >> 8) & 0xFF));
			dout.write((int)((i >> 16) & 0xFF));
			dout.write((int)((i >> 24) & 0xFF));
			dout.write((int)((i >> 32) & 0xFF));
			dout.write((int)((i >> 40) & 0xFF));
			dout.write((int)((i >> 48) & 0xFF));
			dout.write((int)((i >>> 56) & 0xFF));
		} else {
			dout.writeLong(i);
		}
	}

	private static final void writeFloat(DataOutputStream dout, float i) throws IOException {
		if (DataCompiler.littleEndian) {
			leBos.reset();
			leDout.writeFloat(i);
			byte[] d = leBos.toByteArray();
			
			dout.write((int)(d[3] & 0xFF));
			dout.write((int)(d[2] & 0xFF));
			dout.write((int)(d[1] & 0xFF));
			dout.write((int)(d[0] & 0xFF));
		} else {
			dout.writeFloat(i);
		}
	}

	private static final void writeDouble(DataOutputStream dout, double i) throws IOException {
		if (DataCompiler.littleEndian) {
			leBos.reset();
			leDout.writeDouble(i);
			byte[] d = leBos.toByteArray();

			dout.write((int)(d[7] & 0xFF));
			dout.write((int)(d[6] & 0xFF));
			dout.write((int)(d[5] & 0xFF));
			dout.write((int)(d[4] & 0xFF));
			dout.write((int)(d[3] & 0xFF));
			dout.write((int)(d[2] & 0xFF));
			dout.write((int)(d[1] & 0xFF));
			dout.write((int)(d[0] & 0xFF));
		} else {
			dout.writeDouble(i);
		}
	}
	
	private final void writeString(DataOutputStream dout, String s, int maxLen, LineItem li) throws IOException {
		byte[] data = s.getBytes("UTF8");
		if (data.length >= maxLen) {
			String warning = "String size is too big: " + data.length + " (max: " + maxLen + ") ";
						
			if (DataCompiler.useWarningsAsErrors) {
				throw new IllegalArgumentException(warning);
			} else {
				System.out.println("Warning: " + warning + li.getSource());
			}
			dout.write(data, 0, maxLen);
		} else {
			dout.write(data);
			// write the padding
			int max = maxLen - data.length;
			while (max > 0) {
				int block = max > ZERO_ARRAY.length ? ZERO_ARRAY.length : max;
				dout.write(ZERO_ARRAY, 0, block);
				max -= block;
			}
		}
		
	}
	
	public void store(DataOutputStream dout, String data, int dataType, HashMap constants, String parentPath, boolean strictTypes, LineItem li) throws Exception {
		data = data.trim();
		//try to find/resolve a variable
		DataElement de = (DataElement)constants.get(data);
		if (de == null) {
			de = new DataElement("<anonymous>", data, dataType, sourceLine);
		}
			
		switch (dataType) {
			case TYPE_BYTE: {
				int elementType  = TYPE_BYTE; // by default treat each element as byte
				if (de != null) {
					if (!de.checkedForExpression) {
						de.resolveExpressions(constants);
					}
					data = de.value;
					elementType = de.type;
				}
				long l = 0;
				
				try {
					l = Long.decode(data);
				} catch (NumberFormatException e) {
					String match = findBestMatch(constants, data);
					if (match == null) {
						match = "";
					} else {
						match = " Did you mean ".concat(match).concat("?");
					}
					throw new NumberFormatException(e.getMessage().concat(match));
				}
				if (strictTypes || elementType ==  TYPE_BYTE) {
					if (DataCompiler.warnInConversion) {
						if (l > Byte.MAX_VALUE || l < Byte.MIN_VALUE) {
							throwWarning("byte", data);
						}
					} else {
						long lx = l - Byte.MIN_VALUE;
						if (lx < 0 || lx > 0x17f) {
							throwWarning("byte", data);
						}
					}
					dout.writeByte((int)l);
				} else
				if (elementType == TYPE_SHORT) {
					writeShort(dout, (short)l);
				} else
				if (elementType == TYPE_INT) {
					writeInt(dout, (int) l);
				} else
				if (elementType == TYPE_LONG) {
					writeLong(dout, l);
				}
			} break;
			case TYPE_SHORT: {
				int elementType  = TYPE_SHORT; // by default treat each element as short
				if (de != null) {
					if (!de.checkedForExpression) {
						de.resolveExpressions(constants);
					}
					data = de.value;
					elementType = de.type;
				}
				long l = 0;
				
				try {
					l = Long.decode(data);
				} catch (NumberFormatException e) {
					String match = findBestMatch(constants, data);
					if (match == null) {
						match = "";
					} else {
						match = " Did you mean ".concat(match).concat("?");
					}
					throw new NumberFormatException(e.getMessage().concat(match));
				}
				if (strictTypes || elementType == TYPE_BYTE || elementType == TYPE_SHORT) {
					if (l > Short.MAX_VALUE || l < Short.MIN_VALUE) {
						throwWarning("short", data);
					}
					writeShort(dout, (short)l);
				} else
				if (elementType == TYPE_INT){
					writeInt(dout, (int)l);
				} else
				if (elementType == TYPE_LONG) {
					writeLong(dout, l);
				}
			} break;
			case TYPE_INT: {
				int elementType = TYPE_INT;
				if (de != null) {
					if (!de.checkedForExpression) {
						de.resolveExpressions(constants);
					}
					data = de.value;
					elementType = de.type;
				}
				long l = 0;
				
				try {
					l = Long.decode(data);
				} catch (NumberFormatException e) {
					String match = findBestMatch(constants, data);
					if (match == null) {
						match = "";
					} else {
						match = " Did you mean ".concat(match).concat("?");
					}
					throw new NumberFormatException(e.getMessage().concat(match));
				}
				if (strictTypes || elementType == TYPE_BYTE || elementType == TYPE_SHORT || elementType == TYPE_INT) {
					if (l > Integer.MAX_VALUE || l < Integer.MIN_VALUE) {
						throwWarning("int", data);
					}
					writeInt(dout, (int)l);
				} else
				if (elementType == TYPE_LONG) {
					writeLong(dout, l);
				}
			} break;
			case TYPE_LONG: {
				if (de != null) {
					if (!de.checkedForExpression) {
						de.resolveExpressions(constants);
					}
					data = de.value;
				}
				long l = 0;
				
				try {
					l = Long.decode(data);
				} catch (NumberFormatException e) {
					String match = findBestMatch(constants, data);
					if (match == null) {
						match = "";
					} else {
						match = " Did you mean ".concat(match).concat("?");
					}
					throw new NumberFormatException(e.getMessage().concat(match));
				}
				writeLong(dout, l);
			} break;
			case TYPE_FLOAT : {
				if (de != null) {
					if (!de.checkedForExpression) {
						de.resolveExpressions(constants);
					}
					data = de.value;
				}
				double d = 0;
				
				try {
					d = Double.parseDouble(data);
				} catch (NumberFormatException e) {
					String match = findBestMatch(constants, data);
					if (match == null) {
						match = "";
					} else {
						match = " Did you mean ".concat(match).concat("?");
					}
					throw new NumberFormatException(e.getMessage().concat(match));
				}
				
				writeFloat(dout, (float) d);
			} break;
			case TYPE_DOUBLE : {
				if (de != null) {
					if (!de.checkedForExpression) {
						de.resolveExpressions(constants);
					}
					data = de.value;
				}
				double d = 0;
				try {
					d = Double.parseDouble(data);
				} catch (NumberFormatException e) {
					String match = findBestMatch(constants, data);
					if (match == null) {
						match = "";
					} else {
						match = " Did you mean ".concat(match).concat("?");
					}
					throw new NumberFormatException(e.getMessage().concat(match));
				}
				writeDouble(dout, d);
			} break;
			case TYPE_STRING: {
				int elementType = TYPE_STRING;
				if (de != null) {
					if (!de.checkedForExpression) {
						de.resolveExpressions(constants);
					}
					data = de.value;
					elementType = de.type;
				} else
				//data element was not found and the value doesn't have quotes -> it's a missing variable
				if (!data.startsWith("\"") && !data.endsWith("\"")) {
					throw new RuntimeException("element not found: " + data);
				}

				if (elementType == TYPE_STRING && data.startsWith("\"") && data.endsWith("\"")) {
					data = data.substring(1, data.length() - 1);
				}
				data = UnicodeEscapes.unescape(data);
				if (arrayLen == 0) {
					dout.writeUTF(data);
				} else {
					writeString(dout, data, arrayLen, li);
				}
			} break;
			case TYPE_FILE: {
				if (de != null) {
					if (!de.checkedForExpression) {
						de.resolveExpressions(constants);
					}
					data = de.value;
				}
				if (data.startsWith("\"") && data.endsWith("\"")) {
					data = data.substring(1, data.length() - 1);
				}
				File f = new File(parentPath + "/" + data);
				int fileSize = (int)f.length();
				DataInputStream din = new DataInputStream(new BufferedInputStream(new FileInputStream(f)));
				byte[] buf = new byte[fileSize];
				din.readFully(buf);
				din.close();

				writeInt(dout, fileSize);
				dout.write(buf);
			}break;
			case TYPE_BYTE_ARRAY :
			case TYPE_SHORT_ARRAY :
			case TYPE_INT_ARRAY :
			case TYPE_LONG_ARRAY:
			{
				int elementType = TYPE_BYTE + (dataType - TYPE_BYTE_ARRAY);
				StringTokenizer st = new StringTokenizer(data, " \t");
				int size = st.countTokens();
				int newSize = size;
				//get total size of the array
				if (elementType == TYPE_BYTE || elementType == TYPE_SHORT) {
					for (int i = 0; i < size; i++) {
						String value  = (String) st.nextElement();
						value = value.trim();
						int cnt = getElementCount(value, elementType, constants);
						newSize += (cnt - 1);
					}
				}  else
				if (elementType == 3) {
					elementType = TYPE_INT;
				} else
				if (elementType == 4){
					elementType = TYPE_LONG_ARRAY;
				}

				if (DataCompiler.useIntegerArraySize) {
					writeInt(dout, newSize);
				} else {
					writeShort(dout, (short) newSize);
				}
				st = new StringTokenizer(data, " \t");
				for (int i = 0; i < size; i++) {
					String value  = (String) st.nextElement();
					value = value.trim();
					store(dout, value, elementType, constants, parentPath, false, li);
				}
			} break;
			case TYPE_FLOAT_ARRAY:
			case TYPE_DOUBLE_ARRAY:
			{
				int elementType = TYPE_FLOAT + (dataType - TYPE_FLOAT_ARRAY);
				StringTokenizer st = new StringTokenizer(data, " \t");
				int size = st.countTokens();
				if (DataCompiler.useIntegerArraySize) {
					writeInt(dout, size);
				} else {
					writeShort(dout, (short) size);
				}
				for (int i = 0; i < size; i++) {
					String value  = (String) st.nextElement();
					value = value.trim();
					store(dout, value, elementType, constants, parentPath, false, li);
				}
			} break;
			case TYPE_STRUCT: {
				DataStruct struct = (DataStruct) DataStruct.structs.get(value);
				if (struct == null) {
					throw new RuntimeException("struct name=" + value + " not found");
				}

				int size = struct.getSize();
				for (int i = 0; i < size; i++) {
					DataElement element = struct.getElement(i);
					if (!element.isConstant()) {
						throw new RuntimeException("struct name=" + value + " is not constant. Some element don't have known value.");
					}
					element.store(dout, element.value, element.type, constants, parentPath, true, li);
				}
			}
		}
	}

	private void resolveExpressions(HashMap constants) {
		checkedForExpression = true;
		// check "condition?trueValue:falseValue"
		if (resolveExpressionCondition(constants)) {
			return;
		}
		//check VAR1|VAR2|TEST
		if (resolveExpresionBinaryAddition(constants)) {
			return;
		}
		//check VALUE1 + VALUE2 + "xxx" + VALUE3 + ....
		if (resolveExpressionAddition(constants)) {
			return;
		}
		if (resolveExpressionVariable(constants)) {
			return;
		}
	}

	private boolean resolveExpressionAddition(HashMap constants) {
		//quick and dirty check there is an addition sign
		int index = value.indexOf('+');
		if (index < 2) {
			return false;
		}

		String[] parts = DataCompiler.split(value, '+', true);
		//all plus signs are within quotes as a part of a text
		if (DataCompiler.verbose) {
			System.out.println("expression: " + value);
		}
		if (parts.length < 2) {
			return false;
		}


		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < parts.length; i++) {
			String part = parts[i];
			if (DataCompiler.verbose) {
				System.out.println("part "  + i + ":" + part);
			}
			if (part.startsWith("\"") && part.endsWith("\"")) {
				part = part.substring(1, part.length() - 1);
			} else {
				DataElement de = (DataElement)constants.get(part);
				if (de != null) {
					if (!de.checkedForExpression) {
						de.resolveExpressions(constants);
					}
					part = de.value;
					if (part.startsWith("\"") && part.endsWith("\"")) {
						part = part.substring(1, part.length() - 1);
					}
				} else {
					throw new RuntimeException("element not found: " + part + " while resolving expression: " + value);
				}
			}
			sb.append(part);
		}
		value = sb.toString();

		return true;
	}

	private boolean resolveExpresionBinaryAddition(HashMap constants) {
		if (type == TYPE_BYTE || type == TYPE_SHORT || type == TYPE_INT || type == TYPE_LONG) {
			//must not contain space
			if (value.indexOf(' ') >= 0) {
				return false;
			}
			//must contain at least one sign of binary addition '|'
			if (value.indexOf('|') < 0) {
				return false;
			}
			String[] bits = value.split("\\|");
			long result = 0;
			
			for (int i = 0; i < bits.length; i++) {
				DataElement de = (DataElement)constants.get(bits[i]);
				if (de != null) {
					if (!de.checkedForExpression) {
						de.resolveExpressions(constants);
					}
					long v = Long.decode(de.value);
					result |= v;
				}
			}
			value = Long.toString(result);
			return true;
		}
		return false;
	}
	
	private boolean resolveExpressionVariable(HashMap constants) {
		if (type == TYPE_STRING) {
			if (value.startsWith("\"") && value.endsWith("\"")) {
				return true;
			}
			DataElement de = (DataElement)constants.get(value);
			if (de != null) {
				if (!de.checkedForExpression) {
					de.resolveExpressions(constants);
				}
				value = de.value;
			} else {
				String match = findBestMatch(constants, value);
				if (match == null) {
					match = "";
				} else {
					match = " Did you mean ".concat(match).concat("?");
				}

				throw new RuntimeException("Element not found: " + value + " while resolving expressions.".concat(match));
			}
			return true;
		} else
		if (type == TYPE_BYTE || type == TYPE_SHORT || type == TYPE_INT || type == TYPE_LONG) {
			if (isIntegerNumber(value)) {
				return true;
			}
			DataElement de = (DataElement)constants.get(value);
			if (de != null) {
				if (!de.checkedForExpression) {
					de.resolveExpressions(constants);
				}
				value = de.value;
			}
			//note: an Exception will be thrown when parsing the value to numeric format
			return true;
		} else
		if (type == TYPE_FLOAT || type == TYPE_DOUBLE ) {
			if (isDecimalNumber(value)) {
				return true;
			}
			DataElement de = (DataElement)constants.get(value);
			if (de != null) {
				if (!de.checkedForExpression) {
					de.resolveExpressions(constants);
				}
				value = de.value;
			}
			//note: an Exception will be thrown when parsing the value to numeric format
			return true;
		}
		return false;
	}

	private boolean resolveExpressionCondition(HashMap constants) {
		int colonIndex = value.indexOf(':');
		if (colonIndex < 0) {
			return false;
		}
		int questIndex = value.indexOf('?');
		if (questIndex > 0 && colonIndex > questIndex) {
			String[] parts = new String[3];
			parts[0] = value.substring(0, questIndex).trim();
			parts[1] = value.substring(questIndex + 1, colonIndex).trim();
			parts[2] = value.substring(colonIndex + 1).trim();
			boolean condition = false;
			DataElement de = (DataElement)constants.get(parts[0]);
			//we found a constant
			if (de != null) {
				if (!de.checkedForExpression) {
					de.resolveExpressions(constants);
				}
				switch (de.type) {
					case TYPE_BYTE:
					case TYPE_SHORT:
					case TYPE_INT: {
						int val = Integer.decode(de.value);
						condition = val != 0;
					} break;
					case TYPE_LONG: {
						long val = Long.decode(de.value);
						condition = val != 0;
					} break;
					case TYPE_FLOAT:
					case TYPE_DOUBLE:
					{
						double d = Double.parseDouble(de.value);
						condition = (d != 0.0d);
					} break;
					case TYPE_STRING : {
						//the fact the string exists means the condition is true
						condition = (de.value != null && de.value.length() > 0);
					} break;
					default : {
						throw new IllegalArgumentException("expression cannot be evaluated: " + value + " type=" + de.type);
					}
				}
			}
			value = condition ? parts[1] : parts[2];

			//try to resolve the result
			de = (DataElement)constants.get(value);
			if (de != null) {
				if (!de.checkedForExpression) {
					de.resolveExpressions(constants);
				}
				value = de.value;
			}

			return true;
		}
		return false;
	}
	
	private String findBestMatch(HashMap map, String name) {
		if (map == null || name == null || name.length() < 1 || name.length() > 60) {
			return null;
		}
		int maxScore = 0;
		String result = null;
		Set s = map.keySet();
		Iterator i = s.iterator();
		byte[] nameDensityMap = getDensityMap(name);
		while (i.hasNext()) {
			String key = (String) i.next();
			int score = getMatchingScore(name, key, nameDensityMap);
			if (score > maxScore) {
				maxScore = score;
				result = key;
			}
		}
		return result;
	}
	
	private int getMatchingScore(String s1, String s2, byte[] s1DensityMap) {
		//scores are awarded by the similarity in length and character map density
		int delta = s1.length() - s2.length();
		if (delta < 0) {
			delta = -delta;
		}
		int score = 100 - ((delta * 100) / s1.length());
		
		//check character map density
		byte[] map2 = getDensityMap(s2);
		//compare density maps
		for (int i = 33; i < 128; i++) {
			delta = s1DensityMap[i] - map2[i];
			if (delta < 0) {
				delta = -delta;
			}
			score -= delta;
		}
		return score;
	}
	private byte[] getDensityMap(String s) {
		byte[] result = new byte[128];
		
		byte[] b = s.getBytes();
		for (int i = 0; i < b.length; i++) {
			int index = b[i];
			if (index > 32) {
				result[index]++;
			}
		}
		
		return result;
	}
}
