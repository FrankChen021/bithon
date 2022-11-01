grammar PostAggregatorExpression;

parse
   : expression EOF
   ;

expression
  : NUMBER
  | fieldNameExpression
  | variableExpression
  | expression op=(ADD|SUB|MUL|DIV) expression
  | functionNameExpression '(' expression (',' expression)* ')'
  | '(' expression ')'
  ;

fieldNameExpression
  : ID
  ;

functionNameExpression
  : ID
  ;

variableExpression: '{' ID '}';

NUMBER: [0-9]+('.'?[0-9]+)?;

ID : [a-zA-Z_][a-zA-Z_0-9]*;
COMMA:     ',';
ADD: '+';
SUB: '-';
MUL: '*';
DIV: '/';

WS: [ \n\t\r]+ -> skip;
