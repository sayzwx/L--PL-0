package main;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import lexer.*;
import parser.*;

public class Main {
    public static void main(String[] args) throws Exception {
        System.out.println("PL/0 SemAnal (S-Trans + Quad)");
        if (args.length == 0) {
            runExamples();
        } else {
            File f = new File(args[0]);
            if (f.isDirectory()) {
                batch(f);
            } else {
                file(f);
            }
        }
    }

    static void runExamples() throws Exception {
        /* ---- 测试示例 1： ---- */
        String ex1 = "const a = 10;\n"
            + "var b, c;\n"
            + "procedure p;\n"
            + "if a <= 10 then\n"
            + "begin\n"
            + "c := b + a;\n"
            + "end;\n"
            + "begin\n"
            + "read(b);\n"
            + "while b # 0 do\n"
            + "begin\n"
            + "call p;\n"
            + "write(2 * c);\n"
            + "read(b);\n"
            + "end\n"
            + "end.";

        runOneExample("_ex1.pl0", ex1);
        Files.deleteIfExists(Paths.get("_ex1.pl0"));

        /* ---- 测试示例 2： ---- */
        String ex2 = "const a = 10;\n"
            + "var a, b, c;\n"
            + "procedure p;\n"
            + "if a <= 10 then\n"
            + "bagin\n"
            + "c := b + a;\n"
            + "end;\n"
            + "begin\n"
            + "read(b);\n"
            + "while b # 0 do\n"
            + "begin\n"
            + "call g;\n"
            + "write(2 * f);\n"
            + "read(e);\n"
            + "end\n"
            + "end.";


        runOneExample("_ex2.pl0", ex2);
        Files.deleteIfExists(Paths.get("_ex2.pl0"));
    }

    /** 按行号分组收集错误 */
    static Map<Integer, List<String>> groupLexErrs(List<LexerError> errs) {
        Map<Integer, List<String>> m = new HashMap<>();
        if (errs == null) return m;
        for (LexerError e : errs) {
            m.computeIfAbsent(e.lineno, k -> new ArrayList<>()).add(e.msg);
        }
        return m;
    }

    static Map<Integer, List<String>> groupSemErrs(List<SemanticError> errs) {
        Map<Integer, List<String>> m = new HashMap<>();
        if (errs == null) return m;
        for (SemanticError e : errs) {
            m.computeIfAbsent(e.lineno, k -> new ArrayList<>()).add(e.msg);
        }
        return m;
    }

    /** 执行单个示例 */
    static void runOneExample(String tmpName, String code) throws Exception {
        Files.write(Paths.get(tmpName), code.getBytes("UTF-8"));
        System.out.println("  [1/3] 读取示例程序");

        Lexer l = new Lexer();
        List<Token> toks = l.tokenize(code);
        System.out.println("      识别到 " + toks.size() + " 个 Token");
        System.out.println("  [2/3] 语法+语义分析中...");
        LR1Parser p = new LR1Parser();
        @SuppressWarnings("unchecked")
        Map<String, Object> r = p.parse(toks, l.errs);

        @SuppressWarnings("unchecked")
        List<LexerError> lexErrs = (List<LexerError>) r.get("lexerrs");
        @SuppressWarnings("unchecked")
        List<SemanticError> semErrs = (List<SemanticError>) r.get("errs");

        Map<Integer, List<String>> lexMap = groupLexErrs(lexErrs);
        Map<Integer, List<String>> semMap = groupSemErrs(semErrs);

        /* ---- 逐行打印，错误在行号下面对应报出 ---- */
        System.out.println("\n示例输入:");
        System.out.println("----------------------------------------");
        String[] lines = code.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            int lineno = i + 1;
            System.out.println(lines[i]);

            // 词法错误（在本行下面报出）
            List<String> le = lexMap.get(lineno);
            if (le != null) {
                for (String emsg : le) {
                    System.out.println("  (词法错误," + emsg + ")");
                }
            }

            // 语义错误（在本行下面报出）
            List<String> se = semMap.get(lineno);
            if (se != null) {
                for (String emsg : se) {
                    // 根据错误信息映射到文档格式
                    System.out.println("  (语义错误," + emsg + ")");
                }
            }
        }
        System.out.println("----------------------------------------");

        /* ---- 输出（文档格式） ---- */
        boolean hasAnyErr = (semErrs != null && !semErrs.isEmpty())
                         || (lexErrs != null && !lexErrs.isEmpty());

