package test;

import annotation.Refl;

@Refl
 public interface MumAlg<E, P> {

    //	E ifNode(E e1, E e2, E e3);
    //
    //	E defineNode(String slot, E e);
    //
    //	E quoteNode(E literalNode);
    //
    //	E lambdaNode(List<E> args, List<E> rtns);
    //
    //	E listNode(List<E> args);

	E booleanNode(boolean x);
    P start(E es);

    //	E longNode(Object x);
    //
    //    E mumblerSymbol(String symbolName);
    //
    //	E stringNode(String x);
    //
    //	P start(List<E> es);
    //
    //	E invokeNode(String func, List<E> args);

}
