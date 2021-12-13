package ssw.mj.impl;

import ssw.mj.Parser;
import ssw.mj.codegen.Code;
import ssw.mj.codegen.Operand;
import ssw.mj.symtab.Tab;

import static ssw.mj.Errors.Message.NO_VAL;
import static ssw.mj.Errors.Message.NO_VAR;
import static ssw.mj.codegen.Code.OpCode.*;


public final class CodeImpl extends Code {

    public CodeImpl(Parser p) {
        super(p);
    }

    // TODO Exercise 5 - 6: implementation of code generation

    // source: exercise slides
    void load(Operand operand) {
        loadOp(operand);
        operand.kind = Operand.Kind.Stack; // remember that value is now loaded
    }

    void loadOp(Operand operand) {
        if (operand == null) return;
        switch (operand.kind) {
            case Con:
                loadConst(operand.val);
                break;
            case Local:
                switch (operand.adr) {
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
                        put(operand.adr);
                        break;
                }
                break;
            case Static:
                put(OpCode.getstatic);
                put2(operand.adr);
                break;
            case Stack:
                break; // nothing to do (already loaded)
            case Fld:
                put(OpCode.getfield);
                put2(operand.adr);
                break;
            case Elem:
                if (operand.type == Tab.charType) {
                    put(OpCode.baload);
                } else {
                    put(OpCode.aload);
                }
                break;
            default:
                parser.error(NO_VAL);
        }
    }

    void loadConst(int val) {
        switch (val) {
            case -1:
                put(const_m1);
                break;
            case 0:
                put(const_0);
                break;
            case 1:
                put(const_1);
                break;
            case 2:
                put(const_2);
                break;
            case 3:
                put(const_3);
                break;
            case 4:
                put(const_4);
                break;
            case 5:
                put(const_5);
                break;
            default:
                put(const_);
                put4(val);
        }
    }

    void assign(Operand target, Operand source) {
        loadOp(source);
        store(target);
    }

    void store(Operand operand) {
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
    void duplicate(Operand operand) {
        if (operand.kind == Operand.Kind.Fld) {
            put(OpCode.dup);
        } else {
            put(OpCode.dup2);
        }
    }
}
