package com.roinur.saucetracker.feature.desktopbridge

import com.roinur.saucetracker.data.database.SauceTrackerDatabase

import com.roinur.saucetracker.*

import android.content.Context
import android.net.Uri
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec
import java.math.BigInteger
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Base64
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import javax.security.auth.x500.X500Principal
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLServerSocket
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

data class DesktopBridgeState(
    val running: Boolean,
    val port: Int,
    val token: String,
    val baseUrl: String,
    val challengeCode: String
)

data class DesktopBridgeStartResult(
    val running: Boolean,
    val baseUrl: String,
    val message: String
)

class DesktopBridgeServer(
    private val appContext: Context,
    private val db: SauceTrackerDatabase,
    private val client: NhentaiApiClient,
    private val onDataChanged: () -> Unit = {},
    private val onScreenBlackoutChanged: (Boolean) -> Unit = {},
    private val onAccentModeChanged: (String) -> Unit = {},
    private val onChallengeCodeChanged: (String) -> Unit = {},
    private val currentAccentMode: () -> String = { "AUTO" }
) {
    companion object {
        private const val TLS_KEY_ALIAS = "sauce_tracker_desktop_bridge_tls"
        private const val TLS_CERTIFICATE_LIFETIME_MS = 20L * 365L * 24L * 60L * 60L * 1000L
    }

    private data class PendingCryptoSession(
        val remoteAddress: String,
        val keyPair: KeyPair,
        val createdAtMs: Long
    )

    private val lock = Any()
    private val running = AtomicBoolean(false)
    private val workerPool = Executors.newCachedThreadPool()
    private val unlockedClients = linkedSetOf<String>()
    private val pendingCryptoSessions = mutableMapOf<String, PendingCryptoSession>()
    private val cryptoKeysByClient = mutableMapOf<String, ByteArray>()
    private val tlsContext: SSLContext by lazy { createTlsContext() }

    @Volatile private var serverSocket: ServerSocket? = null
    @Volatile private var acceptThread: Thread? = null
    @Volatile private var port: Int = 0
    @Volatile private var token: String = ""
    @Volatile private var baseUrl: String = ""
    @Volatile private var challengeCode: String = ""
    @Volatile private var unlockStage: Int = 0
    @Volatile private var unlockFailures: Int = 0
    @Volatile private var unlockLockedUntilMs: Long = 0L
    @Volatile private var screenBlackoutEnabled: Boolean = false

    fun state(): DesktopBridgeState {
        return DesktopBridgeState(
            running = running.get(),
            port = port,
            token = token,
            baseUrl = baseUrl,
            challengeCode = challengeCode
        )
    }

    fun start(preferredPort: Int = 17366): DesktopBridgeStartResult {
        synchronized(lock) {
            if (running.get()) {
                return DesktopBridgeStartResult(true, baseUrl, "Desktop bridge already running.")
            }
            val socket = bindTlsServerSocket(preferredPort)
                ?: return DesktopBridgeStartResult(false, "", "Could not start Desktop bridge TLS server.")

            port = socket.localPort
            token = generateToken()
            unlockStage = 0
            unlockFailures = 0
            unlockLockedUntilMs = 0L
            challengeCode = generateChallengeCode()
            onChallengeCodeChanged.invoke(challengeCode)
            baseUrl = "https://${resolveLocalIpv4Address()}:$port/"
            screenBlackoutEnabled = false
            onScreenBlackoutChanged.invoke(false)
            unlockedClients.clear()
            pendingCryptoSessions.clear()
            cryptoKeysByClient.clear()
            serverSocket = socket
            running.set(true)

            acceptThread = Thread({ acceptLoop(socket) }, "SauceTrackerDesktopBridgeAccept").apply {
                isDaemon = true
                start()
            }
            return DesktopBridgeStartResult(true, baseUrl, "Desktop bridge running at $baseUrl")
        }
    }

    fun stop() {
        synchronized(lock) {
            running.set(false)
            runCatching { serverSocket?.close() }
            runCatching { acceptThread?.interrupt() }
            serverSocket = null
            acceptThread = null
            port = 0
            token = ""
            baseUrl = ""
            challengeCode = ""
            unlockStage = 0
            unlockFailures = 0
            unlockLockedUntilMs = 0L
            onChallengeCodeChanged.invoke("")
            screenBlackoutEnabled = false
            onScreenBlackoutChanged.invoke(false)
            unlockedClients.clear()
            pendingCryptoSessions.clear()
            cryptoKeysByClient.clear()
        }
    }

    private fun bindTlsServerSocket(preferredPort: Int): ServerSocket? {
        val start = preferredPort.coerceIn(1024, 65535)
        val candidates = buildList {
            for (offset in 0..12) add((start + offset).coerceAtMost(65535))
            add(0)
        }.distinct()
        candidates.forEach { candidate ->
            val socket = createTlsServerSocket(candidate)
            if (socket != null) return socket
        }
        return null
    }

    private fun createTlsServerSocket(port: Int): ServerSocket? {
        return runCatching {
            val socket = tlsContext.serverSocketFactory.createServerSocket() as SSLServerSocket
            socket.reuseAddress = true
            socket.enabledProtocols = socket.supportedProtocols.filter {
                it.equals("TLSv1.3", ignoreCase = true) || it.equals("TLSv1.2", ignoreCase = true)
            }.toTypedArray()
            socket.needClientAuth = false
            socket.bind(InetSocketAddress("0.0.0.0", port))
            socket
        }.getOrNull()
    }

    private fun createTlsContext(): SSLContext {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (!keyStore.containsAlias(TLS_KEY_ALIAS) || keyStore.getCertificate(TLS_KEY_ALIAS) == null) {
            if (keyStore.containsAlias(TLS_KEY_ALIAS)) {
                keyStore.deleteEntry(TLS_KEY_ALIAS)
            }
            val now = System.currentTimeMillis()
            val serialNumber = BigInteger(160, SecureRandom()).coerceAtLeast(BigInteger.ONE)
            KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore").apply {
                initialize(
                    KeyGenParameterSpec.Builder(
                        TLS_KEY_ALIAS,
                        KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
                    )
                        .setKeySize(2048)
                        .setDigests(KeyProperties.DIGEST_SHA256)
                        .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
                        .setCertificateSubject(X500Principal("CN=Sauce Tracker Desktop Bridge"))
                        .setCertificateSerialNumber(serialNumber)
                        .setCertificateNotBefore(Date(now - 60_000L))
                        .setCertificateNotAfter(Date(now + TLS_CERTIFICATE_LIFETIME_MS))
                        .build()
                )
                generateKeyPair()
            }
        }
        val keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        keyManagerFactory.init(keyStore, null)
        return SSLContext.getInstance("TLS").apply {
            init(keyManagerFactory.keyManagers, null, SecureRandom())
        }
    }

    private fun acceptLoop(socket: ServerSocket) {
        while (running.get()) {
            val clientSocket = runCatching { socket.accept() }.getOrNull() ?: continue
            workerPool.execute { handleClient(clientSocket) }
        }
    }

    private fun handleClient(socket: Socket) {
        socket.use { client ->
            runCatching {
                client.soTimeout = 15_000
                val remote = normalizeClientAddress(runCatching { client.inetAddress?.hostAddress.orEmpty() }.getOrDefault(""))
                val request = parseHttpRequest(client.getInputStream(), remote) ?: return@runCatching
                val response = routeRequest(request)
                writeHttpResponse(client, response)
            }
        }
    }

    private data class HttpRequest(
        val method: String,
        val path: String,
        val query: Map<String, String>,
        val headers: Map<String, String>,
        val body: String,
        val remoteAddress: String
    )

    private data class HttpResponse(
        val code: Int,
        val status: String,
        val contentType: String,
        val bodyBytes: ByteArray
    )

    private fun parseHttpRequest(input: InputStream, remoteAddress: String): HttpRequest? {
        val requestLine = readHttpLine(input)?.trim().orEmpty()
        if (requestLine.isBlank()) return null
        val parts = requestLine.split(" ")
        if (parts.size < 2) return null
        val method = parts[0].uppercase(Locale.US)
        val target = parts[1]

        val headers = linkedMapOf<String, String>()
        while (true) {
            val line = readHttpLine(input) ?: break
            if (line.isBlank()) break
            val idx = line.indexOf(':')
            if (idx <= 0) continue
            headers[line.substring(0, idx).trim().lowercase(Locale.US)] = line.substring(idx + 1).trim()
        }

        val contentLength = headers["content-length"]?.toIntOrNull()?.coerceAtLeast(0) ?: 0
        val bodyBytes = if (contentLength > 0) readExactBytes(input, contentLength) else ByteArray(0)
        val uri = runCatching { Uri.parse(target) }.getOrNull()
        val path = when {
            uri?.path.isNullOrBlank() && target.startsWith("/") -> target.substringBefore('?')
            else -> uri?.path
        }.orEmpty().ifBlank { "/" }
        val query = linkedMapOf<String, String>()
        uri?.queryParameterNames?.forEach { key -> query[key] = uri.getQueryParameter(key).orEmpty() }
        return HttpRequest(method, path, query, headers, bodyBytes.toString(Charsets.UTF_8), remoteAddress)
    }

    private fun readHttpLine(input: InputStream): String? {
        val out = ByteArrayOutputStream(128)
        var sawAny = false
        while (true) {
            val b = input.read()
            if (b == -1) return if (sawAny) out.toString(Charsets.UTF_8.name()) else null
            sawAny = true
            if (b == '\n'.code) break
            if (b != '\r'.code) out.write(b)
            if (out.size() > 8192) break
        }
        return out.toString(Charsets.UTF_8.name())
    }

    private fun readExactBytes(input: InputStream, len: Int): ByteArray {
        val out = ByteArray(len)
        var offset = 0
        while (offset < len) {
            val read = input.read(out, offset, len - offset)
            if (read <= 0) break
            offset += read
        }
        return if (offset == len) out else out.copyOf(offset)
    }

    private fun normalizeClientAddress(raw: String): String {
        val value = raw.trim().lowercase(Locale.US).substringBefore('%')
        return if (value.isBlank()) "unknown" else value
    }

    private fun isClientUnlocked(remoteAddress: String): Boolean {
        synchronized(lock) { return unlockedClients.contains(normalizeClientAddress(remoteAddress)) }
    }

    private fun setClientUnlocked(remoteAddress: String) {
        synchronized(lock) { unlockedClients += normalizeClientAddress(remoteAddress) }
    }

    private fun clearClientCrypto(remoteAddress: String) {
        val normalized = normalizeClientAddress(remoteAddress)
        synchronized(lock) {
            cryptoKeysByClient.remove(normalized)
            val stale = pendingCryptoSessions.filterValues { it.remoteAddress == normalized }.keys.toList()
            stale.forEach { pendingCryptoSessions.remove(it) }
        }
    }

    private fun clientCryptoKey(remoteAddress: String): ByteArray? {
        synchronized(lock) { return cryptoKeysByClient[normalizeClientAddress(remoteAddress)] }
    }

    private fun rotateChallengeCode(): String {
        val next = generateChallengeCode()
        challengeCode = next
        onChallengeCodeChanged.invoke(next)
        return next
    }

    private fun lockoutSecondsForFailureCount(failures: Int): Int {
        return when {
            failures <= 1 -> 0
            failures == 2 -> 10
            failures == 3 -> 60
            failures == 4 -> 300
            else -> 900
        }
    }

    private fun routeRequest(request: HttpRequest): HttpResponse {
        if (request.path == "/health") {
            return jsonResponse(200, JSONObject().put("ok", true).put("running", running.get()).put("port", port))
        }
        if (request.method == "GET" && (request.path == "/" || request.path == "/index.html")) {
            return htmlResponse(desktopHtml(token))
        }

        val providedToken = request.query["token"] ?: request.headers["x-sauce-token"].orEmpty()
        if (providedToken != token || token.isBlank()) {
            return jsonResponse(401, JSONObject().put("ok", false).put("error", "Unauthorized."))
        }

        if (request.method == "POST" && request.path == "/api/unlock") {
            val body = parseJson(request.body) ?: return badRequest("Invalid JSON body.")
            val candidate = body.optString("code", "").trim()
            if (candidate.isBlank()) return badRequest("Unlock code is required.")
            val now = System.currentTimeMillis()
            if (unlockLockedUntilMs > now) {
                val waitSeconds = ((unlockLockedUntilMs - now) + 999L) / 1000L
                return jsonResponse(
                    429,
                    JSONObject()
                        .put("ok", false)
                        .put("error", "Too many failed attempts. Try again in ${waitSeconds}s.")
                        .put("locked", true)
                        .put("wait_seconds", waitSeconds)
                )
            }
            if (candidate != challengeCode) {
                unlockStage = 0
                unlockFailures += 1
                val lockSeconds = lockoutSecondsForFailureCount(unlockFailures)
                if (lockSeconds > 0) {
                    unlockLockedUntilMs = now + lockSeconds * 1000L
                } else {
                    unlockLockedUntilMs = 0L
                }
                rotateChallengeCode()
                clearClientCrypto(request.remoteAddress)
                val waitSeconds = ((unlockLockedUntilMs - now).coerceAtLeast(0L) + 999L) / 1000L
                val error = if (waitSeconds > 0) {
                    "Incorrect code. Sequence reset. Locked for ${waitSeconds}s."
                } else {
                    "Incorrect code. Sequence reset."
                }
                return jsonResponse(
                    if (waitSeconds > 0) 429 else 403,
                    JSONObject()
                        .put("ok", false)
                        .put("error", error)
                        .put("locked", waitSeconds > 0)
                        .put("wait_seconds", waitSeconds)
                )
            }

            unlockStage += 1
            if (unlockStage < 3) {
                rotateChallengeCode()
                return jsonResponse(
                    200,
                    JSONObject()
                        .put("ok", true)
                        .put("unlocked", false)
                        .put("stage", unlockStage)
                        .put("total_rounds", 3)
                        .put("message", "Round $unlockStage/3 correct. Continue.")
                )
            }

            unlockStage = 0
            unlockFailures = 0
            unlockLockedUntilMs = 0L
            rotateChallengeCode()
            clearClientCrypto(request.remoteAddress)
            setClientUnlocked(request.remoteAddress)
            return jsonResponse(200, JSONObject().put("ok", true).put("unlocked", true).put("message", "Unlocked."))
        }

        if (request.method == "GET" && request.path == "/api/unlock-status") {
            val now = System.currentTimeMillis()
            val waitSeconds = if (unlockLockedUntilMs > now) ((unlockLockedUntilMs - now) + 999L) / 1000L else 0L
            if (challengeCode.isBlank()) {
                rotateChallengeCode()
            }
            val choices = JSONArray()
            if (waitSeconds == 0L) {
                buildUnlockChoices(challengeCode).forEach { choices.put(it) }
            }
            return jsonResponse(
                200,
                JSONObject()
                    .put("ok", true)
                    .put("unlocked", isClientUnlocked(request.remoteAddress))
                    .put("round", unlockStage + 1)
                    .put("total_rounds", 3)
                    .put("locked", waitSeconds > 0)
                    .put("wait_seconds", waitSeconds)
                    .put("choices", choices)
            )
        }

        if (request.path.startsWith("/api/") && !isClientUnlocked(request.remoteAddress)) {
            return jsonResponse(403, JSONObject().put("ok", false).put("error", "Bridge locked. Enter the on-device code."))
        }

        return when {
            request.method == "GET" && request.path == "/api/state" -> buildStateResponse(request.remoteAddress)
            request.method == "GET" && request.path == "/api/state-plain" -> buildStateResponsePlain()
            request.method == "POST" && request.path == "/api/crypto/start" -> startCryptoSession(request.remoteAddress)
            request.method == "POST" && request.path == "/api/crypto/finish" -> finishCryptoSession(request.remoteAddress, request.body)
            request.method == "POST" && request.path == "/api/entry/rating" -> updateRating(request.body)
            request.method == "POST" && request.path == "/api/entry/read" -> updateRead(request.body)
            request.method == "POST" && request.path == "/api/entry/pin" -> updatePin(request.body)
            request.method == "POST" && request.path == "/api/entry/delete" -> deleteEntry(request.body)
            request.method == "POST" && request.path == "/api/entry/add" -> addEntry(request.body)
            request.method == "POST" && request.path == "/api/device/screen-blackout" -> updateScreenBlackout(request.body)
            request.method == "POST" && request.path == "/api/settings/accent-mode" -> updateAccentMode(request.body)
            else -> jsonResponse(404, JSONObject().put("ok", false).put("error", "Not found."))
        }
    }

    private fun buildStateResponse(remoteAddress: String): HttpResponse {
        val encryptionKey = clientCryptoKey(remoteAddress)
            ?: return jsonResponse(428, JSONObject().put("ok", false).put("error", "Encrypted session required."))
        return encryptJsonResponse(200, buildStatePayload(), encryptionKey)
    }

    private fun buildStateResponsePlain(): HttpResponse {
        return jsonResponse(200, buildStatePayload().put("enc", false))
    }

    private fun buildStatePayload(): JSONObject {
        val stats = db.getSavedStats()
        val tags = db.listTagCounts("", TagSortField.COUNT, SortDirection.DESC)
        val creators = db.listCreators("", emptyList(), CreatorSortField.COUNT, SortDirection.DESC)
        val tagsArray = JSONArray()
        tags.forEach { row -> tagsArray.put(JSONObject().put("id", row.id).put("name", row.name).put("type", row.type).put("count", row.count)) }
        val creatorsArray = JSONArray()
        creators.forEach { row -> creatorsArray.put(JSONObject().put("id", row.id).put("name", row.name).put("type", row.type).put("entry_count", row.entryCount)) }
        return JSONObject()
            .put("ok", true)
            .put("generated_at", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.US)))
            .put("saved_stats", JSONObject().put("entries", stats.entries).put("artists", stats.artists).put("groups", stats.groups).put("read_entries", stats.readEntries))
            .put("tag_counts", tagsArray)
            .put("creators", creatorsArray)
            .put("bridge_screen_blackout", screenBlackoutEnabled)
            .put("bridge_accent_mode", currentAccentMode.invoke())
            .put("snapshot", db.exportSnapshot())
    }

    private fun startCryptoSession(remoteAddress: String): HttpResponse {
        val normalized = normalizeClientAddress(remoteAddress)
        val keyPair = runCatching {
            KeyPairGenerator.getInstance("EC").apply {
                initialize(ECGenParameterSpec("secp256r1"))
            }.generateKeyPair()
        }.getOrElse {
            return jsonResponse(500, JSONObject().put("ok", false).put("error", "Could not initialize crypto session."))
        }

        val sessionId = generateToken(24)
        synchronized(lock) {
            pendingCryptoSessions[sessionId] = PendingCryptoSession(
                remoteAddress = normalized,
                keyPair = keyPair,
                createdAtMs = System.currentTimeMillis()
            )
            cryptoKeysByClient.remove(normalized)
            val staleSessions = pendingCryptoSessions.filterValues {
                it.remoteAddress == normalized && it.createdAtMs < System.currentTimeMillis() - 120_000L
            }.keys.toList()
            staleSessions.forEach { pendingCryptoSessions.remove(it) }
        }

        return jsonResponse(
            200,
            JSONObject()
                .put("ok", true)
                .put("session_id", sessionId)
                .put("server_public", Base64.getEncoder().encodeToString(keyPair.public.encoded))
        )
    }

    private fun finishCryptoSession(remoteAddress: String, rawBody: String): HttpResponse {
        val body = parseJson(rawBody) ?: return badRequest("Invalid JSON body.")
        val sessionId = body.optString("session_id", "").trim()
        val clientPublicEncoded = body.optString("client_public", "").trim()
        if (sessionId.isBlank() || clientPublicEncoded.isBlank()) {
            return badRequest("session_id and client_public are required.")
        }

        val normalized = normalizeClientAddress(remoteAddress)
        val pending = synchronized(lock) { pendingCryptoSessions.remove(sessionId) }
            ?: return jsonResponse(403, JSONObject().put("ok", false).put("error", "Crypto session expired. Restart handshake."))
        if (pending.remoteAddress != normalized) {
            return jsonResponse(403, JSONObject().put("ok", false).put("error", "Crypto session does not match this client."))
        }

        val clientPublicKey = runCatching {
            val publicBytes = Base64.getDecoder().decode(clientPublicEncoded)
            KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(publicBytes))
        }.getOrElse {
            return jsonResponse(400, JSONObject().put("ok", false).put("error", "Invalid client public key."))
        }

        val sharedSecret = runCatching {
            KeyAgreement.getInstance("ECDH").apply {
                init(pending.keyPair.private)
                doPhase(clientPublicKey, true)
            }.generateSecret()
        }.getOrElse {
            return jsonResponse(500, JSONObject().put("ok", false).put("error", "Could not derive shared key."))
        }

        val tokenBytes = token.toByteArray(Charsets.UTF_8)
        val mixed = ByteArray(sharedSecret.size + tokenBytes.size).apply {
            System.arraycopy(sharedSecret, 0, this, 0, sharedSecret.size)
            System.arraycopy(tokenBytes, 0, this, sharedSecret.size, tokenBytes.size)
        }
        val aesKey = MessageDigest.getInstance("SHA-256").digest(mixed)
        synchronized(lock) {
            cryptoKeysByClient[normalized] = aesKey
        }
        return jsonResponse(200, JSONObject().put("ok", true).put("message", "Encrypted session ready."))
    }

    private fun updateScreenBlackout(rawBody: String): HttpResponse {
        val body = parseJson(rawBody) ?: return badRequest("Invalid JSON body.")
        val enabled = body.optBoolean("enabled", false)
        if (screenBlackoutEnabled == enabled) return jsonResponse(200, JSONObject().put("ok", true).put("enabled", enabled))
        screenBlackoutEnabled = enabled
        onScreenBlackoutChanged.invoke(enabled)
        return jsonResponse(200, JSONObject().put("ok", true).put("enabled", enabled))
    }

    private fun updateAccentMode(rawBody: String): HttpResponse {
        val body = parseJson(rawBody) ?: return badRequest("Invalid JSON body.")
        val mode = body.optString("mode", "").trim().ifBlank { body.optString("accent_mode", "").trim() }
        if (mode.isBlank()) return badRequest("Accent mode is required.")
        onAccentModeChanged.invoke(mode)
        return jsonResponse(200, JSONObject().put("ok", true).put("accent_mode", currentAccentMode.invoke()))
    }

    private fun updateRating(rawBody: String): HttpResponse {
        val body = parseJson(rawBody) ?: return badRequest("Invalid JSON body.")
        val code = body.optInt("code", 0)
        val rating = body.optInt("rating", 0).coerceIn(0, 5)
        if (code <= 0) return badRequest("Invalid code.")
        db.setEntryRating(code, rating)
        db.setEntryRead(code, true)
        onDataChanged.invoke()
        return jsonResponse(200, JSONObject().put("ok", true).put("message", "Set rating for $code to $rating."))
    }

    private fun updateRead(rawBody: String): HttpResponse {
        val body = parseJson(rawBody) ?: return badRequest("Invalid JSON body.")
        val code = body.optInt("code", 0)
        val read = body.optBoolean("read", false)
        if (code <= 0) return badRequest("Invalid code.")
        db.setEntryRead(code, read)
        onDataChanged.invoke()
        return jsonResponse(200, JSONObject().put("ok", true).put("message", if (read) "Marked $code read." else "Marked $code unread."))
    }

    private fun updatePin(rawBody: String): HttpResponse {
        val body = parseJson(rawBody) ?: return badRequest("Invalid JSON body.")
        val code = body.optInt("code", 0)
        val pinned = body.optBoolean("pinned", false)
        if (code <= 0) return badRequest("Invalid code.")
        db.setEntryPinned(code, pinned)
        onDataChanged.invoke()
        return jsonResponse(200, JSONObject().put("ok", true).put("message", if (pinned) "Pinned $code." else "Unpinned $code."))
    }

    private fun deleteEntry(rawBody: String): HttpResponse {
        val body = parseJson(rawBody) ?: return badRequest("Invalid JSON body.")
        val code = body.optInt("code", 0)
        if (code <= 0) return badRequest("Invalid code.")
        db.deleteEntry(code)
        onDataChanged.invoke()
        return jsonResponse(200, JSONObject().put("ok", true).put("message", "Deleted $code."))
    }

    private fun addEntry(rawBody: String): HttpResponse {
        val body = parseJson(rawBody) ?: return badRequest("Invalid JSON body.")
        val code = body.optInt("code", 0)
        if (code <= 0) return badRequest("Invalid code.")
        val gallery = runCatching { client.fetchGallery(code) }.getOrElse { exc ->
            return jsonResponse(404, JSONObject().put("ok", false).put("error", exc.message ?: "Could not fetch code $code."))
        }
        db.upsertGallery(gallery)
        onDataChanged.invoke()
        return jsonResponse(200, JSONObject().put("ok", true).put("message", "Saved/updated $code."))
    }

    private fun parseJson(raw: String): JSONObject? {
        val cleaned = raw.trim()
        if (cleaned.isBlank()) return null
        return runCatching { JSONObject(cleaned) }.getOrNull()
    }

    private fun badRequest(message: String): HttpResponse {
        return jsonResponse(400, JSONObject().put("ok", false).put("error", message))
    }

    private fun jsonResponse(code: Int, payload: JSONObject): HttpResponse {
        return HttpResponse(code, httpStatusText(code), "application/json; charset=utf-8", payload.toString().toByteArray(Charsets.UTF_8))
    }

    private fun encryptJsonResponse(code: Int, payload: JSONObject, keyBytes: ByteArray): HttpResponse {
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val encrypted = runCatching {
            Cipher.getInstance("AES/GCM/NoPadding").apply {
                init(Cipher.ENCRYPT_MODE, SecretKeySpec(keyBytes, "AES"), GCMParameterSpec(128, iv))
            }.doFinal(payload.toString().toByteArray(Charsets.UTF_8))
        }.getOrElse {
            return jsonResponse(500, JSONObject().put("ok", false).put("error", "Could not encrypt response."))
        }
        val wrapper = JSONObject()
            .put("ok", true)
            .put("enc", true)
            .put("iv", Base64.getEncoder().encodeToString(iv))
            .put("ct", Base64.getEncoder().encodeToString(encrypted))
        return jsonResponse(code, wrapper)
    }

    private fun htmlResponse(html: String): HttpResponse {
        return HttpResponse(200, "OK", "text/html; charset=utf-8", html.toByteArray(Charsets.UTF_8))
    }

    private fun writeHttpResponse(socket: Socket, response: HttpResponse) {
        val out = socket.getOutputStream()
        val header = buildString {
            append("HTTP/1.1 ${response.code} ${response.status}\r\n")
            append("Content-Type: ${response.contentType}\r\n")
            append("Content-Length: ${response.bodyBytes.size}\r\n")
            append("Connection: close\r\n")
            append("Cache-Control: no-store\r\n")
            append("\r\n")
        }.toByteArray(Charsets.UTF_8)
        out.write(header)
        out.write(response.bodyBytes)
        out.flush()
    }

    private fun resolveLocalIpv4Address(): String {
        return runCatching {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return@runCatching null
            while (interfaces.hasMoreElements()) {
                val netIf = interfaces.nextElement() ?: continue
                if (!netIf.isUp || netIf.isLoopback || netIf.isVirtual) continue
                val addresses = netIf.inetAddresses ?: continue
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement() ?: continue
                    if (addr is Inet4Address) {
                        val host = addr.hostAddress.orEmpty()
                        if (host.isNotBlank() && host != "127.0.0.1") return@runCatching host
                    }
                }
            }
            null
        }.getOrNull() ?: "127.0.0.1"
    }

    private fun generateToken(length: Int = 32): String {
        val alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val random = SecureRandom()
        return buildString { repeat(length.coerceIn(12, 64)) { append(alphabet[random.nextInt(alphabet.length)]) } }
    }

    private fun generateChallengeCode(): String = (SecureRandom().nextInt(90) + 10).toString()

    private fun buildUnlockChoices(correctCode: String): List<String> {
        val random = SecureRandom()
        val set = linkedSetOf(correctCode)
        while (set.size < 3) set += (random.nextInt(90) + 10).toString()
        return set.shuffled(random)
    }

    private fun httpStatusText(code: Int): String {
        return when (code) {
            200 -> "OK"
            400 -> "Bad Request"
            401 -> "Unauthorized"
            403 -> "Forbidden"
            404 -> "Not Found"
            428 -> "Precondition Required"
            429 -> "Too Many Requests"
            500 -> "Internal Server Error"
            else -> "Error"
        }
    }

    private fun desktopHtml(activeToken: String): String {
        val safeToken = activeToken
            .replace("\\", "\\\\")
            .replace("'", "\\'")
        return DESKTOP_BRIDGE_HTML.replace("__SAUCE_TOKEN__", safeToken)
    }
}

