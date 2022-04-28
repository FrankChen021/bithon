grammar RuleExpression;

prog
   : expression
   ;

expression
  : ID
  | expression op=(AND|OR) expression
  | '(' expression ')'
  ;

AND: '&&';
OR: '||';

ID
   : [A-Z]
   ;
WS: [ \n\t\r]+ -> skip;
