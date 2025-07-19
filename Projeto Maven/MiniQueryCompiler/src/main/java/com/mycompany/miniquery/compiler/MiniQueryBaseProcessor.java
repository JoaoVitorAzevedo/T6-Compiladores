package com.mycompany.miniquery.compiler;

import com.mycompany.miniquery.grammar.*;
import java.util.*;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

public class MiniQueryBaseProcessor extends MiniQueryBaseListener {

    protected final List<Command> commands = new ArrayList<>();
    protected String currentJsonFile;
    protected final List<String> warnings = new ArrayList<>();

    public List<Command> getCommands() {
        return new ArrayList<>(commands);
    }

    public List<String> getWarnings() {
        return new ArrayList<>(warnings);
    }

    protected String unquoteString(String text) {
        return text.replaceAll("^\"|\"$", "");
    }

    @Override
    public void enterLoadCommand(MiniQueryParser.LoadCommandContext ctx) {
        String filePath = unquoteString(ctx.STRING().getText());
        commands.add(new LoadCommand(filePath, ctx.start.getLine()));
        currentJsonFile = filePath;
    }

    
    @Override
    public void enterFilterCommand(MiniQueryParser.FilterCommandContext ctx) {
        // Captura o operando da esquerda (ex: .cliente.idade)
        String leftOperand = processValue(ctx.left);

        // Captura o operador (ex: >) usando o método gerado pelo ANTLR
        String operator = ctx.OPERATOR().getText();

        // Captura o operando da direita (ex: 18)
        String rightOperand = processValue(ctx.right);

        // Cria o comando 'FilterCondition' com os argumentos corretos.
        // O primeiro argumento é o caminho do campo a ser filtrado.
        commands.add(new FilterCondition(leftOperand, operator, rightOperand, ctx.start.getLine()));
    }

   
    private String processValue(MiniQueryParser.ValueContext valueCtx) {
        if (valueCtx.STRING() != null) {
            return unquoteString(valueCtx.STRING().getText());
        } else if (valueCtx.NUMBER() != null) {
            return valueCtx.NUMBER().getText();
        } else {
            return valueCtx.jsonPath().getText();
        }
    }

    @Override
    public void enterSelectCommand(MiniQueryParser.SelectCommandContext ctx) {
        List<String> fields = new ArrayList<>();
        for (MiniQueryParser.JsonPathContext path : ctx.jsonPath()) {
            fields.add(path.getText());
        }
        commands.add(new SelectCommand(fields, ctx.start.getLine()));
    }

    @Override
    public void enterSaveCommand(MiniQueryParser.SaveCommandContext ctx) {
        String outputFile = unquoteString(ctx.STRING().getText());
        commands.add(new SaveCommand(outputFile, ctx.start.getLine()));
    }

    
    public static abstract class Command {
        public final int lineNumber;
        public Command(int lineNumber) { this.lineNumber = lineNumber; }
        @Override public abstract String toString();
    }

    public static class LoadCommand extends Command {
        public final String filePath;
        public LoadCommand(String filePath, int lineNumber) { super(lineNumber); this.filePath = filePath; }
        @Override public String toString() { return String.format("LOAD \"%s\"", filePath); }
    }

    public static class FilterCondition extends Command {
        public final String jsonPath; // Representa o operando da esquerda
        public final String operator;
        public final String value;    // Representa o operando da direita
        public FilterCondition(String jsonPath, String operator, String value, int lineNumber) {
            super(lineNumber);
            this.jsonPath = jsonPath;
            this.operator = operator;
            this.value = value;
        }
        @Override public String toString() {
            String formattedValue = value.matches("-?\\d+(\\.\\d+)?") ? value : "\"" + value + "\"";
            return String.format("FILTER %s %s %s", jsonPath, operator, formattedValue);
        }
    }

    public static class SelectCommand extends Command {
        public final List<String> fields;
        public SelectCommand(List<String> fields, int lineNumber) { super(lineNumber); this.fields = fields; }
        @Override public String toString() { return "SELECT " + String.join(", ", fields); }
    }

    public static class SaveCommand extends Command {
        public final String outputFile;
        public SaveCommand(String outputFile, int lineNumber) { super(lineNumber); this.outputFile = outputFile; }
        @Override public String toString() { return String.format("SAVE AS \"%s\"", outputFile); }
    }
}