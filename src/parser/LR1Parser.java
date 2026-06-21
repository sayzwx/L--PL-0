package parser;

import java.util.*;
import quadruple.*;
import symbol.*;
import lexer.*;

public class LR1Parser {
    private QuadrupleManager qm;
    private SymbolTable st;
    private List<SemanticError> errs;
    private List<Token> toks;
    private int ti;
    private Token cur;

    public LR1Parser() {
        qm = new QuadrupleManager();
        st = new SymbolTable();
        errs = new ArrayList<>();
    }

    // ---- Error Reporting ----
    private void err(String m, int l) {
        errs.add(new SemanticError(m, l));
    }
    private void err(String m) {
        err(m, cur != null ? cur.lineno : 0);
    }

    // ---- Token Helpers ----
    private Token peek() {
        return ti < toks.size() ? toks.get(ti) : null;
    }

    private Token next() {
        cur = ti < toks.size() ? toks.get(ti++) : null;
        return cur;
    }

    private String sym(Token t) {
        if (t == null) return "EOF";
        switch (t.value) {
            case "const": return "CONST";
            case "var": return "VAR";
            case "procedure": return "PROC";
            case "call": return "CALL";
            case "begin": return "BEG";
            case "end": return "END";
            case "if": return "IF";
            case "then": return "THEN";
            case "else": return "ELSE";
            case "while": return "WHILE";
            case "do": return "DO";
            case "read": return "READ";
            case "write": return "WRITE";
            case "odd": return "ODD";
            case ":=": return "ASGN";
            case "=": return "EQ";
            case "#": return "NE";
            case "<": return "LT";
            case "<=": return "LE";
            case ">": return "GT";
            case ">=": return "GE";
            case "+": return "PLUS";
            case "-": return "MIN";
            case "*": return "MUL";
            case "/": return "DIV";
            case "(": return "LP";
            case ")": return "RP";
            case ";": return "SEMI";
            case ",": return "COM";
            case ".": return "DOT";
        }
        if ("Ident".equals(t.kind)) return "id";
        if ("Num".equals(t.kind)) return "num";
        return t.kind;
    }

    private boolean mat(String s) {
        Token t = peek();
        if (t != null && sym(t).equals(s)) {
            next();
            return true;
        }
        return false;
    }

    // ---- Main Entry ----
    public Map<String, Object> parse(List<Token> tks, List<LexerError> le) {
        toks = tks;
        ti = 0;
        errs.clear();
        qm = new QuadrupleManager();
        st = new SymbolTable();

        qm.emit("syss");

        // program -> block .
        parseBlock();

        // Error recovery: after block, try to parse remaining statements
        while (peek() != null && !sym(peek()).equals("DOT")) {
            String s = sym(peek());
            if (s.equals("id") || s.equals("READ") || s.equals("WRITE") ||
                s.equals("CALL") || s.equals("BEG") || s.equals("IF") ||
                s.equals("WHILE") || s.equals("ODD")) {
                parseStmt();
            } else if (s.equals("END")) {
                next();
            } else if (s.equals("SEMI")) {
                next();
            } else {
                break;
            }
        }

        if (!mat("DOT")) {
            Token t = peek();
            err("Expect . got " + (t != null ? t.value : "EOF"),
                t != null ? t.lineno : 0);
        }
        qm.emit("syse");

        Map<String, Object> r = new HashMap<>();
        r.put("ok", errs.isEmpty());
        r.put("quads", qm.toStr());
        r.put("symtbl", st.toString());
        r.put("errs", errs);
        r.put("lexerrs", le != null ? le : new ArrayList<>());
        return r;
    }

    // ---- Block (no DOT) ----
    private void parseBlock() {
        parseConst();
        parseVar();
        parseProc();
        parseStmt();
    }

    // ---- Const Declaration ----
    private void parseConst() {
        if (!mat("CONST")) return;

        Token id = peek();
        if (id == null || !sym(id).equals("id")) {
            err("Expect id in const");
            return;
        }
        id = next();
        String n = id.value;
        int ln = id.lineno;

        if (!mat("EQ")) { err("Expect =", ln); return; }

        Token nu = peek();
        if (nu == null || !sym(nu).equals("num")) {
            err("Expect num in const", ln);
            return;
        }
        nu = next();
        String v = nu.value;

        if (!mat("SEMI")) { err("Expect ;", ln); return; }

        if (!st.add(n, "const", v)) {
            err("Dup const " + n, ln);
        } else {
            qm.emit("const", n);
            qm.emit("=", v, "_", n);
        }

        parseConst();
    }

