package source;
import visitor.GJDepthFirst;
import syntaxtree.*;
import java.util.*;
import java.io.*;


public class LLVMvisitor extends GJDepthFirst<String, String>{


    SymbolTable table;
    FileWriter file;
    boolean inMethod;
    Class globalClass;                                                          //changes if we enter a new class so we know that we are inside that class everywhere we are to help us find vars 
    Method globalMethod;                                                        //changes if we enter a new method so we know that we are inside that method everywhere we are to help us find vars-args
    int registerNumber = 0;
    String address = "";
    public List<String> args;
    String globalType ="";
    int ifNumber = 0;
    int whileNumber = 0;
    String idName;
    int arraySize = 0;


    public void emit(String text) throws Exception{
        this.file.write(text);
        this.file.flush();
    }

    public boolean isNumber(String expr) throws Exception{                              //Code from geeks for geeks to check if a strng is a number
        try 
        { 
            
            Integer.parseInt(expr); 
            //System.out.println(expr + " is a valid integer number"); 
            return true;
        }  
        catch (NumberFormatException e) 
        { 
            //System.out.println(expr + " is not a valid integer number"); 
            return false;
        } 
    }

    public String typellvm(String type) throws Exception{
        if (type == "int")
            return "i32";
        if (type == "boolean")
            return "i1";
        if (type == "int[]")
            return "i32*";
        if (type == "boolean[]")
            return "i1*";

        return "i8*";

    }
    public LLVMvisitor(SymbolTable table,FileWriter file) throws Exception{
        this.table = table;
        this.file = file;
        emit("\ndeclare i8* @calloc(i32, i32)\n" +  "declare i32 @printf(i8*, ...)\n" + "declare void @exit(i32)\n\n@_cint = constant [4 x i8] c\"%d\\0a\\00\"\n" +
              "@_cOOB = constant [15 x i8] c\"Out of bounds\\0a\\00\"\n" + "define void @print_int(i32 %i) {\n" + "    %_str = bitcast [4 x i8]* @_cint to i8*\n" +
                  "    call i32 (i8*, ...) @printf(i8* %_str, i32 %i)\n" + "    ret void\n" + "}\n\n" + "define void @throw_oob() {\n" + "    %_str = bitcast [15 x i8]* @_cOOB to i8*\n" +
                  "    call i32 (i8*, ...) @printf(i8* %_str)\n" + "    call void @exit(i32 1)\n" +
                  "    ret void\n}\n");

        for (Map.Entry entry : this.table.classes.entrySet()) {                                                    //First add methods from parents to the symbol table because i did not do it on project 2 and it will be needed for the v-tables
            Class cl = (Class)entry.getValue();
            Class par = this.table.getclass(cl.parent);
                if (par != null){
                    for(int i = 0; i < par.methods.size(); i++) {
                    Method meth = par.methods.get(i);   
                    if (cl.ismeth(meth) == null){
                        Method temp = new Method(meth.name,meth.type);
                        temp = meth;
                        if (meth.overriden == "")
                            temp.overriden = par.name;
                        else{
                            temp.overriden = meth.overriden;
                        }
                        cl.methods.add(temp);
                    }
                }
            }
            cl.min_sort(); 
        }
        String temp1;
        //this.table.print();
        for (Map.Entry entry : this.table.classes.entrySet()){
            Class cl = (Class)entry.getValue();
            int size = cl.methods.size();
            if (cl.methods.size() == 1 && cl.methods.get(0).name == "main"){                                    //Special case for the main method
                emit("\n@." + cl.name + "_vtable = global [0 x i8*] []\n");
            }
            else{
                emit( "\n@." + cl.name + "_vtable = global [" + size + " x i8*] [\n");
                for (int i = 0; i < cl.methods.size(); i++){
                    Method m = cl.methods.get(i);
                    emit("\ti8* bitcast ("+ typellvm(m.type) + " (i8*");
                    for (Variable v : m.arguments){
                        emit(", " + typellvm(v.type));
                    }
                    if (m.overriden =="")
                        temp1 = cl.name;
                    else
                        temp1 = m.overriden;
                    emit(")* @" + temp1 + "." + m.name + " to i8*)");
                    if (i < cl.methods.size() - 1){
                        emit(",");
                    }
                    emit("\n");
                }
                emit("]\n");
            }
        }

        this.table.printoffsets();
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
      String _ret=null;
      n.f0.accept(this, argu);
      String type = n.f1.accept(this, "return_name");
      String name = n.f2.accept(this, "return_name");
      this.globalMethod  = globalClass.getmeth(name);
      emit("\ndefine " + typellvm(type) + " @" + this.globalClass.name + "." + name + "(i8* %this");
      for (Variable v : globalMethod.arguments){
        emit(", " + typellvm(v.type) + " %." + v.name);
      }
      emit(") {\n");
      for (Variable v : globalMethod.variables){                                                //Var declaration 
        emit("\t%" + v.name  + " = alloca " + typellvm(v.type) + "\n");
      }
      for (Variable v : globalMethod.arguments){                                                //Var declaration 
        emit("\t%" + v.name  + " = alloca " + typellvm(v.type) + "\n");
        emit("\tstore " + typellvm(v.type) +" %." + v.name + ", " + typellvm(v.type) + "* %" + v.name + "\n");
      }
      n.f3.accept(this, argu);
      //n.f4.accept(this, argu);
      n.f5.accept(this, argu);
      n.f6.accept(this, argu);
      //n.f7.accept(this, argu);
      n.f8.accept(this, argu);
      n.f9.accept(this, argu);
      //System.out.println("prin to state");
      String exp = n.f10.accept(this, argu);
      //System.out.println("meta to state ");
      emit("\tret " + typellvm(type) + " " + exp +"\n}\n");
      

      n.f11.accept(this, argu);
      n.f12.accept(this, argu);
      return _ret;
   }


