grammar AlertExpression;

parse
  : expression EOF
  ;

expression
  : selectExpression alertPredicateExpression alertExpectedExpression #alertExpression
  | expression AND expression   #logicalAlertExpression
  | expression OR expression    #logicalAlertExpression
  | LEFT_PARENTHESIS expression RIGHT_PARENTHESIS            #braceAlertExpression
  ;

// sum by (a,b,c) (metric {})
selectExpression
  : aggregatorExpression groupByExpression? LEFT_PARENTHESIS metricExpression whereExpression? RIGHT_PARENTHESIS durationExpression?
  ;

aggregatorExpression
  : IDENTIFIER
  ;

groupByExpression
  : BY LEFT_PARENTHESIS IDENTIFIER (COMMA IDENTIFIER)? RIGHT_PARENTHESIS
  ;

metricExpression
  : dataSourceExpression DOT metricNameExpression
  ;

dataSourceExpression
  : IDENTIFIER
  ;

metricNameExpression
  : IDENTIFIER
  ;


whereExpression
  : LEFT_CURLY_BRACE RIGHT_CURLY_BRACE
  | LEFT_CURLY_BRACE filterExpression (COMMA filterExpression)* RIGHT_CURLY_BRACE
  ;

durationExpression
  : LEFT_SQUARE_BRACKET DURATION_LITERAL RIGHT_SQUARE_BRACKET
  ;

filterExpression
  : IDENTIFIER predicateExpression literalExpression #simpleFilterExpression
  | IDENTIFIER IN literalListExpression #inFilterExpression
  | IDENTIFIER NOT IN literalListExpression #notInFilterExpression
  | IDENTIFIER NOT LIKE literalExpression #notLikeFilterExpression
  ;

predicateExpression
  : LT|LTE|GT|GTE|NE|EQ|LIKE
  ;

alertPredicateExpression
  : LT|LTE|GT|GTE|NE|EQ|IS
 ;

literalExpression
  : STRING_LITERAL | INTEGER_LITERAL | DECIMAL_LITERAL | PERCENTAGE_LITERAL | NULL_LITERAL | SIZE_LITERAL
  ;

literalListExpression
  : LEFT_PARENTHESIS literalExpression (COMMA literalExpression)? RIGHT_PARENTHESIS
  ;

alertExpectedExpression
  : literalExpression durationExpression?
  ;

//
// Keywords
//
BY: B Y;
AND : A N D;
OR: O R;
ID: [A-Z];

LEFT_PARENTHESIS: '(';
RIGHT_PARENTHESIS: ')';
LEFT_CURLY_BRACE: '{';
RIGHT_CURLY_BRACE: '}';
LEFT_SQUARE_BRACKET: '[';
RIGHT_SQUARE_BRACKET: ']';
COMMA: ',';
DOT: '.';

// Predicate
LT: '<';
LTE: '<=';
GT: '>';
GTE: '>=';
NE: '<>' | '!=';
EQ: '=';
IS: I S;
IN: I N;
NOT: N O T;
LIKE: L I K E;

DURATION_LITERAL: INTEGER_LITERAL [smhd];

// Suppported forms:
// 5K  -- decimal format,          = 5 * 1000
// 5Ki -- simplifed binary format, = 5 * 1024
// 5KiB -- binary format,          = 5 * 1024
SIZE_LITERAL: INTEGER_LITERAL ('K' ('i' | 'iB')? | 'M' ('i' | 'iB')? | 'G' ('i' | 'iB')? | 'T' ('i' | 'iB')? | 'P' ('i' | 'iB')?);
INTEGER_LITERAL: '-'?([1-9][0-9]*|[0]);
DECIMAL_LITERAL: '-'?[0-9]+'.'[0-9]*;
PERCENTAGE_LITERAL:  [0-9]+('.'[0-9]+)*'%';

// Allow using single quote or double quote
STRING_LITERAL
    : '\'' (~('\'' | '\\') | '\\' .)* '\''
    | '"' (~('"' | '\\') | '\\' .)* '"'
    ;

NULL_LITERAL: N U L L;

// Note that the dash character is allowed in the identifier
IDENTIFIER
   : [A-Za-z_][A-Za-z_0-9-]*
   ;

// case insensitive
fragment A : [aA];
fragment B : [bB];
fragment C : [cC];
fragment D : [dD];
fragment E : [eE];
fragment F : [fF];
fragment G : [gG];
fragment H : [hH];
fragment I : [iI];
fragment J : [jJ];
fragment K : [kK];
fragment L : [lL];
fragment M : [mM];
fragment N : [nN];
fragment O : [oO];
fragment P : [pP];
fragment Q : [qQ];
fragment R : [rR];
fragment S : [sS];
fragment T : [tT];
fragment U : [uU];
fragment V : [vV];
fragment W : [wW];
fragment X : [xX];
fragment Y : [yY];
fragment Z : [zZ];

WS: [ \n\t\r]+ -> skip;
