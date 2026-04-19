import java.io.*;

public class VMWriter {

    public enum Segment { CONST, ARG, LOCAL, STATIC, THIS, THAT, POINTER, TEMP }
    public enum Command { ADD, SUB, NEG, EQ, GT, LT, AND, OR, NOT }

    private PrintWriter writer;

    public VMWriter(File outputFile) throws IOException {
        writer = new PrintWriter(new FileWriter(outputFile));
    }

    public void writePush(Segment segment, int index) {
        writer.println("push " + segmentName(segment) + " " + index);
    }

    public void writePop(Segment segment, int index) {
        writer.println("pop " + segmentName(segment) + " " + index);
    }

    public void writeArithmetic(Command command) {
        writer.println(command.name().toLowerCase());
    }

    public void writeLabel(String label) {
        writer.println("label " + label);
    }

    public void writeGoto(String label) {
        writer.println("goto " + label);
    }

    public void writeIf(String label) {
        writer.println("if-goto " + label);
    }

    public void writeCall(String name, int nArgs) {
        writer.println("call " + name + " " + nArgs);
    }

    public void writeFunction(String name, int nLocals) {
        writer.println("function " + name + " " + nLocals);
    }

    public void writeReturn() {
        writer.println("return");
    }

    public void close() {
        writer.close();
    }

    private String segmentName(Segment segment) {
        switch (segment) {
            case CONST:   return "constant";
            case ARG:     return "argument";
            case LOCAL:   return "local";
            case STATIC:  return "static";
            case THIS:    return "this";
            case THAT:    return "that";
            case POINTER: return "pointer";
            case TEMP:    return "temp";
            default:      return "";
        }
    }
}
