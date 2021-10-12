package source;
import visitor.GJDepthFirst;
import syntaxtree.*;

public class FirstVisitor extends GJDepthFirst<String, String>{

    SymbolTable table;


    public FirstVisitor(SymbolTable table){
     this.table = table;
    }





      /**
    * f0 -> "class"
    * f1 -> Identifier()
    * f2 -> "{"
    * f3 -> ( VarDeclaration() )*
    * f4 -> ( MethodDeclaration() )*
    * f5 -> "}"
    */

   public String visit(NodeToken n, String argu) { return n.toString(); }


   public String visit(ClassDeclaration n, String argu) throws Exception{
      String _ret=null;
      String name;
      name = n.f1.accept(this, argu);
      
      if (table.classes.containsKey(name))
        throw new Exception("ERROR: class already defined ");
      else{
        Class temp = new Class(name,"");
        table.classes.put(name,temp);
        //table.print();
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
    String name = n.f1.accept(this, argu);
    String extendName = n.f3.accept(this, argu);
    if ( table.classes.containsKey(name) )
        throw new Exception("ERROR: class already defined");
    else if (!table.classes.containsKey(extendName))
        throw new Exception("ERROR: extended class has not been defined");
    else{
        Class temp = new Class(name,extendName);
        table.classes.put(name,temp);
        //table.print();
      }
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
      Class temp = null;
      if (table.classes.containsKey(name))
        throw new Exception("ERROR: class already defined ");
      else{
        temp = new Class(name,"");
        table.classes.put(name,temp);
        //table.print();
      }
      n.f2.accept(this, argu);
      n.f3.accept(this, argu);
      n.f4.accept(this, argu);
      n.f5.accept(this, argu);
      n.f6.accept(this, argu);
      n.f7.accept(this, argu);
      n.f8.accept(this, argu);
      n.f9.accept(this, argu);
      n.f10.accept(this, argu);
      String n1 = n.f11.accept(this, argu).toString();
      Method m = new Method("main","void");
      Variable v = new Variable(n1,"String[]");
      m.putarg(v);
      temp.putmeth(m);
      table.classes.put(name,temp);

      n.f12.accept(this, argu);
      n.f13.accept(this, argu);
      n.f14.accept(this, argu);
      n.f15.accept(this, argu);
      n.f16.accept(this, argu);
      n.f17.accept(this, argu);
      return _ret;
   }
}