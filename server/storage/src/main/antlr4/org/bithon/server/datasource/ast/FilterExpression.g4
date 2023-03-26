grammar FilterExpression;

parse
   : filterExpression EOF
   ;

filterExpression
   : binaryExpression
   | unaryExpression
   | filterExpression logicOperator filterExpression
   | '(' filterExpression ')'
   ;

binaryExpression
   : unaryExpression comparisonOperator unaryExpression
   ;

unaryExpression
   : constExpression
   | objectExpression
   | functionExpression
   ;

constExpression
   : STRING_LITERAL
   | DECIMAL_LITERAL
   | UNSIGNED_INTEGER_LITERAL
   ;

functionExpression
   : IDENTIFIER '(' ')'
   ;

objectExpression
   : simpleNameExpression
   | arrayAccessorExpression
   | objectExpression '.' objectExpression
   ;

arrayAccessorExpression
   : simpleNameExpression '[' UNSIGNED_INTEGER_LITERAL ']'
   ;

simpleNameExpression
   : IDENTIFIER
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
