package jub.task1.game

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import com.github.ajalt.clikt.parameters.types.int
import jub.task1.game.PageSearch.Companion.KOTLIN_PAGE
import jub.task1.game.PageSearch.Companion.NOT_FOUND
import org.jsoup.Jsoup
import java.io.IOException

class WikiGameApp : CliktCommand() {
    private val finalPage: String by option().default(KOTLIN_PAGE).help("The final page url")
    private val optionalStartPage: String by option().prompt("Url of the start page").help("The person to greet")
    private val threadsCount: Int by option().int().prompt("Threads count").help("Count of threads")
    private val maxDepth: Int by option().int().prompt("Max depth").help("Max search depth")
    init {
        println("""
            Wikipedia race game.
            Rules are simple: define the final page (by default, it is a page about Kotlin language) using arguments,
            define the start page, thread count and maximum search depth - and this program will try to find final 
            page starting from the start page!
            
            If you don't want to pass start page, you may gust leave it blank.
        """.trimIndent())
    }

    override fun run() {
        val startPage = if (optionalStartPage == "") getRandomPage() else optionalStartPage
        println("Started from the page $startPage\n")
        val result = PageSearch(finalPage).search(startPage, maxDepth, threadsCount)
        if (result.steps == NOT_FOUND) printFailure()
        else printSuccess(result)
    }

    private fun getRandomPage(): String {
        val host = "https://en.wikipedia.org/wiki/"
        val defaultPage = "HTTP_404"
        val errorMessage = "Something wrong happened with random generator! For the start we chose page about HTTP 404."
        val title = try {
            val randomURL = "Special:Random"
            val doc = Jsoup.connect(host + randomURL).get()
            doc.selectFirst("h1#firstHeading")?.text() ?: defaultPage.also { println(errorMessage) }
        } catch (e: IOException) {
            println("$errorMessage Error: ${e.message}")
            host + defaultPage
        }
        return host + title.split(' ').joinToString("_")
    }

    private fun printSuccess(result: SearchPath) {
        println("Game ended! Here is the trace to the page:")
        for (page in result.path) {
            println(page)
        }
    }

    private fun printFailure() = println("The final page was not found :(")
}
