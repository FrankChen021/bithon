grammar Expression;

parse
   : expression EOF
   ;

expression
  : expression LEFT_SQUARE_BRACKET INTEGER_LITERAL RIGHT_SQUARE_BRACKET #arrayAccessExpression
  | expression LEFT_SQUARE_BRACKET STRING_LITERAL RIGHT_SQUARE_BRACKET  #mapAccessExpression
  | expression (MUL|DIV) expression                                     #arithmeticExpression
  | expression (ADD|SUB) expression                                     #arithmeticExpression
  | expression (LT|LTE|GT|GTE|NE|EQ|LIKE|NOT LIKE) expression           #comparisonExpression
  | expression (IN|NOT IN) expressionListDecl                           #comparisonExpression
  | functionExpressionDecl                                              #functionExpression
  | notExpressionDecl                                                   #notExpression
  | expression AND expression                                           #logicalExpression
  | expression OR expression                                            #logicalExpression
  | expressionListDecl                                                  #expressionList
  | literalExpressionDecl                                               #literalExpression
  | identifierExpressionDecl                                            #identifierExpression
  | macroExpressionDecl                                                 #macroExpression
  ;

functionExpressionDecl
   : IDENTIFIER expressionListDecl
   ;

notExpressionDecl
  : NOT expression
  ;

expressionListDecl
  : LEFT_PARENTHESIS expression (COMMA expression)* RIGHT_PARENTHESIS
  | LEFT_PARENTHESIS RIGHT_PARENTHESIS
  ;

literalExpressionDecl
  : (INTEGER_LITERAL | DECIMAL_LITERAL | STRING_LITERAL | BOOL_LITERAL)
  ;

identifierExpressionDecl
  : IDENTIFIER (DOT IDENTIFIER)*
  ;

macroExpressionDecl
  : LEFT_CURLY_BRACE IDENTIFIER RIGHT_CURLY_BRACE
  ;

INTEGER_LITERAL: '-'?[0-9]+;
DECIMAL_LITERAL: '-'?[0-9]+'.'[0-9]*;
STRING_LITERAL: SQUOTA_STRING;
BOOL_LITERAL: TRUE | FALSE;


fragment SQUOTA_STRING
  : '\'' ('\\'. | '\'\'' | ~('\'' | '\\'))* '\'';

LEFT_PARENTHESIS: '(';
RIGHT_PARENTHESIS: ')';
LEFT_CURLY_BRACE: '{';
RIGHT_CURLY_BRACE: '}';
LEFT_SQUARE_BRACKET: '[';
RIGHT_SQUARE_BRACKET: ']';
DOT: '.';

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
AND: A N D;
OR: O R;
IN: I N;
LIKE: L I K E;
NOT: N O T;
TRUE: T R U E;
FALSE: F A L S E;

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
