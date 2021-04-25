/* 
DataStruct.java 
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

import java.util.HashMap;
import java.util.Vector;

public class DataStruct {
	
	//global pool of anonymous struct types
	static HashMap structs;
	
	String name;
	LineItem sourceLine;
	Vector elements;
	
	
	public DataStruct (String name, LineItem sourceLine){
		this.name = name;
		this.sourceLine = sourceLine;
		elements = new Vector();
	}
	
	public DataElement addElement(DataElement e) {
		DataElement old = getElement(e.name);
		if (old != null) {
			return old;
		}
		elements.addElement(e);
		return null;
	}
	
	public DataElement getElement(String name) {
		final int max = elements.size();
		for (int i = 0; i < max; i++) {
			DataElement e = (DataElement) elements.elementAt(i);
			if (e.name.equals(name)) {
				return e;
			}
		}
		return null;
	}
	
	public DataElement getElement(int index) {
		if (index < 0 || index > elements.size()) {
			return null;
		}
		return (DataElement) elements.elementAt(index);
	}
	public int getSize() {
		return elements.size();
	}
	
	public boolean isConstant() {
		final int max = elements.size();
		for (int i = 0; i < max; i++) {
			//one of the data elements is not constant ! return false
			if ( ! (((DataElement)elements.elementAt(i)).isConstant())) {
				return false;
			}
		}
		return true;
	}
}
