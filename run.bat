@echo off
chcp 65001>nul
echo ====================================
echo PL/0 Semantic Analyzer
echo Build and Run Script
echo ====================================
cd /d "%~dp0"
if not exist out mkdir out
echo [1/2] Compiling...
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

if %ERRORLEVEL% NEQ 0 (
    echo [ERR] Compilation failed
    pause
    exit /b 1
)
echo [2/2] Running...
if "%1"=="" (
    java -cp out main.Main
) else (
    java -cp out main.Main %1
)
pause
