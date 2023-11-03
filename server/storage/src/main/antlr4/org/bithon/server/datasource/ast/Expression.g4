grammar Expression;

parse
   : expression EOF
   ;

expression
  : expression (MUL|DIV) expression                                     #arithmeticExpression
  | expression (ADD|SUB) expression                                     #arithmeticExpression
  | expression (LT|LTE|GT|GTE|NE|EQ|LIKE|NOT LIKE|IN|NOT IN) expression #comparisonExpression
  | NOT expression                                              #notExpression
  | expression AND expression                                   #logicalExpression
  | expression OR expression                                    #logicalExpression
  | expression '[' INTEGER_LITERAL ']'                          #arrayAccessExpression
  | expression '[' STRING_LITERAL ']'                           #mapAccessExpression
  | IDENTIFIER expressionListImpl                               #functionExpression
  | (INTEGER_LITERAL | DECIMAL_LITERAL | STRING_LITERAL)        #literalExpression
  | expressionListImpl                                          #expressionList
  | IDENTIFIER ('.' IDENTIFIER)*                                #identifierExpression
  | '{' IDENTIFIER '}'                                          #macroExpression
  ;

expressionListImpl
  : '(' expression (COMMA expression)* ')'
  | '(' ')'
  ;

INTEGER_LITERAL: '-'?[0-9]+;
DECIMAL_LITERAL: '-'?[0-9]+'.'[0-9]*;
STRING_LITERAL: SQUOTA_STRING;

fragment SQUOTA_STRING
  : '\'' ('\\'. | '\'\'' | ~('\'' | '\\'))* '\'';

COMMA: ',';
ADD: '+';
SUB: '-';
MUL: '*';
DIV: '/';
LT: '<';
LTE: '<=';
GT: '>';
GTE: '>=';
NE: '<>' | '!=';
EQ: '=';
LEFT_PARENTHESES: '(';
RIGHT_PARENTHESES: ')';
AND: A N D;
OR: O R;
IN: I N;
LIKE: L I K E;
NOT: N O T;

// case insensitive
fragment A : [aA];
fragment B : [bB];
fragment C : [cC];
fragment D : [dD];
fragment E : [eE];
fragment F : [fF];
fragment G : [gG];
fragment H : [hH];
fragment I : [iI];
fragment J : [jJ];
fragment K : [kK];
fragment L : [lL];
fragment M : [mM];
fragment N : [nN];
fragment O : [oO];
fragment P : [pP];
fragment Q : [qQ];
fragment R : [rR];
fragment S : [sS];
fragment T : [tT];
fragment U : [uU];
fragment V : [vV];
fragment W : [wW];
fragment X : [xX];
fragment Y : [yY];
fragment Z : [zZ];

IDENTIFIER : [a-zA-Z_][a-zA-Z_0-9]*;
WS: [ \n\t\r]+ -> skip;
