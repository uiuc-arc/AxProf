MainClass.class: antlr-4.7.1-complete.jar
	java -jar antlr-4.7.1-complete.jar -no-listener -visitor -lib ./ AxProfSpec.g4
	javac -cp antlr-4.7.1-complete.jar *.java

antlr-4.7.1-complete.jar:
	wget https://www.antlr.org/download/antlr-4.7.1-complete.jar

.PHONY: clean
clean:
	rm -f *.class *.interp *.tokens *BaseVisitor.java *Lexer.java *Parser.java *Visitor.java
