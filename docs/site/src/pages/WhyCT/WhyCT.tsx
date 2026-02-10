import { Link } from 'react-router-dom'
import styles from './WhyCT.module.css'

export default function WhyCT() {
  return (
    <article className={styles.whyCT}>
      <h1>Why Certificate Transparency?</h1>
      <p className={styles.intro}>
        How can you trust that a TLS certificate was legitimately issued? Certificates can
        be misissued by compromised or rogue Certificate Authorities, and without transparency,
        these incidents go undetected — leaving users vulnerable to man-in-the-middle attacks.
        Certificate Transparency (CT) solves this problem at an ecosystem level.
      </p>

      {/* ── Section 1: How the Web PKI Works ─────────────────────────── */}
      <section>
        <h2 id="web-pki">How the Web PKI Works</h2>
        <p>
          The Web Public Key Infrastructure (PKI) underpins secure communication on the internet.
          When you visit a website over HTTPS, your browser verifies the site's identity through a
          chain of trust anchored in Certificate Authorities (CAs).
        </p>
        <p>
          A <strong>Certificate Authority</strong> is an organization trusted by browsers and operating
          systems to issue digital certificates. The process works as follows: a domain owner generates
          a key pair, submits a Certificate Signing Request (CSR) to a CA, the CA validates domain
          ownership, and then issues a signed certificate. Browsers trust certificates signed by CAs
          whose root certificates are included in their trust store.
        </p>

        <div className={styles.diagram}>
          <svg viewBox="0 0 720 160" xmlns="http://www.w3.org/2000/svg" role="img" aria-label="Web PKI certificate chain diagram">
            {/* Domain Owner */}
            <rect x="10" y="50" width="140" height="60" rx="10" ry="10"
              fill="var(--color-surface-raised)" stroke="var(--color-primary)" strokeWidth="2" />
            <text x="80" y="77" textAnchor="middle" fill="var(--color-text-primary)" fontSize="14" fontWeight="600">Domain</text>
            <text x="80" y="95" textAnchor="middle" fill="var(--color-text-secondary)" fontSize="11">Owner</text>

            {/* Arrow 1 */}
            <line x1="150" y1="80" x2="200" y2="80" stroke="var(--color-text-secondary)" strokeWidth="2" markerEnd="url(#arrowhead)" />
            <text x="175" y="70" textAnchor="middle" fill="var(--color-text-secondary)" fontSize="10">CSR</text>

            {/* CA */}
            <rect x="200" y="50" width="140" height="60" rx="10" ry="10"
              fill="var(--color-surface-raised)" stroke="var(--color-primary)" strokeWidth="2" />
            <text x="270" y="77" textAnchor="middle" fill="var(--color-text-primary)" fontSize="14" fontWeight="600">Certificate</text>
            <text x="270" y="95" textAnchor="middle" fill="var(--color-text-secondary)" fontSize="11">Authority (CA)</text>

            {/* Arrow 2 */}
            <line x1="340" y1="80" x2="390" y2="80" stroke="var(--color-text-secondary)" strokeWidth="2" markerEnd="url(#arrowhead)" />
            <text x="365" y="70" textAnchor="middle" fill="var(--color-text-secondary)" fontSize="10">Signs</text>

            {/* Certificate */}
            <rect x="390" y="50" width="140" height="60" rx="10" ry="10"
              fill="var(--color-surface-raised)" stroke="var(--color-primary)" strokeWidth="2" />
            <text x="460" y="77" textAnchor="middle" fill="var(--color-text-primary)" fontSize="14" fontWeight="600">Certificate</text>
            <text x="460" y="95" textAnchor="middle" fill="var(--color-text-secondary)" fontSize="11">(X.509)</text>

            {/* Arrow 3 */}
            <line x1="530" y1="80" x2="580" y2="80" stroke="var(--color-text-secondary)" strokeWidth="2" markerEnd="url(#arrowhead)" />
            <text x="555" y="70" textAnchor="middle" fill="var(--color-text-secondary)" fontSize="10">Verifies</text>

            {/* Browser */}
            <rect x="580" y="50" width="130" height="60" rx="10" ry="10"
              fill="var(--color-surface-raised)" stroke="var(--color-primary)" strokeWidth="2" />
            <text x="645" y="77" textAnchor="middle" fill="var(--color-text-primary)" fontSize="14" fontWeight="600">Browser /</text>
            <text x="645" y="95" textAnchor="middle" fill="var(--color-text-secondary)" fontSize="11">Client</text>

            {/* Trust Store */}
            <rect x="580" y="5" width="130" height="35" rx="8" ry="8"
              fill="var(--color-surface-raised)" stroke="var(--color-border)" strokeWidth="1.5" strokeDasharray="4 2" />
            <text x="645" y="27" textAnchor="middle" fill="var(--color-text-secondary)" fontSize="11">Trust Store</text>
            <line x1="645" y1="40" x2="645" y2="50" stroke="var(--color-border)" strokeWidth="1.5" strokeDasharray="3 2" />

            <defs>
              <marker id="arrowhead" markerWidth="8" markerHeight="6" refX="8" refY="3" orient="auto">
                <polygon points="0 0, 8 3, 0 6" fill="var(--color-text-secondary)" />
              </marker>
            </defs>
          </svg>
          <div className={styles.diagramCaption}>
            The Web PKI trust chain: domain owners request certificates from CAs, which browsers verify against a built-in trust store.
          </div>
        </div>
      </section>

      {/* ── Section 2: The Problem — Certificate Misissuance ─────────── */}
      <section>
        <h2 id="misissuance">The Problem: Certificate Misissuance</h2>
        <p>
          The trust model described above has a critical weakness: it relies entirely on CAs behaving
          correctly. If a CA is compromised, coerced, or simply makes an error, it can issue a
          certificate for <strong>any</strong> domain — and there is no built-in mechanism for domain
          owners or the public to detect it.
        </p>
        <p>
          This is not a theoretical risk. Notable real-world incidents include:
        </p>
        <ul>
          <li>
            <strong>DigiNotar (2011)</strong> — A Dutch CA was fully compromised. Attackers issued
            fraudulent certificates for <code>*.google.com</code> and hundreds of other domains.
            The certificates were used for MitM attacks against users in Iran. DigiNotar was
            subsequently removed from all trust stores and went bankrupt.
          </li>
          <li>
            <strong>Symantec (2015–2017)</strong> — Google discovered that Symantec had misissued
            over 30,000 certificates, including certificates that violated the Baseline Requirements.
            Symantec's CA business was eventually transferred and their roots distrusted by browsers.
          </li>
          <li>
            <strong>CNNIC (2015)</strong> — The China Internet Network Information Center issued an
            intermediate CA certificate to an organization that used it to issue unauthorized
            certificates. Google and Mozilla removed CNNIC from their trust stores.
          </li>
        </ul>

        <div className={styles.highlight}>
          <p>
            <strong>Key insight:</strong> Without a public transparency mechanism, misissued certificates
            can exist and be actively exploited for weeks, months, or even years before detection. Domain
            owners have no way to know that a certificate has been issued in their name unless they
            happen to stumble across it.
          </p>
        </div>
      </section>

      {/* ── Section 3: SSL/Certificate Pinning ───────────────────────── */}
      <section>
        <h2 id="ssl-pinning">Traditional Approach: SSL/Certificate Pinning</h2>
        <p>
          One early attempt to mitigate certificate misissuance is <strong>certificate pinning</strong> (also
          called SSL pinning or public key pinning). The idea is straightforward: an application hardcodes
          the expected certificate or public key hash for a given domain. During the TLS handshake, the
          app compares the server's certificate against the pinned value and rejects the connection if
          they do not match.
        </p>

        <div className={styles.diagram}>
          <svg viewBox="0 0 620 240" xmlns="http://www.w3.org/2000/svg" role="img" aria-label="SSL pinning flow diagram">
            {/* App */}
            <rect x="10" y="15" width="120" height="50" rx="10" ry="10"
              fill="var(--color-surface-raised)" stroke="var(--color-primary)" strokeWidth="2" />
            <text x="70" y="36" textAnchor="middle" fill="var(--color-text-primary)" fontSize="11" fontWeight="600">Mobile App</text>
            <text x="70" y="50" textAnchor="middle" fill="var(--color-text-secondary)" fontSize="9">(hardcoded pins)</text>

            {/* Arrow 1: App → Server (TLS Handshake) */}
            <line x1="130" y1="33" x2="270" y2="33" stroke="var(--color-text-secondary)" strokeWidth="2" markerEnd="url(#arrowhead-pin)" />
            <circle cx="200" cy="33" r="8" fill="var(--color-primary)" />
            <text x="200" y="37" textAnchor="middle" fill="white" fontSize="9" fontWeight="700">1</text>
            <text x="200" y="22" textAnchor="middle" fill="var(--color-text-secondary)" fontSize="8">TLS Handshake</text>

            {/* Server */}
            <rect x="270" y="15" width="120" height="50" rx="10" ry="10"
              fill="var(--color-surface-raised)" stroke="var(--color-primary)" strokeWidth="2" />
            <text x="330" y="36" textAnchor="middle" fill="var(--color-text-primary)" fontSize="11" fontWeight="600">Server</text>
            <text x="330" y="50" textAnchor="middle" fill="var(--color-text-secondary)" fontSize="9">(presents cert)</text>

            {/* Arrow 2: Server → App (cert received, dashed) */}
            <path d="M 330 65 L 330 95 L 70 95 L 70 65" fill="none" stroke="var(--color-text-secondary)" strokeWidth="2" markerEnd="url(#arrowhead-pin)" strokeDasharray="6 3" />
            <circle cx="200" cy="95" r="8" fill="var(--color-primary)" />
            <text x="200" y="99" textAnchor="middle" fill="white" fontSize="9" fontWeight="700">2</text>
            <text x="200" y="84" textAnchor="middle" fill="var(--color-text-secondary)" fontSize="8">Cert received</text>

            {/* Arrow 3: App → Decision (compare pin) */}
            <path d="M 70 100 L 70 145 L 240 145" fill="none" stroke="var(--color-text-secondary)" strokeWidth="2" markerEnd="url(#arrowhead-pin)" />
            <circle cx="155" cy="145" r="8" fill="var(--color-primary)" />
            <text x="155" y="149" textAnchor="middle" fill="white" fontSize="9" fontWeight="700">3</text>
            <text x="155" y="134" textAnchor="middle" fill="var(--color-text-secondary)" fontSize="8">Compare pin</text>

            {/* Decision diamond */}
            <polygon points="300,115 360,145 300,175 240,145"
              fill="var(--color-surface-raised)" stroke="var(--color-primary)" strokeWidth="2" />
            <text x="300" y="142" textAnchor="middle" fill="var(--color-text-primary)" fontSize="10" fontWeight="600">Pin</text>
            <text x="300" y="154" textAnchor="middle" fill="var(--color-text-primary)" fontSize="10" fontWeight="600">Match?</text>

            {/* Yes path */}
            <line x1="360" y1="145" x2="420" y2="145" stroke="#38a169" strokeWidth="2" markerEnd="url(#arrowhead-green)" />
            <text x="390" y="137" textAnchor="middle" fill="#38a169" fontSize="8" fontWeight="600">Yes</text>
            <rect x="420" y="127" width="55" height="36" rx="8" ry="8"
              fill="var(--color-surface-raised)" stroke="#38a169" strokeWidth="2" />
            <text x="447" y="149" textAnchor="middle" fill="#38a169" fontSize="10" fontWeight="600">Allow</text>

            {/* No path */}
            <line x1="300" y1="175" x2="300" y2="205" stroke="#e53e3e" strokeWidth="2" markerEnd="url(#arrowhead-red)" />
            <text x="315" y="195" fill="#e53e3e" fontSize="8" fontWeight="600">No</text>
            <rect x="265" y="205" width="70" height="25" rx="8" ry="8"
              fill="var(--color-surface-raised)" stroke="#e53e3e" strokeWidth="2" />
            <text x="300" y="222" textAnchor="middle" fill="#e53e3e" fontSize="10" fontWeight="600">Block</text>

            <defs>
              <marker id="arrowhead-pin" markerWidth="8" markerHeight="6" refX="8" refY="3" orient="auto">
                <polygon points="0 0, 8 3, 0 6" fill="var(--color-text-secondary)" />
              </marker>
              <marker id="arrowhead-green" markerWidth="8" markerHeight="6" refX="8" refY="3" orient="auto">
                <polygon points="0 0, 8 3, 0 6" fill="#38a169" />
              </marker>
              <marker id="arrowhead-red" markerWidth="8" markerHeight="6" refX="8" refY="3" orient="auto">
                <polygon points="0 0, 8 3, 0 6" fill="#e53e3e" />
              </marker>
            </defs>
          </svg>
          <div className={styles.diagramCaption}>
            SSL pinning flow: the app compares the server's certificate against a hardcoded pin and blocks the connection if they do not match.
          </div>
        </div>

        <p>
          While pinning can prevent some MitM attacks, it introduces severe operational risks:
        </p>

        <h3 id="pinning-problems">Problems with Certificate Pinning</h3>
        <ul className={styles.problemList}>
          <li>
            <strong>App bricking risk:</strong> If the pinned certificate expires, is rotated, or the CA
            changes, the app cannot connect to its own servers. This requires an emergency app update
            that users may not install in time.
          </li>
          <li>
            <strong>Requires app updates for pin changes:</strong> Every certificate rotation requires
            shipping a new version of the application with updated pins.
          </li>
          <li>
            <strong>Emergency renewals can brick the fleet:</strong> If a certificate must be replaced
            urgently (e.g., due to a key compromise), all deployed app versions become non-functional.
          </li>
          <li>
            <strong>Bypass via reverse engineering:</strong> Attackers can decompile the app, remove
            pinning logic, and repackage it. Tools like Frida and Objection automate this process.
          </li>
          <li>
            <strong>Purely client-side validation:</strong> All verification logic runs on the client,
            giving attackers full control over the execution environment.
          </li>
          <li>
            <strong>High operational burden:</strong> Maintaining pin lists, coordinating rotations, and
            handling backup pins across multiple platforms creates significant ongoing cost.
          </li>
          <li>
            <strong>Does not detect misissuance:</strong> Pinning only validates that the server
            presents a known certificate. It does not detect if a CA has issued a fraudulent certificate
            for your domain to someone else.
          </li>
        </ul>

        <div className={styles.highlight}>
          <p>
            <strong>Note:</strong> Google deprecated HTTP Public Key Pinning (HPKP) in Chrome 72 due to
            the risk of site operators accidentally bricking their own sites. The deprecation underscores
            the fragility of the pinning approach.
          </p>
        </div>
      </section>

      {/* ── Section 4: OCSP ──────────────────────────────────────────── */}
      <section>
        <h2 id="ocsp">The OCSP Approach</h2>
        <p>
          The Online Certificate Status Protocol (<code>OCSP</code>) provides a way for clients to
          check whether a certificate has been revoked. Instead of downloading large Certificate
          Revocation Lists (CRLs), the client sends a query to an OCSP responder — a server operated
          by the CA — and receives a signed response indicating the certificate's status.
        </p>

        <div className={styles.diagram}>
          <svg viewBox="0 0 620 220" xmlns="http://www.w3.org/2000/svg" role="img" aria-label="OCSP flow diagram">
            {/* Client */}
            <rect x="10" y="15" width="120" height="55" rx="10" ry="10"
              fill="var(--color-surface-raised)" stroke="var(--color-primary)" strokeWidth="2" />
            <text x="70" y="38" textAnchor="middle" fill="var(--color-text-primary)" fontSize="11" fontWeight="600">Client</text>
            <text x="70" y="53" textAnchor="middle" fill="var(--color-text-secondary)" fontSize="9">(Browser / App)</text>

            {/* Arrow 1: Client → Server */}
            <line x1="130" y1="35" x2="220" y2="35" stroke="var(--color-text-secondary)" strokeWidth="2" markerEnd="url(#arrowhead-ocsp)" />
            <circle cx="175" cy="25" r="8" fill="var(--color-primary)" />
            <text x="175" y="29" textAnchor="middle" fill="white" fontSize="9" fontWeight="700">1</text>
            <text x="175" y="15" textAnchor="middle" fill="var(--color-text-secondary)" fontSize="8">TLS Handshake</text>

            {/* Server */}
            <rect x="220" y="15" width="120" height="55" rx="10" ry="10"
              fill="var(--color-surface-raised)" stroke="var(--color-primary)" strokeWidth="2" />
            <text x="280" y="38" textAnchor="middle" fill="var(--color-text-primary)" fontSize="11" fontWeight="600">Server</text>
            <text x="280" y="53" textAnchor="middle" fill="var(--color-text-secondary)" fontSize="9">(presents cert)</text>

            {/* Arrow 2: Client → OCSP (below) */}
            <path d="M 70 70 L 70 155 L 410 155" fill="none" stroke="var(--color-text-secondary)" strokeWidth="2" markerEnd="url(#arrowhead-ocsp)" />
            <circle cx="240" cy="155" r="8" fill="var(--color-primary)" />
            <text x="240" y="159" textAnchor="middle" fill="white" fontSize="9" fontWeight="700">2</text>
            <text x="240" y="145" textAnchor="middle" fill="var(--color-text-secondary)" fontSize="8">"Is this cert valid?"</text>

            {/* OCSP Responder */}
            <rect x="410" y="135" width="130" height="55" rx="10" ry="10"
              fill="var(--color-surface-raised)" stroke="var(--color-primary)" strokeWidth="2" />
            <text x="475" y="158" textAnchor="middle" fill="var(--color-text-primary)" fontSize="11" fontWeight="600">OCSP</text>
            <text x="475" y="173" textAnchor="middle" fill="var(--color-text-secondary)" fontSize="9">Responder</text>

            {/* Arrow 3: OCSP → CA */}
            <line x1="475" y1="135" x2="475" y2="70" stroke="var(--color-text-secondary)" strokeWidth="2" markerEnd="url(#arrowhead-ocsp)" />
            <circle cx="475" cy="102" r="8" fill="var(--color-primary)" />
            <text x="475" y="106" textAnchor="middle" fill="white" fontSize="9" fontWeight="700">3</text>
            <text x="530" y="100" fill="var(--color-text-secondary)" fontSize="8">Check</text>
            <text x="530" y="111" fill="var(--color-text-secondary)" fontSize="8">revocation</text>

            {/* CA */}
            <rect x="410" y="15" width="130" height="55" rx="10" ry="10"
              fill="var(--color-surface-raised)" stroke="var(--color-primary)" strokeWidth="2" />
            <text x="475" y="38" textAnchor="middle" fill="var(--color-text-primary)" fontSize="11" fontWeight="600">Certificate</text>
            <text x="475" y="53" textAnchor="middle" fill="var(--color-text-secondary)" fontSize="9">Authority</text>

            {/* Arrow 4: OCSP → Client (dashed return) */}
            <path d="M 410 180 L 100 180 L 100 70" fill="none" stroke="var(--color-text-secondary)" strokeWidth="2" markerEnd="url(#arrowhead-ocsp)" strokeDasharray="6 3" />
            <circle cx="260" cy="180" r="8" fill="var(--color-primary)" />
            <text x="260" y="184" textAnchor="middle" fill="white" fontSize="9" fontWeight="700">4</text>
            <text x="260" y="198" textAnchor="middle" fill="var(--color-text-secondary)" fontSize="8">"Good" / "Revoked" / "Unknown"</text>

            <defs>
              <marker id="arrowhead-ocsp" markerWidth="8" markerHeight="6" refX="8" refY="3" orient="auto">
                <polygon points="0 0, 8 3, 0 6" fill="var(--color-text-secondary)" />
              </marker>
            </defs>
          </svg>
          <div className={styles.diagramCaption}>
            OCSP flow: the client asks an OCSP responder whether a certificate has been revoked, adding latency to every new connection.
          </div>
        </div>

        <h3 id="ocsp-limitations">Limitations of OCSP</h3>
        <ul className={styles.problemList}>
          <li>
            <strong>Added latency:</strong> Every new TLS connection requires an extra network round-trip
            to the OCSP responder before the handshake can complete.
          </li>
          <li>
            <strong>Single point of failure:</strong> If the OCSP responder is unreachable, the revocation
            check cannot be performed.
          </li>
          <li>
            <strong>Soft-fail behavior:</strong> Most browsers silently skip the OCSP check if the
            responder is unavailable. An attacker who can block OCSP traffic can effectively disable
            revocation checking entirely.
          </li>
          <li>
            <strong>OCSP stapling is not universal:</strong> OCSP stapling lets the server fetch and
            cache the OCSP response, eliminating the client-side round-trip. However, not all servers
            support it, and clients cannot require it.
          </li>
          <li>
            <strong>Only checks revocation, not misissuance:</strong> OCSP can tell you if a known
            certificate has been revoked. It cannot detect a certificate that was fraudulently issued
            by a compromised CA in the first place — because from the CA's perspective, the
            certificate is valid.
          </li>
        </ul>
      </section>

      {/* ── Section 5: Certificate Transparency ──────────────────────── */}
      <section>
        <h2 id="certificate-transparency">Certificate Transparency: A Better Solution</h2>
        <p>
          Certificate Transparency (<code>CT</code>) is a framework defined in{' '}
          <a href="https://datatracker.ietf.org/doc/html/rfc6962" target="_blank" rel="noopener noreferrer">
            RFC 6962
          </a>{' '}
          that makes the issuance of TLS certificates publicly auditable. Instead of trusting CAs
          blindly, CT requires that every certificate is logged in public, append-only,
          cryptographically verifiable logs before it can be trusted by clients.
        </p>

        <div className={styles.diagram}>
          <svg viewBox="0 0 600 390" xmlns="http://www.w3.org/2000/svg" role="img" aria-label="Certificate Transparency ecosystem diagram">
            {/* Row 1: Domain Owner → CA → Certificate */}
            <rect x="10" y="20" width="120" height="50" rx="10" ry="10"
              fill="var(--color-surface-raised)" stroke="var(--color-primary)" strokeWidth="2" />
            <text x="70" y="41" textAnchor="middle" fill="var(--color-text-primary)" fontSize="11" fontWeight="600">Domain</text>
            <text x="70" y="55" textAnchor="middle" fill="var(--color-text-secondary)" fontSize="9">Owner</text>

            {/* Arrow 1: Domain → CA */}
            <line x1="130" y1="45" x2="220" y2="45" stroke="var(--color-text-secondary)" strokeWidth="2" markerEnd="url(#arrowhead-ct)" />
            <circle cx="175" cy="35" r="8" fill="var(--color-primary)" />
            <text x="175" y="39" textAnchor="middle" fill="white" fontSize="9" fontWeight="700">1</text>

            {/* CA */}
            <rect x="220" y="20" width="130" height="50" rx="10" ry="10"
              fill="var(--color-surface-raised)" stroke="var(--color-primary)" strokeWidth="2" />
            <text x="285" y="41" textAnchor="middle" fill="var(--color-text-primary)" fontSize="11" fontWeight="600">Certificate</text>
            <text x="285" y="55" textAnchor="middle" fill="var(--color-text-secondary)" fontSize="9">Authority</text>

            {/* Arrow 4: CA → Certificate (with SCTs) */}
            <line x1="350" y1="45" x2="430" y2="45" stroke="var(--color-text-secondary)" strokeWidth="2" markerEnd="url(#arrowhead-ct)" />
            <circle cx="390" cy="35" r="8" fill="var(--color-primary)" />
            <text x="390" y="39" textAnchor="middle" fill="white" fontSize="9" fontWeight="700">4</text>

            {/* Certificate (with embedded SCTs) */}
            <rect x="430" y="20" width="140" height="50" rx="10" ry="10"
              fill="var(--color-surface-raised)" stroke="var(--color-primary)" strokeWidth="2" />
            <text x="500" y="38" textAnchor="middle" fill="var(--color-text-primary)" fontSize="11" fontWeight="600">Certificate</text>
            <text x="500" y="51" textAnchor="middle" fill="var(--color-text-secondary)" fontSize="9">(with embedded</text>
            <text x="500" y="62" textAnchor="middle" fill="var(--color-text-secondary)" fontSize="9">SCTs)</text>

            {/* Arrow 2: CA → CT Logs (submit precertificate) */}
            <line x1="270" y1="70" x2="270" y2="150" stroke="var(--color-text-secondary)" strokeWidth="2" markerEnd="url(#arrowhead-ct)" />
            <circle cx="270" cy="105" r="8" fill="var(--color-primary)" />
            <text x="270" y="109" textAnchor="middle" fill="white" fontSize="9" fontWeight="700">2</text>
            <text x="210" y="107" textAnchor="middle" fill="var(--color-text-secondary)" fontSize="8">Submit precertificate</text>

            {/* Arrow 3: CT Logs → CA (return SCTs) */}
            <line x1="300" y1="150" x2="300" y2="70" fill="none" stroke="var(--color-text-secondary)" strokeWidth="2" markerEnd="url(#arrowhead-ct)" strokeDasharray="6 3" />
            <circle cx="300" cy="120" r="8" fill="var(--color-primary)" />
            <text x="300" y="124" textAnchor="middle" fill="white" fontSize="9" fontWeight="700">3</text>
            <text x="345" y="122" textAnchor="middle" fill="var(--color-text-secondary)" fontSize="8">Return SCTs</text>

            {/* CT Logs */}
            <rect x="220" y="150" width="130" height="50" rx="10" ry="10"
              fill="var(--color-surface-raised)" stroke="var(--color-primary)" strokeWidth="2" />
            <text x="285" y="171" textAnchor="middle" fill="var(--color-text-primary)" fontSize="11" fontWeight="600">CT Logs</text>
            <text x="285" y="185" textAnchor="middle" fill="var(--color-text-secondary)" fontSize="9">(append-only)</text>

            {/* Arrow 5: Certificate → Browser (TLS handshake) */}
            <line x1="500" y1="70" x2="500" y2="150" stroke="var(--color-text-secondary)" strokeWidth="2" markerEnd="url(#arrowhead-ct)" />
            <circle cx="500" cy="105" r="8" fill="var(--color-primary)" />
            <text x="500" y="109" textAnchor="middle" fill="white" fontSize="9" fontWeight="700">5</text>
            <text x="553" y="105" textAnchor="middle" fill="var(--color-text-secondary)" fontSize="8">TLS handshake</text>
            <text x="553" y="116" textAnchor="middle" fill="var(--color-text-secondary)" fontSize="8">with SCTs</text>

            {/* Browser / Client */}
            <rect x="430" y="150" width="140" height="50" rx="10" ry="10"
              fill="var(--color-surface-raised)" stroke="var(--color-primary)" strokeWidth="2" />
            <text x="500" y="171" textAnchor="middle" fill="var(--color-text-primary)" fontSize="11" fontWeight="600">Browser / Client</text>
            <text x="500" y="185" textAnchor="middle" fill="var(--color-text-secondary)" fontSize="9">(verifies SCTs)</text>

            {/* Step 6 label */}
            <circle cx="440" cy="222" r="8" fill="var(--color-primary)" />
            <text x="440" y="226" textAnchor="middle" fill="white" fontSize="9" fontWeight="700">6</text>
            <text x="455" y="224" fill="var(--color-text-secondary)" fontSize="8">Verify SCTs against</text>
            <text x="455" y="235" fill="var(--color-text-secondary)" fontSize="8">known CT log keys</text>

            {/* Monitors */}
            <rect x="220" y="280" width="130" height="50" rx="10" ry="10"
              fill="var(--color-surface-raised)" stroke="var(--color-border)" strokeWidth="2" />
            <text x="285" y="301" textAnchor="middle" fill="var(--color-text-primary)" fontSize="11" fontWeight="600">Monitors</text>
            <text x="285" y="315" textAnchor="middle" fill="var(--color-text-secondary)" fontSize="9">(watch for fraud)</text>

            {/* Arrow 7: Monitors → CT Logs (continuously audit) */}
            <line x1="285" y1="280" x2="285" y2="200" stroke="var(--color-text-secondary)" strokeWidth="2" markerEnd="url(#arrowhead-ct)" strokeDasharray="5 3" />
            <circle cx="285" cy="240" r="8" fill="var(--color-primary)" />
            <text x="285" y="244" textAnchor="middle" fill="white" fontSize="9" fontWeight="700">7</text>
            <text x="330" y="237" fill="var(--color-text-secondary)" fontSize="8">Continuously</text>
            <text x="330" y="248" fill="var(--color-text-secondary)" fontSize="8">audit logs</text>

            {/* Alert: Monitors → Domain Owner */}
            <path d="M 220 305 L 70 305 L 70 70" fill="none" stroke="#e53e3e" strokeWidth="1.5" markerEnd="url(#arrowhead-alert)" strokeDasharray="5 3" />
            <text x="285" y="355" textAnchor="middle" fill="#e53e3e" fontSize="8">Alert on unauthorized certificates</text>

            <defs>
              <marker id="arrowhead-ct" markerWidth="8" markerHeight="6" refX="8" refY="3" orient="auto">
                <polygon points="0 0, 8 3, 0 6" fill="var(--color-text-secondary)" />
              </marker>
              <marker id="arrowhead-alert" markerWidth="8" markerHeight="6" refX="8" refY="3" orient="auto">
                <polygon points="0 0, 8 3, 0 6" fill="#e53e3e" />
              </marker>
            </defs>
          </svg>
          <div className={styles.diagramCaption}>
            The CT ecosystem: CAs submit precertificates to logs, receive SCTs, embed them in certificates.
            Browsers verify SCTs during TLS; monitors watch logs for unauthorized certificates.
          </div>
        </div>

        <h3 id="ct-step-by-step">How CT Works, Step by Step</h3>
        <ul>
          <li>
            <strong>Step 1:</strong> A website owner requests a certificate from a CA.
          </li>
          <li>
            <strong>Step 2:</strong> The CA creates a <strong>precertificate</strong> (containing a
            poison extension that prevents browsers from accepting it directly) and submits it to one
            or more CT logs.
          </li>
          <li>
            <strong>Step 3:</strong> Each CT log returns a <strong>Signed Certificate Timestamp</strong>{' '}
            (<code>SCT</code>) — a cryptographic promise to include the certificate in the log within
            the Maximum Merge Delay (typically 24 hours).
          </li>
          <li>
            <strong>Step 4:</strong> The CA embeds the <code>SCT</code>s in the final certificate as
            X.509v3 extensions and issues it to the domain owner.
          </li>
          <li>
            <strong>Step 5:</strong> During the TLS handshake, the server presents the certificate with
            its embedded <code>SCT</code>s to the client.
          </li>
          <li>
            <strong>Step 6:</strong> The browser or client verifies the <code>SCT</code>s by checking
            the signatures against the public keys of known CT logs.
          </li>
          <li>
            <strong>Step 7:</strong> Independently, monitors continuously scan CT logs for certificates
            issued for domains they watch. If an unauthorized certificate appears, the domain owner
            is alerted.
          </li>
        </ul>

        <h3 id="ct-benefits">Key Benefits of CT</h3>
        <ul className={styles.benefitList}>
          <li>
            <strong>Public, append-only logs:</strong> Every issued certificate is recorded in tamper-evident
            logs. Anyone can verify the integrity of the log using{' '}
            <a href="https://certificate.transparency.dev/howctworks/" target="_blank" rel="noopener noreferrer">
              Merkle tree proofs
            </a>.
          </li>
          <li>
            <strong>Early detection of misissued certificates:</strong> Domain owners and monitors can
            discover fraudulent certificates shortly after issuance — not months later.
          </li>
          <li>
            <strong>No app updates required:</strong> Unlike pinning, CT does not require shipping new
            application binaries when certificates change.
          </li>
          <li>
            <strong>No connection-time latency:</strong> Unlike OCSP, <code>SCT</code>s are embedded in
            the certificate itself — there is no extra network round-trip.
          </li>
          <li>
            <strong>Enforced by major browsers:</strong> Chrome has required CT for all publicly trusted
            certificates since April 2018. Safari enforces CT as well. Non-compliant certificates are
            rejected.
          </li>
          <li>
            <strong>Cryptographic integrity:</strong> CT logs use Merkle trees to provide append-only,
            tamper-evident data structures. Any attempt to modify or remove a log entry is detectable.
          </li>
          <li>
            <strong>Ecosystem-wide protection:</strong> CT protects all users of the web, not just
            individual applications. Any domain owner can monitor logs for certificates issued in
            their name.
          </li>
        </ul>
      </section>

      {/* ── Section 6: Comparison Table ──────────────────────────────── */}
      <section>
        <h2 id="comparison">CT vs SSL Pinning: Comparison</h2>
        <p>
          The following table summarizes the key differences between SSL pinning and Certificate
          Transparency as approaches to TLS security:
        </p>

        <table>
          <thead>
            <tr>
              <th>Aspect</th>
              <th>SSL Pinning</th>
              <th>Certificate Transparency</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>Risk of bricking app</td>
              <td><strong style={{ color: '#e53e3e' }}>High</strong> — cert rotation breaks pinned apps</td>
              <td><strong style={{ color: '#38a169' }}>None</strong> — no hardcoded values</td>
            </tr>
            <tr>
              <td>Maintenance burden</td>
              <td><strong style={{ color: '#e53e3e' }}>High</strong> — manual pin rotation</td>
              <td><strong style={{ color: '#38a169' }}>Low</strong> — automatic via log ecosystem</td>
            </tr>
            <tr>
              <td>Detects misissued certs</td>
              <td><strong style={{ color: '#e53e3e' }}>No</strong> — only validates known pins</td>
              <td><strong style={{ color: '#38a169' }}>Yes</strong> — all certs are publicly logged</td>
            </tr>
            <tr>
              <td>Requires app updates</td>
              <td><strong style={{ color: '#e53e3e' }}>Yes</strong> — for every pin change</td>
              <td><strong style={{ color: '#38a169' }}>No</strong> — verification is automatic</td>
            </tr>
            <tr>
              <td>Browser enforcement</td>
              <td><strong style={{ color: '#e53e3e' }}>No</strong> — app-level only</td>
              <td><strong style={{ color: '#38a169' }}>Yes</strong> — Chrome, Safari require CT</td>
            </tr>
            <tr>
              <td>Protection scope</td>
              <td>Single app only</td>
              <td>Entire ecosystem</td>
            </tr>
            <tr>
              <td>Operational complexity</td>
              <td><strong style={{ color: '#e53e3e' }}>High</strong> — coordination across platforms</td>
              <td><strong style={{ color: '#38a169' }}>Low</strong> — handled by infrastructure</td>
            </tr>
            <tr>
              <td>Connection latency</td>
              <td>None (but pins can be stale)</td>
              <td>None (SCTs embedded in cert)</td>
            </tr>
          </tbody>
        </table>
      </section>

      {/* ── Section 7: How Seal Implements CT ───────────────────────── */}
      <section>
        <h2 id="seal-ct">How Seal Implements CT</h2>
        <p>
          <strong>Seal</strong> brings Certificate Transparency verification to Kotlin Multiplatform
          applications. While browsers like Chrome and Safari already enforce CT, mobile apps
          using platform networking libraries do not get this protection by default.
        </p>
        <p>
          Seal fills this gap by providing:
        </p>
        <ul className={styles.benefitList}>
          <li>
            <strong>An OkHttp network interceptor</strong> for Android apps that verifies{' '}
            <code>SCT</code>s on every HTTPS connection.
          </li>
          <li>
            <strong>A Ktor client plugin</strong> for Kotlin Multiplatform projects that adds CT
            verification across Android and iOS.
          </li>
          <li>
            <strong>Configurable verification policies</strong> — choose how many <code>SCT</code>s
            are required, which CT logs to trust, and how to handle verification failures.
          </li>
          <li>
            <strong>A shared core library</strong> (<code>seal-core</code>) implementing{' '}
            <code>SCT</code> parsing, signature verification, and log list management in pure Kotlin
            Multiplatform code.
          </li>
        </ul>

        <div className={styles.highlight}>
          <p>
            <strong>Ready to add CT verification to your app?</strong> Check out
            the <Link to="/getting-started">Getting Started</Link> guide to integrate Seal in minutes.
          </p>
        </div>
      </section>
    </article>
  )
}
