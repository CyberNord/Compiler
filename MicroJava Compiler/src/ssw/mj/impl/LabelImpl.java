package ssw.mj.impl;

import ssw.mj.codegen.Code;
import ssw.mj.codegen.Label;

import java.util.List;

public final class LabelImpl extends Label {

    // DONE Exercise 6: Implementation of Labels for management of jump targets
    // Note: basically every code is taken from the exercise slides.

	public LabelImpl(Code code) {
        super(code);
    }
    List<Integer> fixupList;

    /**
     * Generates code for a jump to this label.
     */
    @Override
    public void put() {
        if (isDefined()) {
            code.put2(adr - (code.pc - 1));
        }
        else {
            fixupList.add(code.pc);
            // insert place holder
            code.put2(0);
        }
    }

    /**
     * Defines <code>this</code> label to be at the current pc position
     */
    @Override
    public void here() {
        if (isDefined()) {
            // should never happen
            throw new IllegalStateException("label defined twice");
        }
        for (int pos : fixupList) {
            code.put2(pos, code.pc - (pos - 1));
        }
        fixupList = null;
        adr = code.pc;
    }

    private boolean isDefined() {
        return this.adr >= 0;
    }

}
