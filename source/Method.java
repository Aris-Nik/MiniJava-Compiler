package source;

import java.util.*;

public class Method{

	public List<Variable> variables;
	public List<Variable> arguments;
	public String name;
	public String type;
  public String overriden;
	public int offset;

	public Method(String name, String type){
		this.variables = new ArrayList<>();
		this.arguments = new ArrayList<>();
		this.name = name;
		this.type = type;
    this.overriden = "";
		this.offset = 0;
		
	}

	public boolean correctTypes(String s1,String s2,SymbolTable table){
      if (s1 == s2)
        return true;
      if (s1 == "int" || s1 == "int[]" || s1 =="boolean" || s1 == "boolean[]"){
      	return false;												//because s1 is int or int[] or boolean or boolean[] and s2 is (probably a class type) but if it is not a class type then we already checked for the first case that tehy dont match
      }
      if  (s2 == "int" || s2 == "int[]" || s2 =="boolean" || s2 == "boolean[]"){						//because s2 is int or int[] or boolean or boolean[] and s1 is (probably a class type) but if it is not a class type then we already checked for the first case that tehy dont match
      	return false;																		
      }
      Class c1 = table.getclass(s1);
      Class c2 = table.getclass(s2);
      while (c1 != null){																		//find all extends till the first parent 
      	if (c1.parent == c2.name){
        	//System.out.println("to " + c1.name + " exei parent to ")
        	return true;
     	 }
     	 c1 = table.getclass(c1.parent);
  		}
      return false;
    }

	public boolean putvar(Variable var,SymbolTable table){
		for(Variable v : this.variables){
            if(v.name == var.name){
                return false;
            }
        }
        for(Variable a : this.arguments){
            if(a.name == var.name){
               	//System.out.println("EDW???" + a.type + " " + var.type);
               if (!correctTypes(a.type,var.type,table)){
               	return false;
               }
            }
        }
        this.variables.add(var);
        return true;
	}

	public Variable getvar(String name	){
		for(Variable v : this.variables){
            if(v.name == name){
                return v;
            }
        }
        for(Variable v : this.arguments){
            if(v.name == name){
                return v;
            }
        }

        return null;
	}

	public boolean putarg(Variable var){
		for(Variable v : this.arguments){
            if(v.name == var.name){
                return false;
            }
        }
        this.arguments.add(var);
        return true;
	}

}