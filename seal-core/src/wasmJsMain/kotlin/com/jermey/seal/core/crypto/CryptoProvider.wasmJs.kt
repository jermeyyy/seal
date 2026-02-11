package com.jermey.seal.core.crypto

public actual fun createCryptoVerifier(): CryptoVerifier =
    throw UnsupportedOperationException(
        "CT cryptographic verification is not supported in browser environments. " +
            "Browsers handle Certificate Transparency natively."
    )
