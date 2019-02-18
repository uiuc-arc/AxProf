import java.util.List;
import java.util.ArrayList;

/* AST class
   Consists of subclasses for each type of AST node
   Also provides:
   1) Helper functions for obtaining the specification type
   2) Helper functions for comparing types
*/

public class AST {

  /* Specification types
     1) NONE - no accuracy or performance specification detected
     2) INPUTS - contains probability or expectation over inputs in specification
     3) RUNS - contains probability or expectation over runs in specification
     4) ITEMS - contains probability or expectation over items in specification
     5) PERF - contains only performance specification
     6) CONFLICT - contains conflicting probability or expectation specifications
  */
  public enum SpecType { NONE, INPUTS, RUNS, ITEMS, PERF, CONFLICT }

  private static SpecType mixSpecTypes(ASTNode n1, ASTNode n2) {
    SpecType s1, s2;
    s1 = n1.specType();
    s2 = n2.specType();
    if(s1 == SpecType.NONE) {
      return s2;
    } else if(s2 == SpecType.NONE || s1 == s2) {
      return s1;
    } else {
      return SpecType.CONFLICT;
    }
  }

  public static class ASTNode {
    public SpecType specType() {
      return SpecType.NONE;
    }
  }

  public static class dataType extends ASTNode {
    public static final int NONE=-1;
    public static final int REAL=0;
    public static final int MATRIX=1;
    public static final int LIST=2;
    public static final int MAP=3;
    public int baseType;
    public dataType kType, vType;
    public dataType(int t) {
      baseType = t;
    }
    public dataType(int t, dataType kt) {
      baseType = t;
      kType = kt;
    }
    public dataType(int t, dataType kt, dataType vt) {
      baseType = t;
      kType = kt;
      vType = vt;
    }
    public boolean sameAs(dataType other) {
      if(other==null)
        return false;
      if(baseType!=other.baseType)
        return false;
      if(kType==null && other.kType!=null)
        return false;
      if(kType!=null && !kType.sameAs(other.kType))
        return false;
      if(vType==null && other.vType!=null)
        return false;
      if(vType!=null && !vType.sameAs(other.vType))
        return false;
      return true;
    }
  }

  public static class typeDecl extends ASTNode {
    public String name;
    public dataType type;
    public typeDecl(String n, dataType t) {
      name = n;
      type = t;
    }
  }

  public static class spec extends ASTNode {
    public dataExp timeExp, spaceExp;
    public boolExp exp;
    public List<typeDecl> typeDecls;
    public spec() {}
    public void addDecls(List<typeDecl> tds) { typeDecls = tds; }
    public void addTime(dataExp te) { timeExp = te; }
    public void addSpace(dataExp se) { spaceExp = se; }
    public void addAcc(boolExp be) { exp = be; }
    public SpecType specType() {
      if(exp!=null)
        return exp.specType();
      else if(timeExp!=null || spaceExp!=null)
        return SpecType.PERF;
      else
        return SpecType.NONE;
    }
  }

  public static class boolExp extends ASTNode {
    public boolExp() {
    }
  }

  public static class forall extends boolExp {
    public List<range> ranges;
    public boolExp exp;
    public forall(List<range> r, boolExp e) {
      ranges = r;
      exp = e;
    }
    public SpecType specType() {
      return exp.specType();
    }
  }

  public static class let extends boolExp {
    public String name;
    public dataExp value;
    public boolExp exp;
    public let(String n, dataExp v, boolExp e) {
      name = n;
      value = v;
      exp = e;
    }
    public SpecType specType() {
      return exp.specType();
    }
  }

  public static class isInData extends boolExp {
    public dataExp item, data;
    public isInData(dataExp i, dataExp d) {
      item = i;
      data = d;
    }
  }

  public static class approxEq extends boolExp {
    public dataExp e1, e2;
    public approxEq(dataExp _e1, dataExp _e2) {
      e1 = _e1;
      e2 = _e2;
    }
    public SpecType specType() {
      if(mixSpecTypes(e1,e2) != SpecType.NONE)
        return SpecType.CONFLICT;
      else
        return SpecType.RUNS;
    }
  }

