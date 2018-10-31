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
    assertEquals(Env("x" to Num(1)),
        unify(Env(), Var("x"), Num(1)).unwrap())
  }

  @Test
  fun conflictingVariables() {
    assertTrue(unify(Env(),
        Functor("a", arrayListOf(Var("x"), Var("x"))),
        Functor("a", arrayListOf(Num(1), Num(2)))) is Err)
  }

  @Test
  fun multipleVariables() {
    assertEquals(Env("x" to Num(1), "y" to Num(1)),
        unify(Env(),
            Functor("a", arrayListOf(Var("x"), Var("y"))),
            Functor("a", arrayListOf(Num(1), Var("x")))).unwrap())
  }

  @Test
  fun resolveAll() {
    val result = unify(Env(),
        Functor("a", arrayListOf(Var("a"), Var("a"))),
        Functor("a", arrayListOf(Var("b"), Num(1))))

    assertTrue(result is Ok)

    assertEquals(Env("a" to Var("b"), "b" to Num(1)), result.unwrap())
    assertEquals(Env("a" to Num(1), "b" to Num(1)), resolveAll(result.unwrap()))
  }
}

