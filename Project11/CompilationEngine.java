import java.io.*;

public class CompilationEngine {

    private JackTokenizer tokenizer;
    private VMWriter vm;
    private SymbolTable symbols;

    private String className;
    private int labelCounter = 0;

    public CompilationEngine(JackTokenizer tokenizer, File outputFile) throws IOException {
        this.tokenizer = tokenizer;
        this.vm = new VMWriter(outputFile);
        this.symbols = new SymbolTable();
    }

    // Helpers

    private void eat() {
        tokenizer.advance();
    }

    private String eatAndGet() {
        tokenizer.advance();
        switch (tokenizer.tokenType()) {
            case KEYWORD:    return tokenizer.keyWord();
            case IDENTIFIER: return tokenizer.identifier();
            case SYMBOL:     return String.valueOf(tokenizer.symbol());
            case INT_CONST:  return String.valueOf(tokenizer.intVal());
            case STRING_CONST: return tokenizer.stringVal();
            default: return "";
        }
    }

    private String newLabel() {
        return "L" + (labelCounter++);
    }

    // Map symbol table kind to VM segment
    private VMWriter.Segment kindToSegment(SymbolTable.Kind kind) {
        switch (kind) {
            case STATIC: return VMWriter.Segment.STATIC;
            case FIELD:  return VMWriter.Segment.THIS;
            case ARG:    return VMWriter.Segment.ARG;
            case VAR:    return VMWriter.Segment.LOCAL;
            default:     return VMWriter.Segment.TEMP;
        }
    }

    // Top-level

    // 'class' className '{' classVarDec* subroutineDec* '}'
    public void compileClass() {
        eat(); // class
        className = eatAndGet(); // className
        eat(); // {
        while (tokenizer.peek().equals("static") || tokenizer.peek().equals("field")) {
            compileClassVarDec();
        }
        while (tokenizer.peek().equals("constructor") || tokenizer.peek().equals("function")
                || tokenizer.peek().equals("method")) {
            compileSubroutine();
        }
        eat(); // }
        vm.close();
    }

    // Class-level declarations

    // ('static'|'field') type varName (',' varName)* ';'
    public void compileClassVarDec() {
        String kindStr = eatAndGet(); // static | field
        SymbolTable.Kind kind = kindStr.equals("static") ? SymbolTable.Kind.STATIC : SymbolTable.Kind.FIELD;
        String type = eatAndGet(); // type
        String name = eatAndGet(); // varName
        symbols.define(name, type, kind);
        while (tokenizer.peek().equals(",")) {
            eat(); // ,
            name = eatAndGet(); // varName
            symbols.define(name, type, kind);
        }
        eat(); // ;
    }

    // ('constructor'|'function'|'method') ('void'|type) subroutineName
    // '(' parameterList ')' subroutineBody
    public void compileSubroutine() {
        symbols.startSubroutine();

        String keyword = eatAndGet(); // constructor | function | method
        eat(); // return type
        String subroutineName = eatAndGet(); // subroutineName

        // 'method' gets implicit 'this' as argument 0
        if (keyword.equals("method")) {
            symbols.define("this", className, SymbolTable.Kind.ARG);
        }

        eat(); // (
        compileParameterList();
        eat(); // )
        compileSubroutineBody(className + "." + subroutineName, keyword);
    }

    // ((type varName) (',' type varName)*)?
    public void compileParameterList() {
        if (!tokenizer.peek().equals(")")) {
            String type = eatAndGet(); // type
            String name = eatAndGet(); // varName
            symbols.define(name, type, SymbolTable.Kind.ARG);
            while (tokenizer.peek().equals(",")) {
                eat(); // ,
                type = eatAndGet(); // type
                name = eatAndGet(); // varName
                symbols.define(name, type, SymbolTable.Kind.ARG);
            }
        }
    }

    // '{' varDec* statements '}'
    public void compileSubroutineBody(String fullName, String keyword) {
        eat(); // {
        while (tokenizer.peek().equals("var")) {
            compileVarDec();
        }

        int nLocals = symbols.varCount(SymbolTable.Kind.VAR);
        vm.writeFunction(fullName, nLocals);

        if (keyword.equals("constructor")) {
            // Allocate memory for the object's fields
            int nFields = symbols.varCount(SymbolTable.Kind.FIELD);
            vm.writePush(VMWriter.Segment.CONST, nFields);
            vm.writeCall("Memory.alloc", 1);
            vm.writePop(VMWriter.Segment.POINTER, 0); // anchor THIS
        } else if (keyword.equals("method")) {
            // Set THIS to the object passed as argument 0
            vm.writePush(VMWriter.Segment.ARG, 0);
            vm.writePop(VMWriter.Segment.POINTER, 0);
        }

        compileStatements();
        eat(); // }
    }

