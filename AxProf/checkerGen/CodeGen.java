import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

/* Code generation class
   Contains multiple functions for various aspects of code generation
   Generates python function definitions and returns them to AxProf
*/

public class CodeGen {

  private AST.spec spec;
  private AST.SpecType specType;
  private int tempCount;
  private Set<String> declaredVars;
  private String outputSuffix;

  /* Print the specified number of indents
     Python requires correct indentation
  */

  private void printIndents(int indents) {
    for(int i=0; i<indents; ++i)
      System.out.print("  ");
  }

  /* Returns a string to iterate over a range */

  private String getRangeStr(int type, String collName, String itemName) {
    String output = "for "+itemName+" in ";
    switch(type) {
      case AST.range.DIRECT:
        output += collName;
        break;
      case AST.range.UNIQUE:
        output += "set("+collName+")";
        break;
      case AST.range.INDEX:
        output += "range(len("+collName+"))";
        break;
      default:
        assert(false);
    }
    output += " :";
    return output;
  }

  /* Returns a string to perform a binomial test of the appropriate type */

  private String getCheckFreqStr(String count, String trials, String prob, String op) {
    String resultStr = "AxProf.binomialTest("+count+","+trials+","+prob+",alternative='";
    if(op.equals("==")) {
      resultStr += "two-sided";
    } else if(op.charAt(0) == '>') {
      resultStr += "less";
    } else if(op.charAt(0) == '<') {
      resultStr += "greater";
    } else {
      assert(false);
    }
    resultStr += "')";
    return resultStr;
  }

  /* Prints a string to perform a 1 sample t-test of the appropriate type */

  private String genCheckExpCode(String samples, String expVal, String op, boolean returnPVal, int indents) {
    String outTemp = "t"+(tempCount++);
    printIndents(indents);
    System.out.println(outTemp+" = AxProf.ttest_1samp("+samples+","+expVal+")");
    String resultStr = null;
    if(returnPVal) {
      if(op.equals("==")) {
        resultStr = outTemp+"[1]";
      } else {
        assert(false);
      }
    } else {
      if(op.equals("==")) {
        resultStr = "("+outTemp+"[1]>=0.05)";
      } else if(op.charAt(0) == '>') {
        resultStr = "("+outTemp+"[1]/2>=0.05 or "+outTemp+"[0]>=0)";
      } else if(op.charAt(0) == '<') {
        resultStr = "("+outTemp+"[1]/2>=0.05 or "+outTemp+"[0]<=0)";
      } else {
        assert(false);
      }
    }
    return resultStr;
  }

  /* Generate expression code assuming that a boolean expression must be returned */

  public String genExpCode(AST.ASTNode exp, int indents) {
    return genExpCode(exp,indents,false);
  }

  /* Generate expression code for given expression AST node
     It prints the appropriate code and returns a string containing an
     expression that will contain the evaluation result after the printed code
     is executed by AxProf
     If returnPVal parameter is true, it will return the p value of the expression, assuming it is a statistical test
     If returnPVal parameter is false, it will return a boolean expression
  */