    // ---- Var Declaration ----
    private void parseVar() {
        if (!mat("VAR")) return;
        parseIds();
        if (!mat("SEMI")) {
            Token t = peek();
            err("Expect ;", t != null ? t.lineno : 0);
        }
    }

    private void parseIds() {
        Token id = peek();
        if (id == null || !sym(id).equals("id")) {
            err("Expect id in var");
            return;
        }
        id = next();
        String n = id.value;
        int ln = id.lineno;

        if (!st.add(n, "var")) {
            err("Dup var " + n, ln);
        } else {
            qm.emit("var", n);
        }

        if (mat("COM")) parseIds();
    }

    // ---- Procedure Declaration ----
    private void parseProc() {
        if (!mat("PROC")) return;

        Token id = peek();
        if (id == null || !sym(id).equals("id")) {
            err("Expect id in proc");
            return;
        }
        id = next();
        String n = id.value;
        int ln = id.lineno;

        if (!mat("SEMI")) { err("Expect ;", ln); return; }

        if (!st.add(n, "procedure")) {
            err("Dup proc " + n, ln);
        }
        qm.emit("procedure", n);

        st.enterScope();
        parseBlock();
        st.exitScope();

        if (!mat("SEMI")) {
            Token t = peek();
            err("Expect ; after proc", t != null ? t.lineno : ln);
            while (peek() != null) {
                String ss = sym(peek());
                if (ss.equals("SEMI")) { next(); break; }
                if (ss.equals("BEG") || ss.equals("PROC") ||
                    ss.equals("CONST") || ss.equals("VAR") ||
                    ss.equals("DOT") || ss.equals("IF") ||
                    ss.equals("WHILE")) break;
                next();
            }
            return;
        }

        parseProc();
    }

    // ---- Statement ----
    private void parseStmt() {
        Token t = peek();
        if (t == null) return;

        String s = sym(t);
        switch (s) {
            case "id":    parseAssign(); break;
            case "READ":  parseRead(); break;
            case "WRITE": parseWrite(); break;
            case "CALL":  parseCall(); break;
            case "BEG":
                next();
                parseStmtList();
                if (!mat("END")) {
                    Token tt = peek();
                    err("Expect END", tt != null ? tt.lineno : 0);
                }
                break;
            case "IF":    parseIf(); break;
            case "WHILE": parseWhile(); break;
            case "ODD":   parseCond(); break;
            case "END":   next(); break;
            case "SEMI":  next(); break;
            default: break;
        }
    }

    // ---- Assignment ----
    private void parseAssign() {
        Token id = next();
        String n = id.value;
        int ln = id.lineno;

        Symbol s = st.lookup(n);
        if (s == null) {
            err("Undecl " + n, ln);
        } else if (!"var".equals(s.kind)) {
            err(n + " not var", ln);
        }

        if (!mat("ASGN")) {
            err("Expect := after " + n + ", got " + (peek()!=null?peek().value:"EOF"), ln);
            while (peek() != null && !";".equals(peek().value) && !"end".equals(peek().value) && !"begin".equals(peek().value) && !"if".equals(peek().value) && !"while".equals(peek().value) && !"procedure".equals(peek().value)) {
                next();
            }
            return;
        }
        String ep = parseExpr();
        if (!mat("SEMI")) { err("Expect ;", ln); return; }
        qm.emit(":=", ep, "_", n);
    }


    // ---- Read ----
    private void parseRead() {
        next();
        if (!mat("LP")) { err("Expect ("); return; }

        Token id = peek();
        if (id == null || !sym(id).equals("id")) {
            err("Expect id in read");
            return;
        }
        id = next();
        String n = id.value;
        int ln = id.lineno;

        Symbol s = st.lookup(n);
        if (s == null) {
            err("Undecl " + n, ln);
        } else if (!"var".equals(s.kind)) {
            err(n + " not var", ln);
        }

        if (!mat("RP")) { err("Expect )", ln); return; }
        if (!mat("SEMI")) { err("Expect ;", ln); return; }

        qm.emit("read", n);
    }

    // ---- Write ----
    private void parseWrite() {
        next();
        if (!mat("LP")) { err("Expect ("); return; }

        String ep = parseExpr();

        if (!mat("RP")) { err("Expect )"); return; }
        if (!mat("SEMI")) { err("Expect ;"); return; }

        qm.emit("write", ep);
    }

