package org.labs;

public class TacInstruction {
    public enum Op { ASSIGN, ADD, SUB, MUL, DIV, LT, LE, GT, GE, EQ, NE, AND, OR,
        LABEL, GOTO, IF_GOTO, RETURN, CALL, PARAM }

    public Op op;
    public String arg1, arg2, result;
    public int labelCounter = 0;

    public TacInstruction(Op op, String arg1, String arg2, String result) {
        this.op = op;
        this.arg1 = arg1;
        this.arg2 = arg2;
        this.result = result;
    }
}