package ssw.mj.impl;

import ssw.mj.Errors;
import ssw.mj.Parser;
import ssw.mj.Scanner;
import ssw.mj.Token.Kind;
import ssw.mj.codegen.Code;
import ssw.mj.codegen.Code.OpCode;
import ssw.mj.codegen.Label;
import ssw.mj.codegen.Operand;
import ssw.mj.symtab.Obj;
import ssw.mj.symtab.Struct;
import ssw.mj.symtab.Tab;

import java.util.EnumSet;
import java.util.Iterator;

import static ssw.mj.Errors.Message.*;
import static ssw.mj.Token.Kind.*;

public final class ParserImpl extends Parser {

    // DONE Exercise 3 - 6: implementation of parser
    public ParserImpl
    (Scanner scanner) {
        super(scanner);
    }

    // first Kinds of a grammar
    private final EnumSet<Kind> firstOfAssignop = EnumSet.of(assign, plusas, minusas, timesas, slashas, remas);
    private final EnumSet<Kind> firstOfExpr =EnumSet.of(minus, ident, number, charConst, new_, lpar);
    private final EnumSet<Kind> firstOfRelop =EnumSet.of(eql, neq, gtr, geq, lss, leq);
    private final EnumSet<Kind> firstOfAddop =EnumSet.of(plus,minus);
    private final EnumSet<Kind> firstOfQuick =EnumSet.of(pplus,mminus);
    private final EnumSet<Kind> firstOfMulop =EnumSet.of(times, slash, rem);

    // recovery sets for error handling
    private final EnumSet<Kind> recoverStat = EnumSet.of(if_, while_, break_, return_, read, print, rbrace, semicolon, eof);
    private final EnumSet<Kind> recoverDecl = EnumSet.of(final_, class_, lbrace, /* rbrace, */ eof, ident);
    private final EnumSet<Kind> recoverMeth = EnumSet.of(void_, rbrace, eof);

    private int successfulScans = 3;
    private static final int MIN_ERR_DIST = 3;
    private static final int RESET_VAL = 0;

    /**
     * Starts the analysis.
     */
    @Override
    public void parse() {
        scan();
        Program();
        check(eof);
    }

    private void scan() {
        t = la;
        la = scanner.next();
        sym = la.kind;
        successfulScans++;
    }

    private void check (Kind expected) {
        if (sym == expected) {
            scan();
        } else error(TOKEN_EXPECTED, expected);
    }

    // Program = "program" ident { ConstDecl | VarDecl | ClassDecl } "{" {MethodDecl} "}".
    private void Program(){
        check(program);
        check(ident);

        // Create program in universe and open program scope (0)
        Obj program = tab.insert(Obj.Kind.Prog, t.str, Tab.noType);
        tab.openScope();

        while(sym != eof && sym != lbrace){
            if(sym == final_){
                ConstDecl();
            }else if(sym == ident){
                VarDecl();
            }else if( sym == class_){
                ClassDecl();
            }else{
                error(INVALID_DECL);
                recoverDecl();
            }
        }

        if( tab.curScope.nVars() > MAX_GLOBALS){
            error(TOO_MANY_GLOBALS);
        }

        check(lbrace);
        while(sym != rbrace && sym != eof) {
            // error handling & recovery are inside MethodDecl()
            MethodDecl();
        }
        check(rbrace);

        final Obj main = tab.curScope.findLocal("main");
        if (main == null || main.kind != Obj.Kind.Meth) {
            error(METH_NOT_FOUND, "main");
        }

        program.locals = tab.curScope.locals();
        tab.closeScope();
    }

    // ConstDecl = "final" Type ident "=" ( number | charConst ) ";".
    private void ConstDecl(){
        check(final_);
        StructImpl type = Type();
        check(ident);
        String typeName = t.str;
        check(assign);

        if( type == null
                || sym == charConst && type.kind != Struct.Kind.Char
                || sym == number && type.kind != Struct.Kind.Int){
            error(CONST_TYPE);
        } else if(sym == charConst ||sym == number ){
            scan();
            Obj o = tab.insert(Obj.Kind.Con, typeName, type);
            o.val = t.val;
        }else{
            error(CONST_DECL);
        }
        check(semicolon);
    }