    public String visit(IntegerLiteral n, String argu) throws Exception {
      String s =  n.f0.accept(this, argu);
      return s.toString();                                          //return int as a string
    }

    public String visit(TrueLiteral n, String argu) throws Exception {
        return "1";
    }

    public String visit(FalseLiteral n, String argu) throws Exception {
        return "0";
    }

    public String visit(ThisExpression n, String argu) throws Exception {
      this.idName = this.globalClass.name;
     	
      return "%this";                            
    }





   public String visit(Identifier n, String argu)throws Exception {
      //System.out.println("mphka ident");
      String name =  n.f0.accept(this, "return_name");
      
      if (argu != null){
        if (argu.toString().equals("return_name") ){
          
          return name;
        }
      }
     

      Variable var = globalMethod.getvar(name);
      if (var != null){
        this.globalType = var.type;
        this.idName = var.name;
       
        emit("\t%_" + this.registerNumber + " = load " + typellvm(var.type) + ", " + typellvm(var.type) + "* %" + var.name + "\n");
        this.registerNumber = this.registerNumber + 1;
        return "%_" + (this.registerNumber - 1 );
      }
      else{
        
        var = globalClass.getvar(name,table);
        
        this.globalType = var.type;
        this.idName = var.name;
        emit("\t%_" + this.registerNumber + " = getelementptr i8, i8* %this, i32 " + (var.offset + 8 )  + "\n");
        this.registerNumber = this.registerNumber + 1;
        emit("\t%_" + this.registerNumber + " = bitcast i8* %_" + (this.registerNumber - 1) + " to " + typellvm(var.type) + "*\n");
        this.registerNumber = this.registerNumber + 1;
        if (argu != "load"){
          emit("\t%_" + this.registerNumber + " = load " + typellvm(var.type) + ", " + typellvm(var.type) + "* %_" + (this.registerNumber - 1) + "\n");
          this.registerNumber = this.registerNumber + 1;
        }

        return "%_" + (this.registerNumber - 1);
      }
   }

     /**
    * f0 -> IntegerLiteral()
    *       | TrueLiteral()
    *       | FalseLiteral()
    *       | Identifier()
    *       | ThisExpression()
    *       | ArrayAllocationExpression()
    *       | AllocationExpression()
    *       | BracketExpression()
    */
    public String visit(PrimaryExpression n, String argu) throws Exception {
      emit("\n;PrimaryExpression\n");
     
      String name =  n.f0.accept(this, argu);
     
      return name;
   }



     /**
    * f0 -> "new"
    * f1 -> Identifier()
    * f2 -> "("
    * f3 -> ")"
    */
   public String visit(AllocationExpression n,String  argu)throws Exception {
      String _ret=null;
      n.f0.accept(this, argu);
      String id = n.f1.accept(this, "return_name");
      this.idName = id;
      
      Class cl = this.table.getclass(id);
      int size = cl.getVariablesize();
     
      emit("\t%_" + this.registerNumber + " = call i8* @calloc(i32 1, i32 " + (size + 8) + ")\n");
      _ret = "_" + this.registerNumber;
      this.registerNumber = this.registerNumber + 1;

     
      emit("\t%_" + this.registerNumber + " = bitcast i8* %" + _ret + " to i8***\n");
      this.registerNumber = this.registerNumber + 1;
      int size2 = cl.methods.size();
      emit("\t%_" + this.registerNumber + " = getelementptr [" + size2 + " x i8*], [" + size2 + " x i8*]* @." + cl.name + "_vtable, i32 0, i32 0\n");
      this.registerNumber = this.registerNumber + 1;
      emit("\tstore i8** %_" + (this.registerNumber -1 ) + ", i8*** %_" + (this.registerNumber - 2) + "\n");
      n.f2.accept(this, argu);
      n.f3.accept(this, argu);
     
      return "%" + _ret;
   }


