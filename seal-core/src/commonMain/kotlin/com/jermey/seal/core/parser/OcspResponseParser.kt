package com.jermey.seal.core.parser

import com.jermey.seal.core.asn1.Asn1Element
import com.jermey.seal.core.asn1.Asn1Parser
import com.jermey.seal.core.asn1.Asn1Tag
import com.jermey.seal.core.asn1.Oid
import com.jermey.seal.core.model.Origin
import com.jermey.seal.core.model.SignedCertificateTimestamp

internal object OcspResponseParser {
    private val SCT_LIST_OID: Oid = Oid.fromDotNotation("1.3.6.1.4.1.11129.2.4.5")

    fun extractScts(ocspResponseBytes: ByteArray): List<SignedCertificateTimestamp> {
        return try {
            val ocspResponse = Asn1Parser.parse(ocspResponseBytes)
            val responseBytes = ocspResponse.findChild(Asn1Tag.contextSpecific(0, constructed = true))
                ?: return emptyList()
            val responseBytesSeq = responseBytes.findChild(Asn1Tag.SEQUENCE)
                ?: return emptyList()
            val responseOctetString = responseBytesSeq.childAt(1)
                ?: return emptyList()
            val basicOcspResponse = Asn1Parser.parse(responseOctetString.rawValue)
            val tbsResponseData = basicOcspResponse.childAt(0)
                ?: return emptyList()
            val allScts = mutableListOf<SignedCertificateTimestamp>()
            val responseExtensions = tbsResponseData.findChild(Asn1Tag.contextSpecific(1, constructed = true))
            if (responseExtensions != null) {
                allScts.addAll(extractSctsFromExtensions(responseExtensions))
            }
            val responsesSeq = findResponsesSequence(tbsResponseData)
            if (responsesSeq != null) {
                for (singleResponse in responsesSeq.children) {
                    if (singleResponse.tag != Asn1Tag.SEQUENCE) continue
                    val singleExtensions = singleResponse.findChild(Asn1Tag.contextSpecific(1, constructed = true))
                    if (singleExtensions != null) {
                        allScts.addAll(extractSctsFromExtensions(singleExtensions))
                    }
                }
            }
            allScts
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun findResponsesSequence(tbsResponseData: Asn1Element): Asn1Element? {
        for (child in tbsResponseData.children) {
            if (child.tag == Asn1Tag.SEQUENCE && child.children.isNotEmpty() &&
                child.children.all { it.tag == Asn1Tag.SEQUENCE }
            ) {
                return child
            }
        }
        return null
    }

    private fun extractSctsFromExtensions(extensionsWrapper: Asn1Element): List<SignedCertificateTimestamp> {
        val extensionsSeq = extensionsWrapper.findChild(Asn1Tag.SEQUENCE)
            ?: return emptyList()
        for (extension in extensionsSeq.children) {
            if (extension.tag != Asn1Tag.SEQUENCE) continue
            val oidElement = extension.findChild(Asn1Tag.OID) ?: continue
            val oid = Oid.fromDer(oidElement.rawValue)
            if (oid == SCT_LIST_OID) {
                val valueOctetString = extension.findChild(Asn1Tag.OCTET_STRING) ?: continue
                return try {
                    val inner = Asn1Parser.parse(valueOctetString.rawValue)
                    if (inner.tag == Asn1Tag.OCTET_STRING) {
                        SctListParser.parse(inner.rawValue, Origin.OCSP_RESPONSE)
                    } else {
                        SctListParser.parse(valueOctetString.rawValue, Origin.OCSP_RESPONSE)
                    }
                } catch (_: Exception) {
                    emptyList()
                }
            }
        }
        return emptyList()
    }
}
