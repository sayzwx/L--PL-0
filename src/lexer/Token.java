package lexer;

public class Token {
    public String kind;
    public String value;
    public int lineno;

    public Token(String kind, String value, int lineno) {
        this.kind = kind;
        this.value = value;
        this.lineno = lineno;
    }

    @Override
    public String toString() {
        return "(" + kind + "," + value + ")";
    }
}
