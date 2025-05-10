package jub.task1.game

import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

// Stores the number of steps and a list of all links to the destination page
// The path must include the final page
data class SearchPath(
    val steps: Int,
    val path: List<String>,
)

class PageSearch(
    private val finalPage: String = KOTLIN_PAGE,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun search(startPage: String, searchDepth: Int, threadsCount: Int): SearchPath? {
        val page = getHtmlDocument(startPage)
        val links = extractReferences(page)

        val queue: ConcurrentLinkedQueue<String> = ConcurrentLinkedQueue()
        queue.addAll(links)

        val parents: ConcurrentHashMap<String, String> = ConcurrentHashMap()
        parents[startPage] = startPage

        val dispatcher = Dispatchers.IO.limitedParallelism(threadsCount)

        return this.searchOnLevel(queue, searchDepth, parents, dispatcher)
    }

    private suspend fun searchOnLevel(queue: ConcurrentLinkedQueue<String>, maxDepth: Int,
                                      parents: ConcurrentHashMap<String, String>,
                                      dispatcher: CoroutineDispatcher): SearchPath? {
        if (maxDepth == 0) {
            return null
        }

        for (link in queue) {
            if (link == this.finalPage) {
                val path = mutableListOf(this.finalPage)
                var currentLink = link
                while (parents[currentLink] != currentLink) {
                    parents[currentLink]?.let { path.add(it) }
                    currentLink = parents[currentLink].toString()
                }
                return SearchPath(path.size, path.reversed())
            }
        }

        val queueNext: ConcurrentLinkedQueue<String> = ConcurrentLinkedQueue()
        val jobs: MutableList<Job> = mutableListOf()

        while (queue.isNotEmpty()) {
            val parentLink = queue.poll()
            launch(dispatcher) {
                val page = getHtmlDocument(parentLink)
                val list = extractReferences(page)
                list.forEach { childLink ->
                    parents.putIfAbsent(childLink, parentLink) ?: queueNext.add(childLink)
                }
            }.also {
                jobs.add(it)
            }
        }
        jobs.joinAll()
        return searchOnLevel(queueNext, maxDepth - 1, parents, dispatcher)
    }

    companion object {
        const val KOTLIN_PAGE = "https://en.wikipedia.org/wiki/Kotlin_(programming_language)"
        const val NOT_FOUND = -1
    }
}

fun main() {
    val pageSearch = PageSearch()
    runBlocking {
        print(pageSearch.search("https://en.wikipedia.org/wiki/Abstraction_(computer_science)", 5, 16))
    }
}