private val DESKTOP_BRIDGE_HTML = """
<!doctype html>
<html>
<head>
<meta charset="utf-8"/>
<meta name="viewport" content="width=device-width,initial-scale=1"/>
<title>Sauce Tracker Desktop Bridge</title>
<link rel="icon" type="image/jpeg" href="data:image/jpeg;base64,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"/>
<style>
:root{--bg:#121418;--surface:#1b1f24;--surface2:#242a31;--line:#3a434f;--text:#e9edf3;--muted:#a8b1bf;--accent:#5e96ff;--accentInk:#0f1e3d;--green:#22c55e;--red:#ef4444}
body.light{--bg:#f5f8ff;--surface:#fff;--surface2:#f1f5ff;--line:#d2dbf1;--text:#1b2538;--muted:#5e6f90;--accent:#2f6be5;--accentInk:#fff}
*{box-sizing:border-box}body{margin:0;background:var(--bg);color:var(--text);font-family:Segoe UI,system-ui}.wrap{max-width:1680px;margin:0 auto;padding:12px;display:flex;flex-direction:column;gap:10px}
.card{border:1px solid var(--line);border-radius:12px;background:var(--surface);padding:10px}.row{display:flex;flex-wrap:wrap;gap:8px;align-items:center}.muted{color:var(--muted);font-size:12px}
input,button{border:1px solid var(--line);border-radius:10px;background:var(--surface2);color:var(--text);font-size:13px;padding:7px 9px}button{cursor:pointer}
.accent{background:var(--accent);color:var(--accentInk);border-color:var(--accent);font-weight:700}.entries{display:grid;gap:10px;align-items:stretch}
.entry{position:relative;border:1px solid var(--line);border-radius:10px;background:var(--surface2);padding:9px;display:flex;flex-direction:column;gap:8px;height:100%}
.pinCorner{position:absolute;top:8px;right:8px;width:28px;height:28px;border-radius:999px;padding:0;color:var(--muted);display:flex;align-items:center;justify-content:center}
.pinCorner .pinGlyph{position:relative;display:block;width:12px;height:12px;border:2px solid currentColor;border-radius:999px}
.pinCorner .pinGlyph::after{content:"";position:absolute;left:50%;top:9px;width:2px;height:9px;transform:translateX(-50%);background:currentColor;border-radius:2px}
.pinCorner.on{border-color:var(--accent);color:var(--accent);background:color-mix(in srgb,var(--accent) 12%,transparent)}
.entryHead{display:block;padding-right:36px;min-height:40px}
.title{font-size:14px;font-weight:700;line-height:1.25;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden;min-height:2.5em}
.codeInline{border:none;background:transparent;color:var(--accent);font-weight:700;font-size:inherit;padding:0;display:inline;cursor:pointer}
.thumb{width:100%;aspect-ratio:1.56;border:1px solid var(--line);border-radius:9px;overflow:hidden;background:#0b0d12;position:relative;cursor:pointer}.thumb img{width:100%;height:100%;object-fit:cover;display:block}
.thumbEmpty{position:absolute;inset:0;display:flex;align-items:center;justify-content:center;color:var(--muted);font-size:12px}.meta{color:var(--muted);font-size:12px;line-height:1.35}
.sectionLabel{font-size:12px;color:var(--muted);font-weight:700}.chipList{display:flex;flex-wrap:wrap;gap:6px}
.chip{border:1px solid var(--line);border-radius:999px;background:var(--surface2);color:var(--text);font-size:12px;padding:4px 8px;display:inline-flex;gap:6px;align-items:center;white-space:nowrap}
.chip.on{background:var(--accent);border-color:var(--accent);color:var(--accentInk);font-weight:700}.chip.count span:last-child{opacity:.82;font-size:11px;font-weight:700}
.creatorChip{border-color:color-mix(in srgb, var(--accent) 40%, var(--line));color:color-mix(in srgb, var(--accent) 72%, var(--text))}.creatorChip.on{background:var(--accent);border-color:var(--accent);color:var(--accentInk)}
.sortRow{margin-top:8px;display:flex;flex-wrap:wrap;gap:6px}.sortBtn.desc{border-color:var(--green);color:var(--green);font-weight:700}.sortBtn.asc{border-color:var(--red);color:var(--red);font-weight:700}
.readState{border-color:var(--green);color:var(--green);font-weight:700}.unreadState{border-color:var(--red);color:var(--red);font-weight:700}
.readFilterGroup{display:inline-flex;align-items:center;gap:6px;margin-left:12px;padding-left:12px;border-left:1px solid color-mix(in srgb,var(--line) 80%,transparent)}
.readFilterBtn{border-color:var(--line)}
.readFilterBtn.onAll{border-color:var(--accent);color:var(--accent);font-weight:700}
.readFilterBtn.onRead{border-color:var(--green);color:var(--green);font-weight:700}
.readFilterBtn.onUnread{border-color:var(--red);color:var(--red);font-weight:700}
.pinPriorityOn{border-color:var(--green);color:var(--green);font-weight:700;background:color-mix(in srgb,var(--green) 18%, var(--surface2))}
.stars{display:inline-flex;gap:2px}.star{border:none;background:transparent;color:var(--muted);width:20px;height:20px;padding:0;font-size:18px;line-height:1}.star.on{color:var(--accent)}
.summary{margin-top:8px;min-height:28px;display:flex;flex-wrap:wrap;gap:6px}.locked{filter:blur(8px);pointer-events:none;user-select:none}
.overlay{position:fixed;inset:0;background:rgba(3,6,12,.56);display:flex;align-items:center;justify-content:center;padding:14px;z-index:80}.overlayCard{width:min(460px,100%)}
.choice{margin-top:10px;display:grid;grid-template-columns:repeat(3,1fr);gap:8px}.choice button{padding:12px 8px;font-size:20px;font-weight:800}
.hidden{display:none !important}.backdrop{position:fixed;inset:0;background:rgba(0,0,0,.2);z-index:50}.panel{position:fixed;z-index:60;max-height:calc(100vh - 140px);overflow:auto}
.accentTools{display:flex;gap:6px;align-items:center}.accentDots{display:flex;gap:6px;flex-wrap:wrap}.dot{width:18px;height:18px;border-radius:999px;border:1px solid var(--line);padding:0;cursor:pointer}.dot.active{outline:2px solid var(--accent);outline-offset:1px}
.rangeAccent{-webkit-appearance:none;appearance:none;min-width:190px;height:8px;padding:0;border:none;border-radius:999px;background:linear-gradient(to right,var(--accent) 0%,var(--accent) var(--rangePct,50%),color-mix(in srgb, var(--line) 84%, var(--bg)) var(--rangePct,50%),color-mix(in srgb, var(--line) 84%, var(--bg)) 100%)}
.rangeAccent:focus{outline:none}
.rangeAccent::-webkit-slider-runnable-track{height:8px;background:transparent;border-radius:999px}
.rangeAccent::-moz-range-track{height:8px;background:transparent;border-radius:999px}
.rangeAccent::-webkit-slider-thumb{-webkit-appearance:none;appearance:none;margin-top:-4px;width:16px;height:16px;border-radius:999px;border:2px solid var(--accentInk);background:var(--accent)}
.rangeAccent::-moz-range-thumb{width:16px;height:16px;border-radius:999px;border:2px solid var(--accentInk);background:var(--accent)}
#imageOverlay{position:fixed;inset:0;z-index:95;background:rgba(0,0,0,.9);display:flex;align-items:center;justify-content:center;padding:16px}#imageOverlay img{max-width:95vw;max-height:95vh;object-fit:contain;border-radius:12px;border:1px solid #465065}
</style>
</head>
<body>
<div id="unlockOverlay" class="overlay"><div class="card overlayCard"><div style="font-size:20px;font-weight:800">Verify Desktop Access</div><div class="muted" style="margin-top:6px">Which number do you see on your device?</div><div id="unlockChoices" class="choice"></div><div id="unlockStatus" class="muted" style="margin-top:8px"></div></div></div>
<div id="imageOverlay" class="hidden"><img id="imageFull" alt="Preview"/></div>
<div id="panelBackdrop" class="backdrop hidden"></div>
<div id="root" class="locked"><div class="wrap"><div class="card"><div style="font-size:22px;font-weight:800">Sauce Tracker Desktop Bridge</div><div class="row" style="margin-top:8px"><button id="refreshBtn">Refresh</button><input id="searchInput" placeholder="Search everything..." style="min-width:260px;flex:1"/><input id="addCodeInput" placeholder="Code" style="width:120px"/><button id="addCodeBtn" class="accent">Add/Update</button><button id="resetBtn">Reset Filters</button><button id="tagsBtn">Tags</button><button id="creatorsBtn">Artists/Groups</button><button id="themeBtn">Theme: Auto</button><div class="accentTools"><button id="accentBtn" class="accent">Accent color</button><div id="accentWrap" class="accentDots hidden"></div></div><button id="screenBtn">Screen: On</button></div><div class="row" style="margin-top:8px"><span class="muted">Columns</span><input id="cols" class="rangeAccent" type="range" min="1" max="6" step="1" value="3"/><span id="colsVal" class="muted">3</span></div><div id="stats" class="muted" style="margin-top:8px"></div><div id="syncMode" class="muted" style="margin-top:4px">Mode: Locked</div><div id="status" class="muted" style="margin-top:4px"></div><div id="summary" class="summary"></div></div><div class="card"><div style="font-weight:700">Entries</div><div id="entrySort" class="sortRow"></div><div id="entries" class="entries" style="margin-top:8px"></div></div></div></div>
<div id="tagsPanel" class="card panel hidden"><div style="font-weight:700">Tag Filter</div><div id="tagSort" class="sortRow"></div><div class="muted" style="margin-top:8px">Selected tags</div><div id="activeTags" class="chipList" style="margin-top:8px"></div><div class="muted" style="margin-top:8px">All tags</div><div id="tagsList" class="chipList" style="margin-top:8px"></div></div>
<div id="creatorsPanel" class="card panel hidden"><div style="font-weight:700">Artists / Groups</div><div id="creatorSort" class="sortRow"></div><div class="muted" style="margin-top:8px">Selected creators</div><div id="activeCreators" class="chipList" style="margin-top:8px"></div><div class="muted" style="margin-top:8px">All creators</div><div id="creatorsList" class="chipList" style="margin-top:8px"></div></div>
<script>
(function(){
const token="__SAUCE_TOKEN__";
const STAR=String.fromCodePoint(9733),ARROW_DOWN=String.fromCodePoint(9660),ARROW_UP=String.fromCodePoint(9650);
let data=null,unlocked=false,panelOpen="",themeMode="auto",accentMode="auto",ratingSortOn=false,pinPriorityEnabled=true,entryReadFilter="all",screenBlackout=false,cryptoReady=false,cryptoKey=null,unlockPollTimer=null,imageOpenCode=0;
const cryptoSupported=!!(window.crypto&&window.crypto.subtle);
let entrySort={f:"added",d:"desc"},tagSort={f:"count",d:"desc"},creatorSort={f:"count",d:"desc"};
const activeTags=new Set(),activeCreators=new Set();
const byId=(id)=>document.getElementById(id);
const ui={unlockOverlay:byId("unlockOverlay"),unlockChoices:byId("unlockChoices"),unlockStatus:byId("unlockStatus"),root:byId("root"),panelBackdrop:byId("panelBackdrop"),tagsPanel:byId("tagsPanel"),creatorsPanel:byId("creatorsPanel"),refreshBtn:byId("refreshBtn"),searchInput:byId("searchInput"),addCodeInput:byId("addCodeInput"),addCodeBtn:byId("addCodeBtn"),resetBtn:byId("resetBtn"),tagsBtn:byId("tagsBtn"),creatorsBtn:byId("creatorsBtn"),themeBtn:byId("themeBtn"),screenBtn:byId("screenBtn"),cols:byId("cols"),colsVal:byId("colsVal"),accentBtn:byId("accentBtn"),accentWrap:byId("accentWrap"),stats:byId("stats"),syncMode:byId("syncMode"),status:byId("status"),summary:byId("summary"),entrySort:byId("entrySort"),entries:byId("entries"),tagSort:byId("tagSort"),creatorSort:byId("creatorSort"),activeTags:byId("activeTags"),tagsList:byId("tagsList"),activeCreators:byId("activeCreators"),creatorsList:byId("creatorsList"),imageOverlay:byId("imageOverlay"),imageFull:byId("imageFull")};
const accentOptions=[{key:"auto",value:""},{key:"red",value:"#ef4444"},{key:"orange",value:"#f97316"},{key:"amber",value:"#f59e0b"},{key:"green",value:"#22c55e"},{key:"teal",value:"#14b8a6"},{key:"blue",value:"#3b82f6"},{key:"indigo",value:"#6366f1"},{key:"pink",value:"#ec4899"}];
const nm=(v)=>(v||"").toString().trim().toLowerCase(),ntag=(v)=>nm(v).replace(/\s+/g," "),creatorKey=(t,n)=>nm(t)+"|"+ntag(n),num=(v)=>{const x=Number(v||0);return Number.isFinite(x)?x:0},dateMs=(v)=>{const x=Date.parse((v||"").toString().replace(" ","T"));return Number.isFinite(x)?x:0},formatCount=(v)=>{const x=num(v);return x>=1000?(x/1000).toFixed(x>=10000?0:1).replace(/\.0$/,"")+"k":String(x)};
const clear=(n)=>{while(n.firstChild)n.removeChild(n.firstChild)};const setStatus=(m)=>{ui.status.textContent=m||""};const setMode=(m)=>{ui.syncMode.textContent=m||"Mode: Unknown"};const setUnlockStatus=(m)=>{ui.unlockStatus.textContent=m||""};
const api=(p)=>p+(p.includes("?")?"&":"?")+"token="+encodeURIComponent(token);
const b64ToBytes=(v)=>Uint8Array.from(atob(v||""), (ch)=>ch.charCodeAt(0));
const bytesToB64=(value)=>{const bytes=value instanceof Uint8Array?value:new Uint8Array(value);let bin="";for(let i=0;i<bytes.length;i+=1)bin+=String.fromCharCode(bytes[i]);return btoa(bin)};
const clearUnlockTimer=()=>{if(unlockPollTimer){clearTimeout(unlockPollTimer);unlockPollTimer=null}};
async function decryptEnvelope(wrapper){if(!cryptoKey)throw new Error("Encrypted session not ready.");const iv=b64ToBytes(wrapper.iv);const ct=b64ToBytes(wrapper.ct);const plain=await crypto.subtle.decrypt({name:"AES-GCM",iv},cryptoKey,ct);const txt=new TextDecoder().decode(plain);return JSON.parse(txt)}
async function fetchJson(p,o){const response=await fetch(api(p),o||{cache:"no-store"});let payload;try{payload=await response.json()}catch(_){payload={ok:false,error:"Invalid response"}}if(payload&&payload.enc){payload=await decryptEnvelope(payload)}if(!response.ok||!payload.ok)throw new Error((payload&&payload.error)||("HTTP "+response.status));return payload}
const postJson=(p,b)=>fetchJson(p,{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify(b||{})});
async function ensureCryptoSession(){if(!cryptoSupported)return;if(cryptoReady&&cryptoKey)return;const start=await postJson("/api/crypto/start",{});const serverPub=await crypto.subtle.importKey("spki",b64ToBytes(start.server_public),{name:"ECDH",namedCurve:"P-256"},false,[]);const pair=await crypto.subtle.generateKey({name:"ECDH",namedCurve:"P-256"},true,["deriveBits"]);const shared=await crypto.subtle.deriveBits({name:"ECDH",public:serverPub},pair.privateKey,256);const exportedClientPub=await crypto.subtle.exportKey("spki",pair.publicKey);await postJson("/api/crypto/finish",{session_id:start.session_id,client_public:bytesToB64(exportedClientPub)});const tokenBytes=new TextEncoder().encode(token);const sharedBytes=new Uint8Array(shared);const mixed=new Uint8Array(sharedBytes.length+tokenBytes.length);mixed.set(sharedBytes,0);mixed.set(tokenBytes,sharedBytes.length);const digest=await crypto.subtle.digest("SHA-256",mixed);cryptoKey=await crypto.subtle.importKey("raw",digest,{name:"AES-GCM"},false,["decrypt"]);cryptoReady=true}
const copyText=(v)=>{const t=String(v||"");if(!t)return;if(navigator.clipboard&&navigator.clipboard.writeText)navigator.clipboard.writeText(t).then(()=>setStatus("Copied "+t+".")).catch(()=>setStatus("Copy failed."))};
const openImage=(u,code)=>{if(!nm(u))return;imageOpenCode=num(code);ui.imageFull.src=u;ui.imageOverlay.classList.remove("hidden")};
const openImagePageOne=()=>{if(imageOpenCode<=0)return closeImage();window.open("https://nhentai.net/g/"+imageOpenCode+"/1/","_blank");closeImage()};
const closeImage=()=>{imageOpenCode=0;ui.imageFull.src="";ui.imageOverlay.classList.add("hidden")};
const normalizeAccent=(v)=>accentOptions.some((o)=>o.key===nm(v))?nm(v):"auto";
const normalizeReadFilter=(v)=>{const key=nm(v);return key==="read"||key==="unread"?"read"===key?"read":"unread":"all"};
const themeLabel=(m)=>m==="dark"?"Dark":(m==="light"?"Light":"Auto");
function applyTheme(mode,persist=true){themeMode=mode;let effective=mode;if(mode==="auto")effective=window.matchMedia&&window.matchMedia("(prefers-color-scheme: light)").matches?"light":"dark";document.body.classList.toggle("light",effective==="light");ui.themeBtn.textContent="Theme: "+themeLabel(mode);if(persist)localStorage.setItem("stb_theme_mode",mode)}
function buildAccentDots(){clear(ui.accentWrap);accentOptions.forEach((o)=>{const d=document.createElement("button");d.type="button";d.className="dot"+(accentMode===o.key?" active":"");d.title=o.key;d.style.background=o.value||"linear-gradient(135deg, transparent 48%, var(--line) 48%, var(--line) 52%, transparent 52%)";d.addEventListener("click",()=>applyAccent(o.key,true,true));ui.accentWrap.appendChild(d)})}
function applyAccent(mode,persist=true,push=false){const key=normalizeAccent(mode);accentMode=key;const style=document.documentElement.style;const p=accentOptions.find((o)=>o.key===key)||accentOptions[0];if(p.value)style.setProperty("--accent",p.value);else style.removeProperty("--accent");if(persist)localStorage.setItem("stb_accent_mode",key);buildAccentDots();if(push)postJson("/api/settings/accent-mode",{mode:key.toUpperCase()}).catch(()=>{})}
function applyColumns(v){const c=Math.max(1,Math.min(6,num(v)||3));ui.cols.value=String(c);ui.colsVal.textContent=String(c);ui.entries.style.gridTemplateColumns="repeat("+c+", minmax(220px,1fr))";const min=num(ui.cols.min||1),max=num(ui.cols.max||6);const pct=max>min?((c-min)*100/(max-min)):100;ui.cols.style.setProperty("--rangePct",pct.toFixed(2)+"%");localStorage.setItem("stb_columns",String(c))}
const rowsEntries=()=>data&&data.snapshot&&Array.isArray(data.snapshot.entries)?data.snapshot.entries.slice():[];const rowsTags=()=>data&&Array.isArray(data.tag_counts)?data.tag_counts.slice():[];const rowsCreators=()=>data&&Array.isArray(data.creators)?data.creators.slice():[];
const thumbnailUrl=(e)=>{if(nm(e.thumbnail_url))return e.thumbnail_url;const m=num(e.media_id);return m>0?("https://t.nhentai.net/galleries/"+m+"/cover."+(nm(e.cover_ext)||"jpg")):""};
function sortRows(rows,state,countField){const dir=state.d==="asc"?1:-1;rows.sort((a,b)=>{let cmp=0;if(state.f==="name")cmp=nm(a.name).localeCompare(nm(b.name));else if(state.f==="type")cmp=nm(a.type).localeCompare(nm(b.type));else cmp=num(a[countField])-num(b[countField]);if(!cmp)cmp=nm(a.name).localeCompare(nm(b.name));return cmp*dir});return rows}
function filterEntries(){const rawQ=nm(ui.searchInput.value);const queryVariants=[rawQ];if(rawQ.startsWith("#"))queryVariants.push(rawQ.substring(1));return rowsEntries().filter((e)=>{const tags=Array.isArray(e.tags)?e.tags:[];const names=tags.map((t)=>ntag(t.name));for(const t of activeTags)if(!names.includes(t))return false;if(activeCreators.size>0){const keys=tags.filter((t)=>{const ty=nm(t.type);return ty==="artist"||ty==="group"}).map((t)=>creatorKey(t.type,t.name));for(const c of activeCreators)if(!keys.includes(c))return false}const readState=num(e.read)!==0;if(entryReadFilter==="read"&&!readState)return false;if(entryReadFilter==="unread"&&readState)return false;if(!rawQ)return true;const fields=[e.code,"#"+num(e.code),e.title,e.subtitle,e.upload_date,e.fetched_at,e.added_at].concat(tags.map((t)=>t.name)).concat(tags.map((t)=>t.type)).map(nm);return queryVariants.some((q)=>q&&fields.some((f)=>f.includes(q)))})}
function sortEntries(rows){const dir=entrySort.d==="asc"?1:-1;rows.sort((a,b)=>{if(pinPriorityEnabled){const pinCmp=num(b.pinned)-num(a.pinned);if(pinCmp)return pinCmp}if(ratingSortOn){const rc=num(b.rating)-num(a.rating);if(rc)return rc}let cmp=0;if(entrySort.f==="title")cmp=nm(a.title).localeCompare(nm(b.title));else if(entrySort.f==="pages")cmp=num(a.num_pages)-num(b.num_pages);else if(entrySort.f==="upload")cmp=dateMs(a.upload_date)-dateMs(b.upload_date);else cmp=dateMs(a.added_at)-dateMs(b.added_at);if(!cmp)cmp=num(a.code)-num(b.code);return cmp*dir});return rows}
const button=(label,klass,onClick)=>{const n=document.createElement("button");n.type="button";n.textContent=label;if(klass)n.className=klass;n.addEventListener("click",onClick);return n};
const chip=(label,count,selected,onClick,extra)=>{let k=selected?"chip on":"chip";if(extra)k+=" "+extra;const n=button("",k,onClick);if(count!=null)n.classList.add("count");const t=document.createElement("span");t.textContent=label;n.appendChild(t);if(count!=null){const c=document.createElement("span");c.textContent=count;n.appendChild(c)}return n};
function renderSortButtons(container,state,defs){clear(container);defs.forEach((d)=>{const sel=state.f===d.f;const lbl=d.label+(sel?(" "+(state.d==="desc"?ARROW_DOWN:ARROW_UP)):"");let klass="sortBtn";if(sel)klass+=state.d==="desc"?" desc":" asc";container.appendChild(button(lbl,klass,()=>{if(state.f===d.f)state.d=state.d==="desc"?"asc":"desc";else{state.f=d.f;state.d=d.defaultDir}renderAll()}))})}
function renderSummary(){clear(ui.summary);if(activeTags.size===0&&activeCreators.size===0){const e=document.createElement("span");e.className="muted";e.textContent="No active filters";ui.summary.appendChild(e);return}Array.from(activeTags).sort().forEach((t)=>ui.summary.appendChild(chip("Tag: "+t,null,true,()=>{activeTags.delete(t);renderAll()})));Array.from(activeCreators).sort().forEach((k)=>{const p=k.split("|");const ty=p[0]||"";const nmv=p.slice(1).join("|");ui.summary.appendChild(chip(ty+": "+nmv,null,true,()=>{activeCreators.delete(k);renderAll()},"creatorChip"))})}
function renderTagsPanel(){clear(ui.activeTags);Array.from(activeTags).sort().forEach((t)=>ui.activeTags.appendChild(chip(t,null,true,()=>{activeTags.delete(t);renderAll()})));clear(ui.tagsList);sortRows(rowsTags(),tagSort,"count").forEach((t)=>{const k=ntag(t.name);const on=activeTags.has(k);ui.tagsList.appendChild(chip(t.name+" ["+t.type+"]",formatCount(t.count),on,()=>{if(on)activeTags.delete(k);else activeTags.add(k);renderAll()}))})}
function renderCreatorsPanel(){clear(ui.activeCreators);Array.from(activeCreators).sort().forEach((k)=>{const p=k.split("|");const ty=p[0]||"";const nmv=p.slice(1).join("|");ui.activeCreators.appendChild(chip(ty+": "+nmv,null,true,()=>{activeCreators.delete(k);renderAll()},"creatorChip"))});clear(ui.creatorsList);sortRows(rowsCreators(),creatorSort,"entry_count").forEach((c)=>{const k=creatorKey(c.type,c.name);const on=activeCreators.has(k);ui.creatorsList.appendChild(chip(c.name+" ["+c.type+"]",formatCount(c.entry_count),on,()=>{if(on)activeCreators.delete(k);else activeCreators.add(k);renderAll()},"creatorChip"))})}
function splitTagsByCreator(tags){const creators=[],normal=[];tags.forEach((t)=>{const ty=nm(t.type);if(ty==="artist"||ty==="group")creators.push(t);else normal.push(t)});return{creators,normal}}
function renderEntries(){clear(ui.entries);const rows=sortEntries(filterEntries());const s=(data&&data.saved_stats)||{};ui.stats.textContent="Saved totals: "+(s.entries||0)+" entries, "+(s.artists||0)+" artists, "+(s.groups||0)+" groups, "+(s.read_entries||0)+" read. Showing: "+rows.length;if(!rows.length){const e=document.createElement("span");e.className="muted";e.textContent="No entries match current filters.";ui.entries.appendChild(e);return}rows.forEach((e)=>{const code=num(e.code),isPinned=num(e.pinned)!==0,isRead=num(e.read)!==0,rating=num(e.rating),tags=Array.isArray(e.tags)?e.tags:[],sections=splitTagsByCreator(tags);const card=document.createElement("div");card.className="entry";const pinNode=button("","pinCorner"+(isPinned?" on":""),()=>postJson("/api/entry/pin",{code,pinned:!isPinned}).then((j)=>{setStatus(j.message||"Updated pin.");return reload(false)}).catch((er)=>setStatus(er.message||"Could not update pin.")));const pinGlyph=document.createElement("span");pinGlyph.className="pinGlyph";pinNode.appendChild(pinGlyph);pinNode.title=isPinned?"Unpin":"Pin";card.appendChild(pinNode);const head=document.createElement("div");head.className="entryHead";const titleNode=document.createElement("span");titleNode.className="title";const titleText=(e.title||("Gallery "+code));const codeNode=button("#"+code,"codeInline",()=>copyText("#"+code));titleNode.appendChild(codeNode);titleNode.appendChild(document.createTextNode(" "+titleText));head.appendChild(titleNode);card.appendChild(head);const tw=document.createElement("div");tw.className="thumb";const url=thumbnailUrl(e);if(url){const im=document.createElement("img");im.src=url;im.loading="lazy";im.referrerPolicy="no-referrer";im.onerror=()=>{im.style.display="none";if(!tw.querySelector(".thumbEmpty")){const ph=document.createElement("div");ph.className="thumbEmpty";ph.textContent="No preview";tw.appendChild(ph)}};tw.appendChild(im);tw.addEventListener("click",()=>openImage(url,code))}else{const ph=document.createElement("div");ph.className="thumbEmpty";ph.textContent="No preview";tw.appendChild(ph)}card.appendChild(tw);const me=document.createElement("div");me.className="meta";me.textContent="Pages: "+num(e.num_pages)+" | Uploaded: "+(e.upload_date||"-")+" | Fetched: "+(e.added_at||"-")+" | Rating: "+rating+" | "+(isRead?"Read":"Unread")+(isPinned?" | Pinned":"");card.appendChild(me);const top=document.createElement("div");top.className="row";top.appendChild(button("Open","",()=>window.open("https://nhentai.net/g/"+code+"/","_blank")));top.appendChild(button(isRead?"Read":"Unread",isRead?"readState":"unreadState",()=>postJson("/api/entry/read",{code,read:!isRead}).then((j)=>{setStatus(j.message||"Updated read state.");return reload(false)}).catch((er)=>setStatus(er.message||"Could not update read state."))));top.appendChild(button("Delete","",()=>{if(!confirm("Delete #"+code+"?"))return;postJson("/api/entry/delete",{code}).then((j)=>{setStatus(j.message||"Deleted.");return reload(false)}).catch((er)=>setStatus(er.message||"Could not delete entry."))}));card.appendChild(top);const ratingRow=document.createElement("div");ratingRow.className="row";const rl=document.createElement("span");rl.className="muted";rl.textContent="Rating:";ratingRow.appendChild(rl);const stars=document.createElement("div");stars.className="stars";for(let i=1;i<=5;i+=1){stars.appendChild(button(STAR,i<=rating?"star on":"star",()=>postJson("/api/entry/rating",{code,rating:i}).then((j)=>{setStatus(j.message||"Updated rating.");return reload(false)}).catch((er)=>setStatus(er.message||"Could not update rating."))))}ratingRow.appendChild(stars);ratingRow.appendChild(button("Reset","",()=>postJson("/api/entry/rating",{code,rating:0}).then((j)=>{setStatus(j.message||"Reset rating.");return reload(false)}).catch((er)=>setStatus(er.message||"Could not reset rating."))));card.appendChild(ratingRow);if(sections.creators.length){const cl=document.createElement("div");cl.className="sectionLabel";cl.textContent="Artists / Groups";card.appendChild(cl);const cg=document.createElement("div");cg.className="chipList";sections.creators.forEach((t)=>{const k=creatorKey(t.type,t.name);const on=activeCreators.has(k);const prefix=nm(t.type)==="group"?"Group: ":"Artist: ";cg.appendChild(chip(prefix+t.name,null,on,()=>{if(on)activeCreators.delete(k);else activeCreators.add(k);renderAll()},"creatorChip"))});card.appendChild(cg)}if(sections.normal.length){const tl=document.createElement("div");tl.className="sectionLabel";tl.textContent="Tags";card.appendChild(tl);const tg=document.createElement("div");tg.className="chipList";sections.normal.forEach((t)=>{const k=ntag(t.name);const on=activeTags.has(k);tg.appendChild(chip(t.name,null,on,()=>{if(on)activeTags.delete(k);else activeTags.add(k);renderAll()}))});card.appendChild(tg)}ui.entries.appendChild(card)})}
function placePanel(panelEl,anchorEl){const r=anchorEl.getBoundingClientRect();const wrap=(ui.root.querySelector(".wrap")||document.body).getBoundingClientRect();const leftBound=Math.max(12,wrap.left+8);const rightBound=Math.min(window.innerWidth-12,wrap.right-8);const available=Math.max(260,rightBound-leftBound);const w=Math.min(620,Math.max(320,available));let l=r.left;if(l+w>rightBound)l=rightBound-w;if(l<leftBound)l=leftBound;panelEl.style.width=Math.max(260,Math.min(w,available))+"px";panelEl.style.left=l+"px";panelEl.style.top=(r.bottom+8)+"px"}
function setPanel(which){panelOpen=which;const t=which==="tags",c=which==="creators";ui.tagsPanel.classList.toggle("hidden",!t);ui.creatorsPanel.classList.toggle("hidden",!c);ui.panelBackdrop.classList.toggle("hidden",!(t||c));if(t)placePanel(ui.tagsPanel,ui.tagsBtn);if(c)placePanel(ui.creatorsPanel,ui.creatorsBtn)}
function renderUnlockChoices(choices){clear(ui.unlockChoices);(choices||[]).forEach((choice)=>ui.unlockChoices.appendChild(button(String(choice),"accent",()=>postJson("/api/unlock",{code:String(choice)}).then((j)=>{if(j&&j.unlocked){unlocked=true;cryptoReady=false;cryptoKey=null;clearUnlockTimer();ui.root.classList.remove("locked");ui.unlockOverlay.classList.add("hidden");setUnlockStatus("");setStatus("Desktop bridge unlocked.");return reload(true)}setUnlockStatus((j&&j.message)||"Round accepted. Continue.");return checkUnlock()}).catch((er)=>{setUnlockStatus(er.message||"Unlock failed.");return checkUnlock()})))) }
function renderAll(){
renderSortButtons(ui.entrySort,entrySort,[{f:"title",label:"Title",defaultDir:"asc"},{f:"pages",label:"Pages",defaultDir:"desc"},{f:"upload",label:"Uploaded",defaultDir:"desc"},{f:"added",label:"Fetched",defaultDir:"desc"}]);
ui.entrySort.appendChild(button("Rating",ratingSortOn?"sortBtn desc":"sortBtn",()=>{ratingSortOn=!ratingSortOn;localStorage.setItem("stb_rating_toggle",ratingSortOn?"1":"0");renderAll()}));
ui.entrySort.appendChild(button("Pin",pinPriorityEnabled?"sortBtn pinPriorityOn":"sortBtn",()=>{pinPriorityEnabled=!pinPriorityEnabled;localStorage.setItem("stb_pin_priority_on",pinPriorityEnabled?"1":"0");renderAll()}));
const readFilterWrap=document.createElement("div");
readFilterWrap.className="readFilterGroup";
[
{key:"all",label:"All",activeClass:"onAll"},
{key:"read",label:"Read",activeClass:"onRead"},
{key:"unread",label:"Unread",activeClass:"onUnread"}
].forEach((def)=>{
let klass="sortBtn readFilterBtn";
if(entryReadFilter===def.key)klass+=" "+def.activeClass;
readFilterWrap.appendChild(button(def.label,klass,()=>{entryReadFilter=def.key;localStorage.setItem("stb_entry_read_filter",entryReadFilter);renderAll()}));
});
ui.entrySort.appendChild(readFilterWrap);
renderSortButtons(ui.tagSort,tagSort,[{f:"name",label:"Tag",defaultDir:"asc"},{f:"type",label:"Type",defaultDir:"asc"},{f:"count",label:"Count",defaultDir:"desc"}]);
renderSortButtons(ui.creatorSort,creatorSort,[{f:"name",label:"Name",defaultDir:"asc"},{f:"type",label:"Type",defaultDir:"asc"},{f:"count",label:"Count",defaultDir:"desc"}]);
renderSummary();renderTagsPanel();renderCreatorsPanel();renderEntries()}
function reload(show){
if(!unlocked)return Promise.resolve();
const applyState=(j,compat)=>{data=j;screenBlackout=!!j.bridge_screen_blackout;ui.screenBtn.textContent=screenBlackout?"Screen: Off":"Screen: On";ui.screenBtn.className=screenBlackout?"sortBtn asc":"sortBtn desc";const serverAccent=normalizeAccent(j.bridge_accent_mode||"auto");if(serverAccent!==accentMode)applyAccent(serverAccent,true,false);setMode(compat?"Mode: Compatibility (TLS only)":"Mode: Encrypted (WebCrypto + TLS)");renderAll();if(show)setStatus("Synced at "+(j.generated_at||"")+(compat?" (compatibility mode)":" (encrypted mode)"))};
const loadEncrypted=()=>fetchJson("/api/state",{cache:"no-store"}).then((j)=>applyState(j,false));
const loadCompat=(prefix)=>fetchJson("/api/state-plain",{cache:"no-store"}).then((j)=>{applyState(j,true);if(prefix)setStatus(prefix+" "+("Synced at "+(j.generated_at||"")+" (compatibility mode)"))});
if(!cryptoSupported){setMode("Mode: Compatibility (TLS only)");return loadCompat("")}
const ensure=cryptoReady?Promise.resolve():ensureCryptoSession();
return ensure.then(loadEncrypted).catch((er)=>{cryptoReady=false;cryptoKey=null;const reason=(er&&er.message)?er.message:"Encrypted mode unavailable.";return loadCompat("Encrypted mode unavailable: "+reason).catch((inner)=>setStatus(inner.message||"Failed to load state."))})
}
function checkUnlock(){clearUnlockTimer();return fetchJson("/api/unlock-status",{cache:"no-store"}).then((j)=>{unlocked=!!j.unlocked;if(unlocked){ui.root.classList.remove("locked");ui.unlockOverlay.classList.add("hidden");setUnlockStatus("");if(!cryptoSupported){const reason=window.isSecureContext?"WebCrypto not exposed by this browser for this page.":"Page is not in a secure context (HTTPS/localhost), so WebCrypto is disabled.";setStatus("Encrypted mode unavailable: "+reason+" Running in compatibility mode.")}else{setMode("Mode: Encrypted (handshake pending...)")}return reload(true)}ui.root.classList.add("locked");ui.unlockOverlay.classList.remove("hidden");setMode("Mode: Locked");const round=num(j.round||1),total=num(j.total_rounds||3),wait=num(j.wait_seconds||0);if(wait>0){clear(ui.unlockChoices);setUnlockStatus("Locked for "+wait+"s. Round 1/"+total);unlockPollTimer=setTimeout(()=>{checkUnlock()},1000)}else{setUnlockStatus("Round "+round+"/"+total+": pick the number on your device.");renderUnlockChoices(j.choices||[])}return Promise.resolve()}).catch(()=>{ui.root.classList.add("locked");ui.unlockOverlay.classList.remove("hidden");setMode("Mode: Locked");setUnlockStatus("Unable to contact bridge.");unlockPollTimer=setTimeout(()=>{checkUnlock()},1500)})}
ui.refreshBtn.addEventListener("click",()=>reload(true));ui.searchInput.addEventListener("input",()=>renderAll());ui.resetBtn.addEventListener("click",()=>{activeTags.clear();activeCreators.clear();entryReadFilter="all";localStorage.setItem("stb_entry_read_filter",entryReadFilter);ui.searchInput.value="";renderAll()});ui.tagsBtn.addEventListener("click",()=>setPanel(panelOpen==="tags"?"":"tags"));ui.creatorsBtn.addEventListener("click",()=>setPanel(panelOpen==="creators"?"":"creators"));ui.panelBackdrop.addEventListener("click",()=>setPanel(""));window.addEventListener("resize",()=>{if(panelOpen==="tags")placePanel(ui.tagsPanel,ui.tagsBtn);if(panelOpen==="creators")placePanel(ui.creatorsPanel,ui.creatorsBtn)});
ui.addCodeBtn.addEventListener("click",()=>{const raw=(ui.addCodeInput.value||"").trim();const code=Number(raw.replace("#",""));if(!Number.isFinite(code)||code<=0){setStatus("Enter a valid numeric code.");return}setStatus("Fetching and saving...");postJson("/api/entry/add",{code}).then((j)=>{setStatus(j.message||"Saved.");ui.addCodeInput.value="";return reload(false)}).catch((er)=>setStatus(er.message||"Failed to add/update."))});
ui.addCodeInput.addEventListener("keydown",(ev)=>{if(ev.key==="Enter"){ev.preventDefault();ui.addCodeBtn.click()}});
ui.themeBtn.addEventListener("click",()=>{const modes=["auto","dark","light"];const i=modes.indexOf(themeMode);applyTheme(modes[(i+1)%modes.length],true)});
ui.screenBtn.addEventListener("click",()=>{const next=!screenBlackout;postJson("/api/device/screen-blackout",{enabled:next}).then(()=>{screenBlackout=next;ui.screenBtn.textContent=screenBlackout?"Screen: Off":"Screen: On";ui.screenBtn.className=screenBlackout?"sortBtn asc":"sortBtn desc";setStatus(screenBlackout?"Phone screen blackout enabled.":"Phone screen blackout disabled.")}).catch((er)=>setStatus(er.message||"Could not toggle phone screen state."))});
ui.cols.addEventListener("input",()=>{applyColumns(ui.cols.value);renderAll()});ui.accentBtn.addEventListener("click",()=>ui.accentWrap.classList.toggle("hidden"));ui.imageFull.addEventListener("click",(ev)=>{ev.stopPropagation();openImagePageOne()});ui.imageOverlay.addEventListener("click",closeImage);
const savedTheme=localStorage.getItem("stb_theme_mode")||"auto",savedAccent=localStorage.getItem("stb_accent_mode")||"auto",savedColumns=Number(localStorage.getItem("stb_columns")||3),savedRating=localStorage.getItem("stb_rating_toggle")||"0",savedPinPriority=localStorage.getItem("stb_pin_priority_on")||"1",savedReadFilter=localStorage.getItem("stb_entry_read_filter")||"all";ratingSortOn=savedRating==="1";pinPriorityEnabled=savedPinPriority!=="0";entryReadFilter=normalizeReadFilter(savedReadFilter);applyTheme(savedTheme,true);applyAccent(savedAccent,true,false);applyColumns(savedColumns);checkUnlock();setInterval(()=>reload(false),5000);
})();
</script>
</body>
</html>
""".trimIndent()
