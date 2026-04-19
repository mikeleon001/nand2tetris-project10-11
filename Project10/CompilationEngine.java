import java.io.*;

public class CompilationEngine {

    private JackTokenizer tokenizer;
    private PrintWriter writer;
    private int indent = 0;

    public CompilationEngine(JackTokenizer tokenizer, File outputFile) throws IOException {
        this.tokenizer = tokenizer;
        this.writer = new PrintWriter(new FileWriter(outputFile));
    }

    // Helpers

    private String ind() {
        return "  ".repeat(indent);
    }

    private void writeTag(String tag) {
        writer.println(ind() + tag);
    }

    private void openTag(String tag) {
        writeTag("<" + tag + ">");
        indent++;
    }

    private void closeTag(String tag) {
        indent--;
        writeTag("</" + tag + ">");
    }

    // Advance the tokenizer and write the current token as an XML terminal
    private void eat() {
        tokenizer.advance();
        JackTokenizer.TokenType type = tokenizer.tokenType();
        switch (type) {
            case KEYWORD:
                writeTag("<keyword> " + tokenizer.keyWord() + " </keyword>");
                break;
            case SYMBOL:
                char sym = tokenizer.symbol();
                String symStr;
                switch (sym) {
                    case '<': symStr = "&lt;"; break;
                    case '>': symStr = "&gt;"; break;
                    case '&': symStr = "&amp;"; break;
                    case '"': symStr = "&quot;"; break;
                    default:  symStr = String.valueOf(sym);
                }
                writeTag("<symbol> " + symStr + " </symbol>");
                break;
            case INT_CONST:
                writeTag("<integerConstant> " + tokenizer.intVal() + " </integerConstant>");
                break;
            case STRING_CONST:
                writeTag("<stringConstant> " + tokenizer.stringVal() + " </stringConstant>");
                break;
            case IDENTIFIER:
                writeTag("<identifier> " + tokenizer.identifier() + " </identifier>");
                break;
        }
    }

    // Top-level

    // 'class' className '{' classVarDec* subroutineDec* '}'
    public void compileClass() {
        openTag("class");
        eat(); // class
        eat(); // className
        eat(); // {
        while (tokenizer.peek().equals("static") || tokenizer.peek().equals("field")) {
            compileClassVarDec();
        }
        while (tokenizer.peek().equals("constructor") || tokenizer.peek().equals("function")
                || tokenizer.peek().equals("method")) {
            compileSubroutine();
        }
        eat(); // }
        closeTag("class");
        writer.close();
    }

    // Class-level declarations

    // ('static'|'field') type varName (',' varName)* ';'
    public void compileClassVarDec() {
        openTag("classVarDec");
        eat(); // static | field
        eat(); // type
        eat(); // varName
        while (tokenizer.peek().equals(",")) {
            eat(); // ,
            eat(); // varName
        }
        eat(); // ;
        closeTag("classVarDec");
    }

    // ('constructor'|'function'|'method') ('void'|type) subroutineName
    // '(' parameterList ')' subroutineBody
    public void compileSubroutine() {
        openTag("subroutineDec");
        eat(); // constructor | function | method
        eat(); // void | type
        eat(); // subroutineName
        eat(); // (
        compileParameterList();
        eat(); // )
        compileSubroutineBody();
        closeTag("subroutineDec");
    }

    // ((type varName) (',' type varName)*)?
    public void compileParameterList() {
        openTag("parameterList");
        // Empty parameter list
        if (!tokenizer.peek().equals(")")) {
            eat(); // type
            eat(); // varName
            while (tokenizer.peek().equals(",")) {
                eat(); // ,
                eat(); // type
                eat(); // varName
            }
        }
        closeTag("parameterList");
    }

    // '{' varDec* statements '}'
    public void compileSubroutineBody() {
        openTag("subroutineBody");
        eat(); // {
        while (tokenizer.peek().equals("var")) {
            compileVarDec();
        }
        compileStatements();
        eat(); // }
        closeTag("subroutineBody");
    }

    // 'var' type varName (',' varName)* ';'
    public void compileVarDec() {
        openTag("varDec");
        eat(); // var
        eat(); // type
        eat(); // varName
        while (tokenizer.peek().equals(",")) {
            eat(); // ,
            eat(); // varName
        }
        eat(); // ;
        closeTag("varDec");
    }

