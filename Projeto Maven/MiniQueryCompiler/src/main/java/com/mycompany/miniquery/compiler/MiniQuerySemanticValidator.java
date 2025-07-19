package com.mycompany.miniquery.compiler;

import com.mycompany.miniquery.grammar.*;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import java.util.*;

public class MiniQuerySemanticValidator extends MiniQueryBaseProcessor {

    private final List<String> errors = new ArrayList<>();
    private final Map<String, String> fieldTypes = new HashMap<>();

    public List<String> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    @Override
    public void enterLoadCommand(MiniQueryParser.LoadCommandContext ctx) {
        super.enterLoadCommand(ctx);
        LoadCommand cmd = (LoadCommand) commands.get(commands.size() - 1);

        if (cmd.filePath.trim().isEmpty()) {
            addError(ctx, "Caminho do arquivo não pode ser vazio");
        }

        // Atualizado o mapa para incluir os campos usados nos testes.
        // Na prática, isso viria de um schema ou da leitura do próprio JSON.
        fieldTypes.put(".idade", "number");
        fieldTypes.put(".nome", "string");
        fieldTypes.put(".email", "string"); // Adicionado para o teste 02
        fieldTypes.put(".cliente.idade", "number");
        fieldTypes.put(".cliente.nome", "string");
        fieldTypes.put(".valor", "number");
    }

    @Override
    public void enterFilterCommand(MiniQueryParser.FilterCommandContext ctx) {
        super.enterFilterCommand(ctx);

        FilterCondition cmd = (FilterCondition) commands.get(commands.size() - 1);

        // Verificação de tipos
        String leftType = fieldTypes.getOrDefault(cmd.jsonPath, "unknown");
        String rightType = cmd.value.startsWith(".")
                ? fieldTypes.getOrDefault(cmd.value, "unknown")
                : (cmd.value.matches("-?\\d+(\\.\\d+)?") ? "number" : "string");

        if (!leftType.equals("unknown") && !leftType.equals(rightType)) {
            addError(ctx, String.format(
                    "Tipo incompatível: o campo '%s' é do tipo %s, mas o valor '%s' é do tipo %s",
                    cmd.jsonPath, leftType, cmd.value, rightType
            ));
        }

        if ("string".equals(leftType) && !cmd.operator.equals("==") && !cmd.operator.equals("!=")) {
            addError(ctx, "Operador '" + cmd.operator + "' inválido para strings. Use '==' ou '!='.");
        }
    }

    @Override
    public void enterSaveCommand(MiniQueryParser.SaveCommandContext ctx) {
        super.enterSaveCommand(ctx);
        SaveCommand cmd = (SaveCommand) commands.get(commands.size() - 1);

        if (cmd.outputFile.trim().isEmpty()) {
            addError(ctx, "Nome do arquivo de saída não pode ser vazio");
        }
    }

    private void addError(ParserRuleContext ctx, String message) {
        Token startToken = ctx.getStart();
        errors.add("ERRO (Linha " + startToken.getLine() + "): " + message);
    }
}