  public String genExpCode(AST.ASTNode exp, int indents, boolean returnPVal) {
    //begin boolean expressions
    if(exp instanceof AST.forall) {
      //universal quantification - run multiple tests and combine the result
      AST.forall forall = (AST.forall)exp;
      boolean containsApproxComp = (forall.specType() == AST.SpecType.RUNS);
      int numRanges = forall.ranges.size();
      String resultTemp = "t"+(tempCount++);
      String pValsTemp = null;
      printIndents(indents);
      if(containsApproxComp) {
        pValsTemp = "t"+(tempCount++);
        System.out.println(pValsTemp+" = []");
      } else {
        System.out.println(resultTemp+" = 1");
      }
      List<String> collections = new ArrayList<String>();
      for(int i=0; i<numRanges; ++i) {
        AST.range range = forall.ranges.get(i);
        String itemName = ((AST.varId)range.item).name;
        String collName = genExpCode(range.coll,indents+i);
        collections.add(collName);
        declaredVars.add(itemName);
        printIndents(indents+i);
        System.out.println(getRangeStr(range.type,collName,itemName));
      }
      String bodyResult = genExpCode(forall.exp,indents+numRanges,containsApproxComp);
      printIndents(indents+numRanges);
      if(containsApproxComp) {
        System.out.println(pValsTemp+".append("+bodyResult+")");
        printIndents(indents);
        System.out.println(resultTemp+" = AxProf.combine_pvalues("+pValsTemp+")[1]");
      } else {
        System.out.println(resultTemp+" *= "+bodyResult);
        for(int i=numRanges-1; i>=0; --i) {
          printIndents(indents+i+1);
          System.out.println("if not "+resultTemp+":");
          printIndents(indents+i+2);
          System.out.println("break");
        }
      }
      if(returnPVal)
        return resultTemp;
      else
        return "("+resultTemp+">=0.05)";
    } else if(exp instanceof AST.let) {
      AST.let let = (AST.let)exp;
      String val = genExpCode(let.value,indents);
      printIndents(indents);
      System.out.println(let.name+" = "+val);
      declaredVars.add(let.name);
      String result = genExpCode(let.exp,indents);
      return result;
    } else if(exp instanceof AST.isInData) {
      AST.isInData isInData = (AST.isInData)exp;
      String item = genExpCode(isInData.item,indents);
      if(isInData.item.type.baseType == AST.dataType.LIST) {
        item = "tuple("+item+")";
      }
      String data = genExpCode(isInData.data,indents);
      String resultStr = null;
      if(isInData.data.type.baseType == AST.dataType.LIST) {
        resultStr = "("+item+" in "+data+")";
      } else if(isInData.data.type.baseType == AST.dataType.MAP) {
        resultStr = data+".get("+item+",0)";
      } else {
        assert(false);
      }
      if(returnPVal)
        return "(1 if ("+resultStr+") else 0)";
      else
        return resultStr;
    } else if(exp instanceof AST.approxEq) {
      AST.approxEq approxEq = (AST.approxEq)exp;
      AST.varId lhs = (AST.varId)approxEq.e1;
      assert(lhs.name.equals("Output"));
      String rhs = genExpCode(approxEq.e2,indents);
      String outerTemp = "t"+(tempCount++);
      String innerTemp = "t"+(tempCount++);
      printIndents(indents);
      System.out.println(outerTemp+" = ["+innerTemp+"-"+rhs+" for "+innerTemp+" in Output]");
      String pValue = "AxProf.wilcoxon("+outerTemp+").pvalue";
      if(returnPVal)
        return pValue;
      else
        return "("+pValue+">=0.05)";
    } else if(exp instanceof AST.comparison) {
      //comparisons are handled differently if they contain a probabilistic expression
      AST.comparison comparison = (AST.comparison)exp;
      if(comparison.e1 instanceof AST.probabilityInputs) {
        AST.probabilityInputs probInputs = (AST.probabilityInputs)comparison.e1;
        String counterTemp = "t"+(tempCount++);
        outputSuffix = "t"+(tempCount++);
        printIndents(indents);
        System.out.println(counterTemp+" = 0");
        printIndents(indents);
        System.out.println("for "+outputSuffix+" in range(Inputs):");
        String bodyResult = genExpCode(probInputs.exp,indents+1);
        outputSuffix = null;
        printIndents(indents+1);
        System.out.println(counterTemp+" += 1 if "+bodyResult+" else 0");
        String rhs = genExpCode(comparison.e2,indents);
        String pValue = getCheckFreqStr(counterTemp,"Inputs",rhs,comparison.op);
        if(returnPVal)
          return pValue;
        else
          return "("+pValue+">=0.05)";
      } else if(comparison.e1 instanceof AST.probabilityRuns) {
        AST.probabilityRuns probRuns = (AST.probabilityRuns)comparison.e1;
        String counterTemp = "t"+(tempCount++);
        outputSuffix = "t"+(tempCount++);
        printIndents(indents);
        System.out.println(counterTemp+" = 0");
        printIndents(indents);
        System.out.println("for "+outputSuffix+" in range(Runs):");
        String bodyResult = genExpCode(probRuns.exp,indents+1);
        outputSuffix = null;
        printIndents(indents+1);
        System.out.println(counterTemp+" += 1 if "+bodyResult+" else 0");
        String rhs = genExpCode(comparison.e2,indents);
        String pValue = getCheckFreqStr(counterTemp,"Runs",rhs,comparison.op);
        if(returnPVal)
          return pValue;
        else
          return "("+pValue+">=0.05)";
      } else if(comparison.e1 instanceof AST.expectationInputs) {
        AST.expectationInputs expInputs = (AST.expectationInputs)comparison.e1;
        AST.dataExp expInExp = expInputs.exp;
        String valueString = null;
        if(expInExp instanceof AST.varId) {
          assert(((AST.varId)expInExp).name.equals("Output"));
          valueString = "Output";
        } else {
          assert(false);
        }
        String rhs = genExpCode(comparison.e2,indents);
        return genCheckExpCode(valueString,rhs,comparison.op,returnPVal,indents);
      } else if(comparison.e1 instanceof AST.expectationRuns) {
        AST.expectationRuns expRuns = (AST.expectationRuns)comparison.e1;
        AST.dataExp expInExp = expRuns.exp;
        String valueString = null;
        if(expInExp instanceof AST.varId) {
          assert(((AST.varId)expInExp).name.equals("Output"));
          valueString = "Output";
        } else {
          assert(false);
        }
        String rhs = genExpCode(comparison.e2,indents);
        return genCheckExpCode(valueString,rhs,comparison.op,returnPVal,indents);
      } else if(comparison.e1 instanceof AST.probabilityItems) {
        AST.probabilityItems probItems = (AST.probabilityItems)comparison.e1;
        int numRanges = probItems.ranges.size();
        String counterTemp = "t"+(tempCount++);
        printIndents(indents);
        System.out.println(counterTemp+" = 0");
        List<String> collections = new ArrayList<String>();
        for(int i=0; i<numRanges; ++i) {
          AST.range range = probItems.ranges.get(i);
          String itemName = ((AST.varId)range.item).name;
          String collName = genExpCode(range.coll,indents+i);
          collections.add(collName);
          declaredVars.add(itemName);
          printIndents(indents+i);
          System.out.println(getRangeStr(range.type,collName,itemName));
        }
        String bodyResult = genExpCode(probItems.exp,indents+numRanges);
        printIndents(indents+numRanges);
        System.out.println(counterTemp+" += 1 if "+bodyResult+" else 0");
        String rhs = genExpCode(comparison.e2,indents);
        String trialsStr = "1";
        for(int i=0; i<numRanges; ++i) {
          String collStr = collections.get(i);
          if(probItems.ranges.get(i).type == AST.range.UNIQUE)
            collStr = "set("+collStr+")";
          trialsStr += "*len("+collStr+")";
        }
        String pValue = getCheckFreqStr(counterTemp,trialsStr,rhs,comparison.op);
        if(returnPVal)
          return pValue;
        else
          return "("+pValue+">=0.05)";
      } else if(comparison.e1 instanceof AST.expectationItems) {
        AST.expectationItems expItems = (AST.expectationItems)comparison.e1;
        int numRanges = expItems.ranges.size();
        String samplesTemp = "t"+(tempCount++);
        printIndents(indents);
        System.out.println(samplesTemp+" = []");
        for(int i=0; i<numRanges; ++i) {
          AST.range range = expItems.ranges.get(i);
          String itemName = ((AST.varId)range.item).name;
          String collName = genExpCode(range.coll,indents+i);
          declaredVars.add(itemName);
          printIndents(indents+i);
          System.out.println(getRangeStr(range.type,collName,itemName));
        }
        String bodyResult = genExpCode(expItems.exp,indents+numRanges);
        printIndents(indents+numRanges);
        System.out.println(samplesTemp+".append("+bodyResult+")");
        String rhs = genExpCode(comparison.e2,indents);
        return genCheckExpCode(samplesTemp,rhs,comparison.op,returnPVal,indents);
      } else {
        String num1 = genExpCode(comparison.e1,indents);
        String num2 = genExpCode(comparison.e2,indents);
        String comp = "("+num1+comparison.op+num2+")";
        if(returnPVal)
          return "(1 if "+comp+" else 0)";
        else
          return comp;
      }
    } else if(exp instanceof AST.boolAndOr) {
      AST.boolAndOr boolAndOr = (AST.boolAndOr)exp;
      String bool1 = genExpCode(boolAndOr.e1,indents,returnPVal);
      String bool2 = genExpCode(boolAndOr.e2,indents,returnPVal);
      if(returnPVal) {
        if(boolAndOr.op.equals("or"))
          return "max("+bool1+","+bool2+")";
        else
          return "min("+bool1+","+bool2+")";
      } else {
        return "("+bool1+" "+boolAndOr.op+" "+bool2+")";
      }
    } else if(exp instanceof AST.boolNot) {
      String inner = genExpCode(((AST.boolNot)exp).exp,indents,returnPVal);
      if(returnPVal)
        return "(1-"+inner+")";
      else
        return "(not "+inner+")";
    } else
    //begin data expressions
    if(exp instanceof AST.realConst) {
      return ((AST.realConst)exp).val;
    } else if(exp instanceof AST.lookup) {
      AST.lookup lookup = (AST.lookup)exp;
      String collStr = genExpCode(lookup.coll,indents);
      String keyStr = genExpCode(lookup.key,indents);
      if(lookup.coll.type.baseType == AST.dataType.LIST) {
        return collStr+"["+keyStr+"]";
      } else if(lookup.coll.type.baseType == AST.dataType.MAP) {
        return collStr+".get("+keyStr+",0)";
      } else {
        assert(false);
        return null;
      }
    } else if(exp instanceof AST.varId) {
      String varName = ((AST.varId)exp).name;
      if(declaredVars.contains(varName))
        if(varName.equals("Output") && outputSuffix != null)
          return "(Output["+outputSuffix+"])";
        else
          return varName;
      else
        //undeclared variables are assumed to be configuration parameters
        return "Config['"+varName+"']";
    } else if(exp instanceof AST.dataExpList) {
      List<String> items = new ArrayList<String>();
      for(AST.dataExp item : ((AST.dataExpList)exp).list) {
        items.add(genExpCode(item,indents));
      }
      int numItems = items.size();
      String resultStr = "[";
      for(int i=0; i<numItems; ++i) {
        if(i>0)
          resultStr += ",";
        resultStr += items.get(i);
      }
      resultStr += "]";
      return resultStr;
    } else if(exp instanceof AST.dataOp) {
      AST.dataOp dataOp = (AST.dataOp)exp;
      String num1 = genExpCode(dataOp.e1,indents);
      String num2 = genExpCode(dataOp.e2,indents);
      AST.dataType t1 = dataOp.e1.type;
      AST.dataType t2 = dataOp.e2.type;
      if(t1.sameAs(t2)) {
        if(t1.baseType == AST.dataType.MATRIX) {
          if(dataOp.op.equals("+")) {
            return "mm_add("+num1+","+num2+")";
          } else if(dataOp.op.equals("-")) {
            return "mm_sub("+num1+","+num2+")";
          } else if(dataOp.op.equals("*")) {
            return "mm_mul("+num1+","+num2+")";
          } else {
            assert(false);
            return null;
          }
        } else {
          return "("+num1+dataOp.op+num2+")";
        }
      } else {
        assert(false);
        return null;
      }
    } else if(exp instanceof AST.dataSize) {
      return "len("+genExpCode(((AST.dataSize)exp).coll,indents)+")";
    } else if(exp instanceof AST.funcCall) {
      AST.funcCall funcCall = (AST.funcCall)exp;
      int numParams = funcCall.params.size();
      List<String> paramStrs = new ArrayList<String>();
      for(int i=0; i<numParams; ++i){
        AST.ASTNode param = funcCall.params.get(i);
        if(param instanceof AST.dataExp) {
          paramStrs.add(genExpCode((AST.dataExp)param,indents));
        } else {
          assert(false);
        }
      }
      String resultStr = funcCall.funcName+"(";
      for(int i=0; i<numParams; ++i){
        resultStr += paramStrs.get(i);
        if(i<numParams-1)
          resultStr += ",";
      }
      resultStr += ")";
      return resultStr;
    } else {
      assert(false);
      return null;
    }
  }

