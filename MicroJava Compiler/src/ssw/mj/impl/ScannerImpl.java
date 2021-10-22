package ssw.mj.impl;

import ssw.mj.Scanner;
import ssw.mj.Token;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;

import static ssw.mj.Errors.Message.*;
import static ssw.mj.Token.Kind.*;

public final class ScannerImpl extends Scanner {

    private static final HashMap<String, Token.Kind> LABELS;

    static {
        LABELS = new HashMap<>();
        LABELS.put("break", break_);
        LABELS.put("class", class_);
        LABELS.put("else", else_);
        LABELS.put("final", final_);
        LABELS.put("if", if_);
        LABELS.put("new", new_);
        LABELS.put("print", print);
        LABELS.put("program", program);
        LABELS.put("read", read);
        LABELS.put("return", return_);
        LABELS.put("void", void_);
        LABELS.put("while", while_);
        LABELS.put("end of file", eof);
    }

    // Exercise 2: implementation of scanner
    public ScannerImpl(Reader r) {
        super(r);
        line = 1;
        col = 0;
        nextCh();
    }

    /**
     * Returns next token. To be used by parser. */
    @Override
    public Token next() {
        // skip blanks, tabs, eols
        while (Character.isWhitespace(ch)) {
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
                // hash
                case '#':
                    t.kind = hash;
                    nextCh();
                    break;
                // End of File
                case EOF:
                    t.kind = eof;
                    break;
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
                // slash
                case '/':
                    nextCh();
                    if (ch == '=') {
                        t.kind = slashas;
                        nextCh();
                    } else if (ch == '*') {
                        skipComment(t);
                        t = next();     // overwrite the token
                    } else {
                        t.kind = slash;
                    }
                    break;
                // ' charConst
                case '\'':
                    readCharConst(t);
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
                        error(t, INVALID_CHAR, '!');
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
                        nextCh();
                    } else {
                        error(t, INVALID_CHAR, '&');
                    }
                    break;
                //OR
                case '|':
                    nextCh();
                    if (ch == '|') {
                        t.kind = or;
                        nextCh();
                    } else {
                        error(t, INVALID_CHAR, '|');
                    }
                    break;
                // semicolon
                case ';':
                    nextCh();
                    t.kind = semicolon;
                    break;
                // comma
                case ',':
                    t.kind = comma;
                    nextCh();
                    break;
                // dot(s)
                case '.':
                    nextCh();
                    if (ch == '.') {
                        nextCh();
                        if (ch == '.') {
                            t.kind = ppperiod;
                            nextCh();
                        } else {
                            t.kind = pperiod;
                        }
                    } else {
                        t.kind = period;
                    }
                    break;
                //round bracket
                case '(':
                    t.kind = lpar;
                    nextCh();
                    break;
                case ')':
                    t.kind = rpar;
                    nextCh();
                    break;
                //square bracket
                case '[':
                    t.kind = lbrack;
                    nextCh();
                    break;
                case ']':
                    t.kind = rbrack;
                    nextCh();
                    break;
                //curved bracket
                case '{':
                    t.kind = lbrace;
                    nextCh();
                    break;
                case '}':
                    t.kind = rbrace;
                    nextCh();
                    break;
                default:
                    t.kind = none;
                    error(t, INVALID_CHAR, ch);
                    nextCh();
                    break;
            }
        }
        return t;
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
        try {
            ch = (char) in.read();
            col++;
            if (ch == '\n') {
                line++;
                col = 0;
            }
        } catch (IOException e) {
            ch = EOF;
        }
    }

    /*
    Read an identifier
    Recognizes keywords (HashMap String -->Token.Kind) */
    void readName(Token t) {
        t.kind = ident;
        StringBuilder sb = new StringBuilder();

        // read in all following chars (numbers or letter)
        // Todo underscore
        while (isNumber(ch) || isLetter(ch) || ch == '_') {
            sb.append(ch);
            nextCh();
        }
        t.str = sb.toString();
        // check if the string is a special label otherwise it's an ident
        if (LABELS.containsKey(t.str)) {
            t.kind = LABELS.get(t.str);
        }
    }

    //Reads a number
    void readNumber(Token t) {
        t.kind = number;
        StringBuilder sb = new StringBuilder();
        while (isNumber(ch)) {
            sb.append(ch);
            nextCh();
        }
        t.str = sb.toString();
        try {
            t.val = Integer.parseInt(t.str);
        } catch (NumberFormatException e) {
            error(t, BIG_NUM, t.str);
        }
    }

    //Reads a character constant
    // Todo complete mess
    void readCharConst(Token t){
        t.kind = charConst;
        nextCh(); // jump to '

        switch (ch) {
            case EOF:
                error(t, EOF_IN_CHAR);
                return;
            // Illegal linefeed"
            case LF:
            case '\r':
                error(t, ILLEGAL_LINE_END);
                nextCh();
                return;
            case '\'':
                error(t, EMPTY_CHARCONST);
                nextCh();
                return;
            case '\\':  // #1
                nextCh();
                switch (ch) {
                    case 'n':
                        t.val = '\n';
                        break;
                    case 'r':
                        t.val = '\r';
                        break;
                    case '\'':
                    case '\\':
                        t.val = ch;
                    case LF:
                    case '\r':
                        error(t, MISSING_QUOTE);
                        nextCh();
                        return;
                    default:
                        error(t, UNDEFINED_ESCAPE, ch);
                        break;
                }
                break;
            default:
                t.val = ch;
                nextCh();
                if (ch == '\'') {
                    nextCh();
                } else {
                    error(t, MISSING_QUOTE);
                }
                break;
        }

    }

    /*
    Skips nested comments
    ch then contains the character after the comment */
    void skipComment(Token t) {
        int counter = 1;
        nextCh();
        while (counter > 0) {

            if (ch == '/') {
                nextCh();
                if (ch == '*') {
                    counter++;
                    nextCh();
                }
            } else if (ch == '*') {
                nextCh();
                if (ch == '/') {
                    counter--;
                    nextCh();
                }
            } else if (ch == EOF) {
                error(t, EOF_IN_COMMENT);
                break;

            } else {
                nextCh();
            }
        }


    }
}

// #1
//    nextCh();
//                if (ch == 'n') {
//                        t.val = '\n';
//                        } else if (ch == 'r') {
//                        t.val = '\r';
//                        } else if (ch == '\'' || ch == '\\') {
//                        t.val = ch;
//                        } else if (ch == LF || ch == '\r') {
//                        error(t, ILLEGAL_LINE_END);
//                        nextCh();
//                        return;
//                        } else {
//                        error(t, UNDEFINED_ESCAPE, ch);
//                        }
//                        break;