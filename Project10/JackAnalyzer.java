import java.io.*;
import java.util.*;

public class JackAnalyzer {

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Usage: JackAnalyzer <file.jack | directory>");
            System.exit(1);
        }

        File input = new File(args[0]);
        List<File> jackFiles = new ArrayList<>();

        if (input.isDirectory()) {
            for (File f : input.listFiles()) {
                if (f.getName().endsWith(".jack")) jackFiles.add(f);
            }
        } else {
            jackFiles.add(input);
        }

        for (File jackFile : jackFiles) {
            String outputPath = jackFile.getAbsolutePath().replace(".jack", ".xml");
            File outputFile = new File(outputPath);

            JackTokenizer tokenizer = new JackTokenizer(jackFile);
            CompilationEngine engine = new CompilationEngine(tokenizer, outputFile);
            engine.compileClass();

            System.out.println("Compiled: " + jackFile.getName() + " -> " + outputFile.getName());
        }
    }
}