  public static class comparison extends boolExp {
    public dataExp e1, e2;
    public String op;
    public comparison(dataExp _e1, String _op, dataExp _e2) {
      e1 = _e1;
      op = _op;
      e2 = _e2;
    }
    public SpecType specType() {
      return mixSpecTypes(e1,e2);
    }
  }

  public static class boolAndOr extends boolExp {
    public boolExp e1, e2;
    public String op;
    public boolAndOr(boolExp _e1, String _op, boolExp _e2) {
      e1 = _e1;
      op = _op;
      e2 = _e2;
    }
    public SpecType specType() {
      return mixSpecTypes(e1,e2);
    }
  }

  public static class boolNot extends boolExp {
    public boolExp exp;
    public boolNot(boolExp e) {
      exp = e;
    }
    public SpecType specType() {
      return exp.specType();
    }
  }

  public static class dataExp extends ASTNode {
    public dataType type;
    public dataExp() {
    }
  }

  public static class realConst extends dataExp {
    public String val;
    public realConst(String v) {
      val = v;
    }
  }

  public static class probabilityInputs extends dataExp {
    public boolExp exp;
    public probabilityInputs(boolExp e) {
      exp = e;
    }
    public SpecType specType() {
      return SpecType.INPUTS;
    }
  }

  public static class probabilityRuns extends dataExp {
    public boolExp exp;
    public probabilityRuns(boolExp e) {
      exp = e;
    }
    public SpecType specType() {
      return SpecType.RUNS;
    }
  }

  public static class probabilityItems extends dataExp {
    public List<range> ranges;
    public boolExp exp;
    public probabilityItems(List<range> r, boolExp e) {
      ranges = r;
      exp = e;
    }
    public SpecType specType() {
      return SpecType.ITEMS;
    }
  }

  public static class expectationInputs extends dataExp {
    public dataExp exp;
    public expectationInputs(dataExp e) {
      exp = e;
    }
    public SpecType specType() {
      return SpecType.INPUTS;
    }
  }

  public static class expectationRuns extends dataExp {
    public dataExp exp;
    public expectationRuns(dataExp e) {
      exp = e;
    }
    public SpecType specType() {
      return SpecType.RUNS;
    }
  }

  public static class expectationItems extends dataExp {
    public List<range> ranges;
    public dataExp exp;
    public expectationItems(List<range> r, dataExp e) {
      ranges = r;
      exp = e;
    }
    public SpecType specType() {
      return SpecType.ITEMS;
    }
  }

  public static class lookup extends dataExp {
    public dataExp coll, key;
    public lookup(dataExp c, dataExp k) {
      coll = c;
      key = k;
    }
  }

  public static class varId extends dataExp {
    public String name;
    public varId(String n) {
      name = n;
    }
  }

  public static class dataExpList extends dataExp {
    public List<dataExp> list;
    public dataExpList(List<dataExp> l) {
      list = l;
    }
  }

  public static class dataOp extends dataExp {
    public dataExp e1, e2;
    public String op;
    public dataOp(dataExp _e1, String _op, dataExp _e2) {
      e1 = _e1;
      op = _op;
      e2 = _e2;
    }
    public SpecType specType() {
      return mixSpecTypes(e1,e2);
    }
  }

  public static class dataSize extends dataExp {
    public dataExp coll;
    public dataSize(dataExp c) {
      coll = c;
    }
  }

  public static class funcCall extends dataExp {
    public String funcName;
    public List<ASTNode> params;
    public funcCall(String fn, List<ASTNode> p) {
      funcName = fn;
      params = p;
    }
  }

  public static class range extends ASTNode {
    public static final int NONE=-1;
    public static final int DIRECT=0;
    public static final int UNIQUE=1;
    public static final int INDEX=2;
    public dataExp item, coll;
    public int type;
    public range(dataExp i, dataExp c, int t) {
      item = i;
      coll = c;
      type = t;
    }
  }

}
