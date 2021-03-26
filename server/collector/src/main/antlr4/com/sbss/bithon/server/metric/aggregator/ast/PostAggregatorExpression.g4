grammar PostAggregatorExpression;


prog
   : expression
   ;

expression
  : ID
  | NUMBER
  | VARIABLE
  | expression op=(ADD|SUB|MUL|DIV) expression
  | '(' expression ')'
  ;

VARIABLE: 'interval';
ADD: '+';
SUB: '-';
MUL: '*';
DIV: '/';

NUMBER: [0-9]+('.'?[0-9]+)?;

ID
   : [a-zA-Z_0-9]+
   ;
WS: [ \n\t\r]+ -> skip;
