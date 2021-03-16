grammar PostAggregatorExpression;


prog
   : expression
   ;

expression
  : ID
  | CONST
  | expression op=(ADD|SUB|MUL|DIV) expression
  | '(' expression ')'
  ;

ADD: '+';
SUB: '-';
MUL: '*';
DIV: '/';

CONST: [0-9]+('.'?[0-9]+)?;

ID
   : [a-zA-Z_0-9]+
   ;
WS: [ \n\t\r]+ -> skip;
