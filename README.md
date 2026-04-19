# Nand2Tetris Projects 10 & 11 - Jack Compiler

A two-stage compiler for the Jack programming language, built as part of the Nand2Tetris course (The Elements of Computing Systems).

## Project 10 - Syntax Analyzer
Parses Jack source code and outputs a structured XML parse tree.

## Project 11 - Code Generator
Compiles Jack source code into Hack VM code.

## Files
- `JackAnalyzer.java` - Top-level driver, handles file/directory input
- `JackTokenizer.java` - Lexical analyzer, tokenizes Jack source code
- `CompilationEngine.java` - Recursive descent compiler, emits VM code
- `SymbolTable.java` - Tracks variables across class and subroutine scopes
- `VMWriter.java` - Writes VM commands to output file

## How to Compile
```bash
javac *.java
```

## How to Run
```bash
java JackAnalyzer <file.jack | directory>
```

## Test Programs
All 6 Nand2Tetris Project 11 test programs pass:
- Seven
- Average
- ConvertToBin
- Square
- Pong
- ComplexArrays
