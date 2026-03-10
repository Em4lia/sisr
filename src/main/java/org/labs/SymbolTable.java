package org.labs;
import java.util.*;

public class SymbolTable {
    private final Stack<Map<String, Symbol>> scopes = new Stack<>();
    private final List<String> warnings;

    public SymbolTable(List<String> warnings) {
        this.warnings = warnings;
        enterScope();
    }

    public void enterScope() { scopes.push(new HashMap<>()); }

    public void exitScope() {
        if (scopes.isEmpty()) return;
        Map<String, Symbol> current = scopes.pop();
        for (Symbol sym : current.values()) {
            if (!sym.isUsed() && !sym.isParameter() && !sym.isMethod()) {
                warnings.add(String.format("→ Попередження (Рядок %d:%d): змінна/поле '%s' оголошена, але не використовується.",
                        sym.getDeclarationLine(), sym.getDeclarationColumn(), sym.getName()));
            }
        }
    }

    public boolean define(Symbol symbol) {
        if (scopes.isEmpty()) return false;
        Map<String, Symbol> currentScope = scopes.peek();
        if (currentScope.containsKey(symbol.getName())) return false;
        currentScope.put(symbol.getName(), symbol);
        return true;
    }

    public Symbol resolve(String name) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            Map<String, Symbol> scope = scopes.get(i);
            if (scope.containsKey(name)) return scope.get(name);
        }
        return null;
    }
}