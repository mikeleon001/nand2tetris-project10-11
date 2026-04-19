# Nand2Tetris Projects 10 & 11 - Jack Compiler

A two-stage compiler for the Jack programming language, built as part of the Nand2Tetris course (The Elements of Computing Systems).

## Repository Structure

### Project10/ - Syntax Analyzer
Parses Jack source code and outputs a structured XML parse tree.
- `JackAnalyzer.java` - Top-level driver, handles file/directory input
- `JackTokenizer.java` - Lexical analyzer, tokenizes Jack source code
- `CompilationEngine.java` - Recursive descent parser, outputs XML

### Project11/ - Code Generator
Compiles Jack source code into Hack VM code.
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

## Test Results
### Project 10
All 7 parse tree comparisons passed using the Nand2Tetris TextComparer utility across all three test programs (ArrayTest, ExpressionLessSquare, Square).

### Project 11
All 6 test programs compile and run correctly:
- Seven
- Average
- ConvertToBin
- Square
- Pong
- ComplexArrays
