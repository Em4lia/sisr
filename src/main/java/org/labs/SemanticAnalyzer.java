package org.labs;

import org.antlr.v4.runtime.Token;
import java.util.ArrayList;
import java.util.List;

public class SemanticAnalyzer extends javaLBaseVisitor<String> {
    private final SymbolTable symbolTable;
    private final List<String> errors = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();

    private int loopDepth = 0;
    private int breakCount = 0; 
    private String currentMethodReturnType = null;
    private boolean hasReturnedValue = false;

    public SemanticAnalyzer() {
        this.symbolTable = new SymbolTable(this.warnings);
    }

    public SymbolTable getSymbolTable(){
        return symbolTable;
    }

    public List<String> getErrors() { return errors; }
    public List<String> getWarnings() { return warnings; }

    private void reportError(Token token, String message) {
        errors.add(String.format(">>> Помилка (Рядок %d:%d): %s", token.getLine(), token.getCharPositionInLine(), message));
    }
    private void reportWarning(Token token, String message) {
        warnings.add(String.format("→ Попередження (Рядок %d:%d): %s", token.getLine(), token.getCharPositionInLine(), message));
    }


    @Override
    public String visitProgram(javaLParser.ProgramContext ctx) {
        visitChildren(ctx);
        return null;
    }

    @Override
    public String visitBlockStat(javaLParser.BlockStatContext ctx) {
        symbolTable.enterScope();
        for (javaLParser.StatementContext s : ctx.statement()) {
            visit(s);
        }
        symbolTable.exitScope();
        return null;
    }

    @Override
    public String visitMethodDeclaration(javaLParser.MethodDeclarationContext ctx) {
        String methodName = ctx.ID().getText();
        String returnType = ctx.VOID() != null ? "void" : ctx.type().getText();
        Token idToken = ctx.ID().getSymbol();

        Symbol methodSym = new Symbol(methodName, returnType, idToken.getLine(), idToken.getCharPositionInLine());
        methodSym.setMethod(true);
        methodSym.setMethodReturnType(returnType);

        if (ctx.parameter() != null) {
            for (javaLParser.ParameterContext pCtx : ctx.parameter()) {
                methodSym.addParameterType(pCtx.type().getText());
            }
        }

        if (!symbolTable.define(methodSym)) {
            reportError(idToken, "Метод '" + methodName + "' вже оголошено.");
        }

        symbolTable.enterScope();
        currentMethodReturnType = returnType;
        hasReturnedValue = false;

        if (ctx.parameter() != null) {
            for (javaLParser.ParameterContext pCtx : ctx.parameter()) visit(pCtx);
        }

        for (javaLParser.StatementContext sCtx : ctx.statement()) visit(sCtx);

        if (!returnType.equals("void") && !hasReturnedValue) {
            reportError(idToken, "Метод '" + methodName + "' має повертати значення типу " + returnType);
        }

        currentMethodReturnType = null;
        symbolTable.exitScope();
        return null;
    }

    @Override
    public String visitParameter(javaLParser.ParameterContext ctx) {
        String name = ctx.ID().getText();
        Token idToken = ctx.ID().getSymbol();
        Symbol sym = new Symbol(name, ctx.type().getText(), idToken.getLine(), idToken.getCharPositionInLine());
        sym.setParameter(true);
        sym.markAsInitialized(); 
        if (!symbolTable.define(sym)) {
            reportError(idToken, "Параметр '" + name + "' вже оголошено.");
        }
        return null;
    }

    @Override
    public String visitVariableDeclaration(javaLParser.VariableDeclarationContext ctx) {
        String type = ctx.type().getText();
        String name = ctx.ID().getText();
        Token idToken = ctx.ID().getSymbol();

        Symbol sym = new Symbol(name, type, idToken.getLine(), idToken.getCharPositionInLine());
        if (!symbolTable.define(sym)) {
            reportError(idToken, "Ідентифікатор '" + name + "' вже оголошено.");
        }

        if (ctx.ASSIGN() != null) {
            String exprType = visit(ctx.expression());
            if (exprType != null && !areTypesCompatible(type, exprType)) {
                reportError(ctx.ASSIGN().getSymbol(), "Несумісні типи: неможливо присвоїти " + exprType + " у змінну типу " + type);
            }
            sym.markAsInitialized();
        }
        return null;
    }

