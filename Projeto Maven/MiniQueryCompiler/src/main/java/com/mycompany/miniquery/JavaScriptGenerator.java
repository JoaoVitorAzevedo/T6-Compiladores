package com.mycompany.miniquery;

import com.mycompany.miniquery.compiler.MiniQueryBaseProcessor;
import com.mycompany.miniquery.compiler.MiniQueryBaseProcessor.SaveCommand;
import java.util.List;

public class JavaScriptGenerator {

    // Percorre a lista de comandos e constrói o script JavaScript final.
    public String generate(List<MiniQueryBaseProcessor.Command> commands) {
        StringBuilder jsCode = new StringBuilder();
        jsCode.append("// Código gerado automaticamente\n");
        jsCode.append("const fs = require('fs');\n\n");

        String currentDataVariable = null;
        boolean filterApplied = false;

        for (MiniQueryBaseProcessor.Command cmd : commands) {
            if (cmd instanceof MiniQueryBaseProcessor.LoadCommand) {
                jsCode.append(generateLoadCode((MiniQueryBaseProcessor.LoadCommand) cmd));
                currentDataVariable = "input";

            } else if (cmd instanceof MiniQueryBaseProcessor.FilterCondition) {
                jsCode.append(generateFilterCode((MiniQueryBaseProcessor.FilterCondition) cmd, currentDataVariable, filterApplied));
                currentDataVariable = "filteredData";
                filterApplied = true;

            } else if (cmd instanceof MiniQueryBaseProcessor.SelectCommand) {
                jsCode.append(generateSelectCode((MiniQueryBaseProcessor.SelectCommand) cmd, currentDataVariable));
                currentDataVariable = "result";

            } else if (cmd instanceof MiniQueryBaseProcessor.SaveCommand) {
                jsCode.append(generateSaveCode((SaveCommand) cmd, currentDataVariable));
            }
        }
        return jsCode.toString();
    }

    // Gera o código JS para carregar e decodificar o arquivo JSON inicial.
    private String generateLoadCode(MiniQueryBaseProcessor.LoadCommand cmd) {
        return String.format("const input = JSON.parse(fs.readFileSync('%s', 'utf-8'));\n", cmd.filePath);
    }

    // Cria a lógica de filtro (.filter) do JavaScript, tratando os operandos e operadores.
    private String generateFilterCode(MiniQueryBaseProcessor.FilterCondition cmd, String sourceVariable, boolean isChained) {
        String leftPath = convertJsonPathToJs(cmd.jsonPath);
        String rightOperand;

        if (cmd.value.startsWith(".")) {
            rightOperand = "item" + convertJsonPathToJs(cmd.value);
        } else if (cmd.value.matches("-?\\d+(\\.\\d+)?")) {
            rightOperand = cmd.value;
        } else {
            rightOperand = "\"" + cmd.value + "\"";
        }

        String operator = cmd.operator;
        if (operator.equals("==")) {
            operator = "===";
        }
        if (operator.equals("!=")) {
            operator = "!==";
        }

        String declaration = isChained ? "filteredData = filteredData" : "const filteredData = " + sourceVariable;

        return String.format(
                "%s.filter(item => item%s %s %s);\n",
                declaration, leftPath, operator, rightOperand
        );
    }

    // Produz o código de mapeamento (.map) para selecionar campos e criar novos objetos.
    private String generateSelectCode(MiniQueryBaseProcessor.SelectCommand cmd, String sourceVariable) {
        StringBuilder js = new StringBuilder(String.format("const result = %s.map(item => ({\n", sourceVariable));
        for (int i = 0; i < cmd.fields.size(); i++) {
            String field = cmd.fields.get(i);
            String cleanField = field.substring(1).replace(".", "_");
            String jsPath = convertJsonPathToJs(field);
            js.append(String.format("  %s: item%s", cleanField, jsPath));
            if (i < cmd.fields.size() - 1) {
                js.append(",");
            }
            js.append("\n");
        }
        js.append("}));\n");
        return js.toString();
    }

    // Gera o comando para salvar o resultado final em um novo arquivo JSON.
    private String generateSaveCode(MiniQueryBaseProcessor.SaveCommand cmd, String sourceVariable) {
        return String.format(
                "fs.writeFileSync('%s', JSON.stringify(%s, null, 2));\n",
                cmd.outputFile, sourceVariable
        );
    }

    // Converte um caminho de acesso MiniQuery (ex: .cliente.idade) para um formato JS seguro (ex: .cliente?.idade).
    private String convertJsonPathToJs(String jsonPath) {
        if (!jsonPath.startsWith(".")) {
            return jsonPath;
        }
        // Pega o caminho sem o primeiro ponto: "cliente.idade"
        String pathWithoutFirstDot = jsonPath.substring(1);
        // Substitui os pontos restantes por optional chaining: "cliente?.idade"
        String safePath = pathWithoutFirstDot.replace(".", "?.");
        // Retorna o caminho completo para acesso: ".cliente?.idade"
        return "." + safePath;
    }
}