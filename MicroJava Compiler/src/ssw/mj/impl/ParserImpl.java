package ssw.mj.impl;

import ssw.mj.Errors;
import ssw.mj.Parser;
import ssw.mj.Scanner;
import ssw.mj.Token.Kind;
import ssw.mj.codegen.Code;
import ssw.mj.codegen.Operand;
import ssw.mj.symtab.Obj;
import ssw.mj.symtab.Struct;
import ssw.mj.symtab.Tab;

import java.util.EnumSet;

import static ssw.mj.Errors.Message.*;
import static ssw.mj.Token.Kind.*;

public final class ParserImpl extends Parser {

    // TODO Exercise 3 - 6: implementation of parser
    public ParserImpl
    (Scanner scanner) {
        super(scanner);
    }

    // first Kinds of a grammar
    private final EnumSet<Kind> firstOfAssignop = EnumSet.of(assign, plusas, minusas, timesas, slashas, remas);
    private final EnumSet<Kind> firstOfExpr =EnumSet.of(minus, ident, number, charConst, new_, lpar);
    private final EnumSet<Kind> firstOfRelop =EnumSet.of(eql, neq, gtr, geq, lss, leq);
    private final EnumSet<Kind> firstOfAddop =EnumSet.of(plus,minus);
    private final EnumSet<Kind> firstOfMulop =EnumSet.of(times, slash, rem);

    // recovery sets for error handling
    private final EnumSet<Kind> recoverStat = EnumSet.of(if_, while_, break_, return_, read, print, rbrace, semicolon, eof);
    private final EnumSet<Kind> recoverDecl = EnumSet.of(final_, class_, lbrace, rbrace, eof);
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
        tab.insert(Obj.Kind.Var, t.str, type);
        while (sym == comma){
            scan();
            check(ident);
            tab.insert(Obj.Kind.Var, t.str, type);
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
        }

        check(ident);
        String methodName = t.str;
        Obj meth = tab.insert(Obj.Kind.Meth, methodName, type);
        meth.adr = code.pc;
        check(lpar);
        tab.openScope();

        if(sym == ident){
            FormPars(meth);
        }
        meth.nPars = tab.curScope.nVars();
        check(rpar);

        // Error Case for main
        if ("main".equals(methodName) && meth.name != null) {
            if(meth.nPars != 0){
                error(MAIN_WITH_PARAMS);
            }
            if(meth.type != Tab.noType){
                error(MAIN_NOT_VOID);
            }
        }

        while (sym == ident){
            VarDecl();
        }

        if(tab.curScope.nVars() > MAX_LOCALS) {
            error(Errors.Message.TOO_MANY_LOCALS);
        }
        Block();
        meth.locals = tab.curScope.locals();
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

