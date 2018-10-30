package com.github.dariusf.rpqlol;

import junit.framework.TestCase.assertEquals
import org.junit.Test

class UnifyTest {

  @Test(expected = UnificationFailure::class)
  fun primitiveFailure() {
    unify(Env(), Num(1), Num(2))
  }

  @Test
  fun primitiveSuccess() {
    assertEquals(Env(), unify(Env(), Num(1), Num(1)))
  }

  @Test
  fun variables() {
    assertEquals(Env("x" to Num(1)), unify(Env(), Var("x"), Num(1)))
  }

  @Test(expected = UnificationFailure::class)
  fun conflictingVariables() {
    unify(Env(),
        Functor("a", arrayListOf(Var("x"), Var("x"))),
        Functor("a", arrayListOf(Num(1), Num(2))))
  }

  @Test
  fun multipleVariables() {
    assertEquals(Env("x" to Num(1), "y" to Num(1)),
        unify(Env(),
            Functor("a", arrayListOf(Var("x"), Var("y"))),
            Functor("a", arrayListOf(Num(1), Var("x")))))
  }

  @Test
  fun noResolveAll() {
    assertEquals(Env("a" to Var("b"), "b" to Num(1)),
        unify(Env(),
            Functor("a", arrayListOf(Var("a"), Var("a"))),
            Functor("a", arrayListOf(Var("b"), Num(1)))))
  }
}

