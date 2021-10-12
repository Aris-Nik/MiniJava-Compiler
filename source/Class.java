package source;

import java.util.*;

public class Class{

	public List<Variable> variables;
	public List<Method> methods;
	public String name;
	public String parent;
	public int varoffset;
	public int methodoffset;

	public Class(String name,String parent){
		this.variables = new ArrayList<>();
		this.methods = new ArrayList<>();
		this.name = name;
		this.parent = parent;
		this.varoffset = 0;
		this.methodoffset = 0;

	}

    public int getVariablesize(){
        int counter = 0;
        for (int i = 0; i < variables.size(); i++){
            Variable var = variables.get(i);
            if (var.type == "int")
                counter = counter + 4;
            else if (var.type == "boolean")
                counter = counter + 1;
            else
                counter = counter + 8;
        }
        return counter;
    }

	public void print(){
		System.out.print("\n" + this.name + "  variables are : ");
		for(int i = 0; i < variables.size(); i++) {
			Variable var = variables.get(i);   
    		System.out.print(var.type + " " + var.name + ", ");
		}
		System.out.print("\n"); 
		System.out.print("\n" + this.name + " methods are : ");
		for(int i = 0; i < methods.size(); i++) {
			Method meth = methods.get(i);   
    		System.out.print(meth.type + " " + meth.name + " with arguments : ");
    		for (int j = 0; j < meth.arguments.size(); j++){
    			Variable var1 = meth.arguments.get(j);
    			System.out.print(var1.type + " " + var1.name + ", ");
    		}
    		System.out.print(" and variables : ");
    		for (int j = 0; j < meth.variables.size(); j++){
    			Variable var1 = meth.variables.get(j);
    			System.out.print(var1.type + " " + var1.name + ", ");
    		}
		}
		System.out.print("\n"); 
	}

	public boolean putvar(Variable var){
		for(Variable v : this.variables){
            if(v.name == var.name){
                //System.out.println(v.type + v.name + " already exists in class " + this.name);
                return false;
            }
        }
        this.variables.add(var);
        return true;
	}

	public Variable getvar(String name,SymbolTable table){
        String temp;
		for(Variable v : this.variables){
            temp = v.name;
            //System.out.println("an to " + v.name + " einai idio me to " + name );
            if((temp.toString()).equals(name.toString())){
                return v;
            }
        }
        Class par = table.getclass(this.parent);
        if (par == null)
        	return null;
        for(Variable v : par.variables){
            if(v.name == name){
                return v;
            }
        }
        return null;
	}

	public boolean putmeth(Method meth){
		for(Method m : this.methods){
            if(m.name == meth.name){
                //System.out.println(v.type + v.name + " already exists in class " + this.name);
                return false;
            }
        }
        
        this.methods.add(meth);
        return true;
	}

	public Method getmeth(String name){
        String temp;
		for(Method m : this.methods){
            temp = m.name;
            //System.out.println("ama to " + temp + " einai idio me to " + name);
            if((temp.toString()).equals(name.toString()) == true ){
                //System.out.println("tote  to " + m.name + " einai idio me to " + name);
                //System.out.println(v.type + v.name + " already exists in class " + this.name);
                return m;
            }
        }
        return null;
	}

    public Method getmethwithparent(String name,SymbolTable table){
        for(Method m : this.methods){
            if(m.name == name){
                //System.out.println(v.type + v.name + " already exists in class " + this.name);
                return m;
            }
        }
        Class par = table.getclass(this.parent);
        if (par == null)
            return null;
        for(Method m : par.methods){
            if(m.name == name){
                //System.out.println(v.type + v.name + " already exists in class " + this.name);
                return m;
            }
        }

        return null;
    }

	public Method ismeth(Method meth){
		for(Method m : this.methods){
            if(m.name == meth.name){
                //System.out.println(v.type + v.name + " already exists in class " + this.name);
                return m;
            }
        }
        return null;
	}

    public void min_sort(){
        int min_j;
        int min_offset;
        Method temp;
        for (int i = 0; i < this.methods.size(); i++){
            temp = this.methods.get(i);
            min_offset = temp.offset;
            min_j = i;
            for (int j = i + 1; j < this.methods.size(); j++){
                Method m = this.methods.get(j);
                if (m.offset < min_offset){
                    min_offset = m.offset;
                    //min_method = m;
                    min_j = j;
                }
            }
            Collections.swap(this.methods, i, min_j); 
        }
    }

}