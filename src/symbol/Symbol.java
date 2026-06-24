package symbol;

public class Symbol {
    public String name;
    public String kind;
    public String value;
    public int level;
    public int addr;

    public Symbol(String name, String kind, String value, int level, int addr) {
        this.name = name;
        this.kind = kind;
        this.value = value;
        this.level = level;
        this.addr = addr;
    }

    public String toString() {
        return kind + " " + name + " " + value;
    }
}
