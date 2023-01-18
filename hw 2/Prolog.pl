% This file illustrates Prolog.pl from HW2-P.
%
% Although some rules can be combined using disjunction,
% I seperated them to make them more readable.
%
% I assume we don't have to handle male/male or female/female marriage.
%
% author Michael Lee, ml3406@rit.edu


% children's children
grandchild(X0,Y0):-child(X0,Z0),child(Z0,Y0).

% parents' parents
grandparent(X1,Y1):-child(Y1,Z1),child(Z1,X1).

% anyone who is elder than me and is derect-blood relative to me
ancestor(X2,Y2):-child(Y2,X2).
ancestor(X2,Y2):-child(Y2,Z2),ancestor(X2,Z2).
% I am not sure if my definition of ancestor is right.
% If the definition is grandparents' parents, the rule should be:
% ancestor(X2,Y2):-grandchild(Y2,Z2),child(Z2,X2).

% mom's(or father's) sons except himself
brother(X3,Y3):-male(X3),child(X3,Z3),child(Y3,Z3),female(Z3),X3\=Y3.

% mom's(or father's) daughters except herself
sister(X4,Y4):-female(X4),child(X4,Z4),child(Y4,Z4),female(Z4),X4\=Y4.

% female children
daughter(X5,Y5):-female(X5),child(X5,Y5).

% male children
son(X6,Y6):-male(X6),child(X6,Y6).

% parents' siblings' children
firstCousin(X7,Y7):-child(X7,Z7),child(Y7,W7),sister(Z7,W7);child(X7,Z7),child(Y7,W7),brother(Z7,W7).

% spouse's brothers
brotherInLaw(X8,Y8):-spouse(Y8,Z8),brother(X8,Z8);spouse(Z8,Y8),brother(X8,Z8).
% sisters' spouses
brotherInLaw(X8,Y8):-sister(Z8,Y8),spouse(X8,Z8);sister(Z8,Y8),spouse(Z8,X8).
% spouse's sisters' spouses
brotherInLaw(X8,Y8):-spouse(Y8,Z8),sister(W8,Z8),spouse(W8,X8);spouse(Y8,Z8),sister(W8,Z8),spouse(X8,W8).
brotherInLaw(X8,Y8):-spouse(Z8,Y8),sister(W8,Z8),spouse(W8,X8);spouse(Z8,Y8),sister(W8,Z8),spouse(X8,W8).

% spouse's sisters
sisterInLaw(X9,Y9):-spouse(Y9,Z9),sister(X9,Z9);spouse(Z9,Y9),sister(X9,Z9).
% brothers' spouses
sisterInLaw(X9,Y9):-brother(Z9,Y9),spouse(X9,Z9);brother(Z9,Y9),spouse(Z9,X9).
% spouse's brothers' spouses
sisterInLaw(X8,Y8):-spouse(Y8,Z8),brother(W8,Z8),spouse(W8,X8);spouse(Y8,Z8),brother(W8,Z8),spouse(X8,W8).
sisterInLaw(X8,Y8):-spouse(Z8,Y8),brother(W8,Z8),spouse(W8,X8);spouse(Z8,Y8),brother(W8,Z8),spouse(X8,W8).

% parents' sisters
aunt(X10,Y10):-female(X10),child(Y10,Z10),sister(X10,Z10).

% parents' brothers
uncle(X11,Y11):-male(X11),child(Y11,Z11),brother(X11,Z11).


















