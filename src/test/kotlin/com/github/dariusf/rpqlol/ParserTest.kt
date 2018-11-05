package com.github.dariusf.rpqlol

import junit.framework.TestCase.assertEquals
import org.junit.Test

class ParserTest {

  @Test
  fun fact() {
    assertEquals(
        arrayListOf(Fact(Functor("a", Str("b"), Num(1)))),
        parseProgram("a(b, 1)."))
  }

  @Test
  fun factRule() {
    assertEquals(
        arrayListOf(Rule(Fact(Functor("a", Var("X"), Num(1))))),
        parseProgram("a(X, 1)."))
  }

  @Test
  fun rule() {
    assertEquals(
        arrayListOf(Rule(Fact(Functor("a", Var("X"), Num(1))),
            Fact(Functor("b", Functor("path", Num(1), Var("X")))),
            Fact(Functor("c", Num(2))))),
        parseProgram("a(X, 1) :- b(path(1, X)), c(2)."))
  }

  @Test
  fun query() {
    assertEquals(
        arrayListOf(Functor("a", Num(1)), Functor("c", Num(value = 2))),
        parseQuery("a(1), c(2)."))
  }
}
