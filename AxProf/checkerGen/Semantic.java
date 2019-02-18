import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

/* Semantic analysis class
   Run after AST is generated
   Traverses the specification, assigning types to each subexpression
   Performs basic type checking
*/

public class Semantic {

  private Map<String,AST.dataType> typeMap;
  AST.SpecType specType;
  AST.dataType realType, matrixType;

  public Semantic(AST.spec spec) {
    //get specification type
    specType = spec.specType();
    //some basic data types
    realType = new AST.dataType(AST.dataType.REAL);
    matrixType = new AST.dataType(AST.dataType.MATRIX);
    //map variable name to type
    typeMap = new HashMap<String,AST.dataType>();
    //add type declarations to map
    boolean outputDeclared = false, inputDeclared = false;
    for(AST.typeDecl typeDecl : spec.typeDecls) {
      typeMap.put(typeDecl.name,typeDecl.type);
      if(typeDecl.name.equals("Output"))
        outputDeclared = true;
      if(typeDecl.name.equals("Input"))
        inputDeclared = true;
    }
    assert(outputDeclared && inputDeclared);
    //traverse time/space/acc expressions
    if(spec.exp!=null) traverseBoolExp(spec.exp);
    if(spec.timeExp!=null) traverseDataExp(spec.timeExp);
    if(spec.spaceExp!=null) traverseDataExp(spec.spaceExp);
  }

  private void traverseBoolExp(AST.boolExp exp) {
    if(exp instanceof AST.forall) {
      AST.forall forall = (AST.forall)exp;
      traverseRanges(forall.ranges);
      traverseBoolExp(forall.exp);
    } else if (exp instanceof AST.let) {
      AST.let let = (AST.let)exp;
      traverseDataExp(let.value);
      typeMap.put(let.name,let.value.type);
      traverseBoolExp(let.exp);
    } else if(exp instanceof AST.isInData) {
      AST.isInData isInData = (AST.isInData)exp;
      traverseDataExp(isInData.item);
      traverseDataExp(isInData.data);
    } else if(exp instanceof AST.approxEq) {
      AST.approxEq approxEq = (AST.approxEq)exp;
      traverseDataExp(approxEq.e1);
      traverseDataExp(approxEq.e2);
    } else if(exp instanceof AST.comparison) {
      AST.comparison comparison = (AST.comparison)exp;
      traverseDataExp(comparison.e1);
      traverseDataExp(comparison.e2);
    } else if(exp instanceof AST.boolAndOr) {
      AST.boolAndOr boolAndOr = (AST.boolAndOr)exp;
      traverseBoolExp(boolAndOr.e1);
      traverseBoolExp(boolAndOr.e2);
    } else if(exp instanceof AST.boolNot) {
      traverseBoolExp(((AST.boolNot)exp).exp);
    } else {
      assert(false);
    }
  }

  private void traverseDataExp(AST.dataExp exp) {
    if(exp instanceof AST.realConst) {
      exp.type = realType;
    } else if(exp instanceof AST.probabilityInputs) {
      traverseBoolExp(((AST.probabilityInputs)exp).exp);
      exp.type = realType;
    } else if(exp instanceof AST.probabilityRuns) {
      traverseBoolExp(((AST.probabilityRuns)exp).exp);
      exp.type = realType;
    } else if(exp instanceof AST.probabilityItems) {
      AST.probabilityItems probItems = (AST.probabilityItems)exp;
      traverseRanges(probItems.ranges);
      traverseBoolExp(probItems.exp);
      exp.type = realType;
    } else if(exp instanceof AST.expectationInputs) {
      traverseDataExp(((AST.expectationInputs)exp).exp);
      exp.type = realType;
    } else if(exp instanceof AST.expectationRuns) {
      traverseDataExp(((AST.expectationRuns)exp).exp);
      exp.type = realType;
    } else if(exp instanceof AST.expectationItems) {
      AST.expectationItems expItems = (AST.expectationItems)exp;
      traverseRanges(expItems.ranges);
      traverseDataExp(expItems.exp);
      exp.type = realType;
    } else if(exp instanceof AST.lookup) {
      AST.lookup lookup = (AST.lookup)exp;
      traverseDataExp(lookup.coll);
      traverseDataExp(lookup.key);
      switch(lookup.coll.type.baseType) {
        case AST.dataType.LIST:
          exp.type = lookup.coll.type.kType;
          break;
        case AST.dataType.MAP:
          exp.type = lookup.coll.type.vType;
          break;
        default:
          assert(false);
          break;
      }
    } else if(exp instanceof AST.varId) {
      String varName = ((AST.varId)exp).name;
      if(typeMap.containsKey(varName))
        exp.type = typeMap.get(varName);
      else
        //assume all config variables are realConst
        exp.type = realType;
    } else if(exp instanceof AST.dataExpList) {
      AST.dataExpList dataExpList = (AST.dataExpList)exp;
      for(AST.dataExp item : dataExpList.list)
        traverseDataExp(item);
      exp.type = new AST.dataType(AST.dataType.LIST,dataExpList.list.get(0).type);
    } else if(exp instanceof AST.dataOp) {
      AST.dataOp dataOp = (AST.dataOp)exp;
      traverseDataExp(dataOp.e1);
      traverseDataExp(dataOp.e2);
      exp.type = dataOp.e1.type;
    } else if(exp instanceof AST.dataSize) {
      AST.dataSize dataSize = (AST.dataSize)exp;
      traverseDataExp(dataSize.coll);
      exp.type = realType;
    } else if(exp instanceof AST.funcCall) {
      AST.funcCall funcCall = (AST.funcCall)exp;
      for(AST.ASTNode param : funcCall.params) {
        if(param instanceof AST.dataExp) {
          traverseDataExp((AST.dataExp)param);
        } else {
          assert(false);
        }
      }
      if(typeMap.containsKey(funcCall.funcName))
        exp.type = typeMap.get(funcCall.funcName);
      else
        exp.type = realType;
    } else {
      assert(false);
    }
  }

  private void traverseRanges(List<AST.range> ranges) {
    for(AST.range range : ranges) {
      AST.varId iter = (AST.varId)range.item;
      traverseDataExp(range.coll);
      //collection must be iterable
      assert(range.coll.type.baseType == AST.dataType.LIST);
      //get type of iterator, add to map
      if(range.type==AST.range.INDEX) {
        iter.type = realType;
      } else {
        iter.type = range.coll.type.kType;
      }
      typeMap.put(iter.name,iter.type);
    }
  }

}
