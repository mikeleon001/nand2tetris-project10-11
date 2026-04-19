import java.util.*;

public class SymbolTable {

    public enum Kind { STATIC, FIELD, ARG, VAR, NONE }

    private static class Entry {
        String type;
        Kind kind;
        int index;
        Entry(String type, Kind kind, int index) {
            this.type = type;
            this.kind = kind;
            this.index = index;
        }
    }

    private Map<String, Entry> classScope = new LinkedHashMap<>();
    private Map<String, Entry> subroutineScope = new LinkedHashMap<>();
    private Map<Kind, Integer> counts = new HashMap<>();

    public SymbolTable() {
        counts.put(Kind.STATIC, 0);
        counts.put(Kind.FIELD,  0);
        counts.put(Kind.ARG,    0);
        counts.put(Kind.VAR,    0);
    }

    // Call at the start of every new subroutine
    public void startSubroutine() {
        subroutineScope.clear();
        counts.put(Kind.ARG, 0);
        counts.put(Kind.VAR, 0);
    }

    // Define a new variable in the appropriate scope
    public void define(String name, String type, Kind kind) {
        int index = counts.get(kind);
        counts.put(kind, index + 1);
        Entry entry = new Entry(type, kind, index);
        if (kind == Kind.STATIC || kind == Kind.FIELD) {
            classScope.put(name, entry);
        } else {
            subroutineScope.put(name, entry);
        }
    }

    // Number of variables of a given kind defined so far
    public int varCount(Kind kind) {
        return counts.get(kind);
    }

    private Entry lookup(String name) {
        if (subroutineScope.containsKey(name)) return subroutineScope.get(name);
        if (classScope.containsKey(name))      return classScope.get(name);
        return null;
    }

    public Kind kindOf(String name) {
        Entry e = lookup(name);
        return e == null ? Kind.NONE : e.kind;
    }

    public String typeOf(String name) {
        Entry e = lookup(name);
        return e == null ? "" : e.type;
    }

    public int indexOf(String name) {
        Entry e = lookup(name);
        return e == null ? -1 : e.index;
    }
}
