/* AxProf specification generator grammar */

grammar AxProfSpec;

@header{
import java.util.List;
import java.util.ArrayList;
}

/* Type rule
   supports real values, matrices of real values, lists, and maps
*/

type returns [AST.dataType value]
    : 'real' { $value = new AST.dataType(AST.dataType.REAL); }
    | 'matrix' { $value = new AST.dataType(AST.dataType.MATRIX); }
    | 'list' 'of' it=type { $value = new AST.dataType(AST.dataType.LIST,$it.value); }
    | 'map' 'from' kt=type 'to' vt=type { $value = new AST.dataType(AST.dataType.MAP,$kt.value,$vt.value); }
    | '(' t=type ')' { $value = $t.value; } //evaluation order disambiguation
    ;

/* Type declaration rule
   associates a name with a type for type checking and code generation
*/

typeDecl returns [AST.typeDecl value]
    : name=Id t=type ';' { $value = new AST.typeDecl($name.getText(),$t.value); }
    ;

/* Type declaration list rule
   a list of one or more type declarations present at the start of the specification
   the types of the input and output must be specified; this is enforced later
*/

typeDeclList returns [List<AST.typeDecl> value]
    @init{ $value = new ArrayList<AST.typeDecl>(); }
    : (td=typeDecl { $value.add($td.value); } )+
    ;

/* Main specification rule
   consists of a list of type declarations, followed by optional time, space, and accuracy specifications in that order
*/

spec returns [AST.spec value]
    @init{ $value = new AST.spec(); }
    : tds=typeDeclList { $value.addDecls($tds.value); }
        ('TIME' de1=dataExp ';' { $value.addTime($de1.value); })?
        ('SPACE' de2=dataExp ';' { $value.addSpace($de2.value); })?
        ('ACC' be=boolExp { $value.addAcc($be.value); })?
    ;

/* Boolean expression rule
   consists of the following general types of rules:
   1) for all (universal quantification)
   2) let
   3) item present in set
   4) binary comparisions
   5) boolean operations (and, or, not)
*/

boolExp returns [AST.boolExp value]
    : 'forall' ranges=rangeList ':' be=boolExp { $value = new AST.forall($ranges.value,$be.value); }
    | 'let' name=Id '=' dat=dataExp 'in' be=boolExp { $value = new AST.let($name.getText(),$dat.value,$be.value); }
    | item=dataExp 'in' dat=dataExp { $value = new AST.isInData($item.value,$dat.value); }
    | de1=dataExp '.==' de2=dataExp { $value = new AST.approxEq($de1.value,$de2.value); }
    | de1=dataExp '==' de2=dataExp { $value = new AST.comparison($de1.value,"==",$de2.value); }
    | de1=dataExp '!=' de2=dataExp { $value = new AST.comparison($de1.value,"!=",$de2.value); }
    | de1=dataExp '>'  de2=dataExp { $value = new AST.comparison($de1.value,">" ,$de2.value); }
    | de1=dataExp '<'  de2=dataExp { $value = new AST.comparison($de1.value,"<" ,$de2.value); }
    | de1=dataExp '>=' de2=dataExp { $value = new AST.comparison($de1.value,">=",$de2.value); }
    | de1=dataExp '<=' de2=dataExp { $value = new AST.comparison($de1.value,"<=",$de2.value); }
    | be1=boolExp '&&' be2=boolExp { $value = new AST.boolAndOr($be1.value,"and",$be2.value); }
    | be1=boolExp '||' be2=boolExp { $value = new AST.boolAndOr($be1.value,"or" ,$be2.value); }
    | '!' be=boolExp { $value = new AST.boolNot($be.value); }
    | '(' be=boolExp ')' { $value = $be.value; } //evaluation order disambiguation
    ;

/* Data expression rule
   rule for all types of data other than boolean
   consists of the following general types of rules:
   1) constants
   2) probability and expected values
   3) list/set/map lookup
   4) variables
   5) lists/tuples
   6) numerical binary operations
   7) list/set/map size
   8) external function call
*/