    // VarDecl = Type ident { "," ident } ";".
    private void VarDecl(){
        StructImpl type = Type();
        check(ident);
        if(type != Tab.noType) {tab.insert(Obj.Kind.Var, t.str, type);}     // only insert if type is valid
        while (sym == comma){
            scan();
            check(ident);
            if(type != Tab.noType) {tab.insert(Obj.Kind.Var, t.str, type);}
        }
        check(semicolon);
    }

    // ClassDecl = "class" ident "{" { VarDecl } "}".
    private void ClassDecl(){
        check(class_);
        check(ident);
        Obj clazz = tab.insert(Obj.Kind.Type, t.str, new StructImpl(StructImpl.Kind.Class));

        check(lbrace);
        tab.openScope();

        while (sym == ident){
            VarDecl();
        }

        if (tab.curScope.nVars() > MAX_FIELDS) {
            error(TOO_MANY_FIELDS);
        }

        clazz.type.fields = tab.curScope.locals();
        tab.closeScope();
        check(rbrace);
    }

    // MethodDecl = ( Type | "void" ) ident "(" [ FormPars ] ")" { VarDecl } Block.
    private void MethodDecl(){
        StructImpl type = Tab.noType;
        if( sym == ident){
            type = Type();
        }else if(sym == void_){
            scan();
        }else{
            error(METH_DECL);
            recoverMethodDecl();
            type = tab.noObj.type;
        }

        check(ident);
        String methodName = t.str;
        Obj currMeth = tab.insert(Obj.Kind.Meth, methodName, type);
        currMeth.adr = code.pc;
        check(lpar);
        tab.openScope();

        if(sym == ident) {
            FormPars(currMeth);
        }

        currMeth.nPars = tab.curScope.nVars();
        check(rpar);

        // Error Case for main
        if ("main".equals(methodName) && currMeth.name != null) {
            if(currMeth.nPars != 0){
                error(MAIN_WITH_PARAMS);
            }
            if(currMeth.type != Tab.noType){
                error(MAIN_NOT_VOID);
            }
            code.mainpc = code.pc;
        }

        while (sym == ident){
            VarDecl();
        }
        if(tab.curScope.nVars() > MAX_LOCALS) {
            error(Errors.Message.TOO_MANY_LOCALS);
        }
        code.dataSize = tab.curScope.nVars();

        if (currMeth.kind == Obj.Kind.Meth) {
            currMeth.adr = code.pc;
            code.put(OpCode.enter);
            code.put(currMeth.nPars);
            code.put(tab.curScope.nVars());
        }

        Block(null, currMeth);

        currMeth.locals = tab.curScope.locals();

        if (currMeth.type == Tab.noType) {
            code.exitDefault();
        } else { // end of function reached without a return statement
            code.exitTrap();
        }

        tab.closeScope();
    }

    // FormPars = Type ident { "," Type ident } [ ppperiod ].
    private void FormPars(Obj meth){
        Obj curr;
        for (;;) {
            StructImpl type = Type();
            check(ident);
            curr = tab.insert(Obj.Kind.Var, t.str, type);
            if (sym == comma)
                scan();
            else
                break;
        }
        if(sym == ppperiod ){
            curr.hasVarArg = true;
            curr.type = new StructImpl(curr.type);
            meth.hasVarArg = true;
            scan();
        }
    }

    // Type = ident [ "[" "]" ].
    private StructImpl Type(){
        check(ident);
        Obj o = tab.find(t.str);
        if (o.kind != Obj.Kind.Type) {
            error(NO_TYPE);
        }
        StructImpl type = o.type;

        if(sym == lbrack){
            scan();
            check(rbrack);
            return new StructImpl(type);
        }
        return type;
    }

    // Block = "{" { Statement } "}".
    private void Block(Label breakLabel, Obj currMeth){
        check(lbrace);
        // only Enter if there is no current error case active
        if(successfulScans > RESET_VAL) {
            while (sym != eof && sym != rbrace) {
                Statement(breakLabel, currMeth);
                // moved recover inside due advice of tutor
            }
        }
        check(rbrace);
    }

