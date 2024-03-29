grammar AlertExpression;

parse
  : expression EOF
  ;

expression
  : selectExpression alertPredicateExpression alertExpectedExpression #alertExpression
  | expression AND expression   #logicalAlertExpression
  | expression OR expression    #logicalAlertExpression
  | '(' expression ')'          #braceAlertExpression
  ;

// sum by (a,b,c) (metric {})
selectExpression
  : aggregatorExpression groupByExpression? '(' nameExpression whereExpression? ')' durationExpression?
  ;

aggregatorExpression
  : IDENTIFIER
  ;

groupByExpression
  : BY '(' IDENTIFIER (',' IDENTIFIER)? ')'
  ;

nameExpression
  : IDENTIFIER '.' IDENTIFIER
  ;

whereExpression
  : '{' '}'
  | '{' filterExpression (',' filterExpression)? '}'
  ;

durationExpression
  : '[' DURATION ']'
  ;

filterExpression
  : IDENTIFIER predicateExpression valueExpression #simpleFilterExpression
  ;

predicateExpression
  : LT|LTE|GT|GTE|NE|EQ|IN|NOT IN|LIKE
  ;

alertPredicateExpression
  : LT|LTE|GT|GTE|NE|EQ|IS
 ;

valueExpression
  : literalExpression
  | literalListExpression
  ;

literalExpression
  : STRING_LITERAL | INTEGER_LITERAL | DECIMAL_LITERAL | PERCENTAGE_LITERAL | NULL_LITERAL
  ;

literalListExpression
  : '(' literalExpression (',' literalExpression)? ')'
  ;

alertExpectedExpression
  : literalExpression durationExpression?
  ;

//
// Keywords
//
DURATION: INTEGER_LITERAL [sSmMhHdD];

BY: B Y;
AND : A N D;
OR: O R;
ID: [A-Z];

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
INCR: I N C R;
DECR: D E C R;

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
