package ssw.mj.impl;

import ssw.mj.Parser;
import ssw.mj.Scanner;
import ssw.mj.Token.Kind;

import static ssw.mj.Token.Kind.eof;

public final class ParserImpl extends Parser {

    // TODO Exercise 3 - 6: implementation of parser
    public ParserImpl
    (Scanner scanner) {
        super(scanner);

    }

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
        if (sym == expected) scan();    // recognized, so read on
        else error("TODO");         // TODO
    }

    private void error (String msg) {
        System.out.println("line " + la.line + ", col " + la.col + ": " + msg);
        System.exit(1);     // TODO
    }

    // Program = "program" ident { ConstDecl | VarDecl | ClassDecl } "{" {MethodDecl} "}".
    private void Program(){
        //TODO
    }

    // ConstDecl = "final" Type ident "=" ( number | charConst ) ";".
    private void ConstDecl(){
        //TODO
    }

    // VarDecl = Type ident { "," ident } ";".
    private void VarDecl(){
        //TODO
    }

    // ClassDecl = "class" ident "{" { VarDecl } "}".
    private void MethodDecl(){
        //TODO
    }

    // MethodDecl = ( Type | "void" ) ident "(" [ FormPars ] ")" { VarDecl } Block.
    private void FormPars(){
        //TODO
    }

    // FormPars = Type ident { "," Type ident } [ ppperiod ].
    private void Type(){
        //TODO
    }

    // Block = "{" { Statement } "}".
    private void Block(){
        //TODO
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
        //TODO
    }

    // Assignop = "=" | "+=" | "-=" | "*=" | "/=" | "%=".
    private void Assignop(){
        //TODO
    }

    // ActPars = "(" [ Expr { "," Expr } ] [ VarArgs​] ")".
    private void ActPars(){
        //TODO
    }

    // VarArgs = "#" number [ Expr { "," Expr } ].
    private void VarArgs(){
        //TODO
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
        //TODO
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
