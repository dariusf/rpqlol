package com.github.dariusf.rpqlol

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.base.Stopwatch
import junit.framework.TestCase.assertTrue
import org.junit.Test
import java.util.*
import java.util.concurrent.TimeUnit

class BenchmarkTest {

  fun kotlinStack(graph: Map<Method, Set<Method>>, start: Method, end: Method): ArrayList<Method> {
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
      return path
    } else {
      throw IllegalStateException("no path found")
    }
  }

  fun kotlinRecursion(graph: Map<Method, Set<Method>>, start: Method, end: Method,
                      seen: MutableSet<Method>, path: MutableList<Method>): List<Method>? {

    if (start == end) {
      return path
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

      path.removeAt(path.size-1)
    }

    // The search dead-ended at all neighbours
    return null
  }

  fun <R> time(msg: String, f: () -> R): R {
    val stopwatch = Stopwatch.createStarted()
    val r = f.invoke()
    println("$msg in ${stopwatch.elapsed(TimeUnit.MILLISECONDS)} ms")
    return r
  }

  @Test
  fun test() {
    val calls = time("read file") {
      val t: TypeReference<List<CallSite>> =
          object : TypeReference<List<CallSite>>() {}
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
    println("kotlin with stack: path of length ${stack.size} found in $stackTime ms")

    stopwatch.reset()
    stopwatch.start()
    val recursion = kotlinRecursion(graph, start, end, hashSetOf(), arrayListOf())!!
    val recursionTime = stopwatch.elapsed(TimeUnit.MILLISECONDS)
    println("kotlin with recursion: path of length ${recursion.size} found in $recursionTime ms")

    assertTrue(recursionTime < stackTime)
  }
}

