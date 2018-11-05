package com.github.dariusf.rpqlol

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.base.Stopwatch
import org.junit.Test
import java.util.*
import java.util.concurrent.TimeUnit

class BenchmarkTest {

  data class Result(val path: List<Method>, val explored: Int)

  fun kotlinStack(graph: Map<Method, Set<Method>>, start: Method, end: Method): Result {
    val parent = hashMapOf<Method, Method>()

    val stack = Stack<Method>()
    stack.push(start)

    val seen = hashSetOf<Method>()

    var found = false

    while (stack.isNotEmpty()) {
      val v = stack.pop()

      // Visit
      if (v == end) {
        found = true
        break
      }

      seen.add(v)

      // Filtering duplicates here instead of on v is crucial if we want the parent
      // collection to be coherent and not contain cycles.
      // Also introduces some randomness.
      val neighbours = (graph[v] ?: mutableSetOf()).filter { it !in seen }.shuffled()

      neighbours.forEach { parent[it] = v }

      stack.addAll(neighbours)
    }

    if (found) {
      // Rebuild the path
      var c = end
      val path = arrayListOf<Method>()
      while (c in parent) {
        path.add(c)
        c = parent[c]!!
      }
      return Result(path, seen.size)
    } else {
      throw IllegalStateException("no path found")
    }
  }

  fun kotlinRecursion(graph: Map<Method, Set<Method>>, start: Method, end: Method,
                      seen: MutableSet<Method>, path: MutableList<Method>): Result? {

    if (start == end) {
      return Result(path, seen.size)
    }

    val neighbours = (graph[start] ?: mutableSetOf()).filter { it !in seen }

    for (neighbour in neighbours.shuffled()) {
      path.add(neighbour)
      seen.add(neighbour)
      val r = kotlinRecursion(graph, neighbour, end, seen, path)

      // Pick the first neighbour at which the search succeeds
      if (r != null) {
        return r
      }

      path.removeAt(path.size - 1)
    }

    // The search dead-ended at all neighbours
    return null
  }

  fun rpq(graph: Database): List<Method> {
    Thread.sleep(200)
    return emptyList()
  }

  inline fun <R> time(msg: String, f: () -> R): R {
    val stopwatch = Stopwatch.createStarted()
    val r = f.invoke()
    println("$msg in ${stopwatch.elapsed(TimeUnit.MILLISECONDS)} ms")
    return r
  }

  @Test
  fun test() {
    val calls = time("read file") {
      val t = object : TypeReference<List<CallSite>>() {}
      val r: List<CallSite> = ObjectMapper().readValue(javaClass.getResourceAsStream("/graph.json"), t)
      r
    }

    val graph = time("indexed graph") {
      val g = hashMapOf<Method, MutableSet<Method>>()
      calls.forEach {
        g.computeIfAbsent(it.caller) { mutableSetOf() }.add(it.callee)
      }
      g
    }

    val start = Method("com/sourceclear/librarian/service/web/LibraryInstanceControllerTest", "testCalculateLibraryDeltaScoreUpdateTo", "()")
    val end = Method("com/fasterxml/jackson/databind/deser/BeanDeserializerFactory", "createBeanDeserializer", "(Lcom/fasterxml/jackson/databind/DeserializationContext;Lcom/fasterxml/jackson/databind/JavaType;Lcom/fasterxml/jackson/databind/BeanDescription;)")

    // measure allocation/memory use?
    val stopwatch = Stopwatch.createStarted()
    val stack = kotlinStack(graph, start, end)
    val stackTime = stopwatch.elapsed(TimeUnit.MILLISECONDS)
    println("kotlin with stack: path of length ${stack.path.size} found in $stackTime ms (time per node " +
        "${stackTime.toFloat() / stack.explored})")

    stopwatch.reset()
    stopwatch.start()
    val recursion = kotlinRecursion(graph, start, end, hashSetOf(), arrayListOf())!!
    val recursionTime = stopwatch.elapsed(TimeUnit.MILLISECONDS)
    println("kotlin with recursion: path of length ${recursion.path.size} found in $recursionTime ms (time per node " +
        "${recursionTime.toFloat() / recursion.explored})")

//    assertTrue(recursionTime < stackTime)

    val database = time("conversion into database") {
      val data = arrayListOf<Expr>()
      var i = 0
      val ids = hashMapOf<Method, Int>()

      graph.keys.forEach {
        ids[it] = i++
        data.add(Fact(Functor("node", Num(i - 1))))
      }

      graph.entries.forEach {
        it.value.forEach {
          if (it !in ids) {
            ids[it] = i++
            data.add(Fact(Functor("node", Num(i - 1))))
          }
        }
      }

      graph.entries.forEach {
        val from = it.key
        it.value.forEach {
          data.add(Fact(Functor("edge", Num(ids[from]!!), Num(ids[it]!!))))
        }
      }

      data.add(Fact(Functor("start", Num(ids[start]!!))))
      data.add(Fact(Functor("end", Num(ids[end]!!))))

//      data.addAll(parseProgram("""
//
//      """))

      Database(data)
    }

    stopwatch.reset()
    stopwatch.start()

    val rpq = rpq(database)
    val rpqTime = stopwatch.elapsed(TimeUnit.MILLISECONDS)
    println("rpq: path of length ${rpq.size} found in $rpqTime ms")
  }
}

