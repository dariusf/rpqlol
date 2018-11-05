package com.github.dariusf.rpqlol

import junit.framework.TestCase.assertEquals
import org.junit.Test

class EvalTest {

  val DB = Database(arrayListOf(
      Fact(Functor("node", Num(1))),
      Fact(Functor("node", Num(2))),
      Fact(Functor("node", Num(3))),
      Fact(Functor("node", Num(4))),
      Fact(Functor("edge", Num(1), Num(3))),
      Fact(Functor("edge", Num(2), Num(4))),
      Fact(Functor("edge", Num(3), Num(2))),
      Rule(
          Fact(Functor("path", Var("x"), Var("y"))),
          Fact(Functor("edge", Var("x"), Var("y")))),
      Rule(
          Fact(Functor("path", Var("x"), Var("z"))),
          Fact(Functor("edge", Var("x"), Var("y"))),
          Fact(Functor("path", Var("y"), Var("z"))))
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
        Env("x" to Num(2), "y" to Num(4)),
        Env("x" to Num(3), "y" to Num(2))
    )
    assertEquals(expected, result)
  }

  @Test
  fun rules() {
    assertEquals(
        arrayListOf(
            Env("x" to Num(1)),
            Env("x" to Num(2)),
            Env("x" to Num(3)),
            Env("x" to Num(4))),
        DB.query(Functor("path", Num(1), Var("x"))).toList()
    )
  }
}
