package org.labs;

import java.io.*;
import java.util.*;

public class CodeGenerator extends javaLBaseVisitor<Void> {
    private final List<TacInstruction> ir = new ArrayList<>();
    private int tempCounter = 0;
    private int labelCounter = 0;
    private final Map<String, Integer> varOffset = new HashMap<>();
    private int stackSize = 0;

    private String newTemp() { 
        String t = "t" + tempCounter++; 
        varOffset.put(t, stackSize -= 8);
        return t;
    }
    
    private String newLabel() { return "L" + labelCounter++; }

    private void add(TacInstruction.Op op, String a1, String a2, String res) {
        ir.add(new TacInstruction(op, a1, a2, res));
    }

    private String visitExprWithFolding(javaLParser.ExpressionContext ctx) {

        if (ctx instanceof javaLParser.AddSubExprContext addCtx) {
            String left = visitExprWithFolding(addCtx.expression(0));
            String right = visitExprWithFolding(addCtx.expression(1));

            if (isNumber(left) && isNumber(right)) {
                int res = addCtx.PLUS() != null ?
                        Integer.parseInt(left) + Integer.parseInt(right)
                        : Integer.parseInt(left) - Integer.parseInt(right);
                return String.valueOf(res);
            }

            String t = newTemp();
            TacInstruction.Op op = addCtx.PLUS() != null ? TacInstruction.Op.ADD : TacInstruction.Op.SUB;
            add(op, left, right, t);
            return t;
        } else if (ctx instanceof javaLParser.MulDivExprContext mulCtx) {
            String left = visitExprWithFolding(mulCtx.expression(0));
            String right = visitExprWithFolding(mulCtx.expression(1));

            if (isNumber(left) && isNumber(right)) {
                int res = mulCtx.MUL() != null ?
                        Integer.parseInt(left) * Integer.parseInt(right)
                        : Integer.parseInt(left) / Integer.parseInt(right);
                return String.valueOf(res);
            }

            String t = newTemp();
            TacInstruction.Op op = mulCtx.MUL() != null ? TacInstruction.Op.MUL : TacInstruction.Op.DIV;
            add(op, left, right, t);
            return t;
        } else if (ctx instanceof javaLParser.RelationalExprContext relCtx) {
            String left = visitExprWithFolding(relCtx.expression(0));
            String right = visitExprWithFolding(relCtx.expression(1));
            String t = newTemp();

            TacInstruction.Op op;
            if (relCtx.LESS() != null) op = TacInstruction.Op.LT;
            else if (relCtx.LE() != null) op = TacInstruction.Op.LE;
            else if (relCtx.GREATER() != null) op = TacInstruction.Op.GT;
            else op = TacInstruction.Op.GE;

            add(op, left, right, t);
            return t;
        } else if (ctx instanceof javaLParser.EqualityExprContext eqCtx) {
            String left = visitExprWithFolding(eqCtx.expression(0));
            String right = visitExprWithFolding(eqCtx.expression(1));
            String t = newTemp();

            TacInstruction.Op op = eqCtx.EQUAL() != null ? TacInstruction.Op.EQ : TacInstruction.Op.NE;
            add(op, left, right, t);
            return t;
        } else if (ctx instanceof javaLParser.PrimaryExprContext primExprCtx) {
            javaLParser.PrimaryContext primCtx = primExprCtx.primary();

            if (primCtx instanceof javaLParser.IntLiteralExprContext ||
                    primCtx instanceof javaLParser.IdExprContext) {
                return primCtx.getText(); 
            }
            else if (primCtx instanceof javaLParser.ParensExprContext parensCtx) {
                return visitExprWithFolding(parensCtx.expression());
            } else if (primCtx instanceof javaLParser.BoolLiteralExprContext boolCtx) {
                return boolCtx.getText().equals("true") ? "1" : "0";
            }
        } else if (ctx instanceof javaLParser.LocalMethodCallExprContext callCtx) {
            String methodName = callCtx.ID().getText();
            String t = newTemp();

            add(TacInstruction.Op.CALL, methodName, null, t);
            return t;
        } else if (ctx instanceof javaLParser.LogicalExprContext logCtx) {
            String left = visitExprWithFolding(logCtx.expression(0));
            String right = visitExprWithFolding(logCtx.expression(1));
            String t = newTemp();

            TacInstruction.Op op = logCtx.AND() != null ? TacInstruction.Op.AND : TacInstruction.Op.OR;
            add(op, left, right, t);
            return t;
        }

        return "0";
    }

    private boolean isNumber(String s) {
        if (s == null) return false;
        try { Integer.parseInt(s); return true; } catch (Exception e) { return false; }
    }

