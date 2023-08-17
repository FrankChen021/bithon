grammar FilterExpression;


parse
   : filterExpression EOF
   ;

filterExpression
  : binaryExpression
  | filterExpression logicOperator filterExpression
  | '(' filterExpression ')'
  | NOT filterExpression
  ;

binaryExpression
  : unaryExpression comparisonOperator unaryExpression
  | unaryExpression 'in' experssionList
  ;

unaryExpression
  : nameExpression
  | literalExperssion
  ;

nameExpression
  : qualifiedNameExpression
  | simpleNameExpression
  ;

qualifiedNameExpression
  : VARIABLE ('.' VARIABLE)+
  ;

simpleNameExpression
  : VARIABLE
  ;

literalExperssion
  : STRING_LITERAL | NUMBER_LITERAL
  ;

experssionList
  : '(' unaryExpression (',' unaryExpression)* ')'
  ;

logicOperator
  : AND | OR
  ;

comparisonOperator
    : '='
    | '>'
    | '<'
    | '<='
    | '>='
    | '<>'
    | '!='
    | LIKE
    | NOT LIKE
    ;

AND
  : A N D
  ;

OR
  : O R
  ;

LIKE:
  L I K E;

NOT:
 N O T;

VARIABLE
  : [a-zA-Z_][a-zA-Z_0-9]*
  ;

NUMBER_LITERAL
 : [0-9]+('.'?[0-9]+)?
 ;

STRING_LITERAL
 : SQUOTA_STRING;

fragment SQUOTA_STRING
  : '\'' ('\\'. | '\'\'' | ~('\'' | '\\'))* '\'';

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

WS: [ \n\t\r]+ -> skip;
