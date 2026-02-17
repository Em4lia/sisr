lexer grammar javaL;

// ключові слова
CLASS: 'class';
EXTENDS: 'extends';
BYTE: 'byte';
INT: 'int';
DOUBLE: 'double';
STRING: 'string';
WHILE: 'while';
FOR: 'for';
SWITCH: 'switch';
CASE: 'case';
DEFAULT: 'default';
BREAK: 'break';
NEW: 'new';
VOID: 'void';
PUBLIC: 'public';
STATIC: 'static';

// оператори
ASSIGN: '=';
PLUS: '+';
MINUS: '-';
MUL: '*';
DIV: '/';
EQUAL: '==';
NOT_EQUAL: '!=';
LESS: '<';
GREATER: '>';
INCREMENT: '++';
DECREMENT: '--';

LPAREN: '(';
RPAREN: ')';
LBRACE: '{';
RBRACE: '}';
LBRACKET: '[';
RBRACKET: ']';
SEMICOLON: ';';
COMMA: ',';
DOT: '.';
COLON: ':';

// числа
INT_LITERAL: [0-9]+;
DOUBLE_LITERAL: [0-9]+ '.' [0-9]+;

// ідентифікатори
ID: [a-zA-Z_] [a-zA-Z0-9_]*;

// коментарі
LINE_COMMENT: '//' ~[\r\n]* -> skip;
BLOCK_COMMENT: '/*' .*? '*/' -> skip;

// пробіли
WS: [ \t\r\n]+ -> skip;

STRING_LITERAL: '"' (~["\r\n\\] | '\\' .)* '"';
UNCLOSED_STRING: '"' (~["\r\n\\] | '\\' .)* ;

SINGLE_STRING_LITERAL: '\'' ( ~['\r\n\\] | '\\' . )* '\'';
UNCLOSED_SINGLE_STRING: '\'' ( ~['\r\n\\] | '\\' . )*;

// помилки
ERR: . ;