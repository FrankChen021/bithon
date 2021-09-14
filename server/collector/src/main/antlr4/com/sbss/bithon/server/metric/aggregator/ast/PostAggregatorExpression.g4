grammar PostAggregatorExpression;


prog
   : expression
   ;

expression
  : ID
  | NUMBER
  | variable
  | expression op=(ADD|SUB|MUL|DIV) expression
  | '(' expression ')'
  ;

variable: '{' ID '}';
ADD: '+';
SUB: '-';
MUL: '*';
DIV: '/';

NUMBER: [0-9]+('.'?[0-9]+)?;

ID
   : [a-zA-Z_0-9]+
   ;
WS: [ \n\t\r]+ -> skip;
