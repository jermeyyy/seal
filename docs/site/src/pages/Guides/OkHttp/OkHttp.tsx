import { Link } from 'react-router-dom'
import CodeBlock from '@components/CodeBlock/CodeBlock'
import styles from '../Guides.module.css'
import {
  okhttpBasicExample,
  okhttpDslExample,
  okhttpNetworkInterceptorNote,
} from '@data/codeExamples'

export default function OkHttp() {
  return (
    <article className={styles.guides}>
      <h1>OkHttp Integration</h1>
      <p className={styles.intro}>
        Add Certificate Transparency verification to your Android or JVM Desktop app using OkHttp's 
        network interceptor mechanism. This guide covers setup, configuration, 
        and best practices.
      </p>

      <section>
        <h2 id="basic-setup">Basic Setup</h2>
        <p>
          Add the <code>seal-core</code> dependency and create an OkHttp client with 
          the CT interceptor:
        </p>
        <CodeBlock code={okhttpBasicExample} language="kotlin" title="Basic OkHttp Setup" />
      </section>

      <section>
        <h2 id="network-interceptor">Network vs Application Interceptor</h2>
        <p>
          Seal <strong>must</strong> be added as a network interceptor, not an application interceptor. 
          Network interceptors have access to the TLS connection, which is required 
          to extract SCTs (Signed Certificate Timestamps) from the TLS handshake.
        </p>
        <CodeBlock code={okhttpNetworkInterceptorNote} language="kotlin" title="Important: Use Network Interceptor" />

        <div className={styles.warning}>
          <p><strong>⚠️ Warning:</strong> Using <code>addInterceptor()</code> instead of 
          <code>addNetworkInterceptor()</code> will silently skip CT verification because 
          application interceptors don't have access to the TLS connection details.</p>
        </div>
      </section>

      <section>
        <h2 id="configuration">Configuration DSL</h2>
        <p>
          The interceptor builder supports a rich DSL for configuring host patterns, 
          error handling, and logging:
        </p>
        <CodeBlock code={okhttpDslExample} language="kotlin" title="OkHttp Configuration DSL" />
      </section>

      <section>
        <h2 id="host-patterns">Host Inclusion & Exclusion</h2>
        <p>
          Control which hosts are verified using inclusion and exclusion patterns:
        </p>
        <ul>
          <li><code>+"*.google.com"</code> — Include all subdomains of google.com</li>
          <li><code>+"www.example.com"</code> — Include a specific host</li>
          <li><code>-"internal.example.com"</code> — Exclude a specific host</li>
          <li>Exclusions take precedence over inclusions</li>
          <li>If no inclusions are specified, all hosts are included by default</li>
        </ul>
      </section>

      <section>
        <h2 id="error-handling">Error Handling</h2>
        <p>
          Seal supports two failure modes:
        </p>
        <ul>
          <li>
            <strong>Fail-open (default):</strong> <code>failOnError = false</code> — 
            CT verification failures are logged but don't block the request. 
            Recommended for initial deployment.
          </li>
          <li>
            <strong>Fail-closed:</strong> <code>failOnError = true</code> — 
            CT verification failures throw an exception, blocking the request. 
            Use when you need strict CT enforcement.
          </li>
        </ul>
      </section>

      <section>
        <h2 id="next-steps">Next Steps</h2>
        <ul>
          <li><Link to="/guides/ktor">Ktor Integration</Link> — For multiplatform HTTP clients</li>
          <li><Link to="/guides/custom-policies">Custom Policies</Link> — Chrome vs Apple CT policies</li>
          <li><Link to="/guides/configuration">Full Configuration Reference</Link> — All options</li>
        </ul>
      </section>
    </article>
  )
}
