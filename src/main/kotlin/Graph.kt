package com.github.valentinaebi.capybara

class Graph<T>(val adjSets: Map<T, Set<T>>) {

    val vertices: Set<T> get() = adjSets.keys + adjSets.values.flatten()

    fun adjSetOf(u: T): Set<T> = adjSets[u] ?: emptySet()

    fun hasEdge(from: T, to: T): Boolean = (adjSets[from]?.contains(to) == true)

}

class GraphBuilder<T> {
    private val adjLists: MutableMap<T, MutableSet<T>> = mutableMapOf()

    val immutableViewOnGraph: Graph<T> = Graph(adjLists)

    fun addIsolatedVertex(u: T) {
        if (u !in adjLists) {
            adjLists[u] = mutableSetOf()
        }
    }

    fun addEdge(from: T, to: T) {
        adjLists.getOrPut(from) { mutableSetOf() }.add(to)
    }

}