    // Statements

    public void compileStatements() {
        openTag("statements");
        while (true) {
            String next = tokenizer.peek();
            if      (next.equals("let"))    compileLet();
            else if (next.equals("if"))     compileIf();
            else if (next.equals("while"))  compileWhile();
            else if (next.equals("do"))     compileDo();
            else if (next.equals("return")) compileReturn();
            else break;
        }
        closeTag("statements");
    }

    // 'let' varName ('[' expression ']')? '=' expression ';'
    public void compileLet() {
        openTag("letStatement");
        eat(); // let
        eat(); // varName
        if (tokenizer.peek().equals("[")) {
            eat(); // [
            compileExpression();
            eat(); // ]
        }
        eat(); // =
        compileExpression();
        eat(); // ;
        closeTag("letStatement");
    }

    // 'if' '(' expression ')' '{' statements '}' ('else' '{' statements '}')?
    public void compileIf() {
        openTag("ifStatement");
        eat(); // if
        eat(); // (
        compileExpression();
        eat(); // )
        eat(); // {
        compileStatements();
        eat(); // }
        if (tokenizer.peek().equals("else")) {
            eat(); // else
            eat(); // {
            compileStatements();
            eat(); // }
        }
        closeTag("ifStatement");
    }

    // 'while' '(' expression ')' '{' statements '}'
    public void compileWhile() {
        openTag("whileStatement");
        eat(); // while
        eat(); // (
        compileExpression();
        eat(); // )
        eat(); // {
        compileStatements();
        eat(); // }
        closeTag("whileStatement");
    }

    // 'do' subroutineCall ';'
    public void compileDo() {
        openTag("doStatement");
        eat(); // do
        compileSubroutineCall();
        eat(); // ;
        closeTag("doStatement");
    }

    // 'return' expression? ';'
    public void compileReturn() {
        openTag("returnStatement");
        eat(); // return
        if (!tokenizer.peek().equals(";")) {
            compileExpression();
        }
        eat(); // ;
        closeTag("returnStatement");
    }

    // Expressions

    private static final String OPS = "+-*/&|<>=";

    // term (op term)*
    public void compileExpression() {
        openTag("expression");
        compileTerm();
        while (OPS.indexOf(tokenizer.peek()) >= 0) {
            eat(); // op
            compileTerm();
        }
        closeTag("expression");
    }

    // intConst | strConst | keywordConst | varName | varName'['expr']'
    // | subroutineCall | '('expr')' | unaryOp term
    public void compileTerm() {
        openTag("term");
        String next = tokenizer.peek();

        if (next.equals("(")) {
            eat(); // (
            compileExpression();
            eat(); // )
        } else if (next.equals("-") || next.equals("~")) {
            eat(); // unary op
            compileTerm();
        } else {
            eat(); // varName | intConst | strConst | keyword
            // Check what follows to distinguish varName, array access, subroutine call
            String after = tokenizer.peek();
            if (after.equals("[")) {
                eat(); // [
                compileExpression();
                eat(); // ]
            } else if (after.equals("(") || after.equals(".")) {
                // subroutineCall — we already ate the first identifier/className
                if (after.equals(".")) {
                    eat(); // .
                    eat(); // subroutineName
                }
                eat(); // (
                compileExpressionList();
                eat(); // )
            }
        }
        closeTag("term");
    }

    // subroutineName '(' expressionList ')' |
    // (className|varName) '.' subroutineName '(' expressionList ')'
    private void compileSubroutineCall() {
        eat(); // subroutineName | className | varName
        if (tokenizer.peek().equals(".")) {
            eat(); // .
            eat(); // subroutineName
        }
        eat(); // (
        compileExpressionList();
        eat(); // )
    }

    // (expression (',' expression)*)?
    public void compileExpressionList() {
        openTag("expressionList");
        if (!tokenizer.peek().equals(")")) {
            compileExpression();
            while (tokenizer.peek().equals(",")) {
                eat(); // ,
                compileExpression();
            }
        }
        closeTag("expressionList");
    }
}
