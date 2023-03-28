grammar InterceptorExpression;

// WHEN exist() FOR class_filter ON method_filter
//
// class.name()
// class.annotated(name) ON method_filter
// class.implemented(name)
// Guard


// WHEN functionExpression modifier? return(returnExpression) classExpression#methodExpression(argExpression)
//
// WHEN exists() public|private|internal|protected|private com.alibaba.druid.pool.DruidDataSource#close(length=6 AND arg[6] = '')
// implemented('aaaa')#close(args.length=6 AND args[6] = '')
// in('a','b')#close(0)
// in('a','b')#close*(0)
// in('a','b')#*close()
// in('a','c')#annotated('')()
// in('a','c')#overridden('')()

parse
  : whenExpression? modifierExpression? classExpression '#' methodExpression EOF
  ;

whenExpression
  : ('WHEN' | 'when') functionCallExpression
  ;

modifierExpression
  : 'public'
  | 'protected'
  | 'private'
  ;

classExpression
  : classNameExpression
  | functionCallExpression
  ;

classNameExpression
  : IDENTIFIER ('.' IDENTIFIER)* ('*')?
  ;

methodExpression
  : (methodNameExpression | methodFunctionExpression) methodArgExpression
  |
  ;

methodNameExpression
  : '*' IDENTIFIER
  | IDENTIFIER ('*')?
  ;

methodFunctionExpression
  : functionCallExpression
  ;

methodArgExpression
  : '(' (UNSIGNED_INTEGER_LITERAL)? ')'
  | '(' argFilterExpression ')'
  ;

argFilterExpression
   : binaryExpression
   | argFilterExpression logicExpression argFilterExpression
  ;

binaryExpression
   : objectExpression predicateExpression constExpression
   |
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

objectExpression
   : simpleNameExpression '.' propertyAccessorExpression
   | arrayAccessorExpression
   ;

simpleNameExpression
   : IDENTIFIER
   ;

arrayAccessorExpression
   : simpleNameExpression '[' UNSIGNED_INTEGER_LITERAL ']'
   ;

propertyAccessorExpression
   : IDENTIFIER
   ;

predicateExpression
   : '='
   | '>'
   | '<'
   | '<='
   | '>='
   | '<>'
   | '!='
   ;

logicExpression
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
