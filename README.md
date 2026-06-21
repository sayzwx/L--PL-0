# PL/0 语义分析器 — S-翻译模式 + 四元式生成

> 基于 **递归下降语法分析** 与 **S-翻译模式** 的语义分析程序，将 PL/0 源程序翻译为 **四元式(Quadruple)** 形式的中间代码。

---

## 目录

- [项目概述](#项目概述)
- [快速开始](#快速开始)
- [项目结构](#项目结构)
- [核心组件](#核心组件)
- [四元式格式](#四元式格式)
- [测试用例](#测试用例)
- [设计文档](#设计文档)

---

## 项目概述

### 核心能力

| 功能 | 说明 |
|------|------|
| **词法分析** | 基于 DFA 的手工词法分析器，识别关键字、标识符、数字、运算符、界符、注释 |
| **语法+语义分析** | 递归下降解析 + 语法制导翻译，分析同时执行语义动作 |
| **中间代码生成** | 输出标准四元式 `(op, arg1, arg2, result)` |
| **回填技术** | if/while 控制流的跳转地址延迟回填 |
| **符号表管理** | 栈式作用域，支持嵌套过程 |
| **错误检测** | 非法字符、非法单词、标识符超长、数字越界、未声明变量、重复声明等 |

### 输入 → 输出

```
源代码 (.pl0)
    │
    ▼
┌──────────┐   Token流   ┌──────────────┐   四元式    ┌──────────┐
│  Lexer   │ ──────────► │  LR1Parser   │ ──────────► │  输出    │
│ 词法分析  │            │ 语法+语义分析  │            │          │
└──────────┘             └──────┬───────┘            └──────────┘
                                │
                     ┌──────────┼──────────┐
                     ▼          ▼          ▼
              ┌──────────┐ ┌────────┐ ┌──────────┐
              │  符号表   │ │ 四元式 │ │ 错误列表 │
              └──────────┘ └────────┘ └──────────┘
```

---

## 快速开始

### 环境要求

- JDK 8+
- Windows / Linux / macOS

### 编译 & 运行

```bash
# 一键编译运行（示例模式）
run.bat

# 分析指定文件
run.bat test_cases/correct.pl0

# 手动编译
javac -d out -encoding UTF-8 ^
    src\lexer\Token.java ^
    src\lexer\LexerError.java ^
    src\lexer\Lexer.java ^
    src\quadruple\Quadruple.java ^
    src\quadruple\QuadrupleManager.java ^
    src\symbol\Symbol.java ^
    src\symbol\SymbolTable.java ^
    src\parser\SemanticError.java ^
    src\parser\LR1Parser.java ^
    src\main\Main.java

# 示例模式
java -cp out main.Main

# 单文件模式
java -cp out main.Main test_cases/correct.pl0

# 批量模式
java -cp out main.Main test_cases/
```

---

## 项目结构

```
PL0_SemanticAnalyzer_STranslation/
│
├── README.md                     # 本文档
├── run.bat                       # 一键编译运行脚本
│
├── src/                          # Java 源文件
│   ├── main/
│   │   └── Main.java            # 主入口（示例/单文件/批量）
│   │
│   ├── lexer/                   # 词法分析
│   │   ├── Token.java           # 词法单元 (kind, value, lineno)
│   │   ├── LexerError.java      # 词法错误 (msg, lineno)
│   │   └── Lexer.java           # DFA 词法分析器
│   │
│   ├── parser/                  # 语法+语义分析
│   │   ├── SemanticError.java   # 语义错误
│   │   └── LR1Parser.java       # 递归下降解析器 + 语法制导翻译
│   │
│   ├── symbol/                  # 符号表
│   │   ├── Symbol.java          # 符号条目 (name, kind, value, level)
│   │   └── SymbolTable.java     # 栈式作用域管理器
│   │
│   └── quadruple/               # 四元式
│       ├── Quadruple.java       # 四元式 (op, arg1, arg2, result)
│       └── QuadrupleManager.java# 四元式生成器 + 回填管理器
│
├── out/                         # 编译输出 (.class)
├── test_cases/                  # 测试用例
│   ├── correct.pl0             # 正确程序
│   └── semantic_error.pl0      # 含语义错误的程序
│
├── Docs/                        # 设计文档
│   ├── 文法.md                 # PL/0 语言文法定义
│   ├── 数据结构.md              # 核心数据结构说明
│   ├── 设计原理.md              # 设计原理
│   ├── 设计流程图.md            # 流程图（Mermaid）
│   └── 核心代码.md              # 核心代码提炼
│
└── 编译原理课程设计任务要求2026夏季学期.pdf
```

---

## 核心组件

### 1. 词法分析器 — `Lexer.java`

基于 DFA 的手工词法分析，逐行扫描源代码：

```java
// 识别 14 个关键字
static final Set<String> KW = new HashSet<>(Arrays.asList(
    "const", "var", "procedure", "call", "begin", "end",
    "if", "then", "else", "while", "do", "read", "write", "odd"
));
```

**识别的 Token 类型：**

| 类型 | 举例 |
|------|------|
| 关键字 | `const`, `var`, `if`, `else`, `while`... |
| 标识符 | `a`, `b`, `counter`（长度≤8） |
| 数字 | `0`, `10`, `12345678`（位数≤8） |
| 运算符 | `:=`, `=`, `#`, `<`, `<=`, `>`, `>=`, `+`, `-`, `*`, `/` |
| 界符 | `(`, `)`, `;`, `,`, `.` |
| 注释 | `//` 单行, `/* ... */` 多行 |

**错误检测：** 非法字符(`@ & !`)、非法单词(`2a`)、标识符超长、数字越界

### 2. 四元式管理器 — `QuadrupleManager.java`

核心操作：

| 方法 | 说明 |
|------|------|
| `emit(op, a1, a2, r)` | 发射一条四元式，返回 1-based 序号 |
| `newTemp()` | 分配新临时变量 `T1`, `T2`... |
| `newLabel()` | 创建新回填标签 `$1`, `$2`... |
| `emitWP(op, a1, a2, label)` | 带待回填的发射 |
| `patch(label)` | 回填标签为当前位置 |

### 3. 符号表 — `SymbolTable.java`

栈式作用域管理：

| 方法 | 说明 |
|------|------|
| `enterScope()` | 进入新作用域（push） |
| `exitScope()` | 退出当前作用域（pop） |
| `add(name, kind, value)` | 当前层添加符号，重复返回 false |
| `lookup(name)` | 从内向外逐层查找 |

### 4. 语法+语义分析器 — `LR1Parser.java`

递归下降解析方法链：

```
parseProgram()     程序 → 块 + '.'
  parseBlock()     块 → [常量] [变量] {过程} 语句
    parseConst()    常量声明 const id = num ;
    parseVar()      变量声明 var id {, id} ;
    parseProc()     过程声明 procedure id ; block ;
    parseStmt()     语句分发
      parseAssign()  赋值 id := expr ;
      parseRead()    读 read(id) ;
      parseWrite()   写 write(expr) ;
      parseCall()    调用 call id ;
      parseComp()    复合 begin stmtList end
      parseIf()      条件 if cond then stmt [else stmt]
      parseWhile()   循环 while cond do stmt
      parseExpr()→parseETail()→parseTerm()→parseTTail()→parseFact()
```

---

## 四元式格式

### 算术与赋值

| 四元式 | 含义 |
|--------|------|
| `(+, a, b, T1)` | T1 = a + b |
| `(-, a, b, T1)` | T1 = a - b |
| `(*, a, b, T1)` | T1 = a * b |
| `(/, a, b, T1)` | T1 = a / b |
| `(:=, a, _, x)` | x = a |

### 控制流

| 四元式 | 含义 |
|--------|------|
| `(j, _, _, N)` | 无条件跳转到第 N 条 |
| `(j<, a, b, N)` | if a < b goto N |
| `(j<=, a, b, N)` | if a <= b goto N |
| `(j=, a, b, N)` | if a = b goto N |
| `(j#, a, b, N)` | if a != b goto N |
| `(j>, a, b, N)` | if a > b goto N |
| `(j>=, a, b, N)` | if a >= b goto N |

### I/O 与过程

| 四元式 | 含义 |
|--------|------|
| `(read, _, _, x)` | 从输入读取到 x |
| `(write, _, _, x)` | 输出 x 的值 |
| `(call, p, _, _)` | 调用过程 p |
| `(syss, _, _, _)` | 程序开始标记 |
| `(syse, _, _, _)` | 程序结束标记 |

---

## 测试用例

### 正确程序

```pascal
const a = 10;
var b, c;
procedure p;
if a <= 10 then
begin
    c := b + a;
end;
begin
    read(b);
    while b # 0 do
    begin
        call p;
        write(2 * c);
        read(b);
    end
end.
```

**输出四元式：**

```
(1)(syss,_,_,_)
(2)(const,a,_,_)
(3)(=,10,_,a)
(4)(var,b,_,_)
(5)(var,c,_,_)
(6)(procedure,p,_,_)
(7)(<=,a,10,T1)
(8)(j=,T1,0,12)
(9)(+,b,a,T2)
(10)(:=,T2,_,c)
(11)(j,_,_,12)
(12)(read,b,_,_)
(13)(#,b,0,T3)
(14)(j=,T3,0,20)
(15)(call,p,_,_)
(16)(*,2,c,T4)
(17)(write,T4,_,_)
(18)(read,b,_,_)
(19)(j,_,_,13)
(20)(syse,_,_,_)
```

### 含语义错误的程序

```pascal
const a = 10;
var a, b, c;        ← 行2: a 重复声明
...
```

**输出错误：**

```
(语义错误,行号:2)
```

---

## 设计文档

| 文档 | 说明 |
|------|------|
| [文法.md](Docs/文法.md) | PL/0 语言的词法规则与 EBNF 文法 |
| [数据结构.md](Docs/数据结构.md) | 核心数据结构的类图与字段说明 |
| [设计原理.md](Docs/设计原理.md) | S-翻译模式、回填技术等设计原理 |
| [设计流程图.md](Docs/设计流程图.md) | 各模块的 Mermaid 流程图 |
| [核心代码.md](Docs/核心代码.md) | 各模块核心 Java 代码提炼 |

---

## PL/0 语言文法速查

```
程序      → 块 .
块        → [const 常量定义 {, 常量定义};]
            [var 标识符 {, 标识符};]
            {procedure 标识符; 块;}
            语句
常量定义  → 标识符 = 无符号整数
语句      → 赋值 | 条件 | 循环 | 调用 | 读 | 写 | 复合 | 空
赋值语句  → 标识符 := 表达式 ;
条件语句  → if 条件 then 语句 [else 语句]
循环语句  → while 条件 do 语句
调用语句  → call 标识符 ;
读语句    → read(标识符) ;
写语句    → write(表达式) ;
复合语句  → begin 语句 {; 语句} end
条件      → odd 表达式 | 表达式 relop 表达式
表达式    → [+|-] 项 {(+|-) 项}
项        → 因子 {(*|/) 因子}
因子      → 标识符 | 数字 | ( 表达式 )
```
