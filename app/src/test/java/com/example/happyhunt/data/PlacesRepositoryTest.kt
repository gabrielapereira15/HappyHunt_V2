package com.example.happyhunt.data

import com.example.happyhunt.domain.GeoPoint
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PlacesRepositoryTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val busy = MockWebServer()
    private val good = MockWebServer()
    private val center = GeoPoint(43.6465, -79.4197)
    private val answer = javaClass.getResource("/overpass-sample.json")!!.readText()
    private var now = 1_000_000_000L

    @Before
    fun setUp() {
        busy.start()
        good.start()
    }

    @After
    fun tearDown() {
        busy.close()
        good.close()
    }

    private fun repository(vararg servers: String) =
        PlacesRepository(Http.client(folder.root), folder.root, servers.toList(), now = { now })

    private fun gatewayTimeout() = MockResponse.Builder().code(504).body("<html>504 Gateway Time-out</html>").build()

    private fun ok(body: String = answer) = MockResponse.Builder().code(200).body(body).build()

    @Test
    fun `a busy server hands over to the next`() = runBlocking {
        busy.enqueue(gatewayTimeout())
        good.enqueue(ok())
        val nearby = repository(busy.url("/api/interpreter").toString(), good.url("/api/interpreter").toString()).around(center, 1000)

        assertEquals(5, nearby.places.size)
        assertFalse(nearby.stale)
        assertEquals(1, busy.requestCount)
        val request = good.takeRequest()
        assertEquals("POST", request.method)
        assertTrue(request.headers["User-Agent"]!!.startsWith("HappyHunt/"))
        assertTrue(request.body!!.utf8().startsWith("data="))
    }

    @Test
    fun `an error page with a 200 is not an answer`() = runBlocking {
        busy.enqueue(ok("<html>rate limited</html>"))
        good.enqueue(ok())
        val nearby = repository(busy.url("/").toString(), good.url("/").toString()).around(center, 1000)
        assertEquals(5, nearby.places.size)
    }

    @Test
    fun `the same search is answered from the cache, even after a restart`() = runBlocking {
        good.enqueue(ok())
        repository(good.url("/").toString()).around(center, 1000)
        now += 60 * 60 * 1000L
        val restarted = repository(good.url("/").toString())
        assertEquals(5, restarted.around(center, 1000).places.size)
        assertEquals(1, good.requestCount)
        // Places seen are known to the place screen.
        assertEquals("Tim Hortons", restarted.known("n54965507")?.name)
    }

    @Test
    fun `a different radius is a different search`() = runBlocking {
        good.enqueue(ok())
        good.enqueue(ok("""{"elements":[]}"""))
        val repository = repository(good.url("/").toString())
        repository.around(center, 1000)
        assertEquals(0, repository.around(center, 2000).places.size)
        assertEquals(2, good.requestCount)
    }

    @Test
    fun `an old search is better than nothing when the servers are down`() = runBlocking {
        good.enqueue(ok())
        val repository = repository(good.url("/").toString())
        repository.around(center, 1000)
        now += 3 * 24 * 60 * 60 * 1000L
        good.enqueue(gatewayTimeout())
        val stale = repository.around(center, 1000)
        assertTrue(stale.stale)
        assertEquals(5, stale.places.size)
    }

    @Test
    fun `busy servers and no old search`() = runBlocking {
        busy.enqueue(gatewayTimeout())
        busy.enqueue(gatewayTimeout())
        try {
            repository(busy.url("/a").toString(), busy.url("/b").toString()).around(center, 1000)
            fail("Expected PlacesUnavailable")
        } catch (e: PlacesUnavailable) {
            assertFalse(e.offline)
        }
    }

    @Test
    fun `no connection at all`() = runBlocking {
        val closed = MockWebServer().apply { start() }
        val url = closed.url("/").toString()
        closed.close()
        try {
            repository(url).around(center, 1000)
            fail("Expected PlacesUnavailable")
        } catch (e: PlacesUnavailable) {
            assertTrue(e.offline)
        }
    }
}
