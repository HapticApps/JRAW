package net.dean.jraw.test.util

import com.fasterxml.jackson.databind.JsonNode
import net.dean.jraw.RedditClient
import net.dean.jraw.http.HttpAdapter
import net.dean.jraw.http.HttpRequest
import net.dean.jraw.http.NetworkException
import net.dean.jraw.http.OkHttpAdapter
import net.dean.jraw.test.util.TestConfig.userAgent
import org.awaitility.Awaitility.await
import kotlin.reflect.KClass

fun httpAsync(http: HttpAdapter, r: HttpRequest.Builder, handle: (body: JsonNode) -> Unit) {
    var json: JsonNode? = null

    http.execute(r
        .success { json = it.json }
        .build())
    await().until({ json != null })
    handle(json!!)
}

fun <T : Exception> expectException(clazz: KClass<T>, doWork: () -> Unit) {
    val message = "Should have thrown ${clazz.qualifiedName}"
    try {
        doWork()
        throw IllegalStateException(message)
    } catch (e: Exception) {
        // Make sure rethrow the Exception we created here
        if (e.message == message) throw e
        // Make sure we got the right kind of Exception
        if (e::class != clazz)
            throw IllegalStateException("Expecting function to throw ${clazz.qualifiedName}, instead threw ${e::class.qualifiedName}", e)
    }
}

fun ensureAuthenticated(reddit: RedditClient) {
    try {
        // Make sure the request doesn't fail
        reddit.request {
            it.path("/hot")
        }
    } catch (e: NetworkException) {
        // Wrap the error to make sure the tester knows why the test failed
        if (e.res.code() == 401)
            throw IllegalStateException("Not authenticated, API responded with 401", e)

        // Something else went wrong
        throw e
    }
}

fun newOkHttpAdapter() = OkHttpAdapter(userAgent)
