import java.io.*;
import java.util.*;

public class JackTokenizer {

    public enum TokenType {
        KEYWORD, SYMBOL, INT_CONST, STRING_CONST, IDENTIFIER
    }

    private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
        "class", "constructor", "function", "method", "field", "static",
        "var", "int", "char", "boolean", "void", "true", "false", "null",
        "this", "let", "do", "if", "else", "while", "return"
    ));

    private static final Set<Character> SYMBOLS = new HashSet<>(Arrays.asList(
        '{', '}', '(', ')', '[', ']', '.', ',', ';',
        '+', '-', '*', '/', '&', '|', '<', '>', '=', '~'
    ));

    private List<String> tokens = new ArrayList<>();
    private int current = -1;

    public JackTokenizer(File file) throws IOException {
        String source = readFile(file);
        source = stripComments(source);
        tokenize(source);
    }

    private String readFile(File file) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }

    // Strip // and /* */ comments, but not inside string literals
    private String stripComments(String source) {
        StringBuilder result = new StringBuilder();
        int i = 0;
        while (i < source.length()) {
            // Inside string literal — copy verbatim until closing quote
            if (source.charAt(i) == '"') {
                int end = source.indexOf('"', i + 1);
                result.append(source, i, end + 1);
                i = end + 1;
            }
            // Multi-line comment /* ... */
            else if (i + 1 < source.length() && source.charAt(i) == '/' && source.charAt(i + 1) == '*') {
                int end = source.indexOf("*/", i + 2);
                // Replace with spaces to preserve line count
                String skipped = source.substring(i, end + 2);
                for (char c : skipped.toCharArray()) {
                    result.append(c == '\n' ? '\n' : ' ');
                }
                i = end + 2;
            }
            // Single-line comment // ...
            else if (i + 1 < source.length() && source.charAt(i) == '/' && source.charAt(i + 1) == '/') {
                while (i < source.length() && source.charAt(i) != '\n') i++;
            }
            else {
                result.append(source.charAt(i));
                i++;
            }
        }
        return result.toString();
    }

    private void tokenize(String source) {
        int i = 0;
        while (i < source.length()) {
            char c = source.charAt(i);

            // Skip whitespace
            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }

            // String constant
            if (c == '"') {
                int end = source.indexOf('"', i + 1);
                tokens.add("\"" + source.substring(i + 1, end) + "\"");
                i = end + 1;
                continue;
            }

            // Symbol
            if (SYMBOLS.contains(c)) {
                tokens.add(String.valueOf(c));
                i++;
                continue;
            }

            // Integer constant
            if (Character.isDigit(c)) {
                int start = i;
                while (i < source.length() && Character.isDigit(source.charAt(i))) i++;
                tokens.add(source.substring(start, i));
                continue;
            }

            // Keyword or identifier
            if (Character.isLetter(c) || c == '_') {
                int start = i;
                while (i < source.length() && (Character.isLetterOrDigit(source.charAt(i)) || source.charAt(i) == '_')) i++;
                tokens.add(source.substring(start, i));
                continue;
            }

            i++; // skip unrecognized character
        }
    }

    public boolean hasMoreTokens() {
        return current < tokens.size() - 1;
    }

    public void advance() {
        current++;
    }

    public TokenType tokenType() {
        String t = tokens.get(current);
        if (KEYWORDS.contains(t)) return TokenType.KEYWORD;
        if (t.length() == 1 && SYMBOLS.contains(t.charAt(0))) return TokenType.SYMBOL;
        if (t.startsWith("\"")) return TokenType.STRING_CONST;
        if (Character.isDigit(t.charAt(0))) return TokenType.INT_CONST;
        return TokenType.IDENTIFIER;
    }

    public String keyWord() { return tokens.get(current); }
    public char symbol() { return tokens.get(current).charAt(0); }
    public String identifier() { return tokens.get(current); }
    public int intVal() { return Integer.parseInt(tokens.get(current)); }
    public String stringVal() {
        String t = tokens.get(current);
        return t.substring(1, t.length() - 1); // strip surrounding quotes
    }

    // Peek at the next token without advancing
    public String peek() {
        if (current + 1 < tokens.size()) return tokens.get(current + 1);
        return "";
    }

    // Get current token as raw string
    public String currentToken() {
        return tokens.get(current);
    }
}