    // Statement = Designator ( Assignop Expr | ActPars | "++" | "--" ) ";"
    //           | "if" "(" Condition ")" Statement [ "else" Statement ]
    //           | "while" "(" Condition ")" Statement
    //           | "break" ";"
    //           | "return" [ Expr ] ";"
    //           | "read" "(" Designator ")" ";"
    //           | "print" "(" Expr [ "," number ] ")" ";"
    //           | Block
    //           | ";".
    private void Statement(Label breakLabel, Obj currMeth){
        Operand opA;
        switch(sym){

            // Designator ( Assignop Expr | ActPars | "++" | "--" ) ";"
            case ident:
                opA = Designator();

                // Assignop Expr
                if(firstOfAssignop.contains(sym)){     // (assign, plusas, minusas, timesas, slashas, remas)
                    OpCode opCodeAss = Assignop();

                    if (opCodeAss != OpCode.store && (opA.kind == Operand.Kind.Meth || opA.kind == Operand.Kind.Cond)) {
                        error(NO_VAR);
                    }

                    // contains check if duplication is even needed
                    if(opCodeAss != OpCode.store ) {
                        if(opA.kind == Operand.Kind.Fld || opA.kind == Operand.Kind.Elem) {
                            code.duplicate(opA);
                        }
                        code.loadOp(opA);
                    }

                    Operand opB = Expr();

                    if(opA.obj != null && opA.obj.kind != Obj.Kind.Var) {error(NO_VAR);}

                    if(opCodeAss == OpCode.store) {
                        if (opB.type.assignableTo(opA.type)) {
                            code.assign(opA, opB);
                        } else {
                            error(INCOMP_TYPES);
                        }
                    }else{
                        if (opA.type != Tab.intType || opB.type != Tab.intType){
                            error(Errors.Message.NO_INT_OP);
                        }
                        code.doBasicArithmetic(opA, opCodeAss, opB);    // (add, sub, mul, div, rem)
                    }

                    // ActPars
                }else if(sym == lpar){
                    ActPars(opA);
                    code.call(opA);


                    // "++" | "--"
                }else if(firstOfQuick.contains(sym)){   // (mminus,pplus)
                    if(opA.type != Tab.intType){error(NO_INT);}
                    if(opA.obj != null && opA.obj.kind != Obj.Kind.Var){error(NO_VAR);}
                    if(sym == mminus){
                        scan();
                        code.increment(opA, -1);
                    }else{
                        scan();
                        code.increment(opA, 1);
                    }

                    // Error in -> Designator ( Assignop Expr | ActPars | "++" | "--" )"
                } else{
                    error(DESIGN_FOLLOW);
                }

                check(semicolon);
                break;

            case if_:
                scan();
                check(lpar);
                opA = Condition();
                code.fJump(opA);
                opA.tLabel.here();
                check(rpar);
                Statement(breakLabel, currMeth);
                if(sym == else_){
                    scan();
                    LabelImpl endIf = new LabelImpl(code);
                    code.jump(endIf);
                    opA.fLabel.here();
                    Statement(breakLabel, currMeth);
                    endIf.here();
                }else{
                    opA.fLabel.here();
                }
                break;

            case while_:
                scan();
                check(lpar);
                LabelImpl top = new LabelImpl(code);
                top.here();
                opA = Condition();
                code.fJump(opA);
                opA.tLabel.here();
                check(rpar);
                Statement(opA.fLabel, currMeth);
                code.jump(top);
                opA.fLabel.here();
                break;

            case break_:
                scan();
                if(breakLabel == null){
                    error(NO_LOOP);
                }else{
                    code.jump(breakLabel);
                }
                check(semicolon);
                break;

            case return_:
                scan();
                if(firstOfExpr.contains(sym)){
                    // void method must not return a value
                    if(currMeth.type == Tab.noType){error(RETURN_VOID);}
                    opA = Expr();       // return value
                    // check for correct return value
                    if(currMeth.type.compatibleWith(opA.type)) {
                        code.load(opA);
                    }else{
                        error(RETURN_TYPE);
                    }
                }else if(currMeth.type != Tab.noType){error( RETURN_NO_VAL);}
                code.exitDefault();
                check(semicolon);
                break;

            case read:
                scan();
                check(lpar);
                Operand readOp = Designator();
                code.doReadOp(readOp);
                if(readOp.type.kind != Struct.Kind.Char && readOp.type.kind != Struct.Kind.Int){
                    error(READ_VALUE);
                }
                check(rpar);
                check(semicolon);
                break;

            case print:
                scan();
                check(lpar);
                Operand printOp = Expr();
                if(printOp.type.kind != Struct.Kind.Char && printOp.type.kind != Struct.Kind.Int ){
                    error(PRINT_VALUE);
                }
                int width = 0;
                if(sym == comma){
                    scan();
                    check(number);
                    width = t.val;
                }
                code.doPrintOp(printOp, width);         // print Operation in CodeImpl
                if(printOp.type.kind == Struct.Kind.Int){
                    code.put(OpCode.print);
                }else if(printOp.type.kind == Struct.Kind.Char){
                    code.put(OpCode.bprint);
                }
                check(rpar);
                check(semicolon);
                break;

            case lbrace:
                Block(breakLabel, currMeth);
                break;

            case semicolon:
                scan();
                break;

            default:
                error(INVALID_STAT);
                recoverStat();
                break;
        }
    }

