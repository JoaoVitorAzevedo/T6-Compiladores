package com.mycompany.miniquery;

import com.mycompany.miniquery.compiler.MiniQuerySemanticValidator;
import com.mycompany.miniquery.grammar.MiniQueryLexer;
import com.mycompany.miniquery.grammar.MiniQueryParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;


 // para compilação de um único arquivo.
 
public class Main {

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Uso: java -jar <caminho-do-jar> <arquivo-de-entrada.mq>");
            return;
        }

        String filePath = args[0];
        try {
            String input = Files.readString(Paths.get(filePath), StandardCharsets.UTF_8);
            String output = runCompiler(input);
            System.out.println(output);
        } catch (IOException e) {
            System.err.println("ERRO: Não foi possível ler o arquivo: " + filePath);
            e.printStackTrace();
        }
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

            MiniQuerySemanticValidator validator = new MiniQuerySemanticValidator();
            ParseTreeWalker.DEFAULT.walk(validator, tree);

            if (!validator.getErrors().isEmpty()) {
                result.append("✖ ERROS SEMÂNTICOS:\n");
                validator.getErrors().forEach(err -> result.append(err).append("\n"));
            } else {
                result.append("✔ ANÁLISE BEM-SUCEDIDA. CÓDIGO GERADO:\n\n");
                JavaScriptGenerator generator = new JavaScriptGenerator();
                result.append(generator.generate(validator.getCommands()));
            }
        } catch (Exception e) {
            result.append("💥 ERRO INESPERADO DURANTE A COMPILAÇÃO:\n");
            result.append(e.getMessage());
        }
        return result.toString().trim();
    }

    // Classe interna para capturar erros de sintaxe
    private static class SyntaxErrorListener extends BaseErrorListener {
        private final List<String> errors = new ArrayList<>();
        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
            errors.add("Linha " + line + ":" + charPositionInLine + " - " + msg);
        }
        public List<String> getErrors() { return Collections.unmodifiableList(errors); }
    }
}