        System.out.println("\n输出:");
        if (!hasAnyErr) {
            System.out.println("语义正确");

            String q = (String) r.get("quads");
            if (q != null && !q.isEmpty()) {
                System.out.println("\n中间代码:");
                System.out.println(q);
            }

            String st = (String) r.get("symtbl");
            if (st != null && !st.isEmpty()) {
                System.out.println("符号表:");
                System.out.println(st);
            }
            System.out.println();
            return;
        }

        // 有错误时，按行号顺序输出 (语义错误,行号:N)
        // 合并所有有错误的行号（词法+语义）
        Set<Integer> errLines = new TreeSet<>();
        if (lexMap != null) errLines.addAll(lexMap.keySet());
        if (semMap != null) errLines.addAll(semMap.keySet());

        for (int ln : errLines) {
            List<String> le = lexMap.get(ln);
            if (le != null) {
                for (String emsg : le) {
                    System.out.println("(词法错误,行号:" + ln + ")");
                }
            }
            List<String> se = semMap.get(ln);
            if (se != null) {
                for (String emsg : se) {
                    System.out.println("(语义错误," + emsg + ")");
                }
            }
        }
        System.out.println();
    }

    static void file(File f) throws Exception {
        if (!f.exists()) {
            System.out.println("[Err] Not found: " + f);
            return;
        }
        String code = new String(Files.readAllBytes(f.toPath()), "UTF-8");
        System.out.println("  [1/3] 读取源文件: " + f.getName());

        Lexer l = new Lexer();
        List<Token> toks = l.tokenize(code);
        System.out.println("      识别到 " + toks.size() + " 个 Token");
        System.out.println("  [2/3] 语法+语义分析中...");
        LR1Parser p = new LR1Parser();
        @SuppressWarnings("unchecked")
        Map<String, Object> r = p.parse(toks, l.errs);

        @SuppressWarnings("unchecked")
        List<LexerError> lexErrs = (List<LexerError>) r.get("lexerrs");
        @SuppressWarnings("unchecked")
        List<SemanticError> semErrs = (List<SemanticError>) r.get("errs");

        Map<Integer, List<String>> lexMap = groupLexErrs(lexErrs);
        Map<Integer, List<String>> semMap = groupSemErrs(semErrs);

        /* ---- 逐行打印，错误在行号下面对应报出 ---- */
        System.out.println("\n" + f.getName() + " - 示例输入:");
        System.out.println("----------------------------------------");
        String[] lines = code.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            int lineno = i + 1;
            System.out.println(lines[i]);

            List<String> le = lexMap.get(lineno);
            if (le != null) {
                for (String emsg : le) {
                    System.out.println("  (词法错误," + emsg + ")");
                }
            }
            List<String> se = semMap.get(lineno);
            if (se != null) {
                for (String emsg : se) {
                    System.out.println("  (语义错误," + emsg + ")");
                }
            }
        }
        System.out.println("----------------------------------------");

        /* ---- 输出（文档格式） ---- */
        boolean hasAnyErr = (semErrs != null && !semErrs.isEmpty())
                         || (lexErrs != null && !lexErrs.isEmpty());

        System.out.println("\n输出:");
        if (!hasAnyErr) {
            System.out.println("语义正确");

            String q = (String) r.get("quads");
            if (q != null && !q.isEmpty()) {
                System.out.println("\n中间代码:");
                System.out.println(q);
            }

            String st = (String) r.get("symtbl");
            if (st != null && !st.isEmpty()) {
                System.out.println("符号表:");
                System.out.println(st);
            }
            System.out.println();
            return;
        }

        Set<Integer> errLines = new TreeSet<>();
        if (lexMap != null) errLines.addAll(lexMap.keySet());
        if (semMap != null) errLines.addAll(semMap.keySet());

        for (int ln : errLines) {
            List<String> le = lexMap.get(ln);
            if (le != null) {
                for (String emsg : le) {
                    System.out.println("(词法错误,行号:" + ln + ")");
                }
            }
            List<String> se = semMap.get(ln);
            if (se != null) {
                for (String emsg : se) {
                    System.out.println("(语义错误," + emsg + ")");
                }
            }
        }
        System.out.println();
    }

    static void batch(File d) throws Exception {
        File[] fs = d.listFiles((dir, n) -> n.endsWith(".pl0"));
        if (fs != null) {
            for (int i = 0; i < fs.length; i++) {
                System.out.println("\n===== Test " + (i + 1) + "/" + fs.length
                    + ": " + fs[i].getName() + " =====");
                file(fs[i]);
            }
        }
    }
}
