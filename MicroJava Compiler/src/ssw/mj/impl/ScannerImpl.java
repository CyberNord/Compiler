package ssw.mj.impl;

import ssw.mj.Scanner;
import ssw.mj.Token;

import java.io.Reader;

import static ssw.mj.Token.Kind.none;

public final class ScannerImpl extends Scanner {

    // Exercise 2: implementation of scanner

    // ToDo: not sure here
    public ScannerImpl(Reader r) {
        super(r);
        in = r;
        line = 1;
        col = 0;
        nextCh();
    }

    /**
     * Returns next token. To be used by parser. */
    @Override
    public Token next() {

        // skip blanks, tabs, eols
        while (ch <= ' ') {
            nextCh();
        }

        Token t = new Token(none, line, col);
        switch (ch) {

        }


        return null;
    }

    // checks if char is a-z or A-Z
    boolean isLetter(char c) {
        return 'a' <= c && c <= 'z' || 'A' <= c && c <= 'Z';
    }

    //checks if char is 0-9
    boolean isNumber(char c) {
        return '0' <= c && c <= '9';
    }

    /*
    Reads the next input character and stores it in the ch field
    or EOF at end of file
    Recognizes line breaks: LF and CR LF
    Keeps the position in the fields line and col  */
    void nextCh() {
        //TODO
    }

    /*
    Read an identifier
    Recognizes keywords (HashMap String -->Token.Kind) */
    void readName(Token t){
        //TODO
    }

    //Reads a number
    void readNumber(Token t){
        //TODO
    }

    //Reads a character constant
    void readCharConst(Token t){
        //TODO
    }

    /*
    Skips nested comments
    ch then contains the character after the comment */
    void skipComment(Token t){
        //TODO
    }
}
