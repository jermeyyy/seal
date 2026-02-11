package com.jermey.seal.jvm.chain

import java.security.cert.X509Certificate

internal object CertificateChainCleaner {
    fun clean(chain: List<X509Certificate>): List<X509Certificate> {
        if (chain.size <= 1) return chain
        return try {
            val remaining = chain.toMutableList()
            val leaf = findLeaf(remaining) ?: return chain
            remaining.remove(leaf)
            val ordered = mutableListOf(leaf)
            while (remaining.isNotEmpty()) {
                val current = ordered.last()
                val issuer = remaining.firstOrNull { candidate ->
                    current.issuerX500Principal == candidate.subjectX500Principal
                } ?: break
                ordered.add(issuer)
                remaining.remove(issuer)
            }
            ordered
        } catch (_: Exception) {
            chain
        }
    }

    private fun findLeaf(chain: List<X509Certificate>): X509Certificate? {
        val issuers = chain.map { it.issuerX500Principal }.toSet()
        val leaf = chain.firstOrNull { cert ->
            cert.subjectX500Principal !in issuers || isSelfSigned(cert)
        }
        if (leaf != null && !isSelfSigned(leaf)) return leaf
        return chain.firstOrNull { !isSelfSigned(it) } ?: chain.firstOrNull()
    }

    private fun isSelfSigned(cert: X509Certificate): Boolean =
        cert.subjectX500Principal == cert.issuerX500Principal
}
