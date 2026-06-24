package lexer;

import java.util.*;

public class Lexer {
    static final Set<String> KW = new HashSet<>(Arrays.asList(
        "const", "var", "procedure", "call", "begin", "end",
        "if", "then", "else", "while", "do", "read", "write", "odd"
    ));

    public List<Token> toks = new ArrayList<>();
    public List<LexerError> errs = new ArrayList<>();

    public List<Token> tokenize(String code) {
        toks.clear();
        errs.clear();
        String[] lines = code.split("\n");
        boolean inBlock = false;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            int lineno = i + 1;
            int col = 0;

            if (inBlock) {
                int e = line.indexOf("*/");
                if (e != -1) {
                    inBlock = false;
                    col = e + 2;
                } else {
                    continue;
                }
            }

            while (col < line.length()) {
                char ch = line.charAt(col);

                if (Character.isWhitespace(ch)) {
                    col++;
                    continue;
                }

                // single-line comment //
                if (ch == '/' && col + 1 < line.length() && line.charAt(col + 1) == '/') {
                    break;
                }

                // multi-line comment /* */
                if (ch == '/' && col + 1 < line.length() && line.charAt(col + 1) == '*') {
                    int e = line.indexOf("*/", col + 2);
                    if (e != -1) {
                        col = e + 2;
                        continue;
                    } else {
                        inBlock = true;
                        break;
                    }
                }

                // two-char operators
                if (col + 1 < line.length()) {
                    String t = line.substring(col, col + 2);
                    if (":=".equals(t) || "<=".equals(t) || ">=".equals(t)) {
                        toks.add(new Token("Op", t, lineno));
                        col += 2;
                        continue;
                    }
                }

                // single-char operators
                if ("+-*/=#<>".indexOf(ch) != -1) {
                    toks.add(new Token("Op", "" + ch, lineno));
                    col++;
                    continue;
                }

                // delimiters
                if ("();,.".indexOf(ch) != -1) {
                    toks.add(new Token("Delim", "" + ch, lineno));
                    col++;
                    continue;
                }

                // identifier or keyword
                if (Character.isLetter(ch) || ch == '_') {
                    int s = col;
                    while (col < line.length() && (Character.isLetterOrDigit(line.charAt(col)) || line.charAt(col) == '_')) {
                        col++;
                    }
                    String w = line.substring(s, col);
                    if (KW.contains(w)) {
                        toks.add(new Token("KW", w, lineno));
                    } else {
                        if (w.length() > 8) {
                            errs.add(new LexerError("LongIdent," + w, lineno));
                        }
                        toks.add(new Token("Ident", w, lineno));
                    }
                    continue;
                }

                // number
                if (Character.isDigit(ch)) {
                    int s = col;
                    while (col < line.length() && Character.isDigit(line.charAt(col))) {
                        col++;
                    }
                    String n = line.substring(s, col);

                    // number followed by letter = bad token e.g. 2a
                    if (col < line.length() && Character.isLetter(line.charAt(col))) {
                        while (col < line.length() && (Character.isLetterOrDigit(line.charAt(col)) || line.charAt(col) == '_')) {
                            col++;
                        }
                        String b = line.substring(s, col);
                        errs.add(new LexerError("BadToken," + b, lineno));
                        toks.add(new Token("BadToken", b, lineno));
                        continue;
                    }

                    if (n.length() > 8) {
                        errs.add(new LexerError("NumOverflow," + n, lineno));
                    }
                    toks.add(new Token("Num", n, lineno));
                    continue;
                }

                // illegal character
                errs.add(new LexerError("BadChar," + ch, lineno));
                col++;
            }
        }
        return toks;
    }
}

