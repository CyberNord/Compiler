package ssw.mj.impl;

import ssw.mj.Errors;
import ssw.mj.Parser;
import ssw.mj.Scanner;
import ssw.mj.Token.Kind;
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
    private final EnumSet<Kind> firstOfAssignop = EnumSet.of(assign, plusas, minusas, timesas, slashas);
    private final EnumSet<Kind> firstOfExpr =EnumSet.of(minus, ident, number, charConst, new_, lpar);
    private final EnumSet<Kind> firstOfRelop =EnumSet.of(eql, neq, gtr, geq, lss, leq);
    private final EnumSet<Kind> firstOfAddop =EnumSet.of(plus,minus);
    private final EnumSet<Kind> firstOfMulop =EnumSet.of(times, slash, rem);

    // recovery sets for error handling
    private final EnumSet<Kind> recoverStat = EnumSet.of(if_, while_, break_, return_, read, print, rbrace, semicolon, eof);
    private final EnumSet<Kind> recoverDecl = EnumSet.of(final_, ident, class_, lbrace, eof);
    private final EnumSet<Kind> recoverMeth = EnumSet.of(void_, ident, eof);

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
            // TODO Is this even right?
            if(t.str != null) {     // cancel multiple commas
                tab.insert(Obj.Kind.Var, t.str, type);
            }else{
                tab.insert(Obj.Kind.Var, "", type);
            }
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
    private Obj MethodDecl(){
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
            FormPars();
        }
        meth.nPars = tab.curScope.nVars();
        check(rpar);
        while (sym == ident){
            VarDecl();
        }
        if(tab.curScope.nVars() > MAX_LOCALS) {
            error(Errors.Message.TOO_MANY_LOCALS);
        }
        Block();
        meth.locals = tab.curScope.locals();
        tab.closeScope();
        return meth;
    }

    // FormPars = Type ident { "," Type ident } [ ppperiod ].
    private void FormPars(){
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
            if(curr != null) {
                curr.hasVarArg = true;
                curr.type = new StructImpl(curr.type);
            }
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
                if (successfulScans == RESET_VAL) {
                    recoverStat();
                }
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
    private void Expr(){
        if(sym == minus){
            scan();
        }
        Term();
        for(;;){
            if(firstOfAddop.contains(sym)){
                Addop();
                Term();
            }else{
                break;
            }
        }
    }

    // Term = Factor { Mulop Factor }.
    private void Term(){
        Factor();
        for(;;){
            if(firstOfMulop.contains(sym)){
                Mulop();
                Factor();
            }else{
                break;
            }
        }

    }

    // Factor = Designator [ ActPars ]
    //      | number
    //      | charConst
    //      | "new" ident [ "[" Expr "]" ]
    //| "(" Expr ")".
    private void Factor(){
            switch (sym){
                case ident:
                    Designator();
                    if(sym == lpar){
                        ActPars();
                    }
                    break;
                case number: case charConst:
                    scan();
                    break;
                case new_:
                    scan();
                    check(ident);
                    if(sym == lbrack){
                        scan();
                        Expr();
                        check(rbrack);
                    }
                    break;
                case lpar:
                    scan();
                    Expr();
                    check(rpar);
                    break;
                default:
                    error(INVALID_FACT);
            }
    }

    // Designator = ident { "." ident | "[" Expr "]" }.
    private void Designator() {
        check(ident);
        for(;;){
            if(sym == period){
                scan();
                check(ident);
            }else if(sym == lbrack){
                scan();
                Expr();
                check(rbrack);
            }else{
                break;
            }
        }
    }

    // Addop = "+" | "–".
    private void Addop() {
        if(firstOfAddop.contains(sym)){
            scan();
        }else{
            error(ADD_OP);
        }
    }

    // Mulop = "*" | "/" | "%".
    private void Mulop(){
        if(firstOfMulop.contains(sym)){
            scan();
        }else{
            error(MUL_OP);
        }
    }

    // scan until next ConstDecl/VarDecl/ClassDecl
    private void recoverDecl(){
        while(!recoverDecl.contains(sym)){
            scan();
        }
        successfulScans = RESET_VAL;
    }

    // scan until next MethodDecl
    private void recoverMethodDecl(){
        while(!recoverMeth.contains(sym)){
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