    /**
    * f0 -> PrimaryExpression()
    * f1 -> "."
    * f2 -> Identifier()
    * f3 -> "("
    * f4 -> ( ExpressionList() )?
    * f5 -> ")"
    */
   public String visit(MessageSend n, String argu) throws Exception{
      String _ret=null;
      emit("\n;MessageSend\n");
     
      this.args = new ArrayList<>();
      String name = n.f0.accept(this, argu);
      
      Class cl;
      String temp;
      Variable v;
      if (!name.equals("%this")){
       
        
        if (this.table.getclass(this.idName) != null){
          cl = this.table.getclass(this.idName);
        }
        else{
          temp = this.idName;
          v = this.globalMethod.getvar(temp);
          if (v == null)
            v = this.globalClass.getvar(temp,this.table);

          cl = this.table.getclass(v.type);
        }

       
      }
      else{
        cl = this.table.getclass(this.idName);
      }

      

      String id = n.f2.accept(this, "return_name");
     
      
      Method meth = cl.getmeth(id);
      
    

      emit("\t%_" + this.registerNumber + " = bitcast i8* " + name + " to i8***\n");
      this.registerNumber = this.registerNumber + 1;
      emit("\t%_" + this.registerNumber + " = load i8**, i8*** %_" + (this.registerNumber - 1) + "\n" );
      this.registerNumber = this.registerNumber + 1;
      emit("\t%_" + this.registerNumber + " = getelementptr i8*,i8** %_" + (this.registerNumber - 1) + ",i32 " + (meth.offset/8) + "\n");
      this.registerNumber = this.registerNumber + 1;
      emit("\t%_" + this.registerNumber + " = load i8*,i8** %_" + (this.registerNumber - 1) + "\n");
      this.registerNumber = this.registerNumber + 1;
      emit("\t%_" + this.registerNumber + " = bitcast i8* %_" + (this.registerNumber - 1) + " to " + typellvm(meth.type) + " (i8*");
      int bit_reg = this.registerNumber;
      this.registerNumber = this.registerNumber + 1;
     
      
      //Give arguments
      if (meth.arguments.size() == 0)
        emit(")*\n");      //zero arguments 
      else{
        for (Variable v_temp : meth.arguments){
            emit("," + typellvm(v_temp.type));
        }
        emit(")*\n");
      }
      String type;
      n.f3.accept(this, argu);
      n.f4.accept(this, argu);
     
      
      
      emit("\t%_" + this.registerNumber + " = call " + typellvm(meth.type) + " %_" + bit_reg + " (i8* " + name + "\n"); // 
      this.registerNumber = this.registerNumber + 1;

      for (int x = 0; x < this.args.size(); x ++){
        type = typellvm(meth.arguments.get(x).type);
        emit("," + type + " " + this.args.get(x) );
      }
     
      emit(")\n");
      n.f5.accept(this, argu);
      _ret = "%_" + (this.registerNumber - 1);
      return _ret;
   }

   /**
    * f0 -> Expression()
    * f1 -> ExpressionTail()
    */
   public String visit(ExpressionList n, String argu) throws Exception {
      String _ret=null;
      String n1 = n.f0.accept(this, argu);                     //SOS SOS SOS SOS
      this.args.add(n1);
      
      String n2 = n.f1.accept(this, argu);
      
      return null;
   }

      /**
    * f0 -> ( ExpressionTerm() )*
    */
   public String visit(ExpressionTail n, String argu) throws Exception {
      return n.f0.accept(this, argu);
   }

     public String visit(Expression n, String argu)throws Exception {
      return n.f0.accept(this, argu);
   }

     /**
    * f0 -> ","
    * f1 -> Expression()
    */
   public String visit(ExpressionTerm n, String argu) throws Exception {
      String _ret=null;
      n.f0.accept(this, argu);
      String n1 = n.f1.accept(this, argu);
      this.args.add(n1);
      return _ret;
   }

