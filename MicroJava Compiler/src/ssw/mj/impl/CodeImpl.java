package ssw.mj.impl;

import ssw.mj.Parser;
import ssw.mj.codegen.Code;
import ssw.mj.codegen.Label;
import ssw.mj.codegen.Operand;
import ssw.mj.symtab.Struct;
import ssw.mj.symtab.Tab;

import static ssw.mj.Errors.Message.NO_VAL;
import static ssw.mj.Errors.Message.NO_VAR;
import static ssw.mj.codegen.Code.OpCode.*;


public final class CodeImpl extends Code {

    public CodeImpl(Parser p) {
        super(p);
    }

    // DONE Exercise: implementation of code generation

    // source: exercise slides
    public void load(Operand opA) {
        loadOp(opA);
        opA.kind = Operand.Kind.Stack; // remember that value is now loaded
    }

    public void loadOp(Operand opA) {
        if (opA == null) return;
        switch (opA.kind) {
            case Con:
                loadConst(opA.val);
                break;
            case Local:
                switch (opA.adr) {
                    case 0:
                        put(OpCode.load_0);
                        break;
                    case 1:
                        put(OpCode.load_1);
                        break;
                    case 2:
                        put(OpCode.load_2);
                        break;
                    case 3:
                        put(OpCode.load_3);
                        break;
                    default:
                        put(OpCode.load);
                        put(opA.adr);
                        break;
                }
                break;
            case Static:
                put(OpCode.getstatic);
                put2(opA.adr);
                break;
            case Stack:
                break; // nothing to do (already loaded)
            case Fld:
                put(OpCode.getfield);
                put2(opA.adr);
                break;
            case Elem:
                if (opA.type == Tab.charType) {
                    put(OpCode.baload);
                } else {
                    put(OpCode.aload);
                }
                break;
            default:
                parser.error(NO_VAL);
        }
    }

    public void loadConst(int val) {
        switch (val) {
            case -1:    put(const_m1);  break;
            case 0:     put(const_0);   break;
            case 1:     put(const_1);   break;
            case 2:     put(const_2);   break;
            case 3:     put(const_3);   break;
            case 4:     put(const_4);   break;
            case 5:     put(const_5);   break;
            default:    put(const_);    put4(val);
        }
    }

    public void assign(Operand target, Operand source) {
        loadOp(source);
        store(target);
    }

    public void store(Operand operand) {
        switch (operand.kind) {
            case Local:
                switch (operand.adr) {
                    case 0:
                        put(OpCode.store_0);
                        break;
                    case 1:
                        put(OpCode.store_1);
                        break;
                    case 2:
                        put(OpCode.store_2);
                        break;
                    case 3:
                        put(OpCode.store_3);
                        break;
                    default:
                        put(OpCode.store);
                        put(operand.adr);
                        break;
                }
                break;
            case Static:
                put(OpCode.putstatic);
                put2(operand.adr);
                break;
            case Fld:
                put(OpCode.putfield);
                put2(operand.adr);
                break;
            case Elem:
                if (operand.type == Tab.charType) {
                    put(OpCode.bastore);
                } else {
                    put(OpCode.astore);
                }
                break;
            default:
                parser.error(NO_VAR);
        }
    }

    // increments Operand val by value
    public void increment(Operand operand, int value) {
        if (operand.kind == Operand.Kind.Local) {
            put(inc);
            put(operand.adr);
            put(value);
        } else {
            if (operand.kind == Operand.Kind.Fld || operand.kind == Operand.Kind.Elem) {
                duplicate(operand);
            }
            loadOp(operand);
            loadOp(new Operand(value));
            put(add);
            store(operand);
        }
    }

    // creates a duplicate
    public void duplicate(Operand operand) {
        if (operand.kind == Operand.Kind.Fld) {
            put(OpCode.dup);
        } else {
            put(OpCode.dup2);
        }
    }

    // call method
    public void call(Operand operand){
        // TODO change call()
        if (operand.obj == parser.tab.lenObj) {
            put(OpCode.arraylength);
        } else if(operand.obj != parser.tab.chrObj && operand.obj != parser.tab.ordObj) {
            put(OpCode.call);
            put2(operand.adr - (pc - 1));
        }
        operand.kind = Operand.Kind.Stack;
    }

    // normal Jump
    public void jump(Label label){
        put(jmp);
        label.put();
    }

    // True Jump
    public void tJump (Operand operand) {
        condJump(operand.op);  // jeq, jne, jlt, jle, ...
        operand.tLabel.put();
    }

    // False Jump
    public void fJump (Operand operand) {
        condJump(CompOp.invert(operand.op));  // jne, jeq, jge, jgt, ...
        operand.fLabel.put();
    }

    // execute conditional jump operation
    private void condJump(CompOp compOp){
        switch (compOp) {
            case eq: put(jeq); break;
            case ne: put(jne); break;
            case le: put(jle); break;
            case lt: put(jlt); break;
            case ge: put(jge); break;
            case gt: put(jgt); break;
        }
    }

    // (add, sub, mul, div, rem)
    public void doBasicArithmetic(Operand opA, OpCode opCodeAss, Operand opB) {
        load(opB);
        put(opCodeAss);
        store(opA);
    }

    // read
    public void doReadOp(Operand readOp){
        if(readOp.type.kind == Struct.Kind.Int){
            put(OpCode.read);
            assign(readOp,new Operand(Tab.intType));
        }else if(readOp.type.kind == Struct.Kind.Char){
            put(OpCode.bread);
            assign(readOp,new Operand(Tab.charType));
        }
    }

    //print
    public void doPrintOp(Operand printOp, int width) {
        load(printOp);
        loadConst(width);
    }

    // array
    public Operand getArray(Operand opB, StructImpl objType) {
        load(opB);
        put(OpCode.newarray);
        if(objType == Tab.charType){
            put(0);
        }else{
            put(1);
        }
        Operand opA = new Operand(new StructImpl(objType));
        opA.val = opB.val;
        return opA;
    }

    //exit
    public void exitDefault(){
        put(exit);
        put(return_);
    }

    //exit trap
    public void exitTrap(){
        put(OpCode.trap);
        put(1);
    }
}
