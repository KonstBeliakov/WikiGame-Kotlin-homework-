package jub.task1.game

import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
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
    suspend fun search(startPage: String, searchDepth: Int, threadsCount: Int): SearchPath? {
        val page = getHtmlDocument(startPage)
        val links = extractReferences(page)

        val queue: ConcurrentLinkedQueue<String> = ConcurrentLinkedQueue()
        queue.addAll(links)

        val parents: ConcurrentHashMap<String, String> = ConcurrentHashMap()
        parents[startPage] = startPage

        return this._search(queue, searchDepth, parents)
    }

    suspend fun _search(queue: ConcurrentLinkedQueue<String>, maxDepth: Int, parents: ConcurrentHashMap<String, String>): SearchPath? {
        if(maxDepth == 0){
            return null
        }

        for(link in queue){
            if(link == this.finalPage){
                val path = mutableListOf(this.finalPage)
                var currentLink = link
                while(parents[currentLink] != currentLink){
                    parents[currentLink]?.let { path.add(it) }
                    currentLink = parents[currentLink].toString()
                }
                return SearchPath(path.size, path.reversed())
            }
        }

        val queueNext: ConcurrentLinkedQueue<String> = ConcurrentLinkedQueue()
        val jobs: MutableList<Job> = mutableListOf()

        while(queue.isNotEmpty()){
            val parentLink = queue.poll()
            launch{
                val page = getHtmlDocument(parentLink)
                val list = extractReferences(page)
                list.forEach { childLink ->
                    if(parents.putIfAbsent(childLink, parentLink) == null) {
                        queueNext.add(childLink)
                    }
                }
            }.also{
                jobs.add(it)
            }
        }
        jobs.joinAll()
        return _search(queueNext, maxDepth - 1, parents)
    }

    companion object {
        const val KOTLIN_PAGE = "https://en.wikipedia.org/wiki/Kotlin_(programming_language)"
        const val NOT_FOUND = -1
    }
}

fun main(){
    val pageSearch = PageSearch()

}