   /**
    * f0 -> Identifier()
    * f1 -> "="
    * f2 -> Expression()
    * f3 -> ";"
    */
   public String visit(AssignmentStatement n, String argu) throws Exception {
      emit("\n;AssignmentStatement\n");
      String _ret=null;
      String id = n.f0.accept(this, "return_name");
     
      Variable var = globalMethod.getvar(id);
      boolean flag = false;
      if ( var == null){ 
        flag = true;                                        //if we did not find the identifier on arguments or variables of method then check on classes
        var = globalClass.getvar(id,table);
      }
      String type = typellvm(var.type);
      this.address = "" + id ;
      String expr = n.f2.accept(this, argu);
      this.address = "";
      if (flag == true){
        id = n.f0.accept(this,"load");            //if we did not find it in the method then it is on the class so we take the address on class vars
      }
      else{
        id = "%" + id ;     //store on the addres of identifier
      }
      emit("\tstore " + type + " " + expr + ", " + type + "* " + id + "\n" );
      

      
      n.f3.accept(this, argu);
      return _ret;
   }


      /**
    * f0 -> Identifier()
    * f1 -> "["
    * f2 -> Expression()
    * f3 -> "]"
    * f4 -> "="
    * f5 -> Expression()
    * f6 -> ";"
    */
   public String visit(ArrayAssignmentStatement n, String argu)throws Exception {
      emit("\n;ArrayAssignmentStatement\n");
      String _ret=null;
      emit("\n");
      
      String id = n.f0.accept(this, "return_name");                       //isws xreiazetai allagh
      Variable v = globalMethod.getvar(id);
      if (v == null){
        v = globalClass.getvar(id,this.table);
      }
      String type = typellvm(v.type);
      id = n.f0.accept(this,"");
      n.f1.accept(this, argu);
      
      String id_reg = "" + id;
      if (type == "i1*"){
        emit("\t%_" + this.registerNumber + " = bitcast i1* %_" + (this.registerNumber -1) + " to i32* \n");
        this.registerNumber = this.registerNumber + 1;
        
      }
      
      emit("\t%_" + this.registerNumber + " = load i32, i32* %_" + (this.registerNumber - 1) + "\n");
      this.registerNumber = this.registerNumber + 1;
      String size = "_" + (this.registerNumber - 1);

      String exp1 = n.f2.accept(this, argu);
      n.f3.accept(this, argu);
      n.f4.accept(this, argu);
      String exp1_reg = exp1;
      int array1 = this.arraySize;
      this.arraySize = this.arraySize + 1;
      int array2 = this.arraySize;
      this.arraySize = this.arraySize + 1;
      int array3 = this.arraySize;
      this.arraySize = this.arraySize + 1;


      emit("\t%_" + this.registerNumber + " = icmp ult i32 " + exp1_reg + ", %" + size + "\n");
      this.registerNumber = this.registerNumber + 1;
      
      emit("\tbr i1 %_" + (this.registerNumber - 1) + ", label %array_size" + array1 + ", label %array_size" + array2 + "\n");
      
      emit("\tarray_size" + array1 + ":\n");
   
      if ( type == "i1*"){
       
        emit("\t%_" + this.registerNumber + " = add i32 " + exp1_reg + ", 4 \n");              //add 4 because we have stored on [0] the size of the array so if we want to take [0] index we instead should take [4] index (because of boolean array)
        this.registerNumber = this.registerNumber + 1;
        emit("\t%_" + this.registerNumber + " = getelementptr i1, i1* " + id_reg + ", i32 %_" + (this.registerNumber - 1) + "\n" );
        
      }
      else{
        emit("\t%_" + this.registerNumber + " = add i32 " + exp1_reg + ", 1 \n");              //add one because we have stored on [0] the size of the array so if we want to take [0] index we instead should take [1] index
        this.registerNumber = this.registerNumber + 1;
        emit("\t%_" + this.registerNumber + " = getelementptr i32, i32* " + id_reg + ", i32 %_" + (this.registerNumber - 1) + "\n" ); 
      }
      

      this.registerNumber = this.registerNumber + 1;
      String getel_reg = "_" + (this.registerNumber - 1);
      String exp2 = n.f5.accept(this, argu);
      String exp2_reg = exp2;
      

      if (type == "i1*"){
        emit("\tstore i1 " + exp2_reg + ", i1* %" + getel_reg + "\n");
      }
      else{
        emit("\tstore i32 " + exp2_reg + ", i32* %" + getel_reg + "\n"); 
      }
      _ret = "%" + getel_reg;
      emit("\tbr label %array_size" + array3 + "\n");
      
      emit("\tarray_size" + array2 + ":\n");
      emit("\tcall void @throw_oob()\n");
      emit("\tbr label %array_size" + array3 + "\n");
      emit("array_size" + array3 + ":\n");


      n.f6.accept(this, argu);
      return _ret;
   }


       /**
    * f0 -> PrimaryExpression()
    * f1 -> "["
    * f2 -> PrimaryExpression()
    * f3 -> "]"
    */

