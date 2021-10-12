import syntaxtree.*;
import java.io.*;
import source.*;


class Main{
	public static void main (String [] args){
		if (args.length < 1){
			System.err.println("Usage: java Driver <inputfile>");
			System.exit(1);

		}
		int i = 0;
		while (i < args.length){
			FileInputStream fis = null;
			try {
				
				
					
					fis = new FileInputStream(args[i]);

					MiniJavaParser parser = new MiniJavaParser(fis);
					Goal root = parser.Goal();
					System.err.println("Program parsed successfuly and running file with name : " + args[i]);
					SymbolTable table = new SymbolTable(args[i]);
					FirstVisitor eval1 = new FirstVisitor(table);
					root.accept(eval1, null);
					SecondVisitor eval2 = new SecondVisitor(table);
					root.accept(eval2, null);
					String new_file = args[i];
					new_file = (new_file.substring(0,new_file.length() - 5));
					new_file = new_file + ".ll";
					//System.out.println(new_file);
					FileWriter out = new FileWriter(new_file);
					LLVMvisitor llvm = new LLVMvisitor(table,out);
					root.accept(llvm, null);
					
				
				//table.print();
			}
			catch(ParseException ex){
				System.out.println(ex.getMessage());
			}
			catch(FileNotFoundException ex){
				System.err.println(ex.getMessage());
			}
			catch(Exception ex){
				System.err.println(ex.getMessage());
			}
			System.out.println("\n\n");
			i = i + 1;
		}
		
	}
}