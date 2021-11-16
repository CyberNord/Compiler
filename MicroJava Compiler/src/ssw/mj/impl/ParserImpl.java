package ssw.mj.impl;

import ssw.mj.Parser;
import ssw.mj.Scanner;
import ssw.mj.Token.Kind;

import java.util.EnumSet;

import static ssw.mj.Errors.Message.*;
import static ssw.mj.Token.Kind.*;

public final class ParserImpl extends Parser {

    // TODO Exercise 3 - 6: implementation of parser
    public ParserImpl
    (Scanner scanner) {
        super(scanner);
    }

    private static final int MIN_ERR_DIST = 3;
    private final EnumSet<Kind> firstOfAssignop = EnumSet.of(assign, plusas, minusas, timesas, slashas);
    private final EnumSet<Kind> firstOfExpr =EnumSet.of(minus, ident, number, charConst, new_, lpar);
    private final EnumSet<Kind> firstOfRelop =EnumSet.of(eql, neq, gtr, geq, lss, leq);
    private final EnumSet<Kind> firstOfAddop =EnumSet.of(plus,minus);
    private final EnumSet<Kind> firstOfMulop =EnumSet.of(times, slash, rem);
    // first Kinds of a grammar
    private final EnumSet<Kind> firstOfStatement = EnumSet.of(ident, if_, while_, break_, return_, read, print, lbrace, semicolon);
    // recovery sets for error handling
    private final EnumSet<Kind> recoverStat = EnumSet.of(ident, if_, while_, break_, return_, read, print, lbrace, semicolon, eof); // TODO ident/lbrace catching symbol ?
    private final EnumSet<Kind> recoverDecl = EnumSet.of(final_, class_, eof); // TODO VarDecl catching symbol (ident)
    private final EnumSet<Kind> recoverMeth = EnumSet.of(void_, ident, eof); // TODO ident catching symbol ?
    private int successfulScans = 0;

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
        while(true){
            if(sym == final_){
                ConstDecl();
            }else if(sym == ident){
                VarDecl();
            }else if( sym == class_){
                ClassDecl();
            }else{
                // error(INVALID_DECL);
                // recoverDecl();
                break;
            }
        }

//        if(tab.curScope.nVars() > MAX_GLOBALS) {
//            error(TOO_MANY_GLOBALS);
//        }

        check(lbrace);
        while(sym == ident || sym == void_) {
            MethodDecl();
        }
        check(rbrace);
    }

    // ConstDecl = "final" Type ident "=" ( number | charConst ) ";".
    private void ConstDecl(){
        check(final_);
        Type();
        check(ident);
        check(assign);
        if(sym == number || sym == charConst){
            scan();
        }else{
            error(CONST_DECL);
        }
        check(semicolon);
    }

    // VarDecl = Type ident { "," ident } ";".
    private void VarDecl(){
        Type();
        check(ident);
        while (sym == comma){
            scan();
            check(ident);
        }
        check(semicolon);
    }

    // ClassDecl = "class" ident "{" { VarDecl } "}".
    private void ClassDecl(){
        check(class_);
        check(ident);
        check(lbrace);
        while (sym == ident){
            VarDecl();
        }
        check(rbrace);
    }

    // MethodDecl = ( Type | "void" ) ident "(" [ FormPars ] ")" { VarDecl } Block.
    private void MethodDecl(){
        if( sym == ident){
            Type();
        }else if(sym == void_){
            scan();
        }else{
            error(METH_DECL);
        }
        check(ident);
        check(lpar);
        if(sym == ident){
            FormPars();
        }
        check(rpar);
        while (sym == ident){
            VarDecl();
        }
        Block();
    }

    // FormPars = Type ident { "," Type ident } [ ppperiod ].
    private void FormPars(){
        Type();
        check(ident);
        while (sym == comma) {
            scan();
            Type();
            check(ident);
        }
        if(sym == ppperiod){
            scan();
        }
    }

    // Type = ident [ "[" "]" ].
    private void Type(){
        check(ident);
        if(sym == lbrack){
            scan();
            check(rbrack);
        }
    }

    // Block = "{" { Statement } "}".
    private void Block(){
        check(lbrace);
        while(firstOfStatement.contains(sym)){
            Statement();
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

    // recover at beginning of Declaration
    private void recoverDecl(){
        // TODO
    }

    // recover at beginning of Method
    private void recoverMethodDecl(){
        // TODO
    }

    // recover at beginning of Statement
    private void recoverStat(){
        // TODO
    }

}