    public String visit(ArrayLookup n, String argu) throws Exception{
      emit("\n;ArrayLookup\n");
      String _ret=null;
      String exp1 = n.f0.accept(this, argu);
      n.f1.accept(this, argu);
      String type = typellvm(globalType);
      n.f3.accept(this, argu);
      
     
      String exp1_reg = exp1;
      if (type == "i1*"){
        emit("\t%_" + this.registerNumber + " = bitcast i1* %_" + (this.registerNumber -1) + " to i32* \n");
        this.registerNumber = this.registerNumber + 1;
        exp1_reg = "%_" + (this.registerNumber - 1);
      }
     
      String exp2 = n.f2.accept(this, argu);
      String exp2_reg = exp2;
      int array1 = this.arraySize;
      this.arraySize = this.arraySize + 1;
      int array2 = this.arraySize;
      this.arraySize = this.arraySize + 1;
      int array3 = this.arraySize;
      this.arraySize = this.arraySize + 1;
     
      
      emit("\t%_" + this.registerNumber + " = load i32, i32* " + exp1_reg + "\n");
      this.registerNumber = this.registerNumber + 1;
      String size = "_" + (this.registerNumber - 1);
    
      emit("\t%_" + this.registerNumber + " = icmp ult i32 " + exp2_reg + ", %" + size + "\n");
      this.registerNumber = this.registerNumber + 1;
      emit("\tbr i1 %_" + (this.registerNumber - 1) + ", label %array_size" + array1 + ", label %array_size" + array2 + "\n");
      
      emit("\tarray_size" + array1 + ":\n");
    
     
      if (type == "i32*"){
        emit("\t%_" + this.registerNumber + " = add i32 " + exp2_reg + ", 1 \n");              //add one because we have stored on [0] the size of the array so if we want to take [0] index we instead should take [1] index on integer array
        this.registerNumber = this.registerNumber + 1;
        //System.out.println("fffffffff");
        emit("\t%_" + this.registerNumber + " = getelementptr i32, i32* " + exp1_reg + ", i32 %_" + (this.registerNumber - 1) + "\n" );             //PREPEI NA ALLAXTEI KAI GIA TA BOOLEAN SE I8
        this.registerNumber = this.registerNumber + 1;
        emit("\t%_" + this.registerNumber + " = load i32, i32* %_" + (this.registerNumber -1) + "\n");
        _ret = "%_" + this.registerNumber;
        this.registerNumber = this.registerNumber + 1;
        emit("\tbr label %array_size" + array3 + "\n");
        //this.registerNumber = this.registerNumber + 1;
        emit("\t array_size" + array2 + ":\n");
        emit("\tcall void @throw_oob()\n");
        emit("\tbr label %array_size" + array3 + "\n");
        emit("\tarray_size" + array3 + ":\n");
      }
      else{
      	
        emit("\t%_" + this.registerNumber + " = add i32 " + exp2_reg + ", 4 \n");              //add 4 because we have stored on [0] the size of the array so if we want to take [0] index we instead should take [4] index on boolean array
        this.registerNumber = this.registerNumber + 1;
        emit("\t%_" + this.registerNumber + " = getelementptr i1, i1* " + exp1 + ", i32 %_" + (this.registerNumber - 1) + "\n" );             
        this.registerNumber = this.registerNumber + 1;
 
        emit("\t%_" + this.registerNumber + " = load i1, i1* %_" + (this.registerNumber -1) + "\n");
        _ret = "%_" + this.registerNumber;
        this.registerNumber = this.registerNumber + 1;
        emit("\tbr label %array_size" + array3 + "\n");
   
        emit("\t array_size" + array2 + ":\n");
        emit("\tcall void @throw_oob()\n");
        emit("\tbr label %array_size" + array3 + "\n");
        emit("\tarray_size" + array3 + ":\n");

      }
      return _ret;
   }

