package com.github.dariusf.rpqlol

import junit.framework.TestCase.assertEquals
import org.junit.Test

class InstantiateTest {
  @Test
  fun instantiation() {
    assertEquals(
        instantiate(Database(), Rule(
            Fact(Functor("a", Var("v0"))),
            Fact(Functor("b", Var("v0"))), Fact(Functor("c", Var("v1"))))),
        instantiate(Database(), Rule(
            Fact(Functor("a", Var("x"))),
            Fact(Functor("b", Var("x"))), Fact(Functor("c", Var("y")))))
    )
  }
}