    // ---- Call ----
    private void parseCall() {
        next();

        Token id = peek();
        if (id == null || !sym(id).equals("id")) {
            err("Expect id in call");
            return;
        }
        id = next();
        String n = id.value;
        int ln = id.lineno;

        Symbol s = st.lookup(n);
        if (s == null) {
            err("Undecl proc " + n, ln);
        } else if (!"procedure".equals(s.kind)) {
            err(n + " not proc", ln);
        }

        if (!mat("SEMI")) { err("Expect ;", ln); return; }

        qm.emit("call", n);
    }

    // ---- If Statement ----
    private void parseIf() {
        next();

        String fl = qm.newLabel();
        String el = qm.newLabel();

        String cp = parseCond();

        if (!mat("THEN")) { err("Expect THEN"); return; }

        qm.emitWP("j=", cp, "0", fl);
        parseStmt();
        qm.emitWP("j", "_", "_", el);
        qm.patch(fl);

        if (mat("ELSE")) parseStmt();

        qm.patch(el);
    }

    // ---- While Statement ----
    private void parseWhile() {
        next();

        String ls = qm.newLabel();
        int loopStart = qm.patch(ls);

        String ex = qm.newLabel();

        String cp = parseCond();

        if (!mat("DO")) { err("Expect DO"); return; }

        qm.emitWP("j=", cp, "0", ex);
        parseStmt();

        qm.emit("j", "_", "_", String.valueOf(loopStart));
        qm.patch(ex);
    }

    // ---- Statement List ----
    private void parseStmtList() {
        parseStmt();
        Token t = peek();
        if (t != null) {
            String s = sym(t);
            if (s.equals("id") || s.equals("READ") || s.equals("WRITE") ||
                s.equals("CALL") || s.equals("BEG") || s.equals("IF") ||
                s.equals("WHILE") || s.equals("ODD")) {
                parseStmtList();
            }
        }
    }

    // ---- Condition ----
    private String parseCond() {
        Token t = peek();
        if (t == null) return "_";

        if (sym(t).equals("ODD")) {
            next();
            String e = parseExpr();
            String r = qm.newTemp();
            qm.emit("%", e, "2", r);
            return r;
        }

        String l = parseExpr();
        Token ot = peek();
        if (ot != null) {
            String rp = sym(ot);
            if (rp.equals("EQ") || rp.equals("NE") || rp.equals("LT") ||
                rp.equals("LE") || rp.equals("GT") || rp.equals("GE")) {
                next();
                String op = "";
                switch (rp) {
                    case "EQ": op = "="; break;
                    case "NE": op = "#"; break;
                    case "LT": op = "<"; break;
                    case "LE": op = "<="; break;
                    case "GT": op = ">"; break;
                    case "GE": op = ">="; break;
                }
                String rr = parseExpr();
                String r = qm.newTemp();
                qm.emit(op, l, rr, r);
                return r;
            }
        }
        return l;
    }

    // ---- Expression ----
    private String parseExpr() {
        return parseETail(parseTerm());
    }

    private String parseETail(String l) {
        Token t = peek();
        if (t == null) return l;

        String s = sym(t);
        if (s.equals("PLUS") || s.equals("MIN")) {
            next();
            String rr = parseTerm();
            String r = qm.newTemp();
            qm.emit(s.equals("PLUS") ? "+" : "-", l, rr, r);
            return parseETail(r);
        }
        return l;
    }

    // ---- Term ----
    private String parseTerm() {
        return parseTTail(parseFact());
    }

    private String parseTTail(String l) {
        Token t = peek();
        if (t == null) return l;

        String s = sym(t);
        if (s.equals("MUL") || s.equals("DIV")) {
            next();
            String rr = parseFact();
            String r = qm.newTemp();
            qm.emit(s.equals("MUL") ? "*" : "/", l, rr, r);
            return parseTTail(r);
        }
        return l;
    }

    // ---- Factor ----
    private String parseFact() {
        Token t = peek();
        if (t == null) {
            err("Unexpected EOF");
            return "_";
        }

        String s = sym(t);

        if (s.equals("id")) {
            Token id = next();
            String n = id.value;
            int ln = id.lineno;

            Symbol ss = st.lookup(n);
            if (ss == null) {
                err("Undecl " + n, ln);
                return "_";
            }
            return n;
        }

        if (s.equals("num")) {
            return next().value;
        }

        if (s.equals("LP")) {
            next();
            String e = parseExpr();
            if (!mat("RP")) err("Expect )");
            return e;
        }

        err("Bad expr start " + t.value, t.lineno);
        return "_";
    }
}