    private void optimizeIR() {
        Set<String> used = new HashSet<>();
        for (int i = ir.size() - 1; i >= 0; i--) {
            TacInstruction instr = ir.get(i);
            if (instr.result != null) {
                if (!used.contains(instr.result)) {
                    ir.remove(i);
                    continue;
                }
            }
            if (instr.arg1 != null && !isNumber(instr.arg1)) used.add(instr.arg1);
            if (instr.arg2 != null && !isNumber(instr.arg2)) used.add(instr.arg2);
        }

        for (int i = 0; i < ir.size() - 1; i++) {
            if (ir.get(i).op == TacInstruction.Op.ASSIGN && ir.get(i+1).op == TacInstruction.Op.ADD
                    && ir.get(i+1).arg2 != null && ir.get(i+1).arg2.equals("0")) {
                ir.remove(i+1); 
            }
        }
    }

    private void loadArg(PrintWriter pw, String arg) {
        if (isNumber(arg)) {
            pw.println("    mov $" + arg + ", %rax");
        } else {
            pw.println("    mov " + getOffset(arg) + "(%rbp), %rax");
        }
    }

    private String getSetInstruction(TacInstruction.Op op) {
        return switch (op) {
            case LT -> "setl";
            case LE -> "setle";
            case GT -> "setg";
            case GE -> "setge";
            case EQ -> "sete";
            case NE -> "setne";
            default -> "";
        };
    }

