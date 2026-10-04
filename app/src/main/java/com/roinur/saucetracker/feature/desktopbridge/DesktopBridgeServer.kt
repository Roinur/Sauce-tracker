package com.roinur.saucetracker.feature.desktopbridge

import com.roinur.saucetracker.data.source.SourceEntry

import com.roinur.saucetracker.data.database.SauceTrackerDatabase
import com.roinur.saucetracker.data.profile.ProfileStore
import com.roinur.saucetracker.data.source.SourceCapability
import com.roinur.saucetracker.data.source.SourceEntryKey
import com.roinur.saucetracker.data.source.SourceEntryStore
import com.roinur.saucetracker.data.source.SourceId
import com.roinur.saucetracker.data.source.SourceRegistry
import com.roinur.saucetracker.data.source.SourceQuery
import com.roinur.saucetracker.data.source.SourceQueryField
import com.roinur.saucetracker.data.source.SourceQueryParser
import com.roinur.saucetracker.data.source.SourceQueryTerm
import com.roinur.saucetracker.data.source.SourceSortMode
import com.roinur.saucetracker.data.source.uiCode
import com.roinur.saucetracker.core.network.HttpClientFactory
import com.roinur.saucetracker.core.network.HttpClientProfile

import com.roinur.saucetracker.*

