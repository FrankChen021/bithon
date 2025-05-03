grammar PathExpression;

path
   : expression EOF
   ;

expression
  : IDENTIFIER (DOT expression)*    #dotExpression
  | expression LEFT_SQUARE_BRACKET STRING_LITERAL RIGHT_SQUARE_BRACKET  #propertyAccessExpression
  ;

DOT: '.';
LEFT_SQUARE_BRACKET: '[';
RIGHT_SQUARE_BRACKET: ']';
STRING_LITERAL: SQUOTA_STRING;

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

IDENTIFIER : [a-zA-Z_][a-zA-Z_0-9]*;
WS: [ \n\t\r]+ -> skip;