dataExp returns [AST.dataExp value]
    : realVal=Real { $value = new AST.realConst($realVal.getText()); }
    | 'Probability' 'over' 'inputs' '[' be=boolExp ']' { $value = new AST.probabilityInputs($be.value); }
    | 'Probability' 'over' 'runs' '[' be=boolExp ']' { $value = new AST.probabilityRuns($be.value); }
    | 'Probability' 'over' ranges=rangeList '[' be=boolExp ']' { $value = new AST.probabilityItems($ranges.value,$be.value); }
    | 'Expectation' 'over' 'inputs' '[' de=dataExp ']' { $value = new AST.expectationInputs($de.value); }
    | 'Expectation' 'over' 'runs' '[' de=dataExp ']' { $value = new AST.expectationRuns($de.value); }
    | 'Expectation' 'over' ranges=rangeList '[' de=dataExp ']' { $value = new AST.expectationItems($ranges.value,$de.value); }
    | coll=dataExp '[' key=dataExp ']' { $value = new AST.lookup($coll.value,$key.value); }
    | name=Id { $value = new AST.varId($name.getText()); }
    | expList=dataExpList { $value = new AST.dataExpList($expList.value); }
    | de1=dataExp '+' de2=dataExp { $value = new AST.dataOp($de1.value,"+",$de2.value); }
    | de1=dataExp '-' de2=dataExp { $value = new AST.dataOp($de1.value,"-",$de2.value); }
    | de1=dataExp '*' de2=dataExp { $value = new AST.dataOp($de1.value,"*",$de2.value); }
    | de1=dataExp '/' de2=dataExp { $value = new AST.dataOp($de1.value,"/",$de2.value); }
    | de1=dataExp '^' de2=dataExp { $value = new AST.dataOp($de1.value,"**",$de2.value); }
    | '|' coll=dataExp '|' { $value = new AST.dataSize($coll.value); }
    | name=Id args=funcArgs { $value = new AST.funcCall($name.getText(),$args.value); }
    | '(' de=dataExp ')' { $value = $de.value; } //evaluation order disambiguation
    ;

/* Data expression list rule
   for constructing python style lists of data values
*/

dataExpList returns [List<AST.dataExp> value]
    @init{ $value = new ArrayList<AST.dataExp>(); }
    : '[' e1=dataExp { $value.add($e1.value); } ( ',' er=dataExp { $value.add($er.value); } )* ']'
    ;

/* Range specification rule
   for specifying a range for universal quantification or probability/expected value specification
   supports three types of ranges:
   1) range of items in a list/set/map-keys; similar to python "for item in collection:"
   2) range of *unique* items in a list/set/map-keys; similar to python "for item in set(collection):"
   3) range of indices of items in a list/set/map-keys; similar to python "for index in range(len(collection)):"
*/

range returns [AST.range value]
    : item=dataExp 'in' col=dataExp { $value = new AST.range($item.value,$col.value,AST.range.DIRECT); }
    | item=dataExp 'in' 'uniques' '(' col=dataExp ')' { $value = new AST.range($item.value,$col.value,AST.range.UNIQUE); }
    | item=dataExp 'in' 'indices' '(' col=dataExp ')' { $value = new AST.range($item.value,$col.value,AST.range.INDEX); }
    ;

/* Range list rule
   for specifying multiple ranges at once
   necessary because the entire multidimensional range must be considered at once for universal quantification or probability/expected value specification
*/

rangeList returns [List<AST.range> value]
    @init{ $value = new ArrayList<AST.range>(); }
    : r1=range { $value.add($r1.value); } ( ',' rr=range { $value.add($rr.value); } )*
    ;

/* Function argument rule
   wrapper around data expression rule - separate for future extensibility
*/

funcArg returns [AST.ASTNode value]
    : exp=dataExp { $value = $exp.value; }
    ;

/* Function argument list rule
   also looks for the parenthesis around the argument(s)
*/

funcArgs returns [List<AST.ASTNode> value]
    @init{ $value = new ArrayList<AST.ASTNode>(); }
    : '(' ( arg1=funcArg { $value.add($arg1.value); } ( ',' args=funcArg { $value.add($args.value); } )* )? ')'
    ;

/* Id rule
   must start with a letter or underscore
   other characters can be letters, numbers, underscore, or period
*/

Id
    : [a-zA-Z_][a-zA-Z0-9_.]*
    ;

/* Real constant rule */

Real
    : '-'?[0-9]+('.'[0-9]+)?
    ;

/* Skip whitespace */

WS
    : [ \n\r] -> skip
    ;
