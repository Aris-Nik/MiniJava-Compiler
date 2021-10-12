package source;
import visitor.GJDepthFirst;
import syntaxtree.*;

public class SecondVisitor extends GJDepthFirst<String, String>{


    SymbolTable table;
    boolean inMethod;
    Class globalClass;															//changes if we enter a new class so we know that we are inside that class everywhere we are to help us find vars 
    Method globalMethod;														//changes if we enter a new method so we know that we are inside that method everywhere we are to help us find vars-args

    public SecondVisitor(SymbolTable table){
     this.table = table;
    }

    public boolean correctTypes(String s1,String s2){
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


    public boolean checkOverload(){
    	Class child = this.globalClass;
        Class par = table.getclass(child.parent);
        while (par != null){
        	for(Method m : par.methods){
            	//System.out.println("edw? parent onoma  " + par.name + " child name " + child.name);
            	if(m.name == this.globalMethod.name ){
            		if (this.globalMethod.type != m.type)
            			return false;
            		if (this.globalMethod.arguments.size() != m.arguments.size())
                		return false;
                	else{
                		for (int i = 0; i < this.globalMethod.arguments.size(); i++){
					        if (  !correctTypes(this.globalMethod.arguments.get(i).type,m.arguments.get(i).type)){
					          return false;
					        }
					      }
                	}
            	}
        	}
        	par = table.getclass(par.parent);
        }
        return true;
    }

    	   /**
    * f0 -> Type()
    * f1 -> Identifier()
    * f2 -> ";"
    */
   public String visit(VarDeclaration n, String argu) throws Exception{
      String _ret=null;
      String type, ident;
      type = n.f0.accept(this, argu);
      if (type != "int" && type != "int[]" && type != "boolean" && type != "boolean[]"){
        if (table.getclass(type) == null){
          throw new Exception("ERROR: No such type");
          
        }
      }
      ident = n.f1.accept(this, argu);
      //Class classTemp = table.getclass(argu);
      if (inMethod == false){
        if (globalClass != null){
          Variable var = new Variable(ident,type);
          if (globalClass.putvar(var)){
            var.offset = globalClass.varoffset;
            globalClass.varoffset = globalClass.varoffset + var.getoffset();
            System.out.println(globalClass.name + "." + var.name + " : " + var.offset);
          }
          else{
            throw new Exception("ERROR value name already taken");
            
          }
        }
      }
      else{
        if (globalClass != null){
          Variable var = new Variable(ident,type);
          if (globalMethod.putvar(var,table))
                ;
          else{

            throw new Exception("ERROR value name already taken or type is different on overload " + var.name + "  " + globalClass.name);
            
          }
        }
      }
      n.f2.accept(this, argu);
      return _ret;
   }


      public String visit(NodeToken n, String argu) throws Exception { return n.toString(); }

      /**
    * f0 -> "boolean"
    * f1 -> "["
    * f2 -> "]"
    */
   public String visit(BooleanArrayType n, String argu) throws Exception {
      return "boolean[]";
   }

     /**
    * f0 -> "int"
    * f1 -> "["
    * f2 -> "]"
    */
   public String visit(IntegerArrayType n, String argu) throws Exception {
      return "int[]";
   }

   /**
    * f0 -> "class"
    * f1 -> Identifier()
    * f2 -> "{"
    * f3 -> ( VarDeclaration() )*
    * f4 -> ( MethodDeclaration() )*
    * f5 -> "}"
    */
   public String visit(ClassDeclaration n, String argu) throws Exception{
      String _ret=null;
      n.f0.accept(this, argu);
      String name = n.f1.accept(this, argu);
      globalClass = table.getclass(name);
      n.f2.accept(this, argu);
      inMethod = false;
      n.f3.accept(this, name);
      inMethod = true;
      n.f4.accept(this, argu);
      n.f5.accept(this, argu);
      return _ret;
   }



   /**
    * f0 -> "public"
    * f1 -> Type()
    * f2 -> Identifier()
    * f3 -> "("
    * f4 -> ( FormalParameterList() )?
    * f5 -> ")"
    * f6 -> "{"
    * f7 -> ( VarDeclaration() )*
    * f8 -> ( Statement() )*
    * f9 -> "return"
    * f10 -> Expression()
    * f11 -> ";"
    * f12 -> "}"
    */
   public String visit(MethodDeclaration n, String argu) throws Exception {
      inMethod = true;
      String _ret=null;
      String name,type;
      n.f0.accept(this, argu);
      n.f1.accept(this, argu);
      type = n.f1.accept(this, argu);

      if (type != "int" && type != "int[]" && type != "boolean" && type != "boolean[]"){
        if (table.getclass(type) == null){
          throw new Exception("No such type");
          
        }
      }
      name = n.f2.accept(this, argu);
      if (globalClass != null){
        Method meth = new Method(name,type);

        globalMethod = meth;
        if (globalClass.putmeth(meth)){
          Class par = table.getclass(globalClass.parent);
          if (par != null){
            Method m = par.ismeth(meth);
            if (m == null){
              globalMethod.offset = globalClass.methodoffset;
              globalClass.methodoffset = globalClass.methodoffset + 8 ;
              System.out.println(globalClass.name + "." + meth.name + " : " + meth.offset);
            }
            else
              globalMethod.offset = m.offset;
          }else{
            globalMethod.offset = globalClass.methodoffset;
            globalClass.methodoffset = globalClass.methodoffset + 8 ;
            System.out.println(globalClass.name + "." + meth.name + " : " + meth.offset);
          }
        }
        else{
          throw new Exception("ERROR: method name already taken");
          
        }
      }


      //System.out.println(argu + "")
      n.f3.accept(this, argu);
      n.f4.accept(this, argu);
      n.f5.accept(this, argu);
      n.f6.accept(this, argu);
      n.f7.accept(this, name);
      if (checkOverload() == false){
      	throw new Exception("ERROR: wrong overload function");
      }
      n.f8.accept(this, argu);
      n.f9.accept(this, argu);
      n.f10.accept(this, argu);
      n.f11.accept(this, argu);
      n.f12.accept(this, argu);
      return _ret;
   }

    /**
    * f0 -> Type()
    * f1 -> Identifier()
    */
   public String visit(FormalParameter n, String argu) throws Exception {
      String _ret=null;
      String type = n.f0.accept(this, argu);
      if (type != "int" && type != "int[]" && type != "boolean" && type != "boolean[]"){
        if (table.getclass(type) == null){
          throw new Exception("ERROR: No such type");
          
        }
      }
      String name = n.f1.accept(this, argu);
      Variable var = new Variable(name,type);
      boolean flag = globalMethod.putarg(var);
      if (flag == false){
        throw new Exception("ERROR: argument name already taken");
        
      }

      return _ret;
   }

   /**
    * f0 -> "class"
    * f1 -> Identifier()
    * f2 -> "extends"
    * f3 -> Identifier()
    * f4 -> "{"
    * f5 -> ( VarDeclaration() )*
    * f6 -> ( MethodDeclaration() )*
    * f7 -> "}"
    */
   public String visit(ClassExtendsDeclaration n, String argu) throws Exception {
      String _ret=null;
      String name;
      n.f0.accept(this, argu);
      name = n.f1.accept(this, argu);
      globalClass = table.getclass(name);
      Class par = table.getclass(globalClass.parent);
      globalClass.varoffset = par.varoffset;
      globalClass.methodoffset = par.methodoffset;
      
      inMethod = false;
      n.f5.accept(this, name);
      n.f6.accept(this, name);
      return _ret;
   }


     /**
    * f0 -> "class"
    * f1 -> Identifier()
    * f2 -> "{"
    * f3 -> "public"
    * f4 -> "static"
    * f5 -> "void"
    * f6 -> "main"
    * f7 -> "("
    * f8 -> "String"
    * f9 -> "["
    * f10 -> "]"
    * f11 -> Identifier()
    * f12 -> ")"
    * f13 -> "{"
    * f14 -> ( VarDeclaration() )*
    * f15 -> ( Statement() )*
    * f16 -> "}"
    * f17 -> "}"
    */
   public String visit(MainClass n, String argu) throws Exception {
      String _ret=null;

      n.f0.accept(this, argu);
      String name = n.f1.accept(this, argu);
      globalClass=table.getclass(name);
      globalMethod = globalClass.getmeth("main");
      n.f2.accept(this, argu);
      n.f3.accept(this, argu);
      n.f4.accept(this, argu);
      n.f5.accept(this, argu);
      n.f6.accept(this, argu);
      n.f7.accept(this, argu);
      n.f8.accept(this, argu);
      n.f9.accept(this, argu);
      n.f10.accept(this, argu);
      n.f11.accept(this, argu);
      n.f12.accept(this, argu);
      n.f13.accept(this, argu);
      inMethod = true;
      n.f14.accept(this, argu);
      n.f15.accept(this, argu);
      n.f16.accept(this, argu);
      n.f17.accept(this, argu);
      inMethod = false;
      return _ret;
   }

}