  /* Generate curve fit function expression assuming the expression is not at the top level */

  public String fitFuncGen(AST.dataExp exp, List<String> vars) {
    return fitFuncGen(exp,vars,false);
  }

  /* Generate curve fit function expression for given dataExp
     Maintains a list of variables used in the expression (vars)
     topExpression indicates if the expression for which code is being generated is the top expression
     Also keeps track of the number of temporary variables created
  */

  public String fitFuncGen(AST.dataExp exp, List<String> vars, boolean topExpression) {
    if(exp instanceof AST.realConst) {
      if(topExpression) {
        int temp = tempCount;
        tempCount += 1;
        return "p"+Integer.toString(temp);
      } else {
        return ((AST.realConst)exp).val;
      }
    } else if(exp instanceof AST.varId) {
      String name = ((AST.varId)exp).name;
      int index = vars.indexOf(name);
      if(index==-1) {
        index = vars.size();
        vars.add(name);
      }
      int temp = tempCount;
      tempCount += 2;
      return "(p"+Integer.toString(temp)+"*Cfg["+Integer.toString(index)+"]+p"+Integer.toString(temp+1)+")";
    } else if(exp instanceof AST.dataOp) {
      AST.dataOp dataOp = (AST.dataOp)exp;
      String op = dataOp.op;
      if(op.equals("**") && (dataOp.e2 instanceof AST.realConst)) {
        String expStr = ((AST.realConst)dataOp.e2).val;
        int exponent = -1;
        try{exponent=Integer.parseInt(expStr);}catch(NumberFormatException e){exponent=-1;}
        if(exponent==0) {
          return "1";
        } else if(exponent==1) {
          return fitFuncGen(dataOp.e1,vars);
        } else if(exponent>0) {
          String op1 = fitFuncGen(dataOp.e1,vars);
          int temp = tempCount;
          tempCount += exponent+1;
          String result = "(p"+Integer.toString(temp);
          for(int i=exponent; i>0; --i){
            temp++;
            result = result+"+p"+Integer.toString(temp)+"*"+op1+"**"+Integer.toString(i);
          }
          return result+")";
        }
      }
      String op1 = fitFuncGen(dataOp.e1,vars);
      String op2 = fitFuncGen(dataOp.e2,vars);
      if(op.equals("+") || op.equals("-")) {
        return "("+op1+op+op2+")";
      } else if(op.equals("*") || op.equals("/")) {
        int temp = tempCount;
        tempCount += 1;
        return "("+op1+op+op2+"+p"+Integer.toString(temp)+")";
      } else if(op.equals("**")) {
        int temp = tempCount;
        tempCount += 2;
        return "(("+op1+"**"+op2+")*p"+Integer.toString(temp)+"+p"+Integer.toString(temp+1)+")";
      } else {
        assert(false);
        return null;
      }
    } else if(exp instanceof AST.dataSize) {
      String name = ((AST.varId)(((AST.dataSize)exp).coll)).name;
      int index = vars.indexOf(name);
      if(index==-1) {
        index = vars.size();
        vars.add(name);
      }
      int temp = tempCount;
      tempCount += 2;
      return "(p"+Integer.toString(temp)+"*len(Cfg["+Integer.toString(index)+"])+p"+Integer.toString(temp+1)+")";
    } else if(exp instanceof AST.funcCall) {
      AST.funcCall funcCall = (AST.funcCall)exp;
      int numParams = funcCall.params.size();
      List<String> paramStrs = new ArrayList<String>();
      for(int i=0; i<numParams; ++i){
        AST.ASTNode param = funcCall.params.get(i);
        if(param instanceof AST.dataExp) {
          paramStrs.add(fitFuncGen((AST.dataExp)param,vars));
        } else {
          assert(false);
        }
      }
      String resultStr = funcCall.funcName+"(";
      for(int i=0; i<numParams; ++i){
        resultStr += paramStrs.get(i);
        if(i<numParams-1)
          resultStr += ",";
      }
      resultStr += ")";
      int temp = tempCount;
      tempCount += 2;
      return "("+resultStr+"*p"+Integer.toString(temp)+"+p"+Integer.toString(temp+1)+")";
    } else {
      assert(false);
      return null;
    }
  }