import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.graphics.toArgb
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
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
import java.security.Principal
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec
import java.security.cert.X509Certificate
import java.math.BigInteger
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Base64
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import okhttp3.Request
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLServerSocket
import javax.net.ssl.X509KeyManager
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
    private val currentAccentMode: () -> String = { "AUTO" },
    private val currentSuggestions: () -> List<SuggestedEntryRow> = { emptyList() },
    private val currentSourceSuggestions: () -> List<SourceEntry> = { emptyList() }
) {
    private data class BridgeReaderSession(
        val clientAddress: String,
        val pageCandidates: List<List<String>>,
        val createdAtMs: Long
    )

    companion object {
        private const val TLS_KEY_ALIAS = "sauce_tracker_desktop_bridge_tls"
        private const val TLS_KEYSTORE_FILE = "desktop_bridge_tls.p12"
        private const val TLS_PASSWORD_FILE = "desktop_bridge_tls.password"
        private const val TLS_CERTIFICATE_LIFETIME_MS = 20L * 365L * 24L * 60L * 60L * 1000L
    }

    private data class PendingCryptoSession(
        val remoteAddress: String,
        val keyPair: KeyPair,
        val createdAtMs: Long
    )

    private val lock = Any()
    private val profiles = ProfileStore(db)
    private val sourceEntries = SourceEntryStore(db)
    private val sourceRegistry = SourceRegistry.createDefault()
    private val readerHttp = HttpClientFactory.create(HttpClientProfile.SLIDESHOW)
    private val readerSessions = ConcurrentHashMap<String, BridgeReaderSession>()
    @Volatile private var lastReaderNotificationMs = 0L
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
            readerSessions.clear()
            lastReaderNotificationMs = 0L
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
            readerSessions.clear()
            lastReaderNotificationMs = 0L
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
        val (keyStore, password) = loadOrCreateTlsKeyStore()
        val privateKey = keyStore.getKey(TLS_KEY_ALIAS, password) as? PrivateKey
            ?: error("Desktop Bridge TLS private key is unavailable.")
        val certificateChain = keyStore.getCertificateChain(TLS_KEY_ALIAS)
            ?.mapNotNull { it as? X509Certificate }
            ?.toTypedArray()
            ?.takeIf { it.isNotEmpty() }
            ?: error("Desktop Bridge TLS certificate chain is unavailable.")
        val keyManager = object : X509KeyManager {
            private fun supports(keyType: String?): Boolean =
                keyType.isNullOrBlank() || keyType.contains(privateKey.algorithm, ignoreCase = true)

            override fun getClientAliases(
                keyType: String?,
                issuers: Array<out Principal>?
            ): Array<String>? = null

            override fun chooseClientAlias(
                keyType: Array<out String>?,
                issuers: Array<out Principal>?,
                socket: Socket?
            ): String? = null

            override fun getServerAliases(
                keyType: String?,
                issuers: Array<out Principal>?
            ): Array<String>? = if (supports(keyType)) arrayOf(TLS_KEY_ALIAS) else null

            override fun chooseServerAlias(
                keyType: String?,
                issuers: Array<out Principal>?,
                socket: Socket?
            ): String? = if (supports(keyType)) TLS_KEY_ALIAS else null

            override fun getCertificateChain(alias: String?): Array<X509Certificate>? =
                if (alias == TLS_KEY_ALIAS) certificateChain.copyOf() else null

            override fun getPrivateKey(alias: String?): PrivateKey? =
                if (alias == TLS_KEY_ALIAS) privateKey else null
        }
        return SSLContext.getInstance("TLS").apply {
            init(arrayOf(keyManager), null, SecureRandom())
        }
    }

    private fun loadOrCreateTlsKeyStore(): Pair<KeyStore, CharArray> {
        val directory = appContext.noBackupFilesDir.apply { mkdirs() }
        val keyStoreFile = File(directory, TLS_KEYSTORE_FILE)
        val passwordFile = File(directory, TLS_PASSWORD_FILE)
        if (keyStoreFile.isFile && passwordFile.isFile) {
            val loaded = runCatching {
                val password = passwordFile.readText(Charsets.US_ASCII).trim().toCharArray()
                require(password.isNotEmpty())
                val keyStore = KeyStore.getInstance("PKCS12")
                keyStoreFile.inputStream().use { keyStore.load(it, password) }
                require(keyStore.isKeyEntry(TLS_KEY_ALIAS))
                keyStore to password
            }.getOrNull()
            if (loaded != null) return loaded
        }

        val password = generateToken(48).toCharArray()
        val keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        val now = System.currentTimeMillis()
        val subject = X500Name("CN=Sauce Tracker Desktop Bridge")
        val serialNumber = BigInteger(160, SecureRandom()).coerceAtLeast(BigInteger.ONE)
        val certificateBuilder = JcaX509v3CertificateBuilder(
            subject,
            serialNumber,
            Date(now - 60_000L),
            Date(now + TLS_CERTIFICATE_LIFETIME_MS),
            subject,
            keyPair.public
        )
        val certificate = JcaX509CertificateConverter().getCertificate(
            certificateBuilder.build(
                JcaContentSignerBuilder("SHA256withRSA").build(keyPair.private)
            )
        ).apply {
            checkValidity()
            verify(keyPair.public)
        }
        val keyStore = KeyStore.getInstance("PKCS12").apply {
            load(null, password)
            setKeyEntry(TLS_KEY_ALIAS, keyPair.private, password, arrayOf(certificate))
        }
        val keyStoreTemp = File(directory, "$TLS_KEYSTORE_FILE.tmp")
        val passwordTemp = File(directory, "$TLS_PASSWORD_FILE.tmp")
        keyStoreTemp.outputStream().use { keyStore.store(it, password) }
        passwordTemp.writeText(String(password), Charsets.US_ASCII)
        if (!keyStoreTemp.renameTo(keyStoreFile)) {
            keyStoreTemp.copyTo(keyStoreFile, overwrite = true)
            keyStoreTemp.delete()
        }
        if (!passwordTemp.renameTo(passwordFile)) {
            passwordTemp.copyTo(passwordFile, overwrite = true)
            passwordTemp.delete()
        }
        return keyStore to password
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
                val response = runCatching { routeRequest(request) }.getOrElse { error ->
                    val message = if (error is IllegalArgumentException) error.message ?: "Invalid request." else "Bridge request failed."
                    jsonResponse(if (error is IllegalArgumentException) 400 else 500, JSONObject().put("ok", false).put("api_version", 2)
                        .put("error", JSONObject().put("code", if (error is IllegalArgumentException) "invalid_request" else "internal_error").put("message", message)))
                }
                writeHttpResponse(client, response)
            }.onFailure { error ->
                Log.e("SauceTrackerDesktopBridge", "Desktop Bridge client connection failed.", error)
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
            return assetResponse("desktop-bridge/index.html", "text/html; charset=utf-8", token)
        }
        if (request.method == "GET" && request.path == "/bridge.css") {
            return assetResponse("desktop-bridge/bridge.css", "text/css; charset=utf-8")
        }
        if (request.method == "GET" && request.path == "/reader.css") {
            return assetResponse("desktop-bridge/reader.css", "text/css; charset=utf-8")
        }
        if (request.method == "GET" && request.path == "/bridge.js") {
            return assetResponse("desktop-bridge/bridge.js", "application/javascript; charset=utf-8")
        }
        if (request.method == "GET" && request.path == "/app-icon.png") {
            val iconBytes = runCatching {
                appContext.resources.openRawResource(com.roinur.saucetracker.R.drawable.app_icon).use(InputStream::readBytes)
            }.getOrNull() ?: return HttpResponse(404, "Not Found", "text/plain; charset=utf-8", "Not found.".toByteArray())
            return HttpResponse(200, "OK", "image/png", iconBytes)
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
            request.method == "GET" && request.path == "/api/v2/sources" -> v2Response(request, bridgeSources())
            request.method == "GET" && request.path == "/api/v2/profiles" -> v2Response(request, bridgeProfiles())
            request.method == "GET" && request.path == "/api/v2/dashboard" -> v2Response(request, bridgeDashboard(request))
            request.method == "GET" && request.path == "/api/v2/library" -> v2Response(request, bridgeLibrary(request))
            request.method == "GET" && request.path == "/api/v2/entry" -> v2Response(request, bridgeEntry(request))
            request.method == "GET" && request.path == "/api/v2/tags" -> v2Response(request, bridgeExplore(request, creators = false))
            request.method == "GET" && request.path == "/api/v2/creators" -> v2Response(request, bridgeExplore(request, creators = true))
            request.method == "GET" && request.path == "/api/v2/history" -> v2Response(request, bridgeHistory(request))
            request.method == "GET" && request.path == "/api/v2/trends" -> v2Response(request, bridgeTrends(request))
            request.method == "GET" && request.path == "/api/v2/heatmap" -> v2Response(request, bridgeHeatmap(request))
            request.method == "GET" && request.path == "/api/v2/suggestions" -> v2Response(request, bridgeSuggestions(request))
            request.method == "GET" && request.path == "/api/v2/browser/search" -> v2Response(request, bridgeBrowserSearch(request))
            request.method == "GET" && request.path == "/api/v2/browser/entry" -> v2Response(request, bridgeBrowserEntry(request))
            request.method == "GET" && request.path == "/api/v2/reader/chapters" -> v2Response(request, bridgeReaderChapters(request))
            request.method == "GET" && request.path == "/api/v2/reader/manifest" -> v2Response(request, bridgeReaderManifest(request))
            request.method == "GET" && request.path == "/api/v2/reader/page" -> bridgeReaderPage(request)
            request.method == "GET" && request.path == "/api/v2/image" -> bridgeRemoteImage(request)
            request.method == "GET" && request.path == "/api/v2/device" -> v2Response(request, JSONObject()
                .put("screen_blackout", screenBlackoutEnabled)
                .put("accent_mode", currentAccentMode.invoke())
                .put("auto_accent_dark", resolvedAutoAccent(dark = true))
                .put("auto_accent_light", resolvedAutoAccent(dark = false)))
            request.method == "POST" && request.path == "/api/v2/entry/state" -> v2Response(request, bridgeUpdateState(request.body))
            request.method == "POST" && request.path == "/api/v2/entry/remove" -> v2Response(request, bridgeRemoveEntry(request.body))
            request.method == "POST" && request.path == "/api/v2/import" -> v2Response(request, bridgeImport(request.body))
            request.method == "POST" && request.path == "/api/v2/reader/progress" -> v2Response(request, bridgeReaderProgress(request.body))
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

    private data class BridgeScope(val profileId: String, val sourceIds: Set<SourceId>)

    private fun bridgeScope(request: HttpRequest): BridgeScope {
        val profileId = request.query["profile_id"].orEmpty().ifBlank { profiles.activeProfileId() }
        val profile = profiles.profile(profileId) ?: throw IllegalArgumentException("Unknown profile.")
        val requested = request.query["sources"].orEmpty().split(',').map(String::trim).filter(String::isNotBlank).map(::SourceId).toSet()
        val scope = if (requested.isEmpty()) profile.sourceIds else requested
        require(scope.isNotEmpty() && profile.sourceIds.containsAll(scope)) { "The selected sources are not enabled for this profile." }
        return BridgeScope(profileId, scope)
    }

    private fun bridgeSources(): JSONObject = JSONObject().put("sources", JSONArray().apply {
        sourceRegistry.sources.forEach { adapter ->
            put(JSONObject().put("id", adapter.id.value).put("name", adapter.displayName).put("contract_version", adapter.contractVersion)
                .put("capabilities", JSONArray(adapter.capabilities.map(SourceCapability::name).sorted())))
        }
    })

    private fun bridgeProfiles(): JSONObject = JSONObject()
        .put("active_profile_id", profiles.activeProfileId())
        .put("profiles", JSONArray().apply {
            profiles.profiles().forEach { profile ->
                put(JSONObject().put("id", profile.id).put("name", profile.name).put("kind", profile.kind.name)
                    .put("main", profile.isMain).put("sources", JSONArray(profile.sourceIds.map { it.value }.sorted())))
            }
        })

    private fun bridgeDashboard(request: HttpRequest): JSONObject {
        val scope = bridgeScope(request)
        val placeholders = scope.sourceIds.joinToString(",") { "?" }
        val args = (listOf(scope.profileId) + scope.sourceIds.map { it.value }).toTypedArray()
        return db.readableDatabase.rawQuery(
            """SELECT COUNT(*),SUM(CASE WHEN pe.read_state<>0 THEN 1 ELSE 0 END),SUM(CASE WHEN pe.pinned<>0 THEN 1 ELSE 0 END),SUM(CASE WHEN pe.rating>0 THEN 1 ELSE 0 END)
               FROM profile_entries pe JOIN source_entries se ON se.id=pe.source_entry_id
               WHERE pe.profile_id=? AND se.source_id IN ($placeholders)""".trimIndent(), args
        ).use { cursor -> cursor.moveToFirst(); JSONObject().put("entries", cursor.getInt(0)).put("read_entries", cursor.getInt(1))
            .put("pinned_entries", cursor.getInt(2)).put("rated_entries", cursor.getInt(3)) }
    }

    private fun bridgeLibrary(request: HttpRequest): JSONObject {
        val scope = bridgeScope(request)
        val limit = request.query["limit"]?.toIntOrNull()?.coerceIn(1, 100) ?: 40
        val offset = request.query["offset"]?.toIntOrNull()?.coerceAtLeast(0) ?: 0
        val values = sourceEntries.entries(scope.profileId, scope.sourceIds, request.query["q"].orEmpty(), limit + 1, offset,
            stateFilter = request.query["state"].orEmpty(), sort = request.query["sort"].orEmpty().ifBlank { "added" })
        return JSONObject().put("items", JSONArray(values.take(limit).map(::bridgeEntryJson))).put("has_more", values.size > limit)
            .put("offset", offset).put("limit", limit)
    }

    private fun bridgeEntry(request: HttpRequest): JSONObject {
        val scope = bridgeScope(request)
        val key = SourceEntryKey(SourceId(request.query["source_id"].orEmpty()), request.query["remote_id"].orEmpty())
        require(key.sourceId in scope.sourceIds) { "Source is outside the selected profile." }
        val entry = sourceEntries.entry(key) ?: throw IllegalArgumentException("Entry not found.")
        val state = profiles.state(scope.profileId, key) ?: throw IllegalArgumentException("Entry is not in this profile.")
        return JSONObject().put("entry", bridgeEntryJson(com.roinur.saucetracker.data.source.ProfileSourceEntry(entry, state))
            .put("local_tags", JSONArray(sourceEntries.localTags(scope.profileId, key))))
    }

    private fun bridgeEntryJson(value: com.roinur.saucetracker.data.source.ProfileSourceEntry): JSONObject = JSONObject()
        .put("source_id", value.entry.key.sourceId.value).put("remote_id", value.entry.key.remoteId).put("display_id", value.entry.key.displayId)
        .put("title", value.entry.title).put("canonical_url", value.entry.canonicalUrl).put("thumbnail_url", value.entry.thumbnailUrl)
        .put("unit_count", value.entry.unitCount).put("unit_label", value.entry.unitLabel).put("status", value.entry.status)
        .put("in_library", true)
        .put("read", value.state.isRead).put("rating", value.state.rating).put("pinned", value.state.pinned)
        .put("tags", JSONArray(value.entry.tags.map { JSONObject().put("name", it.name).put("type", it.type) }))
        .put("creators", JSONArray(value.entry.creators.map { JSONObject().put("name", it.name).put("type", it.type).put("url", it.url) }))

    private fun bridgeExplore(request: HttpRequest, creators: Boolean): JSONObject {
        val scope = bridgeScope(request)
        val table = if (creators) "source_entry_creators" else "source_entry_tags"
        val alias = if (creators) "c" else "t"
        val placeholders = scope.sourceIds.joinToString(",") { "?" }
        val needle = request.query["q"].orEmpty().trim().lowercase(Locale.US)
        val args = mutableListOf(scope.profileId).apply { addAll(scope.sourceIds.map { it.value }); if (needle.isNotBlank()) add("%$needle%") }
        val items = JSONArray()
        db.readableDatabase.rawQuery(
            """SELECT $alias.name,$alias.type,COUNT(DISTINCT pe.source_entry_id)
               FROM profile_entries pe JOIN source_entries se ON se.id=pe.source_entry_id JOIN $table $alias ON $alias.source_entry_id=se.id
               WHERE pe.profile_id=? AND se.source_id IN ($placeholders)${if (needle.isBlank()) "" else " AND LOWER($alias.name) LIKE ?"}
               GROUP BY LOWER($alias.name),$alias.type ORDER BY 3 DESC,LOWER($alias.name) LIMIT 250""".trimIndent(), args.toTypedArray()
        ).use { cursor -> while (cursor.moveToNext()) items.put(JSONObject().put("name", cursor.getString(0)).put("type", cursor.getString(1)).put("count", cursor.getInt(2))) }
        return JSONObject().put("items", items)
    }

    private fun bridgeHistory(request: HttpRequest): JSONObject {
        val scope = bridgeScope(request)
        val limit = request.query["limit"]?.toIntOrNull()?.coerceIn(1, 250) ?: 120
        val placeholders = scope.sourceIds.joinToString(",") { "?" }
        val args = mutableListOf(scope.profileId).apply { addAll(scope.sourceIds.map { it.value }); add(limit.toString()) }
        val items = JSONArray()
        db.readableDatabase.rawQuery(
            """SELECT substr(COALESCE(NULLIF(s.day_key,''),s.started_at),1,10),s.source_id,s.remote_id,
                      COALESCE(se.title,s.remote_id),COALESCE(se.thumbnail_url,''),MAX(s.started_at),
                      SUM(COALESCE(s.pages_viewed,0)),MAX(COALESCE(s.is_reread,0)),SUM(COALESCE(s.seconds_elapsed,0)),
                      COUNT(*),COUNT(DISTINCT CASE WHEN s.chapter_id<>'' THEN s.chapter_id END)
               FROM reading_sessions s LEFT JOIN source_entries se ON se.source_id=s.source_id AND se.remote_id=s.remote_id
               WHERE s.profile_id=? AND s.source_id IN ($placeholders)
               GROUP BY 1,s.source_id,s.remote_id ORDER BY MAX(s.started_at) DESC LIMIT ?""".trimIndent(), args.toTypedArray()
        ).use { cursor -> while (cursor.moveToNext()) items.put(JSONObject()
            .put("day", cursor.getString(0)).put("source_id", cursor.getString(1)).put("remote_id", cursor.getString(2))
            .put("title", cursor.getString(3)).put("thumbnail_url", cursor.getString(4)).put("started_at", cursor.getString(5))
            .put("units_viewed", cursor.getInt(6)).put("reread", cursor.getInt(7) != 0).put("seconds_elapsed", cursor.getLong(8))
            .put("session_count", cursor.getInt(9)).put("chapter_count", cursor.getInt(10)).put("unit_label", "pages")) }
        return JSONObject().put("items", items)
    }

    private fun bridgeHeatmap(request: HttpRequest): JSONObject {
        val scope = bridgeScope(request)
        val graph = com.roinur.saucetracker.feature.heatmap.HeatmapEngine.computeTagGraphSnapshot(
            db.getTagGraphDataSnapshot(scope.profileId, scope.sourceIds.mapTo(linkedSetOf()) { it.value })
        )
        return JSONObject().put("tags", JSONArray(graph.nodes.map { node ->
            JSONObject().put("name", node.name).put("count", node.localCount)
                .put("x", node.heatX).put("y", node.heatY)
                .put("raw_x", node.rawX).put("raw_y", node.rawY)
                .put("rated_x", node.ratedX).put("rated_y", node.ratedY)
        })).put("entries", JSONArray(graph.entryNodes.map { node ->
            JSONObject().put("title", node.title).put("source_id", node.sourceId).put("remote_id", node.remoteId)
                .put("x", node.x).put("y", node.y).put("rating", node.rating)
        }))
    }

    private fun bridgeTrends(request: HttpRequest): JSONObject {
        val scope = bridgeScope(request)
        val placeholders = scope.sourceIds.joinToString(",") { "?" }
        val args = mutableListOf(scope.profileId).apply { addAll(scope.sourceIds.map { it.value }) }
        val byDate = linkedMapOf<String, JSONObject>()
        db.readableDatabase.rawQuery(
            """SELECT substr(COALESCE(NULLIF(day_key,''),started_at),1,10),source_id,COUNT(*),SUM(COALESCE(pages_viewed,0)),SUM(COALESCE(seconds_elapsed,0)),COUNT(DISTINCT CASE WHEN chapter_id IS NOT NULL AND chapter_id<>'' THEN remote_id || ':' || chapter_id END)
               FROM reading_sessions WHERE profile_id=? AND source_id IN ($placeholders)
               GROUP BY 1,source_id ORDER BY 1 DESC LIMIT 180""".trimIndent(), args.toTypedArray()
        ).use { cursor -> while (cursor.moveToNext()) {
            val date = cursor.getString(0); val item = byDate.getOrPut(date) { JSONObject().put("date", date).put("reads", 0).put("seconds_elapsed", 0L).put("nhentai_pages", 0).put("mangadex_pages", 0).put("mangadex_chapters", 0) }
            item.put("reads", item.optInt("reads") + cursor.getInt(2))
            item.put("seconds_elapsed", item.optLong("seconds_elapsed") + cursor.getLong(4))
            if (cursor.getString(1) == "mangadex") {
                item.put("mangadex_chapters", item.optInt("mangadex_chapters") + cursor.getInt(5))
                item.put("mangadex_pages", item.optInt("mangadex_pages") + cursor.getInt(3))
            }
            else item.put("nhentai_pages", item.optInt("nhentai_pages") + cursor.getInt(3))
        } }
        return JSONObject().put("series", JSONArray(byDate.values.toList().asReversed()))
    }

    private fun bridgeSuggestions(request: HttpRequest): JSONObject {
        val scope = bridgeScope(request)
        val items = JSONArray()
        val seen = linkedSetOf<String>()
        val library = sourceEntries.allEntries(scope.profileId, scope.sourceIds)
        val existing = library.mapTo(hashSetOf()) { it.entry.key.storageKey }
        val activeProfile = scope.profileId == profiles.activeProfileId()
        val mobileRows = if (activeProfile) currentSuggestions.invoke().filter { SourceId(it.sourceId) in scope.sourceIds } else emptyList()
        val mobileScores = mobileRows.associateBy { it.code }
        val mobileSources = if (activeProfile) currentSourceSuggestions.invoke().filter { it.key.uiCode() in mobileScores } else emptyList()
        mobileSources.filter { it.key.sourceId in scope.sourceIds }.forEach { entry ->
            if (entry.key.storageKey !in existing && seen.add(entry.key.storageKey)) {
                items.put(bridgeSourceEntryJson(entry, false, false, 0, false)
                    .put("reason", mobileScores[entry.key.uiCode()]?.whySuggestedReason.orEmpty())
                    .put("score", mobileScores[entry.key.uiCode()]?.score ?: 0f))
            }
        }
        if (scope.profileId == profiles.activeProfileId() && SourceId("nhentai") in scope.sourceIds) {
            val sourceCodes = mobileSources.mapTo(hashSetOf()) { it.key.uiCode() }
            val rows = mobileRows.filter { it.sourceId == "nhentai" && it.code !in sourceCodes }
            rows.forEach { row ->
                val storageKey = "nhentai:${row.code}"
                if (seen.add(storageKey) && storageKey !in existing) items.put(
                    JSONObject().put("source_id", "nhentai").put("remote_id", row.code.toString()).put("display_id", "#${row.code}")
                    .put("title", row.title).put("canonical_url", "https://nhentai.net/g/${row.code}/").put("thumbnail_url", row.thumbnailUrl)
                    .put("unit_count", row.numPages).put("unit_label", "pages").put("score", row.score).put("reason", row.whySuggestedReason).put("in_library", false)
                    .put("read", false).put("rating", 0).put("pinned", false)
                    .put("tags", JSONArray(row.topTags.map { JSONObject().put("name", it).put("type", "tag") }))
                    .put("creators", JSONArray())
                )
            }
        }
        // Missing/cold phone results and non-active profiles use the same taste model/scorer,
        // not a separate popular/tag-only engine. Reading settings must never activate a profile.
        val providersWithRows = mobileRows.mapTo(hashSetOf()) { it.sourceId }
        val missingAdapters = scope.sourceIds.filter { it.value !in providersWithRows }.map(sourceRegistry::requireAdapter)
            .filter { it.supports(SourceCapability.SEARCH) }
        val values = if (activeProfile) com.roinur.saucetracker.core.preferences.SaucePreferences.from(appContext).raw.all
            else com.roinur.saucetracker.data.profile.ProfilePreferenceStore(db).snapshot(scope.profileId)
        val weights = SuggestionWeightCategory.entries.associateWith { category ->
            ((values["suggestion_weight_${category.storageKey}"] as? Number)?.toFloat() ?: 1f).coerceIn(0f, 2f)
        }
        val theme = ((values["suggestion_theme_strength"] as? Number)?.toFloat() ?: 1f).coerceIn(0f, 2f)
        val training = com.roinur.saucetracker.feature.suggestions.TasteTrainingStore.fromSnapshot(values["taste_training_feedback_v1"] as? String ?: "")
        val hidden = com.roinur.saucetracker.data.backup.BackupSnapshotExport.parseHiddenSuggestionCodeList(values["suggestion_hidden_codes"] as? String ?: "").toSet()
        val fallback = kotlinx.coroutines.runBlocking {
            com.roinur.saucetracker.feature.suggestions.collectSourceSuggestions(
                missingAdapters, library, db.readingSessionEntryKeys(scope.profileId, scope.sourceIds.mapTo(linkedSetOf()) { it.value }),
                db.listBlockedPopularTagNames().mapTo(hashSetOf()) { normalizeTagName(it) }, weights, theme,
                training.boundedDriverAdjustments(), SuggestionMode.MIXED, request.query["q"].orEmpty(), emptyList(), hidden, emptySet(), db.tagDao::route
            )
        }
        fallback.candidates.forEach { candidate ->
            val entry = candidate.entry
            if (items.length() < 60 && entry.key.storageKey !in existing && seen.add(entry.key.storageKey)) {
                items.put(bridgeSourceEntryJson(entry, false, false, 0, false)
                    .put("score", candidate.breakdown.score).put("reason", candidate.breakdown.whySuggestedReason))
            }
        }
        return JSONObject().put("items", items).put("message", when {
            fallback.errors.isNotEmpty() -> fallback.errors.entries.joinToString(" · ") { "${it.key}: ${it.value}" }
            items.length() > 0 -> "Suggestions use your phone profile's shared taste model."
            else -> "Read or rate a few entries to build suggestions."
        })
    }

    private fun bridgeBrowserSearch(request: HttpRequest): JSONObject {
        val scope = bridgeScope(request)
        val query = SourceQueryParser.parse(request.query["q"].orEmpty())
        val offset = request.query["offset"]?.toIntOrNull()?.coerceAtLeast(0) ?: 0
        val limit = request.query["limit"]?.toIntOrNull()?.coerceIn(1, 40) ?: 24
        val items = JSONArray()
        val errors = JSONArray()
        var hasMore = false
        scope.sourceIds.sortedBy { it.value }.forEach { sourceId ->
            val adapter = sourceRegistry.requireAdapter(sourceId)
            runCatching { adapter.search(query, offset, limit) }
                .onSuccess { page ->
                    page.entries.forEach { entry ->
                        val state = profiles.state(scope.profileId, entry.key)
                        items.put(bridgeSourceEntryJson(entry, state != null, state?.isRead ?: false, state?.rating ?: 0, state?.pinned ?: false))
                    }
                    hasMore = hasMore || page.hasMore
                }
                .onFailure { error ->
                    errors.put(JSONObject().put("source_id", sourceId.value).put("message", error.message ?: "Provider unavailable."))
                }
        }
        return JSONObject().put("items", items).put("errors", errors).put("has_more", hasMore).put("offset", offset).put("limit", limit)
    }

    private fun bridgeBrowserEntry(request: HttpRequest): JSONObject {
        val scope = bridgeScope(request)
        val key = SourceEntryKey(SourceId(request.query["source_id"].orEmpty()), request.query["remote_id"].orEmpty())
        require(key.sourceId in scope.sourceIds) { "Source is outside the selected profile." }
        val entry = runCatching { sourceRegistry.requireAdapter(key.sourceId).fetchEntry(key.remoteId) }
            .getOrElse { sourceEntries.entry(key) ?: throw it }
        val state = profiles.state(scope.profileId, key)
        return JSONObject().put("entry", bridgeSourceEntryJson(entry, state != null, state?.isRead ?: false, state?.rating ?: 0, state?.pinned ?: false))
    }

    private fun bridgeSourceEntryJson(
        entry: com.roinur.saucetracker.data.source.SourceEntry,
        inLibrary: Boolean,
        read: Boolean,
        rating: Int,
        pinned: Boolean
    ): JSONObject = JSONObject()
        .put("source_id", entry.key.sourceId.value).put("remote_id", entry.key.remoteId).put("display_id", entry.key.displayId)
        .put("title", entry.title).put("canonical_url", entry.canonicalUrl).put("thumbnail_url", entry.thumbnailUrl)
        .put("unit_count", entry.unitCount).put("unit_label", entry.unitLabel).put("status", entry.status)
        .put("in_library", inLibrary).put("read", read).put("rating", rating).put("pinned", pinned)
        .put("tags", JSONArray(entry.tags.map { JSONObject().put("name", it.name).put("type", it.type) }))
        .put("creators", JSONArray(entry.creators.map { JSONObject().put("name", it.name).put("type", it.type).put("url", it.url) }))

    private fun bridgeReaderChapters(request: HttpRequest): JSONObject {
        val scope = bridgeScope(request)
        val key = SourceEntryKey(SourceId(request.query["source_id"].orEmpty()), request.query["remote_id"].orEmpty())
        require(key.sourceId in scope.sourceIds) { "Source is outside the selected profile." }
        val progress = db.sourceChapterProgress(scope.profileId, key.sourceId.value, key.remoteId)
        val saved = db.sourceReaderProgress(scope.profileId, key.sourceId.value, key.remoteId)
        val chapters = if (key.sourceId.value == "nhentai") {
            val entry = sourceEntries.entry(key) ?: sourceRegistry.requireAdapter(key.sourceId).fetchEntry(key.remoteId)
            listOf(com.roinur.saucetracker.data.source.SourceChapter("gallery", "Gallery", pageCount = entry.unitCount, previewUrl = entry.thumbnailUrl))
        } else {
            val adapter = sourceRegistry.requireAdapter(key.sourceId)
            buildList {
                var offset = 0
                repeat(100) {
                    val page = adapter.fetchChapters(key.remoteId, offset, 100)
                    addAll(page.chapters)
                    if (!page.hasMore || page.chapters.isEmpty()) return@buildList
                    offset += page.chapters.size
                }
                throw IllegalArgumentException("This chapter list is too large to load in one request.")
            }.distinctBy { it.id }.sortedWith(
                compareBy<com.roinur.saucetracker.data.source.SourceChapter> { it.number.toDoubleOrNull() ?: Double.NEGATIVE_INFINITY }
                    .thenBy { it.volume.toDoubleOrNull() ?: Double.NEGATIVE_INFINITY }
                    .thenBy { it.publishedAt }
            )
        }
        return JSONObject().put("chapters", JSONArray(chapters.map { chapter ->
            val row = progress[chapter.id]
            JSONObject().put("id", chapter.id).put("label", chapter.label).put("title", chapter.title)
                .put("number", chapter.number).put("volume", chapter.volume).put("language", chapter.language)
                .put("groups", JSONArray(chapter.scanlationGroups)).put("published_at", chapter.publishedAt)
                .put("page_count", maxOf(chapter.pageCount, row?.pageCount ?: 0)).put("preview_url", chapter.previewUrl)
                .put("progress", row?.fraction ?: 0f).put("completed", row?.completed ?: false)
        })).put("resume_chapter_id", saved?.chapterId ?: "").put("resume_page_index", saved?.pageIndex ?: 0)
    }

    private fun bridgeReaderManifest(request: HttpRequest): JSONObject {
        val scope = bridgeScope(request)
        val key = SourceEntryKey(SourceId(request.query["source_id"].orEmpty()), request.query["remote_id"].orEmpty())
        require(key.sourceId in scope.sourceIds) { "Source is outside the selected profile." }
        val requestedChapter = request.query["chapter_id"].orEmpty()
        val chapterId: String
        val title: String
        val candidates: List<List<String>>
        if (key.sourceId.value == "nhentai") {
            val code = key.remoteId.toIntOrNull()?.takeIf { it > 0 } ?: throw IllegalArgumentException("Invalid NHentai code.")
            val gallery = client.fetchGallery(code)
            chapterId = "gallery"
            title = gallery.title
            val preferred = gallery.coverExt.lowercase(Locale.US).ifBlank { "jpg" }.let { if (it == "jpeg") "jpg" else it }
            val extensions = listOf(preferred, "jpg", "png", "webp").distinct()
            candidates = (1..gallery.numPages.coerceAtLeast(0)).map { page -> extensions.map { ext -> "https://i.nhentai.net/galleries/${gallery.mediaId}/$page.$ext" } }
        } else {
            val reader = sourceRegistry.requireAdapter(key.sourceId).fetchReaderContent(key.remoteId, requestedChapter.ifBlank { null })
            chapterId = reader.chapterId
            title = reader.displayTitle
            candidates = reader.pageUrls.indices.map { index ->
                listOfNotNull(reader.dataSaverPageUrls.getOrNull(index), reader.pageUrls.getOrNull(index)).distinct()
            }
        }
        require(candidates.isNotEmpty()) { "No reader pages are available." }
        val sessionId = generateToken()
        val now = System.currentTimeMillis()
        readerSessions.entries.removeIf { now - it.value.createdAtMs > 30L * 60L * 1000L }
        readerSessions[sessionId] = BridgeReaderSession(normalizeClientAddress(request.remoteAddress), candidates, now)
        val resume = db.sourceReaderProgress(scope.profileId, key.sourceId.value, key.remoteId)
        val chapterProgress = db.sourceChapterProgress(scope.profileId, key.sourceId.value, key.remoteId)[chapterId]
        val startIndex = (if (resume?.chapterId == chapterId) resume.pageIndex else chapterProgress?.furthestPageIndex ?: 0)
            .coerceIn(0, candidates.lastIndex)
        return JSONObject().put("reader_session_id", sessionId).put("chapter_id", chapterId).put("title", title)
            .put("page_count", candidates.size).put("start_page_index", startIndex)
    }

    private fun bridgeReaderPage(request: HttpRequest): HttpResponse {
        val session = readerSessions[request.query["reader_session_id"].orEmpty()]
            ?: return jsonResponse(404, JSONObject().put("ok", false).put("error", "Reader session expired."))
        if (session.clientAddress != normalizeClientAddress(request.remoteAddress)) {
            return jsonResponse(403, JSONObject().put("ok", false).put("error", "Reader session belongs to another client."))
        }
        val index = request.query["index"]?.toIntOrNull() ?: -1
        val urls = session.pageCandidates.getOrNull(index)
            ?: return jsonResponse(404, JSONObject().put("ok", false).put("error", "Reader page not found."))
        urls.forEach { url ->
            val response = runCatching { readerHttp.newCall(Request.Builder().url(url).header("Accept", "image/avif,image/webp,image/*,*/*").build()).execute() }.getOrNull() ?: return@forEach
            response.use {
                if (it.isSuccessful) {
                    val bytes = it.body?.bytes() ?: ByteArray(0)
                    if (bytes.isNotEmpty()) return HttpResponse(200, "OK", it.header("Content-Type")?.substringBefore(';') ?: "image/jpeg", bytes)
                }
            }
        }
        return jsonResponse(502, JSONObject().put("ok", false).put("error", "Could not load reader page."))
    }

    private fun bridgeRemoteImage(request: HttpRequest): HttpResponse {
        val rawUrl = request.query["url"].orEmpty().trim()
        val uri = runCatching { Uri.parse(rawUrl) }.getOrNull()
            ?: return jsonResponse(400, JSONObject().put("ok", false).put("error", "Invalid image URL."))
        val host = uri.host?.lowercase(Locale.US).orEmpty()
        val allowed = uri.scheme.equals("https", ignoreCase = true) && (
            host == "uploads.mangadex.org" || host == "mangadex.org" ||
                host == "nhentai.net" || host.endsWith(".nhentai.net")
            )
        if (!allowed) return jsonResponse(403, JSONObject().put("ok", false).put("error", "Image host is not allowed."))
        val builder = Request.Builder().url(rawUrl).header("Accept", "image/avif,image/webp,image/*,*/*")
        if (host == "nhentai.net" || host.endsWith(".nhentai.net")) builder.header("Referer", "https://nhentai.net/")
        val response = runCatching { readerHttp.newCall(builder.build()).execute() }.getOrNull()
            ?: return jsonResponse(502, JSONObject().put("ok", false).put("error", "Could not load image."))
        response.use {
            if (!it.isSuccessful) return jsonResponse(502, JSONObject().put("ok", false).put("error", "Image provider returned ${it.code}."))
            val bytes = it.body?.bytes() ?: ByteArray(0)
            if (bytes.isEmpty()) return jsonResponse(502, JSONObject().put("ok", false).put("error", "Image was empty."))
            return HttpResponse(200, "OK", it.header("Content-Type")?.substringBefore(';') ?: "image/jpeg", bytes)
        }
    }

    private fun resolvedAutoAccent(dark: Boolean): String {
        val argb = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (dark) dynamicDarkColorScheme(appContext).primary.toArgb()
            else dynamicLightColorScheme(appContext).primary.toArgb()
        } else if (dark) 0xFF8BC1FF.toInt() else 0xFF1F63D8.toInt()
        return String.format(Locale.US, "#%06X", argb and 0xFFFFFF)
    }

    private fun bridgeReaderProgress(rawBody: String): JSONObject {
        val body = parseJson(rawBody) ?: throw IllegalArgumentException("Invalid JSON body.")
        val profileId = body.optString("profile_id")
        val key = SourceEntryKey(SourceId(body.optString("source_id")), body.optString("remote_id"))
        val chapterId = body.optString("chapter_id").ifBlank { "gallery" }
        require(profiles.profile(profileId)?.sourceIds?.contains(key.sourceId) == true && profiles.state(profileId, key) != null) { "Entry is outside this profile." }
        val pageCount = body.optInt("page_count", 0).coerceAtLeast(0)
        val pageIndex = body.optInt("page_index", 0).coerceIn(0, (pageCount - 1).coerceAtLeast(0))
        db.saveSourceReaderProgress(profileId, key.sourceId.value, key.remoteId, chapterId, pageIndex, pageCount)
        val completed = body.optBoolean("completed", false)
        if (completed) db.markSourceChapterCompleted(profileId, key.sourceId.value, key.remoteId, chapterId)
        val clientSessionId = body.optString("client_session_id").trim()
        var recorded = false
        if (pageCount > 0 && body.optInt("pages_viewed", 0) > 0 && clientSessionId.matches(Regex("[A-Za-z0-9_-]{8,80}"))) {
            val seconds = body.optLong("seconds_elapsed", 1L).coerceIn(1L, 24L * 60L * 60L)
            val viewed = body.optInt("pages_viewed", 1).coerceIn(1, pageCount.coerceAtLeast(1))
            val end = System.currentTimeMillis()
            val start = body.optLong("started_at_ms", end - seconds * 1000L).coerceIn(end - 24L * 60L * 60L * 1000L, end)
            db.insertReadingSession(key.uiCode(), start, end, viewed, seconds, profileId = profileId,
                sourceId = key.sourceId.value, remoteId = key.remoteId, chapterId = chapterId,
                checkpointSessionKey = "bridge:$profileId:${key.sourceId.value}:${key.remoteId}:$chapterId:$clientSessionId")
            recorded = true
            if (body.optBoolean("finalize", false) || end - lastReaderNotificationMs >= 10_000L) {
                lastReaderNotificationMs = end
                onDataChanged.invoke()
            }
        }
        return JSONObject().put("saved", true).put("recorded", recorded)
    }

    private fun bridgeUpdateState(rawBody: String): JSONObject {
        val body = parseJson(rawBody) ?: throw IllegalArgumentException("Invalid JSON body.")
        val profileId = body.optString("profile_id"); val key = SourceEntryKey(SourceId(body.optString("source_id")), body.optString("remote_id"))
        val profile = profiles.profile(profileId) ?: throw IllegalArgumentException("Unknown profile.")
        require(key.sourceId in profile.sourceIds && profiles.state(profileId, key) != null) { "Entry is outside this profile." }
        val changed = sourceEntries.updateState(profileId, key,
            read = body.optBoolean("read").takeIf { body.has("read") }, rating = body.optInt("rating").takeIf { body.has("rating") },
            pinned = body.optBoolean("pinned").takeIf { body.has("pinned") })
        if (changed) onDataChanged.invoke()
        return JSONObject().put("updated", changed)
    }

    private fun bridgeRemoveEntry(rawBody: String): JSONObject {
        val body = parseJson(rawBody) ?: throw IllegalArgumentException("Invalid JSON body.")
        val profileId = body.optString("profile_id"); val key = SourceEntryKey(SourceId(body.optString("source_id")), body.optString("remote_id"))
        require(profiles.profile(profileId)?.sourceIds?.contains(key.sourceId) == true) { "Entry is outside this profile." }
        val removed = sourceEntries.removeMembership(profileId, key)
        if (removed) onDataChanged.invoke()
        return JSONObject().put("removed", removed)
    }

    private fun bridgeImport(rawBody: String): JSONObject {
        val body = parseJson(rawBody) ?: throw IllegalArgumentException("Invalid JSON body.")
        val profileId = body.optString("profile_id"); val profile = profiles.profile(profileId) ?: throw IllegalArgumentException("Unknown profile.")
        val inputs = body.optJSONArray("inputs") ?: JSONArray(); require(inputs.length() in 1..100) { "Provide between 1 and 100 inputs." }
        val results = JSONArray()
        for (index in 0 until inputs.length()) {
            val input = inputs.optString(index).trim(); val adapter = sourceRegistry.recognize(input)
            if (adapter == null || adapter.id !in profile.sourceIds) { results.put(JSONObject().put("input", input).put("status", "unsupported")); continue }
            val remoteId = adapter.normalizeRemoteId(input)
            if (remoteId == null) { results.put(JSONObject().put("input", input).put("status", "unsupported")); continue }
            runCatching { adapter.fetchEntry(remoteId) }.onSuccess { entry ->
                val added = sourceEntries.upsert(entry, profileId); results.put(JSONObject().put("input", input).put("status", if (added) "added" else "updated"))
            }.onFailure { error -> results.put(JSONObject().put("input", input).put("status", "failed").put("error", error.message ?: "Provider error")) }
        }
        onDataChanged.invoke()
        return JSONObject().put("results", results)
    }

    private fun v2Response(request: HttpRequest, payload: JSONObject): HttpResponse {
        payload.put("ok", true).put("api_version", 2)
        val key = clientCryptoKey(request.remoteAddress)
        return if (key == null) jsonResponse(200, payload.put("enc", false)) else encryptJsonResponse(200, payload, key)
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

    private fun assetResponse(path: String, contentType: String, activeToken: String? = null): HttpResponse {
        val bytes = runCatching { appContext.assets.open(path).use(InputStream::readBytes) }.getOrNull()
            ?: return HttpResponse(404, "Not Found", "text/plain; charset=utf-8", "Not found.".toByteArray())
        val payload = if (activeToken == null) bytes else {
            val safeToken = activeToken.replace("\\", "\\\\").replace("'", "\\'")
            bytes.toString(Charsets.UTF_8).replace("__SAUCE_TOKEN__", safeToken).toByteArray(Charsets.UTF_8)
        }
        return HttpResponse(200, "OK", contentType, payload)
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

}
