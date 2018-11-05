package com.github.dariusf.rpqlol

import com.github.andrewoma.dexx.kollection.ImmutableMap
import com.github.andrewoma.dexx.kollection.immutableMapOf
import com.github.andrewoma.dexx.kollection.toImmutableMap
import com.github.h0tk3y.betterParse.combinators.and
import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.combinators.oneOrMore
import com.github.h0tk3y.betterParse.combinators.or
import com.github.h0tk3y.betterParse.combinators.separatedTerms
import com.github.h0tk3y.betterParse.combinators.skip
import com.github.h0tk3y.betterParse.grammar.Grammar
import com.github.h0tk3y.betterParse.grammar.parseToEnd
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.parser.Parser
import com.github.h0tk3y.betterParse.parser.parseToEnd
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.unwrap
import kotlinx.coroutines.runBlocking
import java.util.*
import kotlin.collections.set

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
data class Functor(val name: String, val args: List<Value>) : Value() {
  constructor(name: String, vararg elements: Value) : this(name, arrayListOf(*elements))
}

sealed class Expr
data class Fact(val f: Functor) : Expr()
data class Rule(val head: Fact, val body: List<Fact>) : Expr() {
  constructor(head: Fact, vararg body: Fact) : this(head, arrayListOf(*body))
}

typealias Program = List<Expr>

fun visitValues(e: Expr, f: (Value) -> Value): Expr {
  return when (e) {
    is Fact -> Fact(Functor(e.f.name, e.f.args.map(f)))
    is Rule -> Rule(visitValues(e.head, f) as Fact, e.body.map { visitValues(it, f) as Fact })
  }
}

fun instantiate(db: Database, e: Expr): Expr {
  // old -> new
  val bindings: MutableMap<Value, Var> = hashMapOf()
  return visitValues(e) {
    when (it) {
      is Var ->
        if (it in bindings) {
          bindings[it]!!
        } else {
          val v = db.v()
          bindings[it] = v
          v
        }
      else -> it
    }
  }
}

class Database(
    val facts: List<Value> = arrayListOf(),
    var fresh: Int = 0
) {
  // Variables are not allowed at the top level?
  fun query(vararg query: Functor): Sequence<Env> {
    return query(arrayListOf(*query))
  }

  fun query(query: List<Functor>): Sequence<Env> {
    return runSearch(this, 0, query, Env())
  }

  fun v(): Var {
    return Var("v${fresh++}")
  }
}

class UnificationFailure(message: String) : Exception(message)

data class Env(val bindings: ImmutableMap<String, Value> = immutableMapOf()) {

  operator fun get(k: String): Value? = bindings[k]
  // Assignment can't be an expression, so we don't overload set

  operator fun contains(k: String) = k in bindings

  fun put(k: String, v: Value): Env = Env(bindings.put(k, v))

  constructor(vararg elements: Pair<String, Value>) : this(immutableMapOf(*elements))

  override fun toString(): String {
    return "Env(${bindings.entries.map { "${it.key}=${it.value}" }.joinToString(", ")})"
  }
}

/**
 * Assuming that v is in env, resolves v recursively.
 */
tailrec fun resolve(env: Env, v: Value): Value =
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

/**
 * Resolves all bindings in the environment recursively
 */
fun resolveAll(env: Env): Env {
  // dexx's map builder and pair type are too painful to use
  val env1 = hashMapOf<String, Value>()

  env.bindings.forEach {
    env1[it.key] = resolve(env, it.value)
  }

  return Env(env1.toImmutableMap())
}

fun unify(
    env: Env,
    left: Value,
    right: Value
): Result<Env, String> {
  return try {
    Ok(unifyE(env, left, right))
  } catch (e: UnificationFailure) {
    Err(e.message!!)
  }
}

@Throws(UnificationFailure::class)
fun unifyE(
    env: Env,
    left: Value,
    right: Value
): Env {

  val l = resolve(env, left)
  val r = resolve(env, right)

  when {
    l is Var && r is Var ->
      // smaller -> larger
      return if (l.name < r.name) {
        env.put(l.name, r)
      } else {
        env.put(r.name, l)
      }
    l !is Var && r !is Var ->
      return when {
        l is Num && r is Num && l.value == r.value ->
          env
        l is Str && r is Str && l.value == r.value ->
          env
        l is Functor && r is Functor && l.name == r.name ->
          l.args.zip(r.args).fold(env) { t, (l, r) -> unifyE(t, l, r) }
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
      }
    else ->
      // Symmetric case
      return unifyE(env, right, left)
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
      val env1 = unify(env, qf, dbf)
      if (env1 is Err) {
        continue
      }
      yieldAll(runSearch(db, n + 1, query, env1.unwrap()))
    }
  }
}

object PGrammar : Grammar<Program>() {

  private val IDENT by token("[a-z]\\w*")
  private val VAR by token("[A-Za-z]\\w*")
  private val NUM by token("[0-9]+")
  private val LPAREN by token("\\(")
  private val RPAREN by token("\\)")
  private val DOT by token("\\.")
  private val COMMA by token(",")
  private val IMPLIES by token(":-")

  private val WS by token("\\s+", ignore = true)
  private val NEWLINE by token("[\r\n]+", ignore = true)

  val num: Parser<Num> by NUM.map { Num(Integer.parseInt(it.text)) }
  val atom: Parser<Str> by IDENT.map { Str(it.text) }
  val variable: Parser<Var> by VAR.map { Var(it.text) }

  val primitive: Parser<Value> by num or atom or variable

  val functor: Parser<Functor> by (IDENT and skip(LPAREN) and
      separatedTerms(parser(this::value), COMMA, acceptZero = false) and
      skip(RPAREN))
      .map { (name, args) ->
        Functor(name.text, args)
      }

  val value: Parser<Value> by functor or primitive

  val fact: Parser<Fact> by (functor and skip(DOT)).map { Fact(it) }

  val body: Parser<List<Functor>> by (separatedTerms(parser(this::functor), COMMA, acceptZero = false) and
      skip(DOT))

  val rule: Parser<Rule> by (functor and skip(IMPLIES) and body)
      .map { (head, body) ->
        Rule(Fact(head), body.map { Fact(it) })
      }

  val decl: Parser<Expr> by rule or fact

  val program: Parser<Program> by oneOrMore(decl)

  override val rootParser: Parser<Program> by program
}

fun parseProgram(text: String): Program = PGrammar.parseToEnd(text)

fun parseQuery(text: String): List<Functor> = PGrammar.body.parseToEnd(PGrammar.tokenizer.tokenize(text))

fun main(args: Array<String>) = runBlocking {

  val data = arrayListOf(
      Functor("node", arrayListOf(Num(1))),
      Functor("node", arrayListOf(Num(2))))

  val db = Database(data)

  val query = arrayListOf(
      Functor("node", arrayListOf(Var("x"))))

  val query1 = db.query(query)
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
