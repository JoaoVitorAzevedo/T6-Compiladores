

## **Vídeo**
[compiled.mp4]



# **MiniQuery Compiler**

**Projeto T6 - Construção de Compiladores**
**Autor**: [João Vitor Azevedo](https://github.com/JoaoVitorAzevedo)
**Repositório**: [github.com/JoaoVitorAzevedo/T6-Compiladores](https://github.com/JoaoVitorAzevedo/T6-Compiladores)

-----

## **Visão Geral**

O **MiniQuery** é um compilador que traduz uma linguagem declarativa para consultas em arquivos JSON em código *JavaScript*. O projeto abrange todas as fases de um compilador moderno: análise léxica, sintática, semântica e geração de código.

### **MiniQuery vs. jq**

Para entender o nicho do MiniQuery, é útil compará-lo com a ferramenta padrão de mercado, o *jq*.

| Característica | *jq* (Ferramenta Padrão) | *MiniQuery* (Projeto Acadêmico) |
| :--- | :--- | :--- |
| **Paradigma** | **Interpretador** de linha de comando, focado em *streaming* e *pipes*. | **Compilador** que gera um script *.js* autocontido. |
| **Sintaxe** | Flexível e poderosa, porém mais complexa para filtros básicos (ex: *.[] \| select(.age > 18)*). | Sintaxe simplificada e fixa, projetada para clareza (ex: *FILTER .idade > 18*). |
| **Validação** | Erros são, em geral, descobertos em tempo de execução. | **Validação estática (fail-fast):** Erros de tipo e lógica são capturados em tempo de compilação. |
| **Saída** | Tipicamente um stream de dados JSON filtrado/transformado. | Um arquivo *.js* que pode ser executado, inspecionado ou integrado a projetos Node.js. |

-----

## **Funcionalidades Implementadas**


## 📋 Funcionalidades
- Analisador léxico, sintático e semântico
- Geração de código JavaScript
- Sistema de testes automatizados
- Validação de tipos e operadores


### **1. Análise Léxica e Sintática (ALS)**

Uma gramática ANTLR robusta define a estrutura da linguagem, suportando os seguintes comandos:

```sql
-- Carrega um arquivo JSON
LOAD "dados.json"

-- Filtra os dados com base em uma condição
FILTER .cliente.idade > 18

-- Seleciona os campos desejados
SELECT .nome, .email

-- Salva o resultado em um novo arquivo
SAVE AS "resultado.json"
```

### **2. Análise Semântica (AS)**

O compilador implementa 4 verificações semânticas distintas para garantir a lógica e a integridade das consultas, prevenindo erros em tempo de execução.

| Verificação | Propósito | Teste que Valida |
| :--- | :--- | :--- |
| **Incompatibilidade de Tipos** | Impede a comparação entre tipos de dados diferentes (ex: *string > number*). | **Teste 02** |
| **Operador Inválido para String**| Garante que apenas operadores de igualdade (*==`, *!=*) sejam usados com strings. | **Teste 02** |
| **Caminho de Arquivo Vazio** | Valida se os comandos *LOAD* e *SAVE* contêm um nome de arquivo não vazio. | **Teste 10** |
| **Checagem de Campos** | Utiliza um mapa interno de tipos para validar os campos conhecidos. | **Testes 01, 02, 03...** |

### **3. Geração de Código (GCI)**

Após a validação bem-sucedida, o MiniQuery gera um script otimizado, com estado, capaz de lidar com sequências complexas de comandos.

```javascript
// Exemplo de código JS gerado para múltiplos filtros
const fs = require('fs');
const input = JSON.parse(fs.readFileSync('data.json', 'utf-8'));
const filteredData = input.filter(item => item.idade >= 18);
filteredData = filteredData.filter(item => item.nome !== "Admin");
```

-----

## **Como Usar**

### **Pré-requisitos**

  * Java 17+
  * Maven 3.8+
 

### **Instalação**

O comando *mvn clean package* irá compilar o projeto e criar um JAR executável autocontido (uber-JAR) em *target/*.

```bash
# 1. Clone o repositório
git clone https://github.com/JoaoVitorAzevedo/T6-Compiladores.git
cd T6-Compiladores

# 2. Compile e empacote o projeto
mvn clean package
```

### **Execução**

Existem duas formas de executar o compilador:

**1. Compilando um Único Arquivo**

Para compilar um arquivo *.mq* e ver o código JavaScript gerado diretamente no terminal.

```bash
# Crie seu arquivo de consulta, por exemplo, consulta.mq
# Em seguida, execute o JAR:
java -jar target/MiniQueryCompiler-1.0-SNAPSHOT.jar consulta.mq
```

**2. Executando a Suíte de Testes Completa**

Para validar todas as funcionalidades do projeto através da suíte de 10 testes automatizados.

```bash
# Este comando executa a classe TestRunner
mvn exec:java -Dexec.mainClass="com.mycompany.miniquery.TestRunner"
```

Após a execução, os resultados detalhados estarão disponíveis em *target/test-results/*.

-----

## **Suíte de Testes**

O projeto é validado por 10 casos de teste que cobrem todos os aspectos do compilador.

| Teste | Descrição Resumida | Propósito da Validação |
| :--- | :--- | :--- |
| **01** | Filtro numérico simples, com seleção e salvamento. | Valida o fluxo completo do compilador no caso de uso principal. |
| **02** | Tentativa de usar operador numérico (*>*) em um campo string. | Garante que a **Análise Semântica** detecta erros de tipo e operador. |
| **03** | Filtro de igualdade (*==*) em um campo string. | Valida a geração de código para filtros com strings e o operador *===*. |
| **04**| Uso de um caminho JSON aninhado (*.cliente.idade*). | Confirma o suporte a acessos profundos e *optional chaining* (*?.*). |
| **05** | Uso de um campo não definido no mapa de tipos do validador. | Demonstra o comportamento atual para campos desconhecidos. |
| **06**| Comando *FILTER* incompleto, sem valor de comparação. | Confirma que a **Análise Sintática** captura erros estruturais. |
| **07**| Encadeamento de dois comandos *FILTER*. | Valida a robustez do gerador de código com estado. |
| **08**| Comando *SELECT* utilizado diretamente após o *LOAD*. | Valida a geração de código quando não há filtros. |
| **09**| Filtro que compara um campo com outro campo. | Confirma a geração de código correta para comparações dinâmicas. |
| **10**| Comando *SAVE* com um nome de arquivo vazio. | Garante que a **Análise Semântica** valida o comando *SAVE*. |

-----

O target\test-results\RELATORIO_COMPLETO.txt indica que o compilador se comporta exatamente como o esperado para todos os 10 casos de teste, desde os mais simples aos mais complexos.

Testes Válidos (01, 03, 04, 05, 07, 08, 09): Todos passaram porque as análises semânticas foram válidas e o código JavaScript gerado correspondeu perfeitamente ao esperado.

Testes de Erro (02, 06, 10): Todos passaram porque as mensagens de erro (sejam de sintaxe ou semântica) foram geradas corretamente.

## **Estrutura do Projeto**

```plaintext
src/
├── main/
│   ├── java/com/mycompany/miniquery/  # Código fonte Java (Main, TestRunner)
│   │   └── compiler/                  # Processadores, Validador, Gerador
│   └── grammar/                       # Gramática ANTLR (MiniQuery.g4)
├── test/
│   ├── expected/                      # Saídas esperadas para os testes
│   └── resources/                     # Arquivos de entrada dos testes (.mq)
target/                                # JARs compilados e resultados dos testes
```


