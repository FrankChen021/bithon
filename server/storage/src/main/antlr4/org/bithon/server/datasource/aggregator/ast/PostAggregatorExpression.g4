grammar PostAggregatorExpression;

parse
   : expression EOF
   ;

expression
  : NUMBER
  | fieldNameExpression
  | variableExpression
  | expression op=(ADD|SUB|MUL|DIV) expression
  | functionNameExpression LEFT_PARENTHESES expression (COMMA expression)* RIGHT_PARENTHESES
  | LEFT_PARENTHESES expression RIGHT_PARENTHESES
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
COMMA: ',';
ADD: '+';
SUB: '-';
MUL: '*';
DIV: '/';
LEFT_PARENTHESES: '(';
RIGHT_PARENTHESES: ')';

WS: [ \n\t\r]+ -> skip;
