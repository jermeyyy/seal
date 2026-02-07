package com.jermey.seal.android.chain

import java.security.cert.X509Certificate

/**
 * Cleans and orders a certificate chain from leaf to root.
 *
 * OkHttp may deliver certificate chains in arbitrary order.
 * The CT verifier requires a properly ordered chain: leaf → intermediate(s) → root.
 */
internal object CertificateChainCleaner {

    /**
     * Order a certificate chain from leaf to root.
     *
     * @param chain The unordered certificate chain.
     * @return The ordered chain: leaf first, root (or closest to root) last.
     *         Returns the original chain if ordering fails.
     */
    fun clean(chain: List<X509Certificate>): List<X509Certificate> {
        if (chain.size <= 1) return chain

        return try {
            val remaining = chain.toMutableList()

            // Find the leaf: a certificate whose subject is not the issuer of any other cert
            val leaf = findLeaf(remaining) ?: return chain
            remaining.remove(leaf)

            val ordered = mutableListOf(leaf)

            // Build the chain by matching issuer → subject
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

    /**
     * Find the leaf certificate in the chain.
     * The leaf is the certificate that is NOT the issuer of any other certificate in the chain.
     * If that heuristic fails, we try to find the non-self-signed cert.
     */
    private fun findLeaf(chain: List<X509Certificate>): X509Certificate? {
        // A leaf cert's subject should not appear as an issuer of another cert in the chain
        val issuers = chain.map { it.issuerX500Principal }.toSet()
        val leaf = chain.firstOrNull { cert ->
            cert.subjectX500Principal !in issuers || isSelfSigned(cert)
        }
        if (leaf != null && !isSelfSigned(leaf)) return leaf

        // Fallback: first non-self-signed certificate
        return chain.firstOrNull { !isSelfSigned(it) } ?: chain.firstOrNull()
    }

    private fun isSelfSigned(cert: X509Certificate): Boolean =
        cert.subjectX500Principal == cert.issuerX500Principal
}
