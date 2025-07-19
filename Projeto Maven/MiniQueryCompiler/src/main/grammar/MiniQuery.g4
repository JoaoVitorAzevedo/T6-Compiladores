grammar MiniQuery;

@header {
package com.mycompany.miniquery.grammar;
}

program
    : command+ EOF
    ;

command
    : loadCommand
    | filterCommand
    | selectCommand
    | saveCommand
    ;

loadCommand
    : 'LOAD' STRING
    ;

// CORREÇÃO: Removido 'jsonPath' e 'WHERE'. 
// A sintaxe agora é a intuitiva: FILTER .caminho.do.campo > 18
filterCommand
    : 'FILTER' left=value OPERATOR right=value
    ;

selectCommand
    : 'SELECT' jsonPath (',' jsonPath)*
    ;

saveCommand
    : 'SAVE' 'AS' STRING
    ;

jsonPath
    : '.' IDENTIFIER ('.' IDENTIFIER)*
    ;

// A regra 'value' já suporta caminhos, números e strings.
value
    : NUMBER
    | STRING
    | jsonPath
    ;

OPERATOR: '>' | '<' | '>=' | '<=' | '==' | '!=';
STRING: '"' (~["\\\r\n] | '\\' .)* '"';
NUMBER: [0-9]+ ('.' [0-9]+)?;
IDENTIFIER: [a-zA-Z_][a-zA-Z0-9_]*;
WS: [ \t\r\n]+ -> skip;
COMMENT: '#' .*? '\n' -> skip;