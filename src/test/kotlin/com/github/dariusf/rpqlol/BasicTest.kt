package com.github.dariusf.rpqlol

import junit.framework.TestCase.assertEquals
import org.junit.Test

class BasicTest {

  @Test
  fun basicNondeterminism() {

    val data = arrayListOf(
        Functor("node", arrayListOf(Num(1))),
        Functor("node", arrayListOf(Num(2)))
    )
    val db = Database(data)

    val query = arrayListOf(
        Functor("node", arrayListOf(Var("x")))
    )

    val query1 = db.query(query)

    val take = query1.toList()
    val expected = arrayListOf(
        Env("x" to Num(1)),
        Env("x" to Num(2))
    )
    println(expected)
    assertEquals(expected, take)
  }
}
