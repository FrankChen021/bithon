grammar Expression;

parse
   : expression EOF
   ;

expression
  : subExpression ((AND|OR) subExpression)+    #logicExpression
  | subExpression                              #subExpressionOnly
  ;

subExpression
  : subExpression (ADD|SUB|MUL|DIV) subExpression #arithmeticExpression
  | subExpression (LT|LTE|GT|GTE|NE|EQ|LIKE|NOT LIKE) subExpression #comparisonExpression
  | subExpression IN '(' literalExpressionImpl (COMMA literalExpressionImpl)* ')' #inExpression
  | NOT subExpression #notExpression
  | subExpression '[' NUMBER_LITERAL ']'    #arrayAccessExpression
  | LEFT_PARENTHESES expression RIGHT_PARENTHESES   #braceExpression
  | functionNameExpression LEFT_PARENTHESES (expression (COMMA expression)*)? RIGHT_PARENTHESES   #functionExpression
  | literalExpressionImpl #literalExpression
  | IDENTIFIER ('.' IDENTIFIER)*            #fieldExpression
  ;

literalExpressionImpl
  : NUMBER_LITERAL
  | STRING_LITERAL
  ;

functionNameExpression
  : IDENTIFIER
  ;

NUMBER_LITERAL: [0-9]+('.'?[0-9]+)?;
STRING_LITERAL
 : SQUOTA_STRING;

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
