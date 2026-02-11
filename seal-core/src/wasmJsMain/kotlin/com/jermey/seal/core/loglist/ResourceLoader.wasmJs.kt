package com.jermey.seal.core.loglist

public actual object ResourceLoader {
    public actual fun loadResource(name: String): ByteArray {
        throw UnsupportedOperationException(
            "Resource loading is not supported in browser environments. " +
                "Browsers handle Certificate Transparency natively."
        )
    }
}
