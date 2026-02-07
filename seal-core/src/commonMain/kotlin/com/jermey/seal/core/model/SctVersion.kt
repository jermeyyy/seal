package com.jermey.seal.core.model

public enum class SctVersion(public val value: Int) {
    V1(0);

    public companion object {
        public fun fromValue(value: Int): SctVersion? = entries.find { it.value == value }
    }
}
