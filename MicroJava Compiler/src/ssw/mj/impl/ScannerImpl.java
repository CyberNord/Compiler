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

        // skip blanks, tabs, eol
        while (Character.isWhitespace(ch)) {
            nextCh();
        }

        Token t = new Token(none, line, col);

            switch (ch) {
                case 'a': case 'b': case 'c': case 'd': case 'e': case 'f': case 'g': case 'h': case 'i': case 'j': case 'k': case 'l': case 'm': case 'n':
                case 'o': case 'p': case 'q': case 'r': case 's': case 't': case 'u': case 'v': case 'w': case 'x': case 'y': case 'z': case 'A': case 'B':
                case 'C': case 'D': case 'E': case 'F': case 'G': case 'H': case 'I': case 'J': case 'K': case 'L': case 'M': case 'N': case 'O': case 'P':
                case 'Q': case 'R': case 'S': case 'T': case 'U': case 'V': case 'W': case 'X': case 'Y': case 'Z':
                    t.kind = ident;
                    readName(t);
                    break;
                case '0': case '1': case '2': case '3': case '4': case '5': case '6': case '7': case '8': case '9':
                    t.kind = number;
                    readNumber(t);
                    break;
                // plus
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
                // minus
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
                // hash
                case '#':
                    t.kind = hash;
                    nextCh();
                    break;
                // End of File
                case EOF:
                    t.kind = eof;
                    break;
                default:
                    t.kind = none;
                    error(t, INVALID_CHAR, ch);
                    nextCh();
                    break;
            }
        return t;
    }

    /*
    Reads the next input character and stores it in the ch field
    or EOF at end of file
    Recognizes line breaks: LF and CR LF
    Keeps the position in the fields line and col  */
    private void nextCh() {
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
    private void readName(Token t) {
        t.kind = ident;
        StringBuilder sb = new StringBuilder();

        // read in all following chars (numbers or letter)
        while (Character.isDigit(ch) || Character.isLetter(ch) || ch == '_') {
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
    private void readNumber(Token t) {
        t.kind = number;
        StringBuilder sb = new StringBuilder();
        while (Character.isDigit(ch)) {
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
    private void readCharConst(Token t){
        // initialize token & Jump to next sign after '
        t.kind = charConst;
        nextCh();

        switch (ch) {
            case EOF:
                error(t, EOF_IN_CHAR);
                return;

            // Illegal linefeed" or Escape
            case LF:
            case '\r':
                error(t, ILLEGAL_LINE_END);
                nextCh();
                return;
            case '\'':        // next sign = '
                error(t, EMPTY_CHARCONST);
                nextCh();
                return;

            case '\\':       // next sign = \
                nextCh();

                switch (ch) {
                    case EOF:
                        error(t, EOF_IN_CHAR);
                        return;

                    // Illegal linefeed" or Escape
                    case LF:
                    case '\r':
                        error(t, ILLEGAL_LINE_END);
                        nextCh();
                        return;

                    // Case '
                    case '\'':
                        nextCh();
                        if (ch == '\'') {
                            t.val = '\'';
                            nextCh();
                        } else {
                            error(t, MISSING_QUOTE);
                        }
                        break;

                    // Case \
                    case '\\':
                        t.val = '\\';
                        missingQuoteCheck(t);
                        break;

                    // Legal LF or \r
                    case 'n':
                        t.val = '\n';
                        missingQuoteCheck(t);
                        break;
                    case 'r':
                        t.val = '\r';
                        missingQuoteCheck(t);
                        break;
                    default:
                        error(t, UNDEFINED_ESCAPE, ch);
                        missingQuoteCheck(t);
                        break;
                }
                break;

            // General case if there is any sign under ''
            default:
                t.val = ch;
                missingQuoteCheck(t);
                break;
        }
    }

    private void missingQuoteCheck(Token t) {
        nextCh();
        if (ch != '\'') {
            error(t, MISSING_QUOTE);
        } else {
            nextCh();
        }
    }

    /*
    Skips nested comments
    ch then contains the character after the comment */
    private void skipComment(Token t) {
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