    @Override
    public String visitFieldDeclaration(javaLParser.FieldDeclarationContext ctx) {
        String type = ctx.type().getText();
        String name = ctx.ID().getText();
        Token idToken = ctx.ID().getSymbol();
        Symbol sym = new Symbol(name, type, idToken.getLine(), idToken.getCharPositionInLine());
        sym.setField(true);
        if (!symbolTable.define(sym)) {
            reportError(idToken, "Поле '" + name + "' вже оголошено.");
        }
        if (ctx.ASSIGN() != null) {
            String exprType = visit(ctx.expression());
            if (exprType != null && !areTypesCompatible(type, exprType)) {
                reportError(ctx.ASSIGN().getSymbol(), "Несумісні типи: неможливо присвоїти " + exprType + " у поле типу " + type);
            }
            sym.markAsInitialized();
        }
        return null;
    }

    @Override
    public String visitClassDeclaration(javaLParser.ClassDeclarationContext ctx) {
        symbolTable.enterScope();
        super.visitClassDeclaration(ctx);
        symbolTable.exitScope();
        return null;
    }

    @Override
    public String visitWhileStatement(javaLParser.WhileStatementContext ctx) {
        String condType = visit(ctx.expression());
        if (condType != null && !condType.equals("boolean") && !condType.equals("unknown")) {
            reportError(ctx.expression().getStart(), "Умова циклу має бути boolean, отримано: " + condType);
        }

        boolean isAlwaysTrue = ctx.expression().getText().equals("true");
        int breaksBefore = breakCount;

        loopDepth++;
        visit(ctx.statement());
        loopDepth--;

        if (isAlwaysTrue && breakCount == breaksBefore) {
            reportWarning(ctx.WHILE().getSymbol(), "Нескінченний цикл: умова завжди true і відсутній break.");
        }
        return null;
    }

    @Override
    public String visitIfStatement(javaLParser.IfStatementContext ctx) {
        String condType = visit(ctx.expression());
        if (condType != null && !condType.equals("boolean") && !condType.equals("unknown")) {
            reportError(ctx.expression().getStart(), "Умова if має бути boolean, отримано: " + condType);
        }

        String condText = ctx.expression().getText();
        if (condText.equals("false")) {
            reportWarning(ctx.statement(0).getStart(), "Недосяжний код: гілка if ніколи не виконається (умова false).");
        } else if (condText.equals("true") && ctx.ELSE() != null) {
            reportWarning(ctx.statement(1).getStart(), "Недосяжний код: гілка else ніколи не виконається (умова завжди true).");
        }

        visit(ctx.statement(0));
        if (ctx.ELSE() != null) visit(ctx.statement(1));
        return null;
    }

    @Override
    public String visitSwitchStatement(javaLParser.SwitchStatementContext ctx) {
        String exprType = visit(ctx.expression());
        if (exprType != null && !exprType.equals("int")) {
             reportError(ctx.expression().getStart(), "Switch підтримує тільки int.");
        }
        symbolTable.enterScope();
        for (int i = 0; i < ctx.getChildCount(); i++) {
            var child = ctx.getChild(i);
            if (child instanceof javaLParser.StatementContext) {
                visit(child);
            }
        }
        symbolTable.exitScope();
        return null;
    }

    @Override
    public String visitBreakStat(javaLParser.BreakStatContext ctx) {
        if (loopDepth == 0) reportError(ctx.getStart(), "Оператор break має знаходитись у тілі циклу.");
        breakCount++;
        return null;
    }

    @Override
    public String visitContinueStat(javaLParser.ContinueStatContext ctx) {
        if (loopDepth == 0) reportError(ctx.getStart(), "Оператор continue має знаходитись у тілі циклу.");
        return null;
    }

    @Override
    public String visitReturnStat(javaLParser.ReturnStatContext ctx) {
        hasReturnedValue = true;
        if (currentMethodReturnType == null) {
            reportError(ctx.getStart(), "Оператор return поза межами методу.");
            return null;
        }

        if (ctx.expression() != null) {
            String exprType = visit(ctx.expression());
            if (currentMethodReturnType.equals("void")) {
                reportError(ctx.getStart(), "void метод не може повертати значення.");
            } else if (exprType != null && !areTypesCompatible(currentMethodReturnType, exprType)) {
                reportError(ctx.getStart(), "Тип значення (" + exprType + ") не відповідає типу методу (" + currentMethodReturnType + ").");
            }
        } else if (!currentMethodReturnType.equals("void")) {
            reportError(ctx.getStart(), "Метод має повертати значення типу " + currentMethodReturnType);
        }
        return null;
    }

    @Override
    public String visitSimpleAssign(javaLParser.SimpleAssignContext ctx) {
        String name = ctx.ID().getText();
        Symbol sym = symbolTable.resolve(name);
        if (sym == null) {
            reportError(ctx.ID().getSymbol(), "Змінну '" + name + "' не оголошено.");
            return null;
        }
        
        String exprType = visit(ctx.expression());
        if (exprType != null && !areTypesCompatible(sym.getType(), exprType)) {
            reportError(ctx.ASSIGN().getSymbol(), "Несумісні типи: неможливо присвоїти " + exprType + " у " + sym.getType());
        }
        
        sym.markAsUsed();
        sym.markAsInitialized();
        return null;
    }

