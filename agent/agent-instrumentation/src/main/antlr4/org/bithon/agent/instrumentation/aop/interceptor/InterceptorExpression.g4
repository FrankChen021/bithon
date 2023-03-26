grammar InterceptorExpression;

// WHEN exist() FOR class_filter ON method_filter
//
// class.name()
// class.annotated(name) ON method_filter
// class.implemented(name)
// Guard

parse
  : whenExpression? classFilterExpression ('ON' | 'on') methodFilterExpression EOF
  ;

whenExpression
  : ('WHEN' | 'when') functionCallExpression
  ;

classFilterExpression
  : ('FOR' | 'for') 'class' '.' functionCallExpression
  ;

methodFilterExpression
   : binaryExpression
   | unaryExpression
   | methodFilterExpression logicOperator methodFilterExpression
   | '(' methodFilterExpression ')'
  ;

binaryExpression
   : unaryExpression comparisonOperator unaryExpression
   |
   ;

unaryExpression
   : constExpression
   | methodObjectExpression
   | functionCallExpression
   ;

constExpression
   : STRING_LITERAL
   | DECIMAL_LITERAL
   | UNSIGNED_INTEGER_LITERAL
   ;

functionCallExpression
   : IDENTIFIER '(' functionCallArgsExpression ')'
   | IDENTIFIER '(' ')'
   ;

functionCallArgsExpression
  : constExpression (',' constExpression)*
  ;

methodObjectExpression
   : simpleNameExpression
   | arrayAccessorExpression
   | methodObjectExpression '.' IDENTIFIER
   ;

simpleNameExpression
   : 'method'
   | 'args'
   ;

arrayAccessorExpression
   : simpleNameExpression '[' UNSIGNED_INTEGER_LITERAL ']'
   ;

comparisonOperator
   : '='
   | '>'
   | '<'
   | '<='
   | '>='
   | '<>'
   | '!='
   ;

logicOperator
   : 'AND'
   | 'and'
   | 'OR'
   | 'or'
   ;

IDENTIFIER
   : [a-zA-Z_] [a-zA-Z_0-9]*
   ;

UNSIGNED_INTEGER_LITERAL
   : [0-9]+
   ;

DECIMAL_LITERAL
   : [0-9]+'.'[0-9]+
   ;

STRING_LITERAL
   : '\'' ( '\\\'' | ~('\'' | '\\') )* '\''
   ;

WS
   : [ \t\r\n] -> skip
   ;
