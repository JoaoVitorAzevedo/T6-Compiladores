package com.mycompany.miniquery;

import com.mycompany.miniquery.compiler.MiniQuerySemanticValidator;
import com.mycompany.miniquery.grammar.MiniQueryLexer;
import com.mycompany.miniquery.grammar.MiniQueryParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

public class TestRunner {

    private static final String TEST_DIR = "src/test/resources/";
    private static final String OUTPUT_DIR = "target/test-results/";
    private static final String EXPECTED_DIR = "src/test/expected/";

    public static void main(String[] args) throws IOException {
        System.out.println("Iniciando execucao dos testes...");
        long startTime = System.currentTimeMillis();

        Files.createDirectories(Paths.get(OUTPUT_DIR));

        List<TestResult> results = Files.list(Paths.get(TEST_DIR))
                .filter(p -> p.toString().endsWith(".mq"))
                .sorted()
                .map(TestRunner::executeTest)
                .collect(Collectors.toList());

        generateReport(results);

        long duration = System.currentTimeMillis() - startTime;
        System.out.printf("Testes concluidos em %dms! Verifique os resultados em: %s%n",
                duration, Paths.get(OUTPUT_DIR).toAbsolutePath());
    }

    private static TestResult executeTest(Path testFile) {
        String testName = testFile.getFileName().toString().replace(".mq", "");
        TestResult result = new TestResult(testName);

        try {
            String testContent = Files.readString(testFile, StandardCharsets.UTF_8);
            result.input = testContent;
            result.output = runCompiler(testContent);

            Path expectedFile = Paths.get(EXPECTED_DIR, testName + ".expected.txt");
            if (Files.exists(expectedFile)) {
                String expected = Files.readString(expectedFile, StandardCharsets.UTF_8);
                
                String normalizedOutput = result.output.trim().replaceAll("\\r\\n", "\n");
                String normalizedExpected = expected.trim().replaceAll("\\r\\n", "\n");

                result.passed = normalizedOutput.equals(normalizedExpected);
            } else {
                result.passed = false;
            }

            Files.writeString(
                    Paths.get(OUTPUT_DIR, testName + ".result.txt"),
                    formatTestOutput(result),
                    StandardCharsets.UTF_8
            );

        } catch (Exception e) {
            result.output = "ERRO DURANTE EXECUÇÃO DO TESTE:\n" + e.getMessage();
            result.passed = false;
        }
        return result;
    }

    private static String runCompiler(String input) {
        StringBuilder result = new StringBuilder();
        try {
            CharStream stream = CharStreams.fromString(input);
            MiniQueryLexer lexer = new MiniQueryLexer(stream);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            MiniQueryParser parser = new MiniQueryParser(tokens);
            parser.removeErrorListeners();
            SyntaxErrorListener errorListener = new SyntaxErrorListener();
            parser.addErrorListener(errorListener);

            ParseTree tree = parser.program();

            if (!errorListener.getErrors().isEmpty()) {
                errorListener.getErrors().forEach(err -> result.append("ERRO DE SINTAXE: ").append(err).append("\n"));
                return result.toString().trim();
            }

            
            // Instanciado MiniQuerySemanticValidator diretamente.
            MiniQuerySemanticValidator validator = new MiniQuerySemanticValidator();

            // validator para percorrer a árvore. Ele vai coletar os comandos e aplicar as validações.
            ParseTreeWalker.DEFAULT.walk(validator, tree);

            // Verifica se o validator encontrou erros semânticos.
            if (!validator.getErrors().isEmpty()) {
                result.append("ERROS SEMÂNTICOS:\n");
                validator.getErrors().forEach(err -> result.append(err).append("\n"));
            } else {
                // Se não há erros, o  mesmo validator é usado para obter os comandos e gerar o código.
                result.append("ANÁLISE SEMÂNTICA VÁLIDA\n\n");
                JavaScriptGenerator generator = new JavaScriptGenerator();
                result.append("CÓDIGO JS GERADO:\n")
                      .append(generator.generate(validator.getCommands()));
            }
        } catch (Exception e) {
            result.append("ERRO DURANTE A ANÁLISE:\n")
                  .append(e.getClass().getSimpleName())
                  .append(": ")
                  .append(e.getMessage());
        }
        return result.toString().trim();
    }


    private static void generateReport(List<TestResult> results) throws IOException {
        StringBuilder report = new StringBuilder();
        report.append("=== RELATORIO DE TESTES ===\n");
        report.append("Data: ").append(new Date()).append("\n\n");
        long passedCount = results.stream().filter(r -> r.passed).count();
        report.append("->  RESUMO: ").append(passedCount).append("/").append(results.size()).append(" testes passaram\n\n");
        for (TestResult result : results) {
            report.append(formatTestOutput(result)).append("\n\n");
        }
        Files.writeString(Paths.get(OUTPUT_DIR, "RELATORIO_COMPLETO.txt"), report.toString(), StandardCharsets.UTF_8);
    }

    private static String formatTestOutput(TestResult result) {
        return String.format("-> Teste: %s\nStatus: %s\n--- Entrada ---\n%s\n--- Resultado ---\n%s",
                result.testName, (result.passed ? "V PASSOU" : "X FALHOU"), result.input, result.output);
    }

    private static class SyntaxErrorListener extends BaseErrorListener {
        private final List<String> errors = new ArrayList<>();
        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
            errors.add("Linha " + line + ":" + charPositionInLine + " - " + msg);
        }
        public List<String> getErrors() { return Collections.unmodifiableList(errors); }
    }

    private static class TestResult {
        String testName;
        String input;
        String output;
        boolean passed = true;
        TestResult(String testName) { this.testName = testName; }
    }
}