  /* Initialize class and declare some variables that are always present */

  public CodeGen(AST.spec s) {
    spec = s;
    specType = spec.specType();
    declaredVars = new HashSet<String>();
    declaredVars.add("Config");
    declaredVars.add("Output");
    if(specType == AST.SpecType.RUNS || specType == AST.SpecType.INPUTS) {
      declaredVars.add("Runs");
    }
    if(specType == AST.SpecType.INPUTS) {
      declaredVars.add("Inputs");
    } else {
      declaredVars.add("Input");
    }
  }

  /* Generate all necessary functions
     Generates a per run function for per run checkers
     Generates a per input function for per input checkers
     Generates a final function to dump time and memory usage data
     Generates aggregators for time, space, and accuracy data
  */

  public void generate() {
    switch(specType) {
      case NONE:
        System.err.println("Error: spec is empty or contains unimplemented elements.");
        return;
      case INPUTS:
        System.out.println("def perConfigFunc(Config, Runs, Inputs, Output):\n  Output = Output['acc']");
        break;
      case RUNS:
        System.out.println("def perInpFunc(Config, Input, Runs, Output):\n  Output = Output['acc']");
        break;
      case ITEMS:
        System.out.println("def perRunFunc(Config, Input, Output):\n  Output = Output['acc']");
        break;
      case PERF:
        break;
      case CONFLICT:
      default:
        System.err.println("Error: conflicting probability types in spec.");
        return;
    }
    if(specType != AST.SpecType.PERF) {
      tempCount = 0;
      String specResult = genExpCode(spec.exp,1);
      if(specResult.length()>3){
        String resultTemp = "t"+(tempCount++);
        System.out.println("  "+resultTemp+" = "+specResult);
        specResult = resultTemp;
      }
      System.out.println("  if not "+specResult+":");
      System.out.println("    print('Checker detected a possible error')");
      System.out.println("  return "+specResult);
    }
    System.out.println("\ndef inpAgg(agg,run,output):\n  if agg==None:\n    agg = {'acc':[], 'time':0, 'space':0}");
    if(specType == AST.SpecType.RUNS || specType == AST.SpecType.INPUTS)
      System.out.println("  agg['acc'].append(output['acc'])");
    System.out.println("  agg['time'] = (agg['time']*run + output['time'])/(run+1)\n  agg['space'] = (agg['space']*run + output['space'])/(run+1)\n  return agg");
    System.out.println("\ndef cfgAgg(agg,input,inpAgg):\n  if agg==None:\n    agg = {'acc':[], 'time':0, 'space':0}");
    if(specType == AST.SpecType.INPUTS)
      System.out.println("  agg['acc'] += inpAgg['acc']");
    System.out.println("  agg['time'] = (agg['time']*input + inpAgg['time'])/(input+1)\n  agg['space'] = (agg['space']*input + inpAgg['space'])/(input+1)\n  return agg");
    System.out.println("\ndef finalFunc(paramNames, outputs, runs, inputs):");
    System.out.println("  times = {k:v['time'] for k, v in outputs.items()}");
    System.out.println("  AxProf.dumpObtainedData(times,'outputs/%FILENAME%-timeData.txt',paramNames,dataName='time')");
    System.out.println("  spaces = {k:v['space'] for k, v in outputs.items()}");
    System.out.println("  AxProf.dumpObtainedData(spaces,'outputs/%FILENAME%-spaceData.txt',paramNames,dataName='space')");
    if(spec.timeExp!=null) {
      List<String> timeFuncVars = new ArrayList<String>();
      tempCount = 0;
      String funcBody = fitFuncGen(spec.timeExp,timeFuncVars,true);
      System.out.print("\n  def timeFitFunc(Cfg");
      for(int i=0; i<tempCount; ++i)
        System.out.print(",p"+Integer.toString(i));
      System.out.println("):\n    return "+funcBody+"\n");
      System.out.print("  try:\n    popt, rsqd = AxProf.fitFuncToData(times,timeFitFunc,[");
      for(int i=0; i<timeFuncVars.size(); ++i) {
        if(i>0)
          System.out.print(",");
        System.out.print("'"+timeFuncVars.get(i)+"'");
      }
      System.out.println("],paramNames)");
      System.out.println("    print('Time usage:\\nOptimal curve fit parameters:',popt,'\\nR^2 metric:',rsqd)");
      //Future work: fully autmoatic visualization generation
      //System.out.println("  AxProf.visualizeOutput(times,'outputs/%FILENAME%-timeData.png',paramNames,/*tbd*/,dataName='time')");
      System.out.println("  except (RuntimeError, TypeError):\n    print('Unable to find optimal curve fit parameters for time data')");
    }
    if(spec.spaceExp!=null) {
      List<String> spaceFuncVars = new ArrayList<String>();
      tempCount = 0;
      String funcBody = fitFuncGen(spec.spaceExp,spaceFuncVars,true);
      System.out.print("\n  def spaceFitFunc(Cfg");
      for(int i=0; i<tempCount; ++i)
        System.out.print(",p"+Integer.toString(i));
      System.out.println("):\n    return "+funcBody+"\n");
      System.out.print("  try:\n    popt, rsqd = AxProf.fitFuncToData(spaces,spaceFitFunc,[");
      for(int i=0; i<spaceFuncVars.size(); ++i) {
        if(i>0)
          System.out.print(",");
        System.out.print("'"+spaceFuncVars.get(i)+"'");
      }
      System.out.println("],paramNames)");
      System.out.println("    print('space usage:\\nOptimal curve fit parameters:',popt,'\\nR^2 metric:',rsqd)");
      //Future work: fully autmoatic visualization generation
      //System.out.println("  AxProf.visualizeOutput(spaces,'outputs/%FILENAME%-spaceData.png',paramNames,/*tbd*/,dataName='space')");
      System.out.println("  except (RuntimeError, TypeError):\n    print('Unable to find optimal curve fit parameters for space data')");
    }
  }
}