    @Override
    public String visitArrayAssign(javaLParser.ArrayAssignContext ctx) {
        String name = ctx.ID().getText();
        Symbol sym = symbolTable.resolve(name);
        if (sym == null) {
            reportError(ctx.ID().getSymbol(), "Масив '" + name + "' не оголошено.");
            return null;
        }
        if (!sym.getType().endsWith("[]")) {
            reportError(ctx.ID().getSymbol(), "'" + name + "' не є масивом.");
        }
        sym.markAsUsed();
        String indexType = visit(ctx.expression(0));
        if (indexType != null && !indexType.equals("int")) {
            reportError(ctx.expression(0).getStart(), "Індекс масиву має бути int, отримано: " + indexType);
        }
        String valueType = visit(ctx.expression(1));
        String elemType = sym.getType().replace("[]", "");
        if (valueType != null && !areTypesCompatible(elemType, valueType)) {
            reportError(ctx.ASSIGN().getSymbol(), "Несумісні типи для елемента масиву: " + valueType + " != " + elemType);
        }
        return null;
    }

    @Override
    public String visitObjectAssign(javaLParser.ObjectAssignContext ctx) {
        String objName = ctx.ID(0).getText();
        Symbol objSym = symbolTable.resolve(objName);
        if (objSym == null) {
            reportError(ctx.ID(0).getSymbol(), "Об'єкт '" + objName + "' не оголошено.");
            return null;
        }
        objSym.markAsUsed();
        visit(ctx.expression());
        return null;
    }


    @Override
    public String visitAddSubExpr(javaLParser.AddSubExprContext ctx) {
        String left = visit(ctx.expression(0));
        String right = visit(ctx.expression(1));
        return checkNumericBinaryOp(left, right, ctx.getStart());
    }

    @Override
    public String visitMulDivExpr(javaLParser.MulDivExprContext ctx) {
        String left = visit(ctx.expression(0));
        String right = visit(ctx.expression(1));
        return checkNumericBinaryOp(left, right, ctx.getStart());
    }

    @Override
    public String visitRelationalExpr(javaLParser.RelationalExprContext ctx) {
        String left = visit(ctx.expression(0));
        String right = visit(ctx.expression(1));
        if (left != null && right != null && (!isNumeric(left) || !isNumeric(right))) {
            if (!left.equals("unknown") && !right.equals("unknown"))
                 reportError(ctx.getStart(), "Операції порівняння доступні лише для чисел.");
        }
        return "boolean";
    }

    @Override
    public String visitEqualityExpr(javaLParser.EqualityExprContext ctx) {
        String left = visit(ctx.expression(0));
        String right = visit(ctx.expression(1));
        if (left != null && right != null && !left.equals(right) && !left.equals("unknown") && !right.equals("unknown")) {
             if (isNumeric(left) && isNumeric(right)) return "boolean";
             reportError(ctx.getStart(), "Операнди рівності мають бути сумісного типу.");
        }
        return "boolean";
    }

    @Override
    public String visitLogicalExpr(javaLParser.LogicalExprContext ctx) {
        String left = visit(ctx.expression(0));
        String right = visit(ctx.expression(1));
        if (left != null && right != null && (!left.equals("boolean") || !right.equals("boolean"))) {
             if (!left.equals("unknown") && !right.equals("unknown"))
                 reportError(ctx.getStart(), "Логічні операції доступні лише для boolean.");
        }
        return "boolean";
    }

    @Override
    public String visitIdExpr(javaLParser.IdExprContext ctx) {
        String name = ctx.ID().getText();
        Symbol sym = symbolTable.resolve(name);
        if (sym == null) {
            reportError(ctx.ID().getSymbol(), "Змінну '" + name + "' не оголошено.");
            return "unknown";
        }
        sym.markAsUsed();
        return sym.getType();
    }

    @Override
    public String visitMethodCallStat(javaLParser.MethodCallStatContext ctx) {
        visit(ctx.methodCall());
        return null;
    }

