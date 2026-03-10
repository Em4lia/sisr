grammar javaL;

@header {
    package org.labs;
}

program: classDeclaration+ EOF ;

classDeclaration: classModifier* CLASS ID (EXTENDS ID)? LBRACE classBody RBRACE ;
classBody: (fieldDeclaration | methodDeclaration)*;
classModifier: 'public' | 'private' | 'abstract' | 'static';

fieldDeclaration: type ID (ASSIGN expression)? SEMICOLON ;

methodDeclaration
    : (VOID | type) ID LPAREN (parameter (COMMA parameter)*)? RPAREN LBRACE statement* RBRACE
    ;

parameter: type ID ;

type: primitiveType | ID | type LBRACKET RBRACKET ;
primitiveType: INT | DOUBLE | BYTE | BOOLEAN ;

statement
    : variableDeclaration                      #VarDeclStat
    | assignment                               #AssignStat
    | ifStatement                              #IfStat
    | whileStatement                           #WhileStat
    | switchStatement                          #SwitchStat
    | methodCall SEMICOLON                     #MethodCallStat
    | LBRACE statement* RBRACE                 #BlockStat
    | BREAK SEMICOLON                          #BreakStat
    | CONTINUE SEMICOLON                       #ContinueStat
    | RETURN expression? SEMICOLON             #ReturnStat
    ;

variableDeclaration: type ID (ASSIGN expression)? SEMICOLON ;

assignment
    : ID ASSIGN expression SEMICOLON                          #SimpleAssign
    | ID LBRACKET expression RBRACKET ASSIGN expression SEMICOLON #ArrayAssign
    | ID DOT ID ASSIGN expression SEMICOLON                   #ObjectAssign
    ;

ifStatement : IF LPAREN expression RPAREN statement (ELSE statement)? ;
whileStatement : WHILE LPAREN expression RPAREN statement ;
switchStatement : SWITCH LPAREN expression RPAREN LBRACE (CASE INT_LITERAL COLON statement*)* (DEFAULT COLON statement*)? RBRACE ;

methodCall: (expression DOT)? ID LPAREN (expression (COMMA expression)*)? RPAREN;

expression
    : primary                                                           #PrimaryExpr
    | expression LBRACKET expression RBRACKET                           #ArrayAccessExpr
    | expression DOT ID LPAREN (expression (COMMA expression)*)? RPAREN #MethodCallExpr
    | expression DOT ID                                                 #FieldAccessExpr
    | NEW ID LPAREN RPAREN                                              #NewObjectExpr
    | expression (MUL | DIV) expression                                 #MulDivExpr
    | expression (PLUS | MINUS) expression                              #AddSubExpr
    | expression (LESS | GREATER | LE | GE) expression                  #RelationalExpr
    | expression (EQUAL | NOT_EQUAL) expression                         #EqualityExpr
    | expression (AND | OR) expression                                  #LogicalExpr
    | ID LPAREN (expression (COMMA expression)*)? RPAREN                #LocalMethodCallExpr
    | expression DOT ID LPAREN (expression (COMMA expression)*)? RPAREN #MethodCallExpr
    ;

primary
    : ID                                                      #IdExpr
    | INT_LITERAL                                             #IntLiteralExpr
    | DOUBLE_LITERAL                                          #DoubleLiteralExpr
    | BooleanLiteral                                          #BoolLiteralExpr
    | LPAREN expression RPAREN                                #ParensExpr
    ;

CLASS: 'class'; EXTENDS: 'extends'; BYTE: 'byte'; INT: 'int'; DOUBLE: 'double'; STRING: 'string';
IF: 'if'; ELSE: 'else'; WHILE: 'while'; FOR: 'for'; SWITCH: 'switch'; CASE: 'case'; DEFAULT: 'default';
BREAK: 'break'; NEW: 'new'; VOID: 'void'; PUBLIC: 'public'; PRIVATE: 'private'; STATIC: 'static';
RETURN: 'return'; CONTINUE: 'continue'; BOOLEAN: 'boolean';
BooleanLiteral: 'true' | 'false'; NullLiteral: 'null';

ASSIGN: '='; PLUS: '+'; MINUS: '-'; MUL: '*'; DIV: '/';
EQUAL: '=='; NOT_EQUAL: '!='; LESS: '<'; GREATER: '>'; LE: '<='; GE: '>=';
INCREMENT: '++'; DECREMENT: '--'; AND: '&&'; OR: '||'; NOT: '!';

LPAREN: '('; RPAREN: ')'; LBRACE: '{'; RBRACE: '}'; LBRACKET: '['; RBRACKET: ']';
SEMICOLON: ';'; COMMA: ','; DOT: '.'; COLON: ':';

INT_LITERAL: [0-9]+;
DOUBLE_LITERAL: [0-9]+ '.' [0-9]+;
ID: [a-zA-Z_] [a-zA-Z0-9_]*;

LINE_COMMENT: '//' ~[\r\n]* -> skip;
BLOCK_COMMENT: '/*' .*? '*/' -> skip;
WS: [ \t\r\n]+ -> skip;
ERR: . ;