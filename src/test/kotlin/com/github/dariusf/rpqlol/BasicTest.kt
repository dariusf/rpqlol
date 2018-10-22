package com.github.dariusf.rpqlol

import kotlinx.coroutines.runBlocking
import org.junit.Test

class BasicTest {

  @Test
  fun test() {

    val data = arrayListOf(
        Functor("node", arrayListOf(Num(1))),
        Functor("node", arrayListOf(Num(2)))
    )
    val db = Database(data)

    val query = arrayListOf(
        Functor("node", arrayListOf(Var("x")))
    )
    runBlocking {
      val query1 = query(db, query)

      val take = query1.take(1).toList()
      println(take)

      val take1 = query1.take(1).toList()
      println(take1)
    }
  }
}
