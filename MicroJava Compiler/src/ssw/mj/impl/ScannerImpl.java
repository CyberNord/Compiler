package ssw.mj.impl;

import ssw.mj.Scanner;
import ssw.mj.Token;
import java.io.Reader;

public final class ScannerImpl extends Scanner {

    // TODO Exercise 2: implementation of scanner

    public ScannerImpl(Reader r) {
        super(r);
        // TODO
    }

    /**
     * Returns next token. To be used by parser. */
    @Override
    public Token next() {
        // TODO
    	return null;
    }

    /*
    Reads the next input character and stores it in the ch field
    or EOF at end of file
    Recognizes line breaks: LF and CR LF
    Keeps the position in the fields line and col  */
    void nextCh(){
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
    –Überliest geschachtelte Kommentare
    –ch enthält anschließend das Zeichen nach dem Kommentar
     */
    void skipComment(Token t){
        //TODO
    }

}
