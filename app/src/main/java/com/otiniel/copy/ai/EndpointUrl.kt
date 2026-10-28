package com.otiniel.copy.ai

import java.net.URI

/** Accepts a provider base URL or full chat-completions URL and returns a canonical URL. */
object EndpointUrl {
    fun chatCompletions(raw: String): String {
        val input = raw.trim()
        require(input.isNotEmpty()) { "Informe o endpoint HTTPS do provedor." }

        val uri = try {
            URI(input)
        } catch (_: Exception) {
            throw IllegalArgumentException("O endpoint não é uma URL válida.")
        }

        require(uri.scheme.equals("https", ignoreCase = true)) {
            "Use um endpoint HTTPS. Conexões HTTP sem criptografia não são aceitas."
        }
        require(!uri.host.isNullOrBlank()) { "O endpoint precisa ter um host válido." }
        require(uri.userInfo == null) { "Não coloque credenciais dentro da URL." }
        require(uri.query == null && uri.fragment == null) {
            "O endpoint não pode conter parâmetros de consulta nem fragmento."
        }

        val path = uri.path.orEmpty().trimEnd('/')
        val normalizedPath = when {
            path.endsWith("/chat/completions", ignoreCase = true) -> path
            path.endsWith("/v1", ignoreCase = true) -> path + "/chat/completions"
            path.isBlank() -> "/v1/chat/completions"
            else -> path + "/v1/chat/completions"
        }

        return URI("https", null, uri.host, uri.port, normalizedPath, null, null).toASCIIString()
    }
}
