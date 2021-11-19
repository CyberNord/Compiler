package ssw.mj.impl;

import ssw.mj.Parser;
import ssw.mj.symtab.Scope;
import ssw.mj.symtab.Tab;

public final class TabImpl extends Tab {

    // TODO Exercise 4: implementation of symbol table

    private Scope curScope;
    private int level;

    /**
     * Set up "universe" (= predefined names).
     */
    public TabImpl(Parser p) {
        super(p);
    }

    // defines a new scope and increases level
    private void openScope(){
        //TODO
    }
    // deletes curScope and decreases level
    private void  closeScope(){
        //TODO
    }

    // creates a symbol list object, sets its attributes and adds it
    // in the curScope in the symbol list.
    private void insert(){
        //TODO
    }

    // searches for a name
    // starting in the current to the outermost area of ​​validity
    private void find(){
        // TODO
    }

    // searches by variable
    // name a field in a class, the struct of which is given in the interface
    private void findField(){
        // TODO
    }

}