    // Assignop = "=" | "+=" | "-=" | "*=" | "/=" | "%=".
    private OpCode Assignop(){
        OpCode code;
        if(firstOfAssignop.contains(sym)){  // (assign, plusas, minusas, timesas, slashas, remas)
            switch (sym){
                case assign:    code = OpCode.store;  scan(); break;
                case plusas:    code = OpCode.add;    scan(); break;
                case minusas:   code = OpCode.sub;    scan(); break;
                case timesas:   code = OpCode.mul;    scan(); break;
                case slashas:   code = OpCode.div;    scan(); break;
                default:        code = OpCode.rem;    scan(); break;
            }
        }else{
            code = OpCode.nop;
            error(ASSIGN_OP);
        }
        return code;
    }

     // ActPars = "(" [ Expr { "," Expr } ] [ VarArgs ] ")".
    private void ActPars(Operand opA){
        check(lpar);

        if(opA.kind != Operand.Kind.Meth) {
            error(Errors.Message.NO_METH);
            return;     // exit ActPars
        }

        int idx = 0;
        Iterator<Obj> itr = opA.obj.locals.iterator();

        int opParams = opA.obj.nPars;
        while (firstOfExpr.contains(sym)){
            Operand opEx = Expr();
            Obj par = null;
            if(itr.hasNext()){
                par = itr.next();
            }
            code.load(opEx);

            if (itr.hasNext() || !opA.obj.hasVarArg) {
                if(par != null && !opEx.type.assignableTo(par.type)){
                    error(PARAM_TYPE);
                }
            }
            idx++;

            if(sym == comma){
                scan();
            }else{      // break out of the while
                break;
            }

        }
        boolean hash_ = false;          // use for an error case

        // empty VarArg
        if(sym == rpar && opA.obj.hasVarArg) {
            idx++;
            code.emptyArray(0,opA);
        } else if(sym == hash){    // has Vararg ?
            hash_ = true;
            VarArgs(opA.obj);
            idx++;
        }

        if(sym != rpar) {
            check(rpar);        // will throw an error at this point
        }else {
            if (!opA.obj.hasVarArg && hash_) {
                error(INVALID_VARARG_CALL);
            }else if (idx > opParams) {
                error(MORE_ACTUAL_PARAMS);
            } else if (idx < opParams) {
                error(LESS_ACTUAL_PARAMS);
            }
            check(rpar);
        }

    }

    // VarArgs = "#" number [ Expr { "," Expr } ].
    private void VarArgs(Obj objVar){
        check(hash);
        check(number);

        final int arrSize = t.val;           // vararg size
        Operand varArgOp;
        int idx = 0;                        // how many varargs are parsed
        StructImpl varArgType = objVar.locals.get(objVar.nPars-1).type.elemType;

        code.loadConst(arrSize);
        code.put(OpCode.newarray);
        if(varArgType == Tab.charType){
            code.put(0);
        }else{
            code.put(1);
        }

        if(firstOfExpr.contains(sym)){
            for(;;){

                code.put(OpCode.dup);
                code.loadConst(idx);

                varArgOp = Expr();

                if (varArgType != null && !varArgOp.type.compatibleWith(varArgType)) { error(PARAM_TYPE); }

                code.load(varArgOp);

                if (varArgType == Tab.charType) {
                    code.put(OpCode.bastore);
                } else {
                    code.put(OpCode.astore);
                }
                idx++;
                if(sym == comma){
                    scan();
                }else{
                    break;
                }
            }
            // Size error check for VarArgs happens here
            if(idx > arrSize){error(MORE_ACTUAL_VARARGS);}
            if(idx < arrSize){error(LESS_ACTUAL_VARARGS);}

        }
    }

