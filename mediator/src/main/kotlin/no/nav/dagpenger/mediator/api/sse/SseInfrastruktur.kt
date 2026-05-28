package no.nav.dagpenger.mediator.api.sse

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.response.respond
import io.ktor.server.sse.SSEServerContent
import io.ktor.server.sse.ServerSSESession
import io.ktor.sse.ServerSentEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Semaphore
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

internal data class SseInnslag(
    val id: Long,
    val event: String,
    val data: String,
)

internal data class SseKilde(
    val defaultCursor: () -> Long,
    val hentInnslag: (cursor: Long) -> List<SseInnslag>,
)

internal suspend fun ApplicationCall.streamSse(
    initialCursor: Long,
    hentInnslag: (cursor: Long) -> List<SseInnslag>,
) {
    response.headers.append("Cache-Control", "no-store")
    response.headers.append("X-Accel-Buffering", "no")
    respond(
        SSEServerContent(this) {
            var cursor = initialCursor
            val klokke = SseStreamKlokke.start()
            while (coroutineContext.isActive) {
                if (klokke.harNåddMaksVarighet()) {
                    sendLukkForReautentisering()
                    break
                }

                val nyCursor = sendNyeInnslag(cursor, hentInnslag)
                if (nyCursor != cursor) {
                    cursor = nyCursor
                    klokke.registrerSendt()
                    continue
                }

                if (klokke.skalSendeHeartbeat()) {
                    sendHeartbeat()
                    klokke.registrerSendt()
                }
                delay(SSE_POLL_INTERVAL)
            }
        },
    )
}

internal suspend fun ApplicationCall.opprettSseStream(kilde: SseKilde) {
    medSseStreamSlot {
        streamSse(sseCursorEller(kilde.defaultCursor), kilde.hentInnslag)
    }
}

private suspend fun ServerSSESession.sendNyeInnslag(
    cursor: Long,
    hentInnslag: (cursor: Long) -> List<SseInnslag>,
): Long {
    var nyCursor = cursor
    hentInnslag(cursor).forEach { innslag ->
        nyCursor = innslag.id
        send(
            ServerSentEvent(
                data = innslag.data,
                event = innslag.event,
                id = innslag.id.toString(),
            ),
        )
    }
    return nyCursor
}

private suspend fun ServerSSESession.sendLukkForReautentisering() {
    send(ServerSentEvent(data = """{"reason":"reauth"}""", event = "close"))
}

private suspend fun ServerSSESession.sendHeartbeat() {
    send(ServerSentEvent(data = "{}", event = "ping"))
}

internal fun ApplicationCall.sseCursorEller(defaultCursor: () -> Long): Long {
    val råCursor = request.headers["Last-Event-ID"] ?: request.queryParameters["since"] ?: return defaultCursor()
    val cursor = råCursor.toLongOrNull() ?: throw BadRequestException("Ugyldig cursor i since/Last-Event-ID")
    if (cursor < 0) throw BadRequestException("Cursor må være 0 eller større")
    return cursor
}

internal suspend fun ApplicationCall.medSseStreamSlot(block: suspend () -> Unit) {
    if (!sseStreamLimiter.tryAcquire()) {
        respond(HttpStatusCode.TooManyRequests, "For mange samtidige SSE-strømmer")
        return
    }
    try {
        block()
    } finally {
        sseStreamLimiter.release()
    }
}

private val SSE_POLL_INTERVAL = 2.seconds
private val SSE_HEARTBEAT_INTERVAL = 20.seconds
private val SSE_MAKS_VARIGHET = 15.minutes
private val sseStreamLimiter = Semaphore(100)

private class SseStreamKlokke private constructor(
    private var sistSendt: TimeSource.Monotonic.ValueTimeMark,
    private val startet: TimeSource.Monotonic.ValueTimeMark,
) {
    fun harNåddMaksVarighet(): Boolean = startet.elapsedNow() >= SSE_MAKS_VARIGHET

    fun skalSendeHeartbeat(): Boolean = sistSendt.elapsedNow() >= SSE_HEARTBEAT_INTERVAL

    fun registrerSendt() {
        sistSendt = TimeSource.Monotonic.markNow()
    }

    companion object {
        fun start(): SseStreamKlokke {
            val nå = TimeSource.Monotonic.markNow()
            return SseStreamKlokke(sistSendt = nå, startet = nå)
        }
    }
}
