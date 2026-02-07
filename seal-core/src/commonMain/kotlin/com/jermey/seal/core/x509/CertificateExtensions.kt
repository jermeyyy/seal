package com.jermey.seal.core.x509

import com.jermey.seal.core.asn1.Oid

/**
 * CT-relevant X.509 extension OIDs.
 */
public object CertificateExtensions {
    /** OID for embedded SCT list extension (RFC 6962). */
    public val SCT_EXTENSION_OID: Oid = Oid.fromDotNotation("1.3.6.1.4.1.11129.2.4.2")

    /** OID for precertificate poison extension (RFC 6962). */
    public val PRECERT_POISON_OID: Oid = Oid.fromDotNotation("1.3.6.1.4.1.11129.2.4.3")

    /** OID for precertificate signing certificate EKU (RFC 6962). */
    public val PRECERT_SIGNING_CERT_OID: Oid = Oid.fromDotNotation("1.3.6.1.4.1.11129.2.4.4")
}