    // Condition = CondTerm { "||" CondTerm }.
    private Operand Condition() {
        Operand opA = CondTerm();
        for (;;) {
            if (sym == or) {
                code.tJump(opA);
                scan();
                opA.fLabel.here();
                Operand opB = CondTerm();
                opA.fLabel = opB.fLabel;
                opA.op = opB.op;
            } else {
                break;
            }
        }
        return opA;
    }

    // CondTerm = CondFact { "&&" CondFact }.
    private Operand CondTerm(){
        Operand opA = CondFact();
        for (;;) {
            if (sym == and) {
                code.fJump(opA);
                scan();
                Operand opB = CondFact();
                opA.op = opB.op;
            } else {
                break;
            }
        }
        return opA;
    }

    // CondFact = Expr Relop Expr.
    private Operand CondFact(){
        boolean equal = false;
        Operand opA = Expr();
        code.load(opA);
        Code.CompOp compOp = Relop();   // eq, ne, lt, le, gt, ge
        Operand opB = Expr();
        code.load(opB);
        if (!(compOp != Code.CompOp.eq && compOp != Code.CompOp.ne)) equal = true;
        if (!opA.type.compatibleWith(opB.type)) {
            error(INCOMP_TYPES);
        }else if (!equal && (opA.type.kind == Struct.Kind.Arr || opA.type.kind == Struct.Kind.Class)) {
            error(Errors.Message.EQ_CHECK);
        }
        return new Operand(compOp, code);
    }

    // Relop = "==" | "!=" | ">" | ">=" | "<" | "<=".
    private Code.CompOp Relop(){
        if(firstOfRelop.contains(sym)){
            switch (sym){
                case eql: scan(); return Code.CompOp.eq;
                case neq: scan(); return Code.CompOp.ne;
                case gtr: scan(); return Code.CompOp.gt;
                case geq: scan(); return Code.CompOp.ge;
                case lss: scan(); return Code.CompOp.lt;
                case leq: scan(); return Code.CompOp.le;
            }
        }else{
            error(REL_OP);
        }
        return Code.CompOp.eq;
    }

    // Expr = [ "–" ] Term { Addop Term }.
    private Operand Expr(){
        Operand opA;
        if(sym == minus){
            scan();
            opA = Term();
            if(opA.type != Tab.intType){error(NO_INT_OP); }
            if (opA.kind == Operand.Kind.Con) {
                opA.val = -opA.val;
            }else {
                code.load(opA);
                code.put(OpCode.neg);
            }
        }else{
            opA = Term();
        }
        for(;;){
            if(firstOfAddop.contains(sym)){     // (plus, minus)
                code.load(opA);
                OpCode addOp = Addop();
                if(opA.type.kind != Struct.Kind.Int){error(NO_INT_OP);}
                Operand opB = Term();
                if(opB.type.kind != Struct.Kind.Int){error(NO_INT_OP);}
                code.load(opB);
                code.put(addOp);
            }else{
                break;
            }
        }
        return opA;
    }

    // Term = Factor { Mulop Factor }.
    private Operand Term(){
        Operand opA = Factor();
        for(;;){
            if(firstOfMulop.contains(sym)){ // (times, slash, rem)
                OpCode opCode = Mulop();
                code.load(opA);
                Operand opB = Factor();
                if(opB.type != Tab.intType|| opA != null && opA.type.kind != Struct.Kind.Int ){error(NO_INT_OP); }
                code.load(opB);
                code.put(opCode);
            }else{
                break;
            }
        }
        return opA;

    }

