package com.github.dariusf.rpqlol

import com.github.andrewoma.dexx.kollection.ImmutableMap
import com.github.andrewoma.dexx.kollection.immutableMapOf
import kotlinx.coroutines.runBlocking
import java.util.*

class Graph(val graph: Map<Int, Set<Int>> = hashMapOf())

fun transitiveClosure(db: Graph, a: Int) = sequence {

  val stack = Stack<Int>()
  stack.push(a)
  val seen = hashSetOf<Int>()

  while (stack.isNotEmpty()) {
    val elt = stack.pop()
    if (elt in seen) {
      continue
    }
    seen.add(elt)
    yield(elt)
    val neighbours = db.graph[elt] ?: emptySet()
    neighbours.forEach { stack.push(it) }
  }
}

sealed class Value
data class Num(val value: Int) : Value()
data class Str(val value: String) : Value()
data class Var(val name: String) : Value()
data class Functor(val name: String, val args: List<Value>) : Value()

sealed class Expr
data class Fact(val atom: String, val args: List<Value>) : Expr()
data class Rule(val head: Fact, val body: List<Value>) : Expr()

class Database(val facts: List<Value> = arrayListOf())

class UnificationFailure(message: String) : Exception(message)

typealias Env = ImmutableMap<String, Value>

/**
 * Assuming that v is in env, resolves v recursively.
 */
tailrec fun resolve(env: ImmutableMap<String, Value>, v: Value): Value =
    when (v) {
      is Var -> {
        val r = env[v.name]
        when (r) {
//          null -> throw Exception("$v not found in environment")
          null -> v
          v -> v
          else -> resolve(env, r)
        }
      }
      else -> v
    }

@Throws(UnificationFailure::class)
fun unify(
    env: ImmutableMap<String, Value>,
    left: Value,
    right: Value
): ImmutableMap<String, Value> {

  val l = resolve(env, left)
  val r = resolve(env, right)

  when {
    l is Var && r is Var ->
      // smaller -> larger
      return if (l.name < r.name) {
        env.put(l.name, r)
  //        env[l.name] = r
      } else {
        env.put(r.name, l)
  //        env[r.name] = l
      }
    l !is Var && r !is Var ->
      return when {
        l is Num && r is Num && l.value == r.value ->
          env
        l is Str && r is Str && l.value == r.value ->
          env
        l is Functor && r is Functor && l.name == r.name ->
//          l.args.zip(r.args).forEach { (l, r) -> unify(env, l, r) }
          l.args.zip(r.args).fold(env) { t, c -> unify(t, c.first, c.second) }
        else ->
          throw UnificationFailure("failed to unify $l and $r")
      }
    l is Var ->
      if (l.name in env && resolve(env, l) != r) {
//        return Result.failure(Exception("unification failure"))
//        return null
        throw UnificationFailure("failed to unify $l and $r")
      } else {
        return env.put(l.name, r)
//        env[l.name] = r
      }
    else ->
      // Symmetric case
      return unify(env, right, left)
  }
}

/**
 * Note that the annotation on the return type is needed to avoid a compiler crash...
 *
 * https://github.com/Kotlin/kotlinx.coroutines/issues/421
 */
fun runSearch(
    db: Database,
    n: Int,
    query: List<Functor>,
    env: Env
): Sequence<Env> = sequence {

  if (n == query.size) {
    yield(env)
  } else {
    val qf = query[n]
    for (dbf in db.facts) {
      val env1 =
          try {
            unify(env, qf, dbf)
          } catch (e: UnificationFailure) {
            continue
          }
      yieldAll(runSearch(db, n + 1, query, env1))
    }
  }
}

fun query(db: Database, query: List<Functor>): Sequence<Env> {
  return runSearch(db, 0, query, immutableMapOf())
}

fun main(args: Array<String>) = runBlocking {

  val data = arrayListOf(
      Functor("node", arrayListOf(Num(1))),
      Functor("node", arrayListOf(Num(2))))

  val db = Database(data)

  val query = arrayListOf(
      Functor("node", arrayListOf(Var("x"))))

  val query1 = query(db, query)
  println(query1.toList())
  println("done")

//  graphTest()
}

private fun graphTest() {
  val graph = hashMapOf(1 to setOf(2, 3), 2 to setOf(3, 4))
  val db = Graph(graph)

  println(transitiveClosure(db, 1).take(3).toList())

  val tc = transitiveClosure(db, 1).iterator()

  while (tc.hasNext()) {
    println("press enter ${tc.next()}")
    readLine()
  }
  println("done")
}
