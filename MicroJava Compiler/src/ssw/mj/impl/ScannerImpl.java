package ssw.mj.impl;

import ssw.mj.Errors;
import ssw.mj.Scanner;
import ssw.mj.Token;

import java.io.Reader;

import static ssw.mj.Token.Kind.*;

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
        if (isLetter(ch)) {
            t.kind = ident;
            readName(t);
        } else if (isNumber(ch)) {
            t.kind = number;
            readNumber(t);
        } else {
            switch (ch) {
                // minus
                case '+':
                    nextCh();
                    if (ch == '+') {
                        t.kind = pplus;
                        nextCh();
                    } else if (ch == '=') {
                        t.kind = plusas;
                        nextCh();
                    } else {
                        t.kind = plus;
                    }
                    break;
                // plus
                case '-':
                    nextCh();
                    if (ch == '-') {
                        t.kind = mminus;
                        nextCh();
                    } else if (ch == '=') {
                        t.kind = minusas;
                        nextCh();
                    } else {
                        t.kind = minus;
                    }
                    break;
                // multiplier
                case '*':
                    nextCh();
                    if (ch == '=') {
                        t.kind = timesas;
                        nextCh();
                    } else {
                        t.kind = times;
                    }
                    break;
                // divide
                case '/':
                    nextCh();
                    if (ch == '=') {
                        t.kind = slashas;
                        nextCh();
                    } else {
                        t.kind = slash;
                    }
                    break;
                // percent
                case '%':
                    nextCh();
                    if (ch == '=') {
                        t.kind = remas;
                        nextCh();
                    } else {
                        t.kind = rem;
                    }
                    break;
                // assign
                case '=':
                    nextCh();
                    if (ch == '=') {
                        t.kind = eql;
                        nextCh();
                    } else {
                        t.kind = assign;
                    }
                    break;
                // exclamation mark
                case '!':
                    nextCh();
                    if (ch == '=') {
                        t.kind = neq;
                        nextCh();
                    } else {
                        error(t, Errors.Message.INVALID_CHAR, ch);
                    }
                    break;
                // lesser
                case '<':
                    nextCh();
                    if (ch == '=') {
                        t.kind = leq;
                        nextCh();
                    } else {
                        t.kind = lss;
                    }
                    break;
                // greater
                case '>':
                    nextCh();
                    if (ch == '=') {
                        t.kind = geq;
                        nextCh();
                    } else {
                        t.kind = gtr;
                    }
                    break;
                //AND
                case '&':
                    nextCh();
                    if (ch == '&') {
                        t.kind = and;
                    } else {
                        error(t, Errors.Message.INVALID_CHAR, ch);
                    }
                    //OR
                case '|':
                    nextCh();
                    if (ch == '|') {
                        t.kind = or;
                    } else {
                        error(t, Errors.Message.INVALID_CHAR, ch);
                    }
            }
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
