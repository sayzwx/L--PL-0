package lexer;

public class LexerError {
    public String msg;
    public int lineno;

    public LexerError(String msg, int lineno) {
        this.msg = msg;
        this.lineno = lineno;
    }

    @Override
    public String toString() {
        return "(LexErr," + msg + ",L:" + lineno + ")";
    }
}
