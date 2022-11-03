grammar FieldExpression;

parse
   : fieldExpression EOF
   ;

fieldExpression
  : NUMBER
  | fieldNameExpression
  | variableExpression
  | fieldExpression op=(ADD|SUB|MUL|DIV) fieldExpression
  | function
  | LEFT_PARENTHESES fieldExpression RIGHT_PARENTHESES
  ;

fieldNameExpression
  : ID
  ;

variableExpression: '{' ID '}';

function
  : functionNameExpression LEFT_PARENTHESES fieldExpression (COMMA fieldExpression)* RIGHT_PARENTHESES
  ;

functionNameExpression
  : ID
  ;

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
