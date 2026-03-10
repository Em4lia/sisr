package org.labs;

import java.util.ArrayList;
import java.util.List;


public class Symbol {
    private String name;
    private String type;
    private int declarationLine;
    private int declarationColumn;
    private boolean isUsed;
    private boolean isInitialized;
    private boolean isField;
    private boolean isParameter;
    private boolean isMethod;
    private String methodReturnType;
    private List<String> parameterTypes;

    public Symbol(String name, String type, int line, int column) {
        this.name = name;
        this.type = type;
        this.declarationLine = line;
        this.declarationColumn = column;
        this.isUsed = false;
        this.isInitialized = false;
        this.isField = false;
        this.isParameter = false;
        this.isMethod = false;
        this.methodReturnType = null;
        this.parameterTypes = new ArrayList<>();
    }

    public String getName() { return name; }
    public String getType() { return type; }
    public int getDeclarationLine() { return declarationLine; }
    public int getDeclarationColumn() { return declarationColumn; }

    public boolean isUsed() { return isUsed; }
    public void markAsUsed() { this.isUsed = true; }

    public boolean isInitialized() { return isInitialized; }
    public void markAsInitialized() { this.isInitialized = true; }

    public boolean isField() { return isField; }
    public void setField(boolean field) { isField = field; }

    public boolean isParameter() { return isParameter; }
    public void setParameter(boolean parameter) { isParameter = parameter; }

    public boolean isMethod() { return isMethod; }
    public void setMethod(boolean method) { isMethod = method; }

    public String getMethodReturnType() { return methodReturnType; }
    public void setMethodReturnType(String returnType) { this.methodReturnType = returnType; }

    public void addParameterType(String pType) { this.parameterTypes.add(pType); }
    public List<String> getParameterTypes() { return parameterTypes; }

    @Override
    public String toString() {
        String kind = isMethod ? "метод" : (isField ? "поле" : (isParameter ? "параметр" : "змінна"));
        return String.format("%s '%s' типу %s (рядок %d:%d) [used=%b, init=%b]",
                kind, name, type, declarationLine, declarationColumn, isUsed, isInitialized);
    }
}