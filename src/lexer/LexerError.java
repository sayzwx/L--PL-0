package lexer;

public class LexerError {
    public String msg;
    public int lineno;

    public LexerError(String msg, int lineno) {
        this.msg = msg;
        this.lineno = lineno;
    }

    /**
     * 将词法分析器错误对象转换为字符串表示形式
     * 
     * @return 格式化的错误信息字符串，格式为 "(LexErr,错误消息,L:行号)"
     */
    @Override
    public String toString() {
        return "(LexErr," + msg + ",L:" + lineno + ")";
    }
}