    @Override
    public String visitMethodCall(javaLParser.MethodCallContext ctx) {
        if (ctx.DOT() != null) {
             visit(ctx.expression(0)); 
             for (int i = 1; i < ctx.expression().size(); i++) {
                 visit(ctx.expression(i));
             }
             return "unknown";
        }
        
        String methodName = ctx.ID().getText();
        Symbol sym = symbolTable.resolve(methodName);

        if (sym == null || !sym.isMethod()) {
            reportError(ctx.ID().getSymbol(), "Виклик неіснуючого методу '" + methodName + "'.");
            return "unknown";
        }
        sym.markAsUsed();

        List<javaLParser.ExpressionContext> args = ctx.expression();
        int expectedArgs = sym.getParameterTypes().size();
        int actualArgs = args.size();

        if (expectedArgs != actualArgs) {
            reportError(ctx.ID().getSymbol(), "Невірна кількість аргументів. Очікується " + expectedArgs + ", отримано " + actualArgs + ".");
        } else {
            for (int i = 0; i < actualArgs; i++) {
                String actualType = visit(args.get(i));
                String expectedType = sym.getParameterTypes().get(i);
                if (actualType != null && !areTypesCompatible(expectedType, actualType)) {
                    reportError(args.get(i).getStart(), "Тип аргументу " + (i+1) + " не співпадає. Очікується " + expectedType + ", отримано " + actualType);
                }
            }
        }
        return sym.getMethodReturnType();
    }

    @Override
    public String visitLocalMethodCallExpr(javaLParser.LocalMethodCallExprContext ctx) {
        String methodName = ctx.ID().getText();
        Symbol sym = symbolTable.resolve(methodName);

        if (sym == null || !sym.isMethod()) {
            reportError(ctx.ID().getSymbol(), "Виклик неіснуючого методу '" + methodName + "'.");
            return "unknown";
        }
        sym.markAsUsed();
        
        if (sym.getMethodReturnType().equals("void")) {
             reportError(ctx.ID().getSymbol(), "Метод типу void не може бути частиною виразу.");
        }

        List<javaLParser.ExpressionContext> args = ctx.expression();
        int expectedArgs = sym.getParameterTypes().size();
        int actualArgs = args.size();

        if (expectedArgs != actualArgs) {
            reportError(ctx.ID().getSymbol(), "Невірна кількість аргументів. Очікується " + expectedArgs + ".");
        } else {
            for (int i = 0; i < actualArgs; i++) {
                String actualType = visit(args.get(i));
                String expectedType = sym.getParameterTypes().get(i);
                if (actualType != null && !areTypesCompatible(expectedType, actualType)) {
                    reportError(args.get(i).getStart(), "Тип аргументу не співпадає.");
                }
            }
        }
        return sym.getMethodReturnType();
    }
    
    @Override
    public String visitMethodCallExpr(javaLParser.MethodCallExprContext ctx) {
        visit(ctx.expression(0)); 
        for (int i = 1; i < ctx.expression().size(); i++) {
            visit(ctx.expression(i));
        }
        return "unknown";
    }

    @Override
    public String visitArrayAccessExpr(javaLParser.ArrayAccessExprContext ctx) {
        String arrayType = visit(ctx.expression(0));
        if (arrayType != null && !arrayType.endsWith("[]") && !arrayType.equals("unknown")) {
            reportError(ctx.expression(0).getStart(), "Доступ [] можливий тільки для масивів.");
        }
        String indexType = visit(ctx.expression(1));
        if (indexType != null && !indexType.equals("int")) {
            reportError(ctx.expression(1).getStart(), "Індекс масиву має бути int.");
        }
        return arrayType != null ? arrayType.replace("[]", "") : "unknown";
    }
    
    @Override
    public String visitNewObjectExpr(javaLParser.NewObjectExprContext ctx) {
        return ctx.ID().getText();
    }
    
    @Override
    public String visitFieldAccessExpr(javaLParser.FieldAccessExprContext ctx) {
        visit(ctx.expression());
        return "unknown"; 
    }

    @Override public String visitIntLiteralExpr(javaLParser.IntLiteralExprContext ctx) { return "int"; }
    @Override public String visitDoubleLiteralExpr(javaLParser.DoubleLiteralExprContext ctx) { return "double"; }
    @Override public String visitBoolLiteralExpr(javaLParser.BoolLiteralExprContext ctx) { return "boolean"; }
    @Override public String visitParensExpr(javaLParser.ParensExprContext ctx) { return visit(ctx.expression()); }


    private boolean isNumeric(String type) { 
        return type.equals("int") || type.equals("double"); 
    }
    
    private boolean areTypesCompatible(String expected, String actual) {
        if (expected.equals(actual)) return true;
        if (expected.equals("double") && actual.equals("int")) return true; 
        if (actual.equals("unknown") || expected.equals("unknown")) return true; 
        return false;
    }

    private String checkNumericBinaryOp(String left, String right, Token startToken) {
        if (left == null || right == null) return "unknown";

        if (!isNumeric(left) || !isNumeric(right)) {
            if (!left.equals("unknown") && !right.equals("unknown"))
                reportError(startToken, "Арифметичні операції доступні лише для числових типів (int, double).");
            return "unknown";
        }
        return (left.equals("double") || right.equals("double")) ? "double" : "int";
    }
}