     /**
    * f0 -> "new"
    * f1 -> "int"
    * f2 -> "["
    * f3 -> Expression()
    * f4 -> "]"
    */
   public String visit(IntegerArrayAllocationExpression n, String argu) throws Exception{

      emit("\n;IntegerArrayAllocation\n");
      String _ret=null;
      n.f0.accept(this, argu);
      n.f1.accept(this, argu);
      n.f2.accept(this, argu);
      String exp = n.f3.accept(this, argu);

     
      globalType = "int[]";
      String real_size = exp;
      emit("\t%_" + this.registerNumber + " = add i32 " + real_size + ", 1 \n" );
      this.registerNumber = this.registerNumber + 1;
      String size = "%_" + (this.registerNumber - 1);

      int array1 = this.arraySize;
      this.arraySize = this.arraySize + 1;
      int array2 = this.arraySize;
      this.arraySize = this.arraySize + 1;
   
      emit("\tcall void (i32) @print_int(i32 " + real_size + ")\n");
      emit("\t%_" + this.registerNumber  + " = icmp slt i32 " + real_size + ", 0 \n");                //check if size  of array is less than 0
      this.registerNumber = this.registerNumber + 1;
      emit("\tbr i1 %_" + (this.registerNumber - 1) + ", label %array_size" + array1 + ", label %array_size" + array2 + "\n" );

      emit("\tarray_size" + array1 + ":\n");
      emit("\t call void @throw_oob()\n");
      emit("\t br label %array_size" + array2 + "\n");

      emit("\tarray_size" + array2 + ":\n");

      emit("\t%_" + this.registerNumber + " = call i8* @calloc(i32 4, i32 " + "" + size + ")\n" );
      this.registerNumber = this.registerNumber + 1;
      emit("\t%_" + this.registerNumber + " = bitcast i8* %_" + (this.registerNumber -1) + " to i32* \n");
      this.registerNumber = this.registerNumber + 1;
      
      emit("\tstore i32 " + real_size + ", i32* %_" + (this.registerNumber - 1) + "\n");
      _ret = "%_" + (this.registerNumber - 1);

      n.f4.accept(this, argu);
      
      return _ret;
   }

   /**
    * f0 -> "new"
    * f1 -> "boolean"
    * f2 -> "["
    * f3 -> Expression()
    * f4 -> "]"
    */
   public String visit(BooleanArrayAllocationExpression n, String argu) throws Exception {
      emit("\n;BooleanArrayAllocation\n");
      String _ret=null;
      n.f0.accept(this, argu);
      n.f1.accept(this, argu);
      n.f2.accept(this, argu);
      globalType = "boolean[]";
      String exp = n.f3.accept(this, argu);
      String real_size = exp;
      emit("\t%_" + this.registerNumber + " = add i32 " + real_size + ", 4 \n" );
      this.registerNumber = this.registerNumber + 1;
      String size = "" + (this.registerNumber - 1);

      emit("\tcall void (i32) @print_int(i32 %_" + (this.registerNumber - 1) + ")\n");
      emit("\t%_" + this.registerNumber  + " = icmp slt i32 " + real_size + ", 0 \n");                //check if size of array is less than 0
      this.registerNumber = this.registerNumber + 1;
      emit("\tbr i1 %_" + (this.registerNumber - 1) + ", label %array_size" + this.registerNumber + ", label %array_size" + (this.registerNumber + 1) + "\n" );
      this.registerNumber = this.registerNumber + 1;
      emit("\tarray_size" + (this.registerNumber - 1) + ":\n");
      emit("\t call void @throw_oob()\n");
      emit("\t br label %array_size" + this.registerNumber + "\n");
      this.registerNumber = this.registerNumber + 1;
      emit("\tarray_size" + (this.registerNumber -1) + ":\n");

      emit("\t%_" + this.registerNumber + " = call i8* @calloc(i32 1, i32 " + "%_" + size + ")\n" );
      this.registerNumber = this.registerNumber + 1;
      emit("\t%_" + this.registerNumber + " = bitcast i8* %_" + (this.registerNumber -1) + " to i32* \n");
      this.registerNumber = this.registerNumber + 1;
      emit("\tstore i32 " + real_size + ", i32* %_" + (this.registerNumber -1) + "\n");
      emit("\t%_" + this.registerNumber + " = bitcast i32* %_" + (this.registerNumber - 1) + " to i1* \n");
      this.registerNumber = this.registerNumber + 1;
      _ret = "%_" + (this.registerNumber - 1);

      n.f4.accept(this, argu);
      
      return _ret;
   }


   /**
    * f0 -> PrimaryExpression()
    * f1 -> "."
    * f2 -> "length"
    */
   public String visit(ArrayLength n, String argu)throws Exception {
      String _ret=null;
      String exp = n.f0.accept(this, argu);
      emit("\n;ArrayLength\n");
      if (typellvm(globalType) == "i1*"){
        emit("\t%_" + this.registerNumber + " = bitcast i1* %_" + (this.registerNumber -1) + " to i32* \n");
        this.registerNumber = this.registerNumber + 1;
        exp = "%_" + (this.registerNumber - 1);
        emit("\t%_" + this.registerNumber + " = load i32, i32* " + exp + "\n");
        this.registerNumber = this.registerNumber + 1;
      }
      else{
        emit("\t%_" + this.registerNumber + " = load i32, i32* " + exp + "\n");
        this.registerNumber = this.registerNumber + 1;
      }
      _ret = "%_" + (this.registerNumber - 1);
      n.f1.accept(this, argu);
      n.f2.accept(this, argu);
      return _ret;
   }

