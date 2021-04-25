/*
UnicodeEscapes.java
Copyright (C) 2014 Inview Technology

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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UnicodeEscapes {

	private static final Pattern unicodePattern = Pattern.compile("\\\\u+\\p{XDigit}{4}");

	/**
	 * Converts Unicode escapes to the corresponding single Unicode characters.
	 * @param stringWithEscapes
	 * @return Converted string
	 */
	public static String unescape(String stringWithEscapes) {
		StringBuilder result = new StringBuilder(stringWithEscapes);
		while( true ) {
			Matcher matcher = unicodePattern.matcher(result);
			if( !matcher.find() ) {
				break;
			}
			int start = matcher.start();
			int end = matcher.end();
			String hex = result.substring(end-4, end);
			char c = (char) Integer.parseInt(hex, 16);
			result.replace(start, end, Character.toString(c));
		}
		return result.toString();
	}

	public static void testMe() {
		String test = "as\u0E15\u0e32gh\u0e21\uu0e17\\ut0e35fg\u0e48rt";
		String data = "as\\u0E15\\u0e32gh\\u0e21\\uu0e17\\ut0e35fg\\u0e48rt";
		String result = unescape(data);
		System.out.println(test);
		System.out.println(result);
		System.out.println(test.equals(result));
	}
}
