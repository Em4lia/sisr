grammar javaL;

program : classDeclaration+ EOF ;

classDeclaration
    : PUBLIC? CLASS ID (EXTENDS ID)? LBRACE classBody RBRACE // public class MyClass extends Parent { ... }
    ;

classBody
    : (fieldDeclaration | methodDeclaration)*
    ;

fieldDeclaration
    : type ID (ASSIGN expression)? SEMICOLON                  // int x = 5;
    | type LBRACKET RBRACKET ID SEMICOLON                     // int[] arr;
    ;

methodDeclaration
    : VOID ID LPAREN (parameter (COMMA parameter)*)? RPAREN LBRACE statement* RBRACE // void calculate(int a, double b) { ... }
    ;

parameter: type ID; // int a

type: INT | DOUBLE | BYTE | ID; // int, double, byte, MyClass

statement
    : variableDeclaration                                     // int a = 10;
    | assignment                                              // a = 20;
    | ifStatement                                             // if (a > 0) { ... }
    | whileStatement                                          // while (a < 10) { ... }
    | switchStatement                                         // switch (a) { case 1: ... }
    | methodCall SEMICOLON                                    // obj.method();
    | LBRACE statement* RBRACE                                // { a = 1; b = 2; }
    | BREAK SEMICOLON                                         // break;
    ;

variableDeclaration: type ID (ASSIGN expression)? SEMICOLON ; // int a = 1;

assignment
    : ID ASSIGN expression SEMICOLON                          // a = 5;
    | ID LBRACKET expression RBRACKET ASSIGN expression SEMICOLON // arr[0] = 5;
    | ID DOT ID ASSIGN expression SEMICOLON                   // obj.x = 5;
    ;

ifStatement : IF LPAREN expression RPAREN statement (ELSE statement)?; // if (x == 5) x = 0; else x = 1;

whileStatement : WHILE LPAREN expression RPAREN statement ; // while (x < 10) { x = x + 1; }

switchStatement
    : SWITCH LPAREN expression RPAREN LBRACE
        (CASE INT_LITERAL COLON statement*)*                  // case 1: a = 2; break;
        (DEFAULT COLON statement*)?                           // default: a = 0;
      RBRACE
    ;

objectCreation : NEW ID LPAREN RPAREN ; // new MyObject()

methodCall: ID DOT ID LPAREN (expression (COMMA expression)*)? RPAREN; // obj.doSomething(x, 10)

expression
    : expression (MUL | DIV) expression                                 # MathOp          // a * b, a / b
    | expression (PLUS | MINUS) expression                              # AddSubOp        // a + b, a - b
    | expression (LESS | GREATER | EQUAL | NOT_EQUAL) expression        # CompareOp       // a < b, a == b, a != b
    | NEW ID LPAREN RPAREN                                              # NewObjExpr      // new Object()
    | expression DOT ID LPAREN (expression (COMMA expression)*)? RPAREN # MethodCallExpr  // obj.method(x, 10)
    | expression DOT ID                                                 # FieldAccessExpr // obj.field
    | ID LBRACKET expression RBRACKET                                   # ArrayAccessExpr // arr[i]
    | ID                                                                # VarReference    // myVar
    | INT_LITERAL                                                       # IntExpr         // 42
    | DOUBLE_LITERAL                                                    # DoubleExpr      // 3.14
    | LPAREN expression RPAREN                                          # ParenExpr       // (a + b)
    ;

CLASS: 'class';
EXTENDS: 'extends';
BYTE: 'byte';
INT: 'int';
DOUBLE: 'double';
STRING: 'string';
IF: 'if';
ELSE: 'else';
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

INT_LITERAL: [0-9]+;
DOUBLE_LITERAL: [0-9]+ '.' [0-9]+;

ID: [a-zA-Z_] [a-zA-Z0-9_]*;

LINE_COMMENT: '//' ~[\r\n]* -> skip;
BLOCK_COMMENT: '/*' .*? '*/' -> skip;

WS: [ \t\r\n]+ -> skip;

STRING_LITERAL: '"' (~["\r\n\\] | '\\' .)* '"';
UNCLOSED_STRING: '"' (~["\r\n\\] | '\\' .)* ;

SINGLE_STRING_LITERAL: '\'' ( ~['\r\n\\] | '\\' . )* '\'';
UNCLOSED_SINGLE_STRING: '\'' ( ~['\r\n\\] | '\\' . )*;

ERR: . ;