    /**
    * f0 -> PrimaryExpression()
    * f1 -> "<"
    * f2 -> PrimaryExpression()
    */
   public String visit(CompareExpression n, String argu)throws Exception {
      String _ret=null;

      String exp1 = n.f0.accept(this, argu);

      n.f1.accept(this, argu);
      String exp2 = n.f2.accept(this, argu);

      emit("\t%_" + this.registerNumber + " = icmp slt i32 " + exp1 + ", " + exp2 + "\n");
      this.registerNumber = this.registerNumber + 1;
      return "%_" + (this.registerNumber - 1);
   }


   /**
    * f0 -> Clause()
    * f1 -> "&&"
    * f2 -> Clause()
    */
   public String visit(AndExpression n, String argu)throws Exception {
      String _ret=null;
      String exp1 = n.f0.accept(this, argu);
      int and1 = this.ifNumber;
      this.ifNumber = this.ifNumber + 1;
      int and2 = this.ifNumber;
      this.ifNumber = this.ifNumber + 1;
      int and3 = this.ifNumber;
      this.ifNumber = this.ifNumber + 1;
      int and4 = this.ifNumber;
      this.ifNumber = this.ifNumber + 1;

      emit("\tbr label %and" + and1 + "\n");
      emit("and" + and1 + ":\n");
      emit("\tbr i1 " + exp1 + ", label %and" + and2 + ", label %and" + and4 + "\n");
      emit("and" + and2 + ":\n");
      n.f1.accept(this, argu);
      String exp2 = n.f2.accept(this, argu);

      emit("\tbr label %and" + and3 + "\n");
      emit("and" + and3 + ":\n");
      emit("\tbr label %and" + and4 + "\n");
      emit("and" + and4 + ":\n");
      emit("\t%_" + this.registerNumber + " = phi i1 [ 0, %and" + and1 + "], [ " + exp2 + ", %and" + and3 + "]\n");
      this.registerNumber = this.registerNumber + 1;
      return "%_" + (this.registerNumber - 1);
   }

   /**
    * f0 -> NotExpression()
    *       | PrimaryExpression()
    */
   public String visit(Clause n, String argu)throws Exception {
      return n.f0.accept(this, argu);
   }



    /**
    * f0 -> "!"
    * f1 -> Clause()
    */
   public String visit(NotExpression n, String argu)throws Exception {
      String _ret=null;
      n.f0.accept(this, argu);
      String name = n.f1.accept(this, argu);
      emit("\t%_" + this.registerNumber + " = xor i1 1, " + name + "\n");                           //We can take Not Expression using xor with 1 
      _ret = "%_" + this.registerNumber;
      this.registerNumber = this.registerNumber + 1;
      return _ret;
   }

   /**
    * f0 -> PrimaryExpression()
    * f1 -> "+"
    * f2 -> PrimaryExpression()
    */
   public String visit(PlusExpression n, String argu)throws Exception {
      String _ret=null;
     // System.out.println("edw eftasa?");
      String expr1 = n.f0.accept(this, argu);
      n.f1.accept(this, argu);
      String expr2 = n.f2.accept(this, argu);


      emit("\t%_" + this.registerNumber + " = add i32 " + expr1 + ", " + expr2 + "\n");
      this.registerNumber = this.registerNumber + 1;
      return "%_" + (this.registerNumber - 1);
      
   }


    /**
    * f0 -> "System.out.println"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> ";"
    */


   public String visit(PrintStatement n,String argu) throws Exception {
        String _ret=null;
        n.f0.accept(this,argu);
        String exp = n.f2.accept(this,argu);
        emit("\tcall void (i32) @print_int(i32 " + exp + ")\n");
        
        return null;
    }
   

    /**
    * f0 -> PrimaryExpression()
    * f1 -> "-"
    * f2 -> PrimaryExpression()
    */
   public String visit(MinusExpression n, String argu)throws Exception {
      String _ret=null;
      String expr1 = n.f0.accept(this, argu);
      n.f1.accept(this, argu);
      String expr2 = n.f2.accept(this, argu);


      emit("\t%_" + this.registerNumber + " = sub i32 " + expr1 + ", " + expr2 + "\n");
      this.registerNumber = this.registerNumber + 1;
      return "%_" + (this.registerNumber - 1);
   }

