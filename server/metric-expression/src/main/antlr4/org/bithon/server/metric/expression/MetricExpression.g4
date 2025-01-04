grammar MetricExpression;

alertExpression
  : metricExpression                                    #simpleAlertExpression
  | alertExpression AND alertExpression                 #logicalAlertExpression
  | alertExpression OR alertExpression                  #logicalAlertExpression
  | LEFT_PARENTHESIS alertExpression RIGHT_PARENTHESIS  #parenthesisAlertExpression
  ;

// sum by (a,b,c) (metric {})
metricExpression
  : aggregatorExpression LEFT_PARENTHESIS metricQNameExpression labelExpression? RIGHT_PARENTHESIS durationExpression? groupByExpression? (metricPredicateExpression metricExpectedExpression)?
  ;

aggregatorExpression
  : IDENTIFIER
  ;

groupByExpression
  : BY LEFT_PARENTHESIS IDENTIFIER (COMMA IDENTIFIER)* RIGHT_PARENTHESIS
  ;

// Qualifed name of a metric
metricQNameExpression
  : dataSourceExpression DOT metricNameExpression
  ;

dataSourceExpression
  : IDENTIFIER
  ;

metricNameExpression
  : IDENTIFIER
  ;

labelExpression
  : LEFT_CURLY_BRACE RIGHT_CURLY_BRACE
  | LEFT_CURLY_BRACE labelSelectorExpression (COMMA labelSelectorExpression)* RIGHT_CURLY_BRACE
  ;

durationExpression
  : LEFT_SQUARE_BRACKET DURATION_LITERAL RIGHT_SQUARE_BRACKET
  ;

labelSelectorExpression
  : IDENTIFIER labelPredicateExpression literalExpression #comparisonExpression
  | IDENTIFIER NOT? IN literalListExpression #inExpression
  ;

labelPredicateExpression
  : LT|LTE|GT|GTE|NE|EQ
  | NOT? (CONTAINS|STARTSWITH|ENDSWITH|HASTOKEN)
  ;

metricPredicateExpression
  : LT|LTE|GT|GTE|NE|EQ
  | IS
 ;

literalExpression
  : STRING_LITERAL | INTEGER_LITERAL | DECIMAL_LITERAL | PERCENTAGE_LITERAL | NULL_LITERAL | SIZE_LITERAL
  ;

literalListExpression
  : LEFT_PARENTHESIS literalExpression (COMMA literalExpression)? RIGHT_PARENTHESIS
  ;

metricExpectedExpression
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
HASTOKEN: H A S T O K E N;
CONTAINS: C O N T A I N S;
STARTSWITH: S T A R T S W I T H;
ENDSWITH: E N D S W I T H;

DURATION_LITERAL: INTEGER_LITERAL [smhd];

// Suppported forms:
// 5K  -- decimal format,          = 5 * 1000
// 5Ki -- simplifed binary format, = 5 * 1024
// 5KiB -- binary format,          = 5 * 1024
SIZE_LITERAL: INTEGER_LITERAL ('K' ('i' | 'iB')? | 'M' ('i' | 'iB')? | 'G' ('i' | 'iB')? | 'T' ('i' | 'iB')? | 'P' ('i' | 'iB')?);
INTEGER_LITERAL: '-'?([1-9][0-9]*|[0]);
DECIMAL_LITERAL: '-'?[0-9]+'.'[0-9]*;
PERCENTAGE_LITERAL:  '-'?[0-9]+('.'[0-9]+)*'%';

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
