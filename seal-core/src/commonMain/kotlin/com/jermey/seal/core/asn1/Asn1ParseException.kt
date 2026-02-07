package com.jermey.seal.core.asn1

/**
 * Exception thrown when ASN.1 DER parsing encounters invalid or unsupported data.
 */
public class Asn1ParseException(message: String) : Exception(message)
