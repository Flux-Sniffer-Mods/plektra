package rocks.pkb.plektra.inputmethod.speech

import java.io.IOException
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import rocks.pkb.plektra.OfflineMode

/** In offline mode the speech clients refuse before anything is sent. */
class OfflineSpeechClientsTest {
    @Before
    fun offline() = OfflineMode.update(true)

    @After
    fun online() = OfflineMode.update(false)

    @Test
    fun theApiClientSendsNothing() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("{}"))
            val client = SpeechApiClient(openAiBase = server.url("/").toString(), openRouterBase = server.url("/").toString())

            assertTrue(runCatching { client.validateKey(SpeechProvider.OPENAI, "test-key") }.exceptionOrNull() is IOException)
            assertTrue(runCatching { client.models() }.exceptionOrNull() is IOException)
            val request = SpeechRequest(SpeechProvider.OPENAI, SpeechModel("test", "test"), "test-key", "en-US", 1)
            assertTrue(runCatching { client.transcribe(request, RecordedSpeech(ByteArray(6400))) }.exceptionOrNull() is IOException)

            assertEquals(0, server.requestCount)
        }
    }

    @Test
    fun theLiveClientOpensNoConnection() = runBlocking {
        MockWebServer().use { server ->
            val recorder = object : SpeechAudioRecorder {
                override suspend fun record(silenceMs: Long, onLevel: (Float, Long?) -> Unit): RecordedSpeech = error("not recording")
                override suspend fun recordStreaming(
                    silenceMs: Long,
                    onLevel: (Float, Long?) -> Unit,
                    onPcm: suspend (ByteArray) -> Unit
                ): RecordedSpeech = error("not recording")
                override fun finish() = Unit
            }
            val request = SpeechRequest(SpeechProvider.OPENAI, SpeechModels.openAiDefaults.first(), "test-key", "en-US", 1)

            val failure = runCatching {
                OpenAiLiveSpeechClient(url = server.url("/").toString()).transcribe(request, recorder, { _, _ -> }, {}, {})
            }.exceptionOrNull()

            assertTrue(failure is IOException)
            assertEquals(0, server.requestCount)
        }
    }
}
