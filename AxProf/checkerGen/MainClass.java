import java.io.FileInputStream;
import java.io.PrintWriter;
import java.io.IOException;
import java.util.Arrays;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.tree.*;
import java.nio.file.Paths;

/* Main checker generator class
   Calls the lexer, parser, semantic analysis, and code generator in order
*/

public class MainClass {
  public static void main(String args[]) throws Exception{
    if(args.length < 1) {
      System.err.println("Error: No spec file given.");
      System.exit(1);
    }
    String filename = args[0];
    CharStream inStream=null;
    try{
      inStream = CharStreams.fromStream(new FileInputStream(filename));
    }catch(Exception e){
      System.err.println("Could not read file "+filename);
      return;
    }
    AxProfSpecLexer lexer = new AxProfSpecLexer(inStream);
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    tokens.fill();
    AxProfSpecParser parser = new AxProfSpecParser(tokens);
    AxProfSpecParser.SpecContext spec = null;
    try{
      spec = parser.spec();
    }catch(Exception e){
      e.printStackTrace();
    }
    Semantic semantic = new Semantic(spec.value);
    CodeGen codeGenerator = new CodeGen(spec.value);
    codeGenerator.generate();
  }
}
