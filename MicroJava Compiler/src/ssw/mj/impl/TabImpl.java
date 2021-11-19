package ssw.mj.impl;

import ssw.mj.Parser;
import ssw.mj.symtab.Obj;
import ssw.mj.symtab.Obj.Kind;
import ssw.mj.symtab.Scope;
import ssw.mj.symtab.Tab;

import static ssw.mj.Errors.Message.DECL_NAME;
import static ssw.mj.Errors.Message.NOT_FOUND;
import static ssw.mj.symtab.Obj.Kind.*;

public final class TabImpl extends Tab {

    // TODO Exercise 4: implementation of symbol table

    /**
     * Set up "universe" (= predefined names).
     */
    public TabImpl(Parser p) {
        super(p);
        // Create Universe (-1)
        openScope();
        curScope = new Scope(null);

        // Standard Types
        insert(Type,"int",intType);
        insert(Type, "char", charType);

        // Standard Constants
        insert(Con, "null", nullType);

        // noObj from Lecture slides
        noObj = new Obj(Var, "none", noType);

        // Standard Methods (0)
        ordObj = insert(Meth, "ord", intType);
        openScope();
        insert(Var, "i", intType);
        chrObj.locals = curScope.locals();
        chrObj.nPars = curScope.nVars();
        closeScope();

        chrObj = insert(Meth, "chr", charType);
        openScope();
        insert(Var, "ch", charType);
        chrObj.locals = curScope.locals();
        chrObj.nPars = curScope.nVars();
        closeScope();

        lenObj = insert(Meth, "len", intType);
        openScope();
        insert(Var, "arr", intType);
        chrObj.locals = curScope.locals();
        chrObj.nPars = curScope.nVars();
        closeScope();

    }

    // defines a new scope and increases level
    public void openScope(){
        curScope = new Scope(curScope);
        curLevel++;
    }

    // deletes curScope and decreases level
    public void  closeScope(){
        curScope = curScope.outer();
        curLevel--;
    }

    // creates a symbol list object, sets its attributes and adds it
    // in the curScope in the symbol list.
    public Obj insert(Kind kind, String name, StructImpl struct){
        if(curScope.findLocal(name) != null){
           parser.error(DECL_NAME, name);
        }
        final Obj obj = new Obj(kind, name, struct);
        curScope.insert(obj);
        return obj;
    }

    // searches for a name
    // starting in the current to the outermost area of validity
    public Obj find(String name){
        Obj obj = curScope.findLocal(name);
        if(obj  == null){
            parser.error(NOT_FOUND, name);
        }
        return obj;
    }

    // searches by variable
    // name a field in a class, the struct of which is given in the interface
    public Obj findField(){
        // TODO
        return null;
    }

}