    /**
    * f0 -> PrimaryExpression()
    * f1 -> "*"
    * f2 -> PrimaryExpression()
    */
   public String visit(TimesExpression n, String argu)throws Exception {
     String _ret=null;
      String expr1 = n.f0.accept(this, argu);
      n.f1.accept(this, argu);
      String expr2 = n.f2.accept(this, argu);


      emit("\t%_" + this.registerNumber + " = mul i32 " + expr1 + ", " + expr2 + "\n");
      this.registerNumber = this.registerNumber + 1;
      return "%_" + (this.registerNumber - 1);
   }

     /**
    * f0 -> "("
    * f1 -> Expression()
    * f2 -> ")"
    */
   public String visit(BracketExpression n, String argu) throws Exception {
      String _ret=null;
      n.f0.accept(this, argu);
      _ret = n.f1.accept(this, argu);
      n.f2.accept(this, argu);
      return _ret;
   }


   /**
    * f0 -> "if"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> Statement()
    * f5 -> "else"
    * f6 -> Statement()
    */
   public String visit(IfStatement n, String argu)throws Exception {
      String _ret=null;
      n.f0.accept(this, argu);
      n.f1.accept(this, argu);
      int if1 = this.ifNumber;
      this.ifNumber = this.ifNumber + 1;
      int if2 = this.ifNumber;
      this.ifNumber = this.ifNumber + 1;
      int if3 = this.ifNumber;
      this.ifNumber = this.ifNumber + 1;
      String exp1 = n.f2.accept(this, argu);
      emit("\tbr i1 " + exp1 + ", label %if" + if1 + ", label %if" + if2 + "\n");
      
      
      emit("\tif" + if1 + ":\n");
      n.f3.accept(this, argu);
     
      String exp2 = n.f4.accept(this, argu);
      emit("\tbr label %if" + if3 + "\n");
      
      
      emit("if" + if2 + ":\n");
      n.f5.accept(this, argu);
      n.f6.accept(this, argu);

      emit("\tbr label %if" + if3 + "\n");
      emit("if" + if3 + ":\n");

      return _ret;
   }

    /**
    * f0 -> "while"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> Statement()
    */
   public String visit(WhileStatement n, String argu)throws Exception {
      String _ret=null;
      n.f0.accept(this, argu);
      int while1 = this.whileNumber;
      this.whileNumber = this.whileNumber + 1;
      int while2 = this.whileNumber;
      this.whileNumber = this.whileNumber + 1;
      int while3 = this.whileNumber;
      this.whileNumber = this.whileNumber + 1;

      emit("\tbr label %while" + while1 + "\n");
      emit("while" + while1 + ":\n");
      n.f1.accept(this, argu);
      String exp = n.f2.accept(this, argu);
      emit("\tbr i1 " + exp + ", label %while" + while2 + ", label %while" + while3 +"\n");
      emit("while" + while2 +":\n");

      n.f3.accept(this, argu);
      n.f4.accept(this, argu);

      emit("\tbr label %while" + while1 + "\n");
      emit("while" + while3 + ":\n");
      return _ret;
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
      String name = n.f1.accept(this, "return_name");
      globalClass = table.getclass(name);
      n.f2.accept(this, argu);
      //n.f3.accept(this, name); 
      n.f4.accept(this, argu);
      n.f5.accept(this, argu);
      return _ret;
   }

    public String visit(ClassExtendsDeclaration n, String argu) throws Exception {
      String _ret=null;
      String name;
      n.f0.accept(this, argu);
      name = n.f1.accept(this, "return_name");
      globalClass = table.getclass(name);
      Class par = table.getclass(globalClass.parent);
      //n.f5.accept(this, name);
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
        String name = n.f1.accept(this, "return_name");
 
        this.globalClass = this.table.getclass(name);
    
        this.globalMethod = this.globalClass.getmeth("main");
        
        n.f2.accept(this, argu);
        n.f3.accept(this, argu);
        n.f4.accept(this, argu);
        n.f5.accept(this, argu);
        n.f6.accept(this, argu);
        emit("\ndefine i32 @main() {\n\n");
        for (Variable v : globalMethod.variables){
            emit("\t%" + v.name + " = alloca " + typellvm(v.type) + " \n");
        }
        
        n.f7.accept(this, argu);
        n.f8.accept(this, argu);
        n.f9.accept(this, argu);
        n.f10.accept(this, argu);
        //n.f11.accept(this, "return_name");
        n.f12.accept(this, argu);
        n.f13.accept(this, argu);
       // System.out.println("edw");
        //n.f14.accept(this, argu);
        n.f15.accept(this, argu);
       // System.out.println("ekei");
        n.f16.accept(this, argu);
        n.f17.accept(this, argu);
        emit("\n\tret i32 0 \n}\n\n");
      return _ret;
   }

   public String visit(NodeToken n, String argu) { return n.toString(); }
        

}