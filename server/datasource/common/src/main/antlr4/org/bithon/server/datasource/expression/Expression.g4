grammar Expression;

parse
   : expression EOF
   ;

expression
  : expression LEFT_SQUARE_BRACKET INTEGER_LITERAL RIGHT_SQUARE_BRACKET #arrayAccessExpression
  | expression LEFT_SQUARE_BRACKET STRING_LITERAL RIGHT_SQUARE_BRACKET  #mapAccessExpression
  | expression (MUL|DIV) expression                                     #arithmeticExpression
  | expression (ADD|SUB) expression                                     #arithmeticExpression
  | expression (simplePredicate | extraPredicate | notPredicate) expression           #comparisonExpression
  | expression (IN|NOT IN) expressionListDecl                           #inExpression
  | expression IS NULL                                                  #isNullExpression
  | functionExpressionDecl                                              #functionExpression
  | notExpressionDecl                                                   #notExpression
  | expression AND expression                                           #logicalExpression
  | expression OR expression                                            #logicalExpression
  | expressionListDecl                                                  #expressionList
  | literalExpressionDecl                                               #literalExpression
  | identifierExpressionDecl                                            #identifierExpression
  | macroExpressionDecl                                                 #macroExpression
  | expression QUESTION_MARK expression COLON expression                #ternaryExpression
  ;

// The 'endsWith' and 'startsWith' functions are supported in previous version,
// but now they're defined as predicate, to make sure the backward compatibility, we put them in the expression below
functionExpressionDecl
   : (IDENTIFIER | ENDSWITH | STARTSWITH | HASTOKEN) expressionListDecl
   ;

notExpressionDecl
  : NOT expression
  ;

expressionListDecl
  : LEFT_PARENTHESIS expression (COMMA expression)* RIGHT_PARENTHESIS
  | LEFT_PARENTHESIS RIGHT_PARENTHESIS
  ;

literalExpressionDecl
  : INTEGER_LITERAL
  | DECIMAL_LITERAL
  | STRING_LITERAL
  | BOOL_LITERAL
  | durationLiteral
  | READABLE_SIZE_LITERAL
  | READABLE_PERCENTAGE_LITERAL
  ;

durationLiteral
    // Time unit converters are supported as a suffix of the duration literal for better syntax suggestion
  : READABLE_DURATION_LITERAL (DOT ('toMilliSeconds' | 'toMicroSeconds' | 'toNanoSeconds'))?
  ;

identifierExpressionDecl
  : IDENTIFIER (DOT IDENTIFIER)*
  ;

macroExpressionDecl
  : LEFT_CURLY_BRACE IDENTIFIER RIGHT_CURLY_BRACE
  ;

simplePredicate
  : LT | LTE | GT | GTE | NE | EQ
  ;

extraPredicate
  : STARTSWITH | ENDSWITH | CONTAINS | HASTOKEN | REGEX_MATCH | NOT_REGEX_MATCH
  ;

notPredicate
  : NOT extraPredicate
  ;

// The underscore in number can't be the first and last character
INTEGER_LITERAL: '-'? DIGIT ((DIGIT | '_')* DIGIT)?;
DECIMAL_LITERAL: '-'? DIGIT ((DIGIT | '_')* DIGIT)? '.' (DIGIT | DIGIT (DIGIT | '_')* DIGIT)?;

STRING_LITERAL: SQUOTA_STRING;
BOOL_LITERAL: TRUE | FALSE;
READABLE_DURATION_LITERAL: INTEGER_LITERAL [smhd];
READABLE_SIZE_LITERAL: INTEGER_LITERAL ('K' ('i' | 'iB')? | 'M' ('i' | 'iB')? | 'G' ('i' | 'iB')? | 'T' ('i' | 'iB')? | 'P' ('i' | 'iB')?);
READABLE_PERCENTAGE_LITERAL:  [0-9]+('.'[0-9]+)*'%';

LEFT_PARENTHESIS: '(';
RIGHT_PARENTHESIS: ')';
LEFT_CURLY_BRACE: '{';
RIGHT_CURLY_BRACE: '}';
LEFT_SQUARE_BRACKET: '[';
RIGHT_SQUARE_BRACKET: ']';
QUESTION_MARK: '?';
COLON: ':';
DOT: '.';

COMMA: ',';
ADD: '+';
SUB: '-';
MUL: '*';
DIV: '/';
LT: '<';
LTE: '<=';
GT: '>';
GTE: '>=';
NE: '<>' | '!=';
EQ: '=';
AND: A N D;
OR: O R;
IN: I N;
REGEX_MATCH: '=~';
NOT_REGEX_MATCH: '!~';
HASTOKEN: H A S T O K E N;
ENDSWITH: E N D S W I T H;
STARTSWITH: S T A R T S W I T H;
CONTAINS: C O N T A I N S;
NOT: N O T;
TRUE: T R U E;
FALSE: F A L S E;
IS: I S;
NULL: N U L L;

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
fragment LETTER: [a-zA-Z];
fragment DIGIT : '0'..'9';
fragment SQUOTA_STRING: '\'' ('\\'. | '\'\'' | ~('\'' | '\\'))* '\'';

IDENTIFIER
 : LETTER (LETTER | DIGIT | '_')*
 | '_' LETTER (LETTER | DIGIT | '_')*
 ;

WS: [ \n\t\r]+ -> skip;