    // 'var' type varName (',' varName)* ';'
    public void compileVarDec() {
        eat(); // var
        String type = eatAndGet(); // type
        String name = eatAndGet(); // varName
        symbols.define(name, type, SymbolTable.Kind.VAR);
        while (tokenizer.peek().equals(",")) {
            eat(); // ,
            name = eatAndGet(); // varName
            symbols.define(name, type, SymbolTable.Kind.VAR);
        }
        eat(); // ;
    }

    // Statements

    public void compileStatements() {
        while (true) {
            String next = tokenizer.peek();
            if      (next.equals("let"))    compileLet();
            else if (next.equals("if"))     compileIf();
            else if (next.equals("while"))  compileWhile();
            else if (next.equals("do"))     compileDo();
            else if (next.equals("return")) compileReturn();
            else break;
        }
    }

    // 'let' varName ('[' expression ']')? '=' expression ';'
    public void compileLet() {
        eat(); // let
        String varName = eatAndGet(); // varName
        boolean isArray = tokenizer.peek().equals("[");

        if (isArray) {
            // Push base address of array
            vm.writePush(kindToSegment(symbols.kindOf(varName)), symbols.indexOf(varName));
            eat(); // [
            compileExpression(); // index
            eat(); // ]
            vm.writeArithmetic(VMWriter.Command.ADD); // base + index = target address
        }

        eat(); // =
        compileExpression(); // right-hand side value is now on stack

        if (isArray) {
            // Store value into array[index]
            vm.writePop(VMWriter.Segment.TEMP, 0);    // save value
            vm.writePop(VMWriter.Segment.POINTER, 1); // set THAT to target address
            vm.writePush(VMWriter.Segment.TEMP, 0);   // restore value
            vm.writePop(VMWriter.Segment.THAT, 0);    // store into that 0
        } else {
            vm.writePop(kindToSegment(symbols.kindOf(varName)), symbols.indexOf(varName));
        }
        eat(); // ;
    }

    // 'if' '(' expression ')' '{' statements '}' ('else' '{' statements '}')?
    public void compileIf() {
        String labelTrue  = newLabel();
        String labelFalse = newLabel();

        eat(); // if
        eat(); // (
        compileExpression();
        eat(); // )

        vm.writeIf(labelTrue);
        vm.writeGoto(labelFalse);
        vm.writeLabel(labelTrue);

        eat(); // {
        compileStatements();
        eat(); // }

        if (tokenizer.peek().equals("else")) {
            String labelEnd = newLabel();
            vm.writeGoto(labelEnd);
            vm.writeLabel(labelFalse);
            eat(); // else
            eat(); // {
            compileStatements();
            eat(); // }
            vm.writeLabel(labelEnd);
        } else {
            vm.writeLabel(labelFalse);
        }
    }

    // 'while' '(' expression ')' '{' statements '}'
    public void compileWhile() {
        String labelStart = newLabel();
        String labelEnd   = newLabel();

        eat(); // while
        vm.writeLabel(labelStart);
        eat(); // (
        compileExpression();
        eat(); // )

        vm.writeArithmetic(VMWriter.Command.NOT);
        vm.writeIf(labelEnd);

        eat(); // {
        compileStatements();
        eat(); // }

        vm.writeGoto(labelStart);
        vm.writeLabel(labelEnd);
    }

    // 'do' subroutineCall ';'
    public void compileDo() {
        eat(); // do
        compileSubroutineCall();
        eat(); // ;
        vm.writePop(VMWriter.Segment.TEMP, 0); // discard return value
    }

    // 'return' expression? ';'
    public void compileReturn() {
        eat(); // return
        if (!tokenizer.peek().equals(";")) {
            compileExpression();
        } else {
            vm.writePush(VMWriter.Segment.CONST, 0); // void return
        }
        eat(); // ;
        vm.writeReturn();
    }

    // Expressions

    private static final String OPS = "+-*/&|<>=";

    // term (op term)*
    public void compileExpression() {
        compileTerm();
        while (OPS.indexOf(tokenizer.peek()) >= 0) {
            char op = tokenizer.peek().charAt(0);
            eat(); // op
            compileTerm();
            switch (op) {
                case '+': vm.writeArithmetic(VMWriter.Command.ADD); break;
                case '-': vm.writeArithmetic(VMWriter.Command.SUB); break;
                case '*': vm.writeCall("Math.multiply", 2);         break;
                case '/': vm.writeCall("Math.divide", 2);           break;
                case '&': vm.writeArithmetic(VMWriter.Command.AND); break;
                case '|': vm.writeArithmetic(VMWriter.Command.OR);  break;
                case '<': vm.writeArithmetic(VMWriter.Command.LT);  break;
                case '>': vm.writeArithmetic(VMWriter.Command.GT);  break;
                case '=': vm.writeArithmetic(VMWriter.Command.EQ);  break;
            }
        }
    }

