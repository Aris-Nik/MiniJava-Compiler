package source;

import java.util.*;

public class Variable{

	public String name;
	public int offset;
	public String type;

	public Variable(String name,String type){

		this.name = name;
		this.type = type;
		this.offset = 0;

	}

	public int getoffset(){

		if(this.type == "int")
			return 4;
		else if(this.type == "boolean" )
			return 1;
		else
			return 8;

	}

}