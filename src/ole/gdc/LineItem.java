/* 
LineItem.java 
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

public class LineItem {
	String line;
	int number;   //row number
	String source; //TODO - optimise: store index to a Vector of sources instead of the string itself
	
	protected LineItem(String l, int n, String s) {
		line = l;
		number = n;
		source = s;
	}
	
	public String getSource() {
		return source + ":" + number;
	}
}
