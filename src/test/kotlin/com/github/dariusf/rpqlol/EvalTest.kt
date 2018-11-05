package com.github.dariusf.rpqlol

import junit.framework.TestCase.assertEquals
import org.junit.Test

class EvalTest {

  val DB = Database("""
        node(1).
        node(2).
        node(3).
        node(4).
        edge(1, 3).
        edge(2, 4).
        edge(3, 2).
        path(X, Y) :- edge(X, Y).
        path(X, Z) :- edge(X, Y), path(Y, Z).
      """)

  @Test
  fun dbFromProgram() {
    assertEquals(DB,
        Database(arrayListOf(
            Fact(Functor("node", Num(1))),
            Fact(Functor("node", Num(2))),
            Fact(Functor("node", Num(3))),
            Fact(Functor("node", Num(4))),
            Fact(Functor("edge", Num(1), Num(3))),
            Fact(Functor("edge", Num(2), Num(4))),
            Fact(Functor("edge", Num(3), Num(2))),
            Rule(
                Fact(Functor("path", Var("X"), Var("Y"))),
                Fact(Functor("edge", Var("X"), Var("Y")))),
            Rule(
                Fact(Functor("path", Var("X"), Var("Z"))),
                Fact(Functor("edge", Var("X"), Var("Y"))),
                Fact(Functor("path", Var("Y"), Var("Z")))))))
  }

  @Test
  fun basicNondeterminism() {
    val result = DB.query(Functor("node", Var("x"))).toList()
    val expected = arrayListOf(
        Env(Var("x") to Num(1)),
        Env(Var("x") to Num(2)),
        Env(Var("x") to Num(3)),
        Env(Var("x") to Num(4))
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
        Env(Var("x") to Num(1), Var("y") to Num(3)),
        Env(Var("x") to Num(2), Var("y") to Num(4)),
        Env(Var("x") to Num(3), Var("y") to Num(2))
    )
    assertEquals(expected, result)
  }

  @Test
  fun rules() {
    assertEquals(
        arrayListOf(
            Env(Var("x") to Num(3)),
            Env(Var("x") to Num(2)),
            Env(Var("x") to Num(4))),
        DB.query(Functor("path", Num(1), Var("x")))
            .map { resolveAll(it) }.toList()
    )
  }

  @Test
  fun computation() {
    val database = Database("""
      append(nil, Y, Y).
      append(cons(X, Xs), Y, cons(X, Z)) :- append(Xs, Y, Z).
    """)

    val query = database.query("append(cons(1, nil), cons(2, cons(3, nil)), Z).")

    assertEquals(arrayListOf(
        Env(Var("Z") to
            Functor("cons", Num(1), Functor("cons", Num(2), Functor("cons", Num(3), Str("nil")))))),
        query.toList().map { resolveAll(it) })

    val query1 = database.query("append(cons(1, nil), Z, cons(1, cons(2, nil))).")

    assertEquals(arrayListOf(
        Env(Var("Z") to
            Functor("cons", Num(2), Str("nil")))),
        query1.toList().map { resolveAll(it) })

    val query2 = database.query("append(X, Y, Z).")

    query2.take(3).map { resolveAll(it) }.toList()
  }
}
