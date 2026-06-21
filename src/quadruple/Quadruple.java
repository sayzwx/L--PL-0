package quadruple;

public class Quadruple {
    public String op;
    public String arg1;
    public String arg2;
    public String result;

    public Quadruple(String op, String arg1, String arg2, String result) {
        this.op = op;
        this.arg1 = arg1;
        this.arg2 = arg2;
        this.result = result;
    }

    public String toString() {
        return "(" + op + "," + arg1 + "," + arg2 + "," + result + ")";
    }
}
