package source;

import java.util.*;

public class SymbolTable{

	public Map<String,Class> classes;
	public String filename;

	public SymbolTable(String name){
		this.classes = new LinkedHashMap<>();
		this.filename = name;
	}

	public void print(){
		for (Map.Entry entry : classes.entrySet()) {
		    //System.out.println(entry.getKey() + ":" + entry.getValue().toString());
		    Class cl = (Class)entry.getValue();
		    cl.print();
		}
	}

	public void printoffsets(){
		for (Map.Entry entry : classes.entrySet()) {
		    //System.out.println(entry.getKey() + ":" + entry.getValue().toString());
		    Class cl = (Class)entry.getValue();
		    for (int i = 0; i < cl.methods.size(); i++){
		    	System.out.println(cl.name + "." + cl.methods.get(i).name + " : " + cl.methods.get(i).offset);
		    }
		}
	}

	public Class getclass(String className){
		return this.classes.get(className);
	}

}