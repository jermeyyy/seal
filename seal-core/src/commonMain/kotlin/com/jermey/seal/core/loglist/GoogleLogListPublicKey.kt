package com.jermey.seal.core.loglist

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Google's public key for verifying CT log list signatures.
 * This is the ECDSA P-256 key used to sign log_list.json.
 *
 * Source: https://www.gstatic.com/ct/log_list/v3/log_list_pubkey.pem
 */
internal object GoogleLogListPublicKey {

    /**
     * DER-encoded SubjectPublicKeyInfo for Google's log list signing key.
     */
    @OptIn(ExperimentalEncodingApi::class)
    val keyBytes: ByteArray by lazy {
        Base64.decode(
            "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE" +
                "xv4mMFVPmbCUiycuaLMVJPr5tFXjMqG0Ky1B" +
                "nuo7YVBrGPcJHJbmzmvMudJl3yczFaOXzOEf" +
                "jSgFMUdizY5xw=="
        )
    }
}
