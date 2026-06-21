package symbol;

import java.util.*;

public class SymbolTable {
    private List<Map<String, Symbol>> scopes = new ArrayList<>();
    private List<Symbol> all = new ArrayList<>();

    public SymbolTable() {
        enterScope();
    }

    public void enterScope() {
        scopes.add(new HashMap<>());
    }

    public void exitScope() {
        if (scopes.size() > 1) {
            scopes.remove(scopes.size() - 1);
        }
    }

    public int level() {
        return scopes.size() - 1;
    }

    public boolean add(String name, String kind, String value) {
        Map<String, Symbol> cur = scopes.get(scopes.size() - 1);
        if (cur.containsKey(name)) {
            return false;
        }
        Symbol s = new Symbol(name, kind, value, level(), 0);
        cur.put(name, s);
        all.add(s);
        return true;
    }

    public boolean add(String name, String kind) {
        return add(name, kind, "0");
    }

    public Symbol lookup(String name) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            Map<String, Symbol> s = scopes.get(i);
            if (s.containsKey(name)) {
                return s.get(name);
            }
        }
        return null;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Symbol s : all) {
            sb.append(s).append("\n");
        }
        return sb.toString().trim();
    }
}