    public void generateAssembly(String filename, String methodName) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(filename, true))) {
            if (methodName.equals("main")) {
                pw.println(".globl main");
                pw.println(".globl _main");
            }
            pw.println(methodName + ":"); 
            if (methodName.equals("main")) {
                pw.println("_main:");
            }
            pw.println("    push %rbp");
            pw.println("    mov %rsp, %rbp");
            int alignStack = (Math.abs(stackSize) + 15) & ~15;
            if (alignStack > 0) {
                pw.println("    sub $" + alignStack + ", %rsp");
            }

            for (TacInstruction instr : ir) {
                switch (instr.op) {
                    case ASSIGN -> {
                        loadArg(pw, instr.arg1);
                        pw.println("    mov %rax, " + getOffset(instr.result) + "(%rbp)");
                    }
                    case ADD -> {
                        loadArg(pw, instr.arg1);
                        if (isNumber(instr.arg2)) {
                            pw.println("    add $" + instr.arg2 + ", %rax");
                        } else {
                            pw.println("    add " + getOffset(instr.arg2) + "(%rbp), %rax");
                        }
                        pw.println("    mov %rax, " + getOffset(instr.result) + "(%rbp)");
                    }
                    case SUB -> {
                        loadArg(pw, instr.arg1);
                        if (isNumber(instr.arg2)) {
                            pw.println("    sub $" + instr.arg2 + ", %rax");
                        } else {
                            pw.println("    sub " + getOffset(instr.arg2) + "(%rbp), %rax");
                        }
                        pw.println("    mov %rax, " + getOffset(instr.result) + "(%rbp)");
                    }
                    case MUL -> {
                        loadArg(pw, instr.arg1);
                        if (isNumber(instr.arg2)) {
                            pw.println("    imul $" + instr.arg2 + ", %rax");
                        } else {
                            pw.println("    imul " + getOffset(instr.arg2) + "(%rbp), %rax");
                        }
                        pw.println("    mov %rax, " + getOffset(instr.result) + "(%rbp)");
                    }
                    case DIV -> {
                        loadArg(pw, instr.arg1);
                        pw.println("    cqo");
                        if (isNumber(instr.arg2)) {
                            pw.println("    mov $" + instr.arg2 + ", %rcx");
                            pw.println("    idiv %rcx");
                        } else {
                            pw.println("    idivq " + getOffset(instr.arg2) + "(%rbp)");
                        }
                        pw.println("    mov %rax, " + getOffset(instr.result) + "(%rbp)");
                    }
                    case LT, LE, GT, GE, EQ, NE -> {
                        loadArg(pw, instr.arg1);
                        if (isNumber(instr.arg2)) {
                            pw.println("    cmp $" + instr.arg2 + ", %rax");
                        } else {
                            pw.println("    cmp " + getOffset(instr.arg2) + "(%rbp), %rax");
                        }
                        String setInst = getSetInstruction(instr.op);

                        pw.println("    " + setInst + " %al");
                        pw.println("    movzbl %al, %eax");
                        pw.println("    mov %rax, " + getOffset(instr.result) + "(%rbp)");
                    }
                    case RETURN -> {
                        if (instr.arg1 != null) {
                            loadArg(pw, instr.arg1);
                        }
                        pw.println("    leave");
                        pw.println("    ret");
                    }
                    case CALL -> {
                        pw.println("    call " + instr.arg1);

                        if (instr.result != null) {
                            pw.println("    mov %rax, " + getOffset(instr.result) + "(%rbp)");
                        }
                    }
                    case IF_GOTO -> {
                        loadArg(pw, instr.arg1);
                        pw.println("    cmp $0, %rax");
                        pw.println("    jne " + instr.arg2);
                    }
                    case AND -> {
                        loadArg(pw, instr.arg1);
                        if (isNumber(instr.arg2)) {
                            pw.println("    and $" + instr.arg2 + ", %rax");
                        } else {
                            pw.println("    and " + getOffset(instr.arg2) + "(%rbp), %rax");
                        }
                        pw.println("    mov %rax, " + getOffset(instr.result) + "(%rbp)");
                    }
                    case OR -> {
                        loadArg(pw, instr.arg1);
                        if (isNumber(instr.arg2)) {
                            pw.println("    or $" + instr.arg2 + ", %rax");
                        } else {
                            pw.println("    or " + getOffset(instr.arg2) + "(%rbp), %rax");
                        }
                        pw.println("    mov %rax, " + getOffset(instr.result) + "(%rbp)");
                    }
                    case GOTO -> pw.println("    jmp " + instr.arg1);
                    case LABEL -> pw.println(instr.arg1 + ":");
                }
            }
            pw.println("    leave");
            pw.println("    ret");
            pw.println("");
        } catch (Exception e) { 
            System.err.println("Помилка генерації assembly: " + e.getMessage());
        }
    }

    private int getOffset(String var) {
        if (!varOffset.containsKey(var)) {
            varOffset.put(var, stackSize -= 8);
        }
        return varOffset.get(var);
    }

    @Override
    public Void visitProgram(javaLParser.ProgramContext ctx) {
        File f = new File("output.s");
        if (f.exists() && !f.delete()) {
            System.err.println("Не вдалося видалити попередній файл output.s");
        }
        return super.visitProgram(ctx);
    }

    @Override
    public Void visitMethodDeclaration(javaLParser.MethodDeclarationContext ctx) {
        String methodName = ctx.ID().getText();
        ir.clear();
        tempCounter = 0;
        stackSize = 0;
        varOffset.clear();

        int offset = -8;
        if (ctx.parameter() != null) {
            for (var p : ctx.parameter()) {
                varOffset.put(p.ID().getText(), offset);
                offset -= 8;
            }
        }
        stackSize = offset + 8;

        visitChildren(ctx);           
//        optimizeIR();

        generateAssembly("output.s", methodName); 
        return null;
    }

    @Override
    public Void visitVariableDeclaration(javaLParser.VariableDeclarationContext ctx) {
        String name = ctx.ID().getText();
        varOffset.put(name, stackSize -= 8);
        if (ctx.ASSIGN() != null) {
            String val = visitExprWithFolding(ctx.expression());
            add(TacInstruction.Op.ASSIGN, val, null, name);
        }
        return null;
    }

    @Override
    public Void visitSimpleAssign(javaLParser.SimpleAssignContext ctx) {
        String name = ctx.ID().getText();
        String val = visitExprWithFolding(ctx.expression());
        add(TacInstruction.Op.ASSIGN, val, null, name);
        return null;
    }

    @Override
    public Void visitIfStatement(javaLParser.IfStatementContext ctx) {
        String cond = visitExprWithFolding(ctx.expression());
        String trueLabel = newLabel();
        String endLabel = newLabel();
        add(TacInstruction.Op.IF_GOTO, cond, trueLabel, null);
        add(TacInstruction.Op.GOTO, endLabel, null, null);
        add(TacInstruction.Op.LABEL, trueLabel, null, null);
        visit(ctx.statement(0));
        
        if (ctx.ELSE() != null) {
            String finalEndLabel = newLabel();
            add(TacInstruction.Op.GOTO, finalEndLabel, null, null);
            add(TacInstruction.Op.LABEL, endLabel, null, null);
            visit(ctx.statement(1));
            add(TacInstruction.Op.LABEL, finalEndLabel, null, null);
        } else {
            add(TacInstruction.Op.LABEL, endLabel, null, null);
        }
        return null;
    }

    @Override
    public Void visitWhileStatement(javaLParser.WhileStatementContext ctx) {
        String startLabel = newLabel();
        String endLabel = newLabel();

        add(TacInstruction.Op.LABEL, startLabel, null, null);

        String cond = visitExprWithFolding(ctx.expression());
        String bodyLabel = newLabel();
        add(TacInstruction.Op.IF_GOTO, cond, bodyLabel, null);
        add(TacInstruction.Op.GOTO, endLabel, null, null);

        add(TacInstruction.Op.LABEL, bodyLabel, null, null);
        visit(ctx.statement());

        add(TacInstruction.Op.GOTO, startLabel, null, null);
        add(TacInstruction.Op.LABEL, endLabel, null, null);
        return null;
    }

    @Override
    public Void visitReturnStat(javaLParser.ReturnStatContext ctx) {
        if (ctx.expression() != null) {
            String val = visitExprWithFolding(ctx.expression());
            add(TacInstruction.Op.RETURN, val, null, null);
        } else {
            add(TacInstruction.Op.RETURN, null, null, null);
        }
        return null;
    }
}