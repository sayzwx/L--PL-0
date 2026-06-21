package parser;

public class SemanticError {
    public String msg;
    public int lineno;

    public SemanticError(String msg, int lineno) {
        this.msg = msg;
        this.lineno = lineno;
    }

    public String toString() {
        return "(SemErr,\"" + msg + "\",L:" + lineno + ")";
    }
}
