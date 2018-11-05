package com.github.dariusf.rpqlol;

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.unwrap
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Test

class UnifyTest {

  @Test
  fun primitiveFailure() {
    assertTrue(unify(Env(), Num(1), Num(2)) is Err)
  }

  @Test
  fun primitiveSuccess() {
    assertEquals(Env(), unify(Env(), Num(1), Num(1)).unwrap())
  }

  @Test
  fun variables() {
    assertEquals(Env(Var("x" )to Num(1)),
        unify(Env(), Var("x"), Num(1)).unwrap())
  }

  @Test
  fun conflictingVariables() {
    assertTrue(unify(Env(),
        Functor("a", Var("x"), Var("x")),
        Functor("a", Num(1), Num(2))) is Err)
  }

  @Test
  fun multipleVariables() {
    assertEquals(Env(Var("x") to Num(1), Var("y") to Num(1)),
        unify(Env(),
            Functor("a", Var("x"), Var("y")),
            Functor("a", Num(1), Var("x"))).unwrap())
  }

  @Test
  fun resolveAll() {
    val result = unify(Env(),
        Functor("a", Var("a"), Var("a")),
        Functor("a", Var("b"), Num(1)))

    assertTrue(result is Ok)

    assertEquals(Env(Var("a") to Var("b"), Var("b") to Num(1)), result.unwrap())
    assertEquals(Env(Var("a") to Num(1), Var("b") to Num(1)), resolveAll(result.unwrap()))
  }
}