        if (o.kind == null){
            error(NOT_FOUND, t.str);
        } else if (o.kind != Obj.Kind.Type) {
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
    private void Block(){
        check(lbrace);
        // only Enter if there is no current error case active
        if(successfulScans > RESET_VAL) {
            while (sym != eof && sym != rbrace) {
                Statement();
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
    private void Statement(){
        switch(sym){

            case ident:
                Designator();
                if(sym == pplus || sym == mminus){
                    scan();
                }else if(firstOfAssignop.contains(sym)){
                    Assignop();
                    Expr();
                }else if(sym == lpar){
                    ActPars();
                }else{
                    error(DESIGN_FOLLOW);
                }
                check(semicolon);
                break;

            case if_:
                scan();
                check(lpar);
                Condition();
                check(rpar);
                Statement();
                if(sym == else_){
                    scan();
                    Statement();
                }
                break;

            case while_:
                scan();
                check(lpar);
                Condition();
                check(rpar);
                Statement();
                break;

            case break_:
                scan();
                check(semicolon);
                break;

            case return_:
                scan();
                if(firstOfExpr.contains(sym)){
                    Expr();
                }
                check(semicolon);
                break;

            case read:
                scan();
                check(lpar);
                Designator();
                check(rpar);
                check(semicolon);
                break;

            case print:
                scan();
                check(lpar);
                Expr();
                if(sym == comma){
                    scan();
                    check(number);
                }
                check(rpar);
                check(semicolon);
                break;

            case lbrace:
                Block();
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
    private void Assignop(){
        if(firstOfAssignop.contains(sym)){
            scan();
        }else{
            error(ASSIGN_OP);
        }
    }

    // ActPars = "(" [ Expr { "," Expr } ] [ VarArgs ] ")".
    private void ActPars(){
        check(lpar);
        if(firstOfExpr.contains(sym)){
            Expr();
            for(;;){
                if(sym == comma){
                    scan();
                    Expr();
                }else{
                    break;
                }
            }
        }
        if(sym == hash){
            VarArgs();
        }
        check(rpar);
    }

    // VarArgs = "#" number [ Expr { "," Expr } ].
    private void VarArgs(){
        check(hash);
        check(number);
        if(firstOfExpr.contains(sym)){
            Expr();
            for(;;){
                if(sym == comma){
                    scan();
                    Expr();
                }else{
                    break;
                }
            }
        }

    }

    // Condition = CondTerm { "||" CondTerm }.
    private void Condition() {
        CondTerm();
        for (;;) {
            if (sym == or) {
                scan();
                CondTerm();
            } else {
                break;
            }
        }
    }

    // CondTerm = CondFact { "&&" CondFact }.
    private void CondTerm(){
        CondFact();
        for (;;) {
            if (sym == and) {
                scan();
                CondFact();
            } else {
                break;
            }
        }
    }

    // CondFact = Expr Relop Expr.
    private void CondFact(){
        Expr();
        Relop();
        Expr();
    }

    // Relop = "==" | "!=" | ">" | ">=" | "<" | "<=".
    private void Relop(){
        if(firstOfRelop.contains(sym)){
            scan();
        }else{
            error(REL_OP);
        }
    }

    // Expr = [ "–" ] Term { Addop Term }.
    private Operand Expr(){
        Operand opA;
        if(sym == minus){
            scan();
            opA = Term();
            if(opA.type != Tab.intType){ error(NO_INT_OP); }
            if (opA.kind == Operand.Kind.Con) {
                code.load(opA);
                code.put(Code.OpCode.neg);
            }
        }else{
            opA = Term();
            if (opA.type.kind != Struct.Kind.Int){ error(NO_INT_OP); }
        }
        for(;;){
            if(firstOfAddop.contains(sym)){
                code.load(opA);
                Code.OpCode addOp = Addop();
                Operand opB = Term();
                if(opB.type.kind != Struct.Kind.Int){ error(NO_INT_OP); }
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
        if(opA.type != Tab.intType){ error(NO_INT_OP); }
        for(;;){
            if(firstOfMulop.contains(sym)){
                Code.OpCode opCode = Mulop();
                code.load(opA);
                Operand opB = Factor();
                if(opB.type != Tab.intType){ error(NO_INT_OP); }
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
        Operand opA = null;
            switch (sym){
                case ident:
                    opA = Designator();
                    if(sym == lpar){
                        opA.type = opA.obj.type;
                        opA.kind = Operand.Kind.Stack;
                        ActPars();
                    }
                    break;
                case number:
                    scan();
                    opA = new Operand(t.val);
                    code.load(opA);
                    break;
                case charConst:
                    scan();
                    opA = new Operand(t.val);
                    opA.type = Tab.charType;
                    code.load(opA);
                    break;
                case new_:
                    scan();
                    check(ident);
                    Obj obj = tab.find(t.str);
                    if(obj.type.kind == Struct.Kind.None){ error(NO_TYPE); }
                    StructImpl struct = obj.type;
                    if(sym == lbrack){
                        scan();
                        Operand opB = Expr();
                        if (opB.type.kind != Struct.Kind.Int){ error(NO_INT_OP); }
                        code.load(opB);
                        code.put(Code.OpCode.newarray);
                        if(obj.type.kind == Struct.Kind.Char){
                            code.put(0);
                        }else{
                            code.put(1);
                        }
                        opA = new Operand(new StructImpl(struct));
                        opA.val = opB.val;
                        check(rbrack);
                    }
                    break;
                case lpar:
                    scan();
                    opA = Expr();
                    check(rpar);
                    break;
                default:
                    error(INVALID_FACT);
            }
            return opA; 
    }

    // Designator = ident { "." ident | "[" Expr "]" }.
    private Operand Designator() {
        Operand x = new Operand(tab.find(t.str), this);
        check(ident);
        for(;;){
            if(sym == period){
                if(x.type.kind != Struct.Kind.Class){ error(NO_CLASS_TYPE); }
                scan();
                code.load(x);
                check(ident);
                Obj obj = tab.findField(t.str, x.type);
                x.kind = Operand.Kind.Fld; x.type = obj.type;
                x.adr = obj.adr;
            }else if(sym == lbrack){
                if(x.type.kind != Struct.Kind.Arr){ error(NO_ARRAY);}
                scan();
                code.load(x);
                Operand y = Expr();
                if(y.type != Tab.intType){ error(NO_INT_OP); }
                code.load(y);
                x.kind = Operand.Kind.Elem;
                x.type = x.type.elemType;
                check(rbrack);
            }else{
                break;
            }

        }
        return x;
    }

    // Addop = "+" | "–".
    private Code.OpCode Addop() {
        if(firstOfAddop.contains(sym)){     // (plus,minus)
            scan();
            if(sym == plus){
                return Code.OpCode.add;
            }else{
                return Code.OpCode.sub;
            }
        }else{
            error(ADD_OP);
            return Code.OpCode.nop;
        }
    }

    // Mulop = "*" | "/" | "%".
    private Code.OpCode Mulop(){
        if(firstOfMulop.contains(sym)){     // (times, slash, rem)
            switch (sym) {
                case times: scan(); return Code.OpCode.mul;
                case slash: scan(); return Code.OpCode.div;
                default:    scan(); return Code.OpCode.rem;
            }
        }else{
            error(MUL_OP);
            return Code.OpCode.nop;
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