    public void compileTerm() {
        String next = tokenizer.peek();

        if (next.equals("(")) {
            eat(); // (
            compileExpression();
            eat(); // )
        } else if (next.equals("-") || next.equals("~")) {
            eat(); // unary op
            compileTerm();
            if (next.equals("-")) vm.writeArithmetic(VMWriter.Command.NEG);
            else                  vm.writeArithmetic(VMWriter.Command.NOT);
        } else {
            // Advance and determine what we got
            tokenizer.advance();
            JackTokenizer.TokenType type = tokenizer.tokenType();

            if (type == JackTokenizer.TokenType.INT_CONST) {
                vm.writePush(VMWriter.Segment.CONST, tokenizer.intVal());

            } else if (type == JackTokenizer.TokenType.STRING_CONST) {
                String str = tokenizer.stringVal();
                vm.writePush(VMWriter.Segment.CONST, str.length());
                vm.writeCall("String.new", 1);
                for (char c : str.toCharArray()) {
                    vm.writePush(VMWriter.Segment.CONST, (int) c);
                    vm.writeCall("String.appendChar", 2);
                }

            } else if (type == JackTokenizer.TokenType.KEYWORD) {
                String kw = tokenizer.keyWord();
                switch (kw) {
                    case "true":
                        vm.writePush(VMWriter.Segment.CONST, 0);
                        vm.writeArithmetic(VMWriter.Command.NOT);
                        break;
                    case "false":
                    case "null":
                        vm.writePush(VMWriter.Segment.CONST, 0);
                        break;
                    case "this":
                        vm.writePush(VMWriter.Segment.POINTER, 0);
                        break;
                }

            } else {
                // IDENTIFIER — could be varName, varName[expr], or subroutineCall
                String name = tokenizer.identifier();
                String after = tokenizer.peek();

                if (after.equals("[")) {
                    // Array access
                    vm.writePush(kindToSegment(symbols.kindOf(name)), symbols.indexOf(name));
                    eat(); // [
                    compileExpression();
                    eat(); // ]
                    vm.writeArithmetic(VMWriter.Command.ADD);
                    vm.writePop(VMWriter.Segment.POINTER, 1);
                    vm.writePush(VMWriter.Segment.THAT, 0);

                } else if (after.equals("(") || after.equals(".")) {
                    // Subroutine call — name is already consumed
                    compileSubroutineCallFromName(name);

                } else {
                    // Plain variable
                    vm.writePush(kindToSegment(symbols.kindOf(name)), symbols.indexOf(name));
                }
            }
        }
    }

    // subroutineCall where first identifier not yet consumed
    private void compileSubroutineCall() {
        String name = eatAndGet(); // subroutineName | className | varName
        compileSubroutineCallFromName(name);
    }

    // subroutineCall where first identifier already consumed
    private void compileSubroutineCallFromName(String name) {
        int nArgs = 0;
        String fullName;

        if (tokenizer.peek().equals(".")) {
            eat(); // .
            String subroutineName = eatAndGet();
            // Check if name is a variable (object call) or a class name (static call)
            SymbolTable.Kind kind = symbols.kindOf(name);
            if (kind != SymbolTable.Kind.NONE) {
                // Object method call — push object as implicit arg 0
                vm.writePush(kindToSegment(kind), symbols.indexOf(name));
                fullName = symbols.typeOf(name) + "." + subroutineName;
                nArgs = 1;
            } else {
                // Static function call
                fullName = name + "." + subroutineName;
            }
        } else {
            // Unqualified call — must be a method on current object
            vm.writePush(VMWriter.Segment.POINTER, 0); // push this
            fullName = className + "." + name;
            nArgs = 1;
        }

        eat(); // (
        nArgs += compileExpressionList();
        eat(); // )
        vm.writeCall(fullName, nArgs);
    }

    // Returns number of expressions compiled
    public int compileExpressionList() {
        int count = 0;
        if (!tokenizer.peek().equals(")")) {
            compileExpression();
            count++;
            while (tokenizer.peek().equals(",")) {
                eat(); // ,
                compileExpression();
                count++;
            }
        }
        return count;
    }
}
