/* 
Utils.java 
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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.Vector;


public class Utils {

	public static String[] getArguments(String[] args) {
		if (args.length != 1) {
			return args;
		}
		String line = args[0];
		StringTokenizer tokenizer = new StringTokenizer(line, " ");
		int size = tokenizer.countTokens();
		String[] result = new String[size];

		for (int i = 0; i < size; i++) {
			result[i] = tokenizer.nextToken();
		}
		return result;
	}
	
	public static Vector getLineList(String inFileName) {
		return getLineList(inFileName, null);
	}
	public static Vector getLineList(String inFileName, String encoding) {
		return getLineList(inFileName, null, new HashMap(), new Vector());
	}	
	public static Vector getLineList(String inFileName, String encoding, HashMap constants, Vector includeDirs) {
		final String path = inFileName.startsWith("/") ? "" : ".";
		return getLineList(path, inFileName, null, null, constants, includeDirs, null, null);			
	}	
	public static Vector getLineList(
			String path, String inFileName, String encoding, 
			Vector result, HashMap constants, Vector includeDirs, LineItem lineItem, String commentDef
	) {
		try {
			if (result == null) {
				result = new Vector();
			}
			File f = findFile(path, inFileName, includeDirs);
			String basePath = f.getParentFile().getCanonicalPath();
			
			if (encoding == null) {
				//use system default encoding
				FileReader fr = new FileReader(f);
				return getLineList(fr, result, basePath, null, f.getCanonicalPath(), constants, commentDef, includeDirs);
			} else {
				InputStreamReader isr = new InputStreamReader(new FileInputStream(f), encoding);
				return getLineList(isr, result, basePath, encoding , f.getCanonicalPath(), constants, commentDef, includeDirs);
			}
		} catch (Exception e) {
			if (lineItem == null) {
				System.out.println(e);
			} else {
				System.out.println(e +  " in source: " + lineItem.source + " (" + lineItem.number + ")" );
			}
		}
		return null;
	}
	
	private static File findFile(String path, String fileName, Vector includeDirs) {
		File f = new File (path + "/" + fileName);
		if (f.exists() && !f.isDirectory()) {
			return f;
		}
		
		//try to find the file in the include dirs
		if (includeDirs != null) {
			int max = includeDirs.size();
			for (int i = 0; i < max; i++) {
				String incPath = (String) includeDirs.elementAt(i);
				if (incPath == null) {
					incPath = ".";
				}
				f = new File (incPath + "/" + fileName);
				if (f.exists() && !f.isDirectory()) {
					return f;
				}
			}
		}
		throw new RuntimeException("file not found ! path=" + path + " name=" + fileName);
	}
	
	public static Vector getLineList(byte[] data) {
		try {
			Vector result = new Vector();
			InputStreamReader isr = new InputStreamReader(new ByteArrayInputStream(data));
			return getLineList(isr, result, null, null, "byte[] data", new HashMap(), null, new Vector());
		} catch (Exception e) {
			System.out.println(e);
		}
		return null;
	}
	
	private static boolean isConstantDefined(String name, HashMap constants) {
		DataElement de = (DataElement) constants.get(name);
		return (de != null);
	}
	
	public static Vector getLineList(
			Reader rd, Vector result, String basePath, String encoding, String source, 
			HashMap constants, String commentDef, Vector includeDirs
	) 
	{
		final int MAX_STACK = 64;
		int lineNumber = 1; //lines are numbered from 1
		DataCompiler.setConstants(constants);
		try {
			LineNumberReader reader = new LineNumberReader(rd);
			String line = ".";
			if (commentDef == null) {
				commentDef = DataCompiler.commentDef;
			}
			boolean lineEnabled = true; 
			final boolean[] lineEnabledStack = new boolean[MAX_STACK];
			int lineEnabledStackIndex = -1;
			
			while (line != null) {
				line = reader.readLine();
				if (line != null && line.length() > 0) {
					line = removeComment(line, commentDef);
					if (line.startsWith("#ifdef") && (line.charAt(6) == ' ' || line.charAt(6) == '\t')) {
						String exp = line.substring(7).trim();
						lineEnabled = isConstantDefined(exp, constants);
						lineEnabledStack[++lineEnabledStackIndex] = lineEnabled;
					} else
					if (line.startsWith("#ifndef")&& (line.charAt(7) == ' ' || line.charAt(7) == '\t')) {
						String exp = line.substring(8).trim();
						lineEnabled = !isConstantDefined(exp, constants);
						lineEnabledStack[++lineEnabledStackIndex] = lineEnabled;
					} else
					if (line.equals("#else")) {
						if (lineEnabledStackIndex < 0) {
							throw new RuntimeException("unexpected #else definition");
						}
						lineEnabled = !lineEnabledStack[lineEnabledStackIndex];
						lineEnabledStack[lineEnabledStackIndex] = lineEnabled;
					} else
					if (line.equals("#endif")) {
						if (lineEnabledStackIndex < 0) {
							throw new RuntimeException("unexpected #endif definition");
						}
						lineEnabledStackIndex--;
						if (lineEnabledStackIndex < 0) {
							lineEnabled = true;
						} else {
							lineEnabled = lineEnabledStack[lineEnabledStackIndex];
						}
					}
							
					if (lineEnabled) {
						if (!line.startsWith("#")) {
							result.add(new LineItem(line, lineNumber, source));
						} else 
						if (line.startsWith("#define") && (line.charAt(7) == ' ' || line.charAt(7) == '\t')) {
							LineItem item = new LineItem(line, lineNumber, source);
							DataCompiler.addConstant(line.substring(7).trim(), item);
						} else
						if (line.startsWith("#error") && (line.charAt(6) == ' ' || line.charAt(6) == '\t')) {
							throw new RuntimeException("Error: " + line.substring(6).trim());
						} else
						if (line.startsWith("#include") && (line.charAt(8) == ' ' || line.charAt(8) == '\t')) {
							LineItem item = new LineItem(line, lineNumber, source);
							String includeName = getIncludeName(line.substring(8));

							//include is valid
							if (includeName != null) {
								if (basePath == null) {
									getLineList("", includeName, encoding, result, constants, includeDirs, item, commentDef);
								} else {
									getLineList(basePath, includeName, encoding, result, constants, includeDirs, item, commentDef);
								}
							}
						} else
						if (line.startsWith("#undef") && (line.charAt(6) == ' ' || line.charAt(6) == '\t')) {
							String exp = line.substring(7).trim();
							constants.remove(exp);
						} 
					}
				}
				lineNumber++;
			}
			reader.close();
			if (lineEnabledStackIndex >= 0) {
				throw new RuntimeException("missing #endif " + (lineEnabledStackIndex + 1) + "x");
			}

		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(e +  " in source: " + source + " (" + lineNumber + ")" );
			result = null;
		}
		
		return result;
	}
	
	private static String getIncludeName(String line) {
		line = line.trim();
		char c = line.charAt(0);
		if (c == '<' || c == '"') {
			return line.substring(1, line.length() - 1);
		}
		return null;
	}
	

	private static final String removeComment(String line, String commentDef) {
		if (line == null) {
			return null;
		}
		int index = line.indexOf(commentDef);
		if (index >= 0) {
			line = line.substring(0, index);
		} 
		return line.trim();
	}
	
	public static Vector removeComments(Vector v, String commentDef) {
		if (v == null) {
			return null;
		}
		Vector result = new Vector();
		int size = v.size();
		for (int i = 0; i < size; i++) {
			LineItem li = (LineItem)v.get(i);
			String line  = li.line;
			if (isWhiteSpace(line) || line.startsWith(commentDef)) {
				continue;
			}
			line = removeComment(line, commentDef);
			boolean multilineString = false;
			if (line.endsWith("\\")) {
				//check whether the '=' is present, and if so then handle string quotes
				int indexEqualSign = line.indexOf('=');
				int indexQuote = line.indexOf('"');
				if (indexEqualSign > 0 && indexQuote > 0 && indexEqualSign < indexQuote) {
					String line2 = line.substring(indexEqualSign + 1, line.length() - 1).trim();
					// the string has both front and back quotes
					if (hasQuotes(line2)) {
						line2 = removeQuotes(line2);
						//reassemble the original line without quotes but keep the backslash
						String line3 = line.substring(0, indexEqualSign + 1);
						line = line3.concat(line2).concat("\\");
						multilineString = true;
					}
				}
			}
			while (line.endsWith("\\")) {
				line = line.substring(0, line.length() - 1);
				line = line.trim();
				i++;
				LineItem li2 = (LineItem)v.get(i);
				String line2 =  li2.line;
				if (isWhiteSpace(line2) || line.startsWith(commentDef)) {
					throw new IllegalArgumentException(" syntax error at line:" + li2.number);
				}
				int index2 = line2.indexOf(commentDef);
				if (index2 > 0) {
					line2 = line2.substring(0, index2);
				} 
				line2 = line2.trim();
				boolean hasBackslah = line.endsWith("\\");
				if (hasBackslah) {
					line2 = line2.substring(0, line.length() - 1);
					line2 = line2.trim();
					boolean quotesExist = hasQuotes(line2);
					if (quotesExist) {
						line2 = removeQuotes(line2);
						line = line.concat(line2).concat("\\");
					} else {
						line = line.concat(" ").concat(line2).concat("\\");
					}
				} else {
					boolean quotesExist = hasQuotes(line2);
					if (quotesExist) {
						line2 = removeQuotes(line2);
						line = line.concat(line2);
					} else  {
						line = line.concat(" ").concat(line2);
					}
				}
			}
			
			if (multilineString) {
				// wrap the text string in quotes
				int indexEqualSign = line.indexOf('=');
				String line2 = line.substring(0, indexEqualSign + 1).concat("\"");
				String line3 = line.substring(indexEqualSign + 1).concat("\"");

				line = line2.concat(line3);
			}
			
			result.add(new LineItem(line, li.number, li.source));
		}
		return result;
	}
	
	private static boolean hasQuotes(String s) {
		return s.startsWith("\"") && s.endsWith("\"");
	}
	private static String removeQuotes(String s) {
		if (s.startsWith("\"") && s.endsWith("\"")) {
			return s.substring(1, s.length() - 1 );
		}
		return s;
	}
	
	public static boolean isWhiteSpace(String line) {
		if (line == null || line.length() < 1) {
			return true;
		}
		StringTokenizer st = new StringTokenizer(line, " \t");
		return st.countTokens() < 1;
	}
		
	
	public static void printInfo(String info, String[] args) {
		System.out.print(info.concat(" ") );
		for (int i = 0; i < args.length; i++) {
			System.out.print(args[i].concat(" "));
		}
		System.out.println();
	}
	
	public static String[] split(String line, String delimiters) {
		StringTokenizer tok = new StringTokenizer(line, delimiters);
		int size = tok.countTokens();
		if (size == 1) {
			return new String[] {line};
		}
		String[] result = new String[size];
		for (int i = 0; i < size; i++) {
			result[i] = tok.nextToken();
		}
		return result;
	}
}
