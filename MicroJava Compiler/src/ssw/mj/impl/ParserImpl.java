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

    private final EnumSet<Kind> firstOfStatement = EnumSet.of(ident, if_, while_, break_, return_, read, print, lbrace, semicolon);
    private final EnumSet<Kind> firstOfAssignop = EnumSet.of(assign, plusas, minusas, timesas, slashas);
    private final EnumSet<Kind> firstOfFactor =EnumSet.of(ident, number, charConst, new_, lpar);
    private final EnumSet<Kind> firstOfExpr =EnumSet.of(minus, ident, number, charConst, new_, lpar);

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
    }

    private void check (Kind expected) {
        if (sym == expected) scan();
        else error(TOKEN_EXPECTED, expected);
    }

    private void error (String msg) {
        System.out.println("line " + la.line + ", col " + la.col + ": " + msg);
        System.exit(1);     // TODO
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
                break;
            }
        }
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
        }
        check(semicolon);
    }

    // VarDecl = Type ident { "," ident } ";".
    private void VarDecl(){
        Type();
        check(ident);
        while (sym != semicolon){
            check(comma);
            check(ident);
        }
        scan();
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
        check(lpar);
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
    private void Condition(){
        //TODO
    }

    // CondTerm = CondFact { "&&" CondFact }.
    private void CondTerm(){
        //TODO
    }

    // CondFact = Expr Relop Expr.
    private void CondFact(){
        //TODO
    }

    // Relop = "==" | "!=" | ">" | ">=" | "<" | "<=".
    private void Relop(){
        //TODO
    }

    // Expr = [ "–" ] Term { Addop Term }.
    private void Expr(){
        //TODO
    }

    // Term = Factor { Mulop Factor }.
    private void Factor(){
        //TODO
    }

    // Factor = Designator [ ActPars ]
    //      | number
    //      | charConst
    //      | "new" ident [ "[" Expr "]" ]
    //| "(" Expr ")".
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
    //TODO
    }

    // Mulop = "*" | "/" | "%".
    private void Mulop(){
        //TODO
    }

}
