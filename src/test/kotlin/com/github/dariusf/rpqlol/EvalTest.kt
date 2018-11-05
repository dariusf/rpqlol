package com.github.dariusf.rpqlol

import junit.framework.TestCase.assertEquals
import org.junit.Test

class EvalTest {

  val DB = Database(arrayListOf(
      Functor("node", Num(1)),
      Functor("node", Num(2)),
      Functor("node", Num(3)),
      Functor("node", Num(4)),
      Functor("edge", Num(1), Num(3)),
      Functor("edge", Num(2), Num(4))
  ))

  @Test
  fun basicNondeterminism() {
    val result = DB.query(Functor("node", Var("x"))).toList()
    val expected = arrayListOf(
        Env("x" to Num(1)),
        Env("x" to Num(2)),
        Env("x" to Num(3)),
        Env("x" to Num(4))
    )
    assertEquals(expected, result)
  }

  @Test
  fun joins() {
    val result = DB.query(
        Functor("node", Var("x")),
        Functor("edge", Var("x"), Var("y")),
        Functor("node", Var("y"))).toList()
    val expected = arrayListOf(
        Env("x" to Num(1), "y" to Num(3)),
        Env("x" to Num(2), "y" to Num(4))
    )
    assertEquals(expected, result)
  }
}