    // Factor = Designator [ ActPars ]
    //      | number
    //      | charConst
    //      | "new" ident [ "[" Expr "]" ]
    //| "(" Expr ")".
    private Operand Factor(){
        Operand opA;

            switch (sym){

                case ident:
                    opA = Designator();
                    if(sym == lpar){
                        if (opA.obj.type == Tab.noType){error(INVALID_CALL);}
                        ActPars(opA);
                        code.call(opA);
                        opA.type = opA.obj.type;
                        opA.kind = Operand.Kind.Stack;
                    }
                    break;

                case number:
                    scan();
                    opA = new Operand(t.val);
                    opA.type = Tab.intType;
                    break;

                case charConst:
                    scan();
                    opA = new Operand(t.val);
                    opA.type = Tab.charType;
                    break;

                case new_:
                    scan();
                    check(ident);
                    Obj obj = tab.find(t.str);
                    StructImpl objType = obj.type;
                    if(objType.kind == Struct.Kind.None){ error(NO_TYPE); }
                    if(sym == lbrack){
                        scan();
                        Operand opB = Expr();
                        if (opB.type.kind != Struct.Kind.Int){error(ARRAY_SIZE); }
                        opA = code.getArray(opB, objType );      // current is identified as Array

                        check(rbrack);
                    }else {
                        if(obj.kind != Obj.Kind.Type ||  objType.kind != Struct.Kind.Class){
                            error(Errors.Message.NO_CLASS_TYPE);
                        }else{
                            code.put(OpCode.new_);
                            code.put2(obj.type.nrFields());
                        }
                        opA = new Operand(objType);
                    }
                    break;

                case lpar:
                    scan();
                    opA = Expr();
                    check(rpar);
                    break;

                default:
                    error(INVALID_FACT);
                    opA = new Operand(Tab.noType);
            }
            return opA; 
    }

    // Designator = ident { "." ident | "[" Expr "]" }.
    private Operand Designator() {
        check(ident);
        Operand opA = new Operand(tab.find(t.str), this);
        for(;;){
            if(sym == period){
                if(opA.type.kind != Struct.Kind.Class){ error(NO_CLASS); }
                scan();
                code.load(opA);
                check(ident);
                Obj obj = tab.findField(t.str, opA.type);
                opA.kind = Operand.Kind.Fld;
                opA.type = obj.type;
                opA.adr = obj.adr;
            }else if(sym == lbrack){
                code.load(opA);
                scan();
                Operand opB = Expr();
                if (opA.obj != null || opB.type != Tab.intType) error(ARRAY_INDEX);
                if(opA.type.kind == Struct.Kind.Arr) {
                    code.load(opB);
                    opA.kind = Operand.Kind.Elem;
                    opA.type = opA.type.elemType;
                    check(rbrack);
                }else {
                    error(NO_ARRAY);
                }
            }else{
                break;
            }
        }
        return opA;
    }

    // Addop = "+" | "–".
    private OpCode Addop() {
        if(firstOfAddop.contains(sym)){     // (plus,minus)
            if(sym == plus){
                scan();
                return OpCode.add;
            }else{
                scan();
                return OpCode.sub;
            }
        }else{
            error(ADD_OP);
            return OpCode.nop;
        }
    }

    // Mulop = "*" | "/" | "%".
    private OpCode Mulop(){
        if(firstOfMulop.contains(sym)){     // (times, slash, rem)
            switch (sym) {
                case times: scan(); return OpCode.mul;
                case slash: scan(); return OpCode.div;
                default:    scan(); return OpCode.rem;
            }
        }else{
            error(MUL_OP);
            return OpCode.nop;
        }
    }

    // scan until next ConstDecl/VarDecl/ClassDecl
    private void recoverDecl(){
        while(!recoverDecl.contains(sym) || (sym == ident && tab.find(sym.label())==null)){
            scan();
        }
        successfulScans = RESET_VAL;
    }

    // scan until next MethodDecl
    private void recoverMethodDecl(){
        while(!recoverMeth.contains(sym) || (sym == ident && tab.find(sym.label())==null)){
            scan();
        }
        successfulScans = RESET_VAL;
    }

    // scan until next Statement sub condition
    private void recoverStat(){
        while(!recoverStat.contains(sym)){
            scan();
        }
        successfulScans = RESET_VAL;
    }

    // no Panic Mode
    @Override
    public void error(Errors.Message msg, Object... msgParams) {
        if (successfulScans >= MIN_ERR_DIST) {
            scanner.errors.error(la.line, la.col, msg, msgParams);
        }
        successfulScans = RESET_VAL;
    }
}
