/* 
DataCompiler.java 
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

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.Vector;


public class DataCompiler {
	
	private static final String VERSION = "1.0";
	
	public static boolean useIntegerArraySize;
	public static boolean useWarningsAsErrors;
	public static boolean warnInConversion ;	//unsigned values stored in signed types 
	public static boolean verbose;
	public static String commentDef = "//";
	
	public static boolean littleEndian = false;
	
	public static int lineNumber;
	public static LineItem li;
	
	private static HashMap elements;
	private static HashMap constants;
	private static Vector includeDirs;
	private static HashMap structs;
	
	public static void main(String[] args) {
		args = Utils.getArguments(args);
		
		String inFile = null;
		String outFile = null;
		String encoding = null;
		String symbols = null;
		includeDirs = new Vector();
		for (int i = 0; i < args.length; i++) {
			String arg = args[i]; 
			if (arg.startsWith("-i=")) {
				inFile = arg.substring(3);
			} else
			if (arg.startsWith("-o=")) {
				outFile = arg.substring(3);
			} else
			if (arg.startsWith("-cd=")) {
				commentDef = arg.substring(4);
			} else
			if (arg.startsWith("-enc=")) {
				encoding = fixEncoding(arg.substring(5));
			} else
			if (arg.startsWith("-charset=")) {
				encoding = fixEncoding(arg.substring(9));
			} else
			//preprocessor symbols - treat them as constants
			if (arg.startsWith("-pp=")) {
				String s = arg.substring(4);
				if (symbols == null) {
					symbols = s;
				}
				//append symbols
				else {
					symbols += "," + s;
				}
			} else
			if (arg.startsWith("-D")) {
				String s = arg.substring(2);
				if (symbols == null) {
					symbols = s;
				}
				//append symbols
				else {
					symbols += "," + s;
				}
			} else
			//integer array size
			if (arg.equals("-ias")) {
				useIntegerArraySize = true;
			} else
			//little endian
			if (arg.equals("-le")) {
				littleEndian = true;
			} else			//verbose mode 
			if (arg.equals("-v")) {
				verbose = true;
			} else
			//Make all warnings into errors. 
			if (arg.equals("-Werror")) {
				useWarningsAsErrors = true;
			} else			
			//warn if unsigned values are stored in the signed data types  
			if (arg.equals("-Wconversion")) {
				warnInConversion = true;
			} else			
			//add include directory. 
			if (arg.startsWith("-I")) {
				includeDirs.add(arg.substring(2));
			}			
		}
		if (encoding != null && verbose) {
			System.out.println("encoding=" + encoding);
		}
		if (inFile == null || outFile == null) {
			printHelp();
			System.exit(-1);
		}
		if (verbose) {
			Utils.printInfo("DataCompiler:", args);
		}
		run(inFile, outFile, encoding, symbols);
	}
	
	private static void printHelp() {
		System.out.println("DataCompiler - generic data compiler; version " + VERSION);
		System.out.println("parameters: ");
		System.out.println("-i=inputFile  : specify input file " );
		System.out.println("-o=outputFile : specify output file");
		System.out.println("-enc=encoding (or -charset=encoding): specify input file encoding");
		System.out.println("     default is utf-8, other values are us-ascii or iso-8859-1");
		System.out.println("-pp=preprocessorSymbols : symbols are separated by comma");
		System.out.println("-DpreprocessorSymbol");
		System.out.println("-ias : use integer type as a size of the array (by default it's a  short type)");
		System.out.println("-Werror : treat all warnings as errors. ");
		System.out.println("-Wconversion : warn if unsigned values are stored in the signed data types.");  		
		System.out.println("-IincludeDirectory : add path to your includes.");
		System.out.println("-le : write data as little endian (default is big endian)");
		System.out.println("-cd=comment : used 'comment' as the comment marker (default is // )");	
	}

	public static final String fixEncoding(String enc) {
		if (enc == null || enc.length() < 1) {
			return "UTF8";
		}
		if (enc.equalsIgnoreCase("utf-8")) {
			return "UTF8";
		}
		if (enc.equalsIgnoreCase("us-ascii") || enc.equalsIgnoreCase("iso-8859-1")) {
			return "ISO8859_1";
		}
		throw new IllegalArgumentException ("unknown encoding:" + enc);
	}

	public static void run(String inFileName, String outFileName, String encoding, String symbols) {
		outFileName = resolveHomePath(outFileName);
		inFileName =  resolveHomePath(inFileName);
		
		createElements(symbols);
		Vector v = Utils.getLineList(inFileName, encoding, constants, includeDirs);
		if (v == null) {
			return;
		}
		v = Utils.removeComments(v, commentDef);

		File f = new File(inFileName);
		OutputStream os = null;
		try {
			os = new BufferedOutputStream(new FileOutputStream(outFileName));
			File  parent = f.getParentFile();
			if (parent == null) {
				parent = new File(".");
			}
			binarize(os, v, parent.getAbsolutePath());
		} catch (Exception e) {
			if (verbose) {
				e.printStackTrace();
			}
			File outFile = new File(outFileName);
			outFile.delete();
			System.exit(-1);
			//throw new IllegalArgumentException();
		}
	}
	public static void run(byte[] inputData, OutputStream os, String path, String symbols) {
		createElements(symbols);
		Vector v = Utils.getLineList(inputData);
		v = Utils.removeComments(v, ";");
		binarize(os, v, path);
	}
	
	private static void checkElements(DataElement newElement, DataElement oldElement) {
		if (oldElement == null) {
			return;
		}
		String msg = "data element '" + newElement.name + "'  redefined in " + li.getSource() + " previous definition is in " + oldElement.sourceLine.getSource();
		if (DataCompiler.useWarningsAsErrors) {
			throw new IllegalArgumentException(msg);
		} else {
			System.out.println( "Warning: " + msg );
		}
	}
	
	public static void setConstants(HashMap constants) {
		DataCompiler.constants = constants;
	}

	public static void addConstant(String line, LineItem item) {
		
		String[] bits = Utils.split(line, "= \t");
		//Symbols without a value are boolean data elements set to true 
		if (bits.length == 1) {
			DataElement de = new DataElement(bits[0], DataElement.TYPE_BYTE, item);
			de.value = "1";	//means true
			constants.put(de.name, de);
			//note: we don't do element check here as boolean duplicates of the elements in pp symbols are ok ;
		} else
		{
			if (bits[1].startsWith("\"")) {
				int index = line.indexOf('=');
				if (index > 0) {
					bits[1] = line.substring(index + 1).trim();
				}
			}
			DataElement de = new DataElement(bits[0], bits[1], item, constants);
			//By default preprocessor symbols are treated as resolved - cannot contain expression
			de.checkedForExpression = true;
			DataElement old = (DataElement) constants.put(de.name, de);
			checkElements(de, old);
		}
		
	}
	private static void createElements(String symbols) {
		elements = new HashMap();
		constants = new HashMap();
		structs = new HashMap();
		DataStruct.structs = structs; 
		
		//parse preprocessor symbols and store them as constants
		if (symbols != null) {
			LineItem item = new LineItem(symbols, 0, "preprocessor symbols");
			String[] symParts = split(symbols, ',', false);
			for (int i = 0; i < symParts.length; i++) {
				addConstant(symParts[i], item);
			}
		}
	}
	
	private static boolean charIsWhiteSpace(char c) {
		return c == ' ' || c == '\t';
	}
	
	/*
	 * Special split that ignores delimiters within quoted strings.
	 */
	static String[] split(final String line, final char delimiter, boolean checkSpace) {
		char[] chars = line.toCharArray();
		
		boolean insideQoutedText = false;
		int startPosition = 0;
		Vector parts = new Vector();
		
		for (int i = 0; i < chars.length; i++) {
			char c = chars[i];
			if (c == '"') {
				insideQoutedText = !insideQoutedText;
			} else
			if (c == delimiter &&  !insideQoutedText) {
				boolean validPosition = !checkSpace;
				
				if (i > 0 && i < chars.length - 2 && (charIsWhiteSpace(chars[i-1])) && (charIsWhiteSpace(chars[i+1]))) {
					validPosition = true;
				}
				if (validPosition) {
					if (i > startPosition + 1) {
						final String part = line.substring(startPosition, i ).trim();
						if (part.length() > 0) {
							parts.add(part);
						}
					}
					startPosition = i + 1;
				}
			}
		}
		//add the last item
		if (startPosition < chars.length) {
			final String part = line.substring(startPosition).trim();
			if (part.length() > 0) {
				parts.add(part);
			}
		}
		
		String[] result = new String[parts.size()];
		if (parts.size() > 0) {
			parts.toArray(result);
		}
		return result;
	}
	
	public static void binarize(OutputStream os, Vector v, String path) {
		final int MAX_STRUCT_LEVEL = 256;
		int size = v.size();
		
		lineNumber = 0;
		try {
			DataStruct struct = null;
			DataStruct[] structStack = new DataStruct[MAX_STRUCT_LEVEL];
			int[] structElementIndices = new int[MAX_STRUCT_LEVEL];
			int structElementIndex = 0;
			DataOutputStream dos = new DataOutputStream(os);
			int level = 0;
			for (int i = 0; i < size; i++) {
				li = (LineItem) v.get(i);
				String line  = li.line;
				lineNumber = li.number;
				//System.out.println(line);
				
				//parse data elements definition at the zero level
				if (level == 0 && !line.startsWith("{")) {
					//start of struct
					if (line.startsWith("struct")&& (line.charAt(6) == ' ' ||  line.charAt(6) == '\t')) {
						if (struct == null) {
							int idx = line.indexOf("{");
							if (idx < 0) {
								throw new RuntimeException("syntax error: struct not opened with '<'");
							}
							String name = line.substring(7, idx).trim();
							if (name == null || name.length() < 1) {
								throw new RuntimeException("syntax error: struc name missing");
							}
							struct = new DataStruct(name, li);
						} else {
							throw new RuntimeException("struct within struct not supported");
						}
					} else
					//end of struct
					if (line.equals("}")) {
						if (struct == null) {
							throw new RuntimeException("end block marker '>' within data definition");
						}
						if (struct.getSize() < 1) {
							throw new RuntimeException("empy struct definition");
						}
						structs.put(struct.name, struct);
						struct = null;
					} else 
					//data element 
					{
						DataElement de = new DataElement(li, constants);
						//add data element into the last defined (current) struct
						if (struct != null) {
							//check the struct name exists
							if (de.type == DataElement.TYPE_STRUCT) {
								DataStruct ds = (DataStruct) structs.get(de.value);
								if (ds == null) {
									throw new RuntimeException ("unknown struct name=" + de.value);
								}
							}
							DataElement old = struct.addElement(de);
							checkElements(de, old);
						} 
						//add data element into global variables
						else {
							//variable cannot be a structure in global definition
							if (de.type == DataElement.TYPE_STRUCT) {
								throw new RuntimeException("variable cannot be a structure");
							}
							if (de.isConstant()) {
								DataElement old = (DataElement) constants.put(de.name, de);
								checkElements(de, old);
							} else {
								DataElement old = (DataElement) elements.put(de.name, de);
								checkElements(de, old);
							}
						}
					}
				}
				//binarize data
				if (level > 0 || line.startsWith("{")) {
					if (line.startsWith("{#}")) {
						int count = getItemCount(i+1, v);
						if (useIntegerArraySize) {
							DataElement.writeInt(dos, count);
						} else {
							DataElement.writeShort(dos, (short) count);
						}
					} else
					if (line.startsWith("{")) {
						DataStruct parentStruct = struct;
						int parentStructureElementIndex = structElementIndex;
						structElementIndices[level] = structElementIndex + 1; //include this struct 
						level++;
						struct = null;
						//check struct
						if (line.length() > 1) {
							String structName = line.substring(1).trim();
							//search in anonymous structures
							if (parentStruct == null) {
								struct = (DataStruct) structs.get(structName);
								if (struct == null) {
									throw new RuntimeException("struct " + structName + " doesn't exist.");
								}
							} else
							//search in named data element in the parent structure
							{
								DataElement de = parentStruct.getElement(structName);
								if (de == null) {
									throw new RuntimeException("struct name=" + structName + " not found ");
								}
								//search in anonymous structures
								struct = (DataStruct) structs.get(de.value);
								if (struct == null) {
									throw new RuntimeException("struct " + structName + " doesn't exist.");
								}
								
								//if not all data were saved from the parent structure
								DataElement structElement = parentStruct.getElement(parentStructureElementIndex);
								if (structElement == null) {
									throw new RuntimeException("data in struct " + struct.name + " is undefined on element index " + structElementIndex);
								}
								if (!structElement.name.equals(structName)) {
									int max = parentStruct.getSize();
									while (!structElement.name.equals(structName)) {
										//element doesn't have a default value
										if (!structElement.isConstant()) {
											throw new RuntimeException("data element mismatch in struct '" + struct.name + "', element index " + structElementIndex + " name=" + structName + " expected=" + structElement.name);
										} 
										//save default value
										structElement.store(dos, structElement.value, constants, path, li);
										parentStructureElementIndex++;
										if (parentStructureElementIndex < max) {
											structElement = parentStruct.getElement(parentStructureElementIndex);
										} else {
											throw new RuntimeException("unknown element in struct " + struct.name + ", element name=" + structName);
										}
									}
									structElementIndices[level - 1] = parentStructureElementIndex + 1;
								} 
							}
						} 
						structStack[level] = struct;
						structElementIndices[level] = 0;
						structElementIndex = 0;
					} else
					if (line.startsWith("}")) {
						//struct exists and not all element were saved yet -> save them
						if (struct != null && structElementIndex < struct.getSize()) {
							int max = struct.getSize();
							while (structElementIndex < max) {
								DataElement structElement = struct.getElement(structElementIndex);
								//element doesn't have a default value
								if (!structElement.isConstant()) {
									throw new RuntimeException("data element doesn't have default value, struct=" + struct.name +  "  element name=" + structElement.name);
								} 
								//save default value
								structElement.store(dos, structElement.value, constants, path, li);
								structElementIndex++;								
							}
						}
						
						level--;
						struct = structStack[level]; 
						structElementIndex = structElementIndices[level]; 
					} else {
						boolean isConstant = false;
						StringTokenizer st = new StringTokenizer(line, "=:");
						int tokenSize = st.countTokens();
						if (tokenSize < 1) {
							throw new IllegalArgumentException("can't convert: " + line );
						}
						String name = st.nextToken().trim();
						String value;
						if (tokenSize < 2) {
							// check whether we try to write a constant
							DataElement de = (DataElement) constants.get(name);
							if (de == null || de.value == null) {
								throw new IllegalArgumentException("can't convert: " + line );
							}
							isConstant = true;
							de.store(dos, de.value, constants, path, li);
							value = de.value;
						} else {
							value = st.nextToken();
						}
						//append rest of the tokens
						if (tokenSize > 2) {
							int idx = line.indexOf(value);
							value = line.substring(idx);
						}
						//no structure defined in this data block -> save as it is
						if (struct == null) {
							if  (!isConstant) {
								DataElement de = (DataElement) elements.get(name);
								if (de == null) {
									throw new IllegalArgumentException("unknown data element: " + name );
								}
								de.store(dos, value, constants, path, li);
							}
						} else {
							//System.out.println("struct element index=" + structElementIndex);
							DataElement structElement = struct.getElement(structElementIndex);
							if (structElement == null) {
								throw new RuntimeException("data in struct " + struct.name + " is undefined on element index " + structElementIndex);
							}
							//System.out.println("sename=" + structElement.name + " name=" + name);
							int max = struct.getSize();
							if (!structElement.name.equals(name)) {
								while (!structElement.name.equals(name)) {
									//element doesn't have a default value
									if (!structElement.isConstant()) {
										throw new RuntimeException("data element mismatch in struct '" + struct.name + "', element index " + structElementIndex + " name=" + name + " expected=" + structElement.name);
									} 
									//save default value
									structElement.store(dos, structElement.value, constants, path, li);
									structElementIndex++;
									if (structElementIndex < max) {
										structElement = struct.getElement(structElementIndex);
									} else {
										throw new RuntimeException("unknown element in struct " + struct.name + ", element name=" + name);
									}
								}
								//we have found the matching name -> save the value
								structElement.store(dos, value, constants, path, li);
								structElementIndex ++;
							} else {
								structElement.store(dos, value, constants, path, li);
								structElementIndex ++;
							}
						}
					}
				}
			}
			dos.close();		
		} catch (Exception e) {
			System.out.println(e + " in source " + li.getSource());
			if (verbose) {
				e.printStackTrace();
			}
			throw new IllegalArgumentException();
		}
	}
	
	public static int getItemCount(int lineIndex, Vector v) {
		int size = v.size();
		int result = 0;
		int level = 0;
		for (int i = lineIndex; i < size; i++) {
			LineItem li = (LineItem) v.get(i);
			String line  = li.line;
			if (line.startsWith("{#}")) {
				if (level == 0) {
					return result;
				}
			} else 
			if (line.startsWith("{")) {
				if (level == 0) {
					result ++;
				}
				level++;
			} else
			if (line.startsWith("}")) {
				level--;
				if (level < 0) {
					return result;
				}
			}
		}
		return result;
	}

	private static String resolveHomePath(String fileName) {
		if (fileName == null) {
			return null;
		}
		
		if (fileName.startsWith("~/") || fileName.startsWith("~\\") ) {
			String homePath = System.getProperty("user.home", "~");
			return homePath + fileName.substring(1);
		}
	
		return fileName;
	}
}
