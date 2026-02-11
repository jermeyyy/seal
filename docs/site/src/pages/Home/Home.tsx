import { Link } from 'react-router-dom'
import CodeBlock from '@components/CodeBlock/CodeBlock'
import {
  versionCatalogConfig,
  okhttpBasicExample,
  ktorBasicExample,
} from '@data/codeExamples'
import styles from './Home.module.css'

export default function Home() {
  return (
    <article className={styles.home}>
      {/* Hero Section */}
      <section className={styles.hero}>
        <img src={`${import.meta.env.BASE_URL}logo.png`} alt="Seal" className={styles.heroLogo} />
        <h1>Seal</h1>
        <p className={styles.subtitle}>
          Certificate Transparency for Kotlin Multiplatform
        </p>
        
        <div className={styles.badges}>
          <a href="https://central.sonatype.com/search?q=io.github.jermeyyy" target="_blank" rel="noopener noreferrer">
            <img src="https://img.shields.io/maven-central/v/io.github.jermeyyy/seal-core?label=Maven%20Central&logo=apache-maven&color=blue" alt="Maven Central" />
          </a>
          <a href="https://kotlinlang.org" target="_blank" rel="noopener noreferrer">
            <img src="https://img.shields.io/badge/Kotlin-2.3.0-7f52ff?logo=kotlin&logoColor=white" alt="Kotlin" />
          </a>
          <a href="https://www.jetbrains.com/kotlin-multiplatform/" target="_blank" rel="noopener noreferrer">
            <img src="https://img.shields.io/badge/Platform-Android%20%7C%20iOS%20%7C%20JVM%20Desktop%20%7C%20Web-4285F4?logo=kotlin&logoColor=white" alt="Platforms" />
          </a>
          <a href="https://github.com/jermeyyy/seal/blob/main/LICENSE" target="_blank" rel="noopener noreferrer">
            <img src="https://img.shields.io/badge/License-MIT-blue.svg" alt="License" />
          </a>
        </div>
        
        <div className={styles.heroButtons}>
          <Link to="/getting-started" className={styles.btnPrimary}>Get Started</Link>
          <a href="https://github.com/jermeyyy/seal" className={styles.btnSecondary} target="_blank" rel="noopener noreferrer">View on GitHub</a>
        </div>
      </section>

      {/* Overview */}
      <section>
        <h2 id="overview">Overview</h2>
        <p>
          <strong>Seal</strong> is a Certificate Transparency (CT) verification library for 
          Kotlin Multiplatform. It enforces CT compliance on HTTPS connections, ensuring that 
          TLS certificates are logged in public transparency logs. Seal supports Android, iOS, 
          JVM Desktop, and Web (wasmJs), with integrations for both OkHttp and Ktor HTTP clients.
        </p>
      </section>

      {/* Why Seal */}
      <section>
        <h2 id="why-seal">Why Seal?</h2>
        <p>
          Certificate Transparency is a modern approach to securing the web's certificate
          ecosystem. <Link to="/why-ct">Learn why it matters</Link> and how it compares to
          alternatives like SSL pinning.
        </p>
        <div className={styles.features}>
          <div className={styles.featureCard}>
            <h4>CT Enforcement</h4>
            <p>Verify that TLS certificates are logged in Certificate Transparency logs, 
            preventing misissued certificates from going undetected.</p>
          </div>

          <div className={styles.featureCard}>
            <h4>Kotlin Multiplatform</h4>
            <p>One library for Android, iOS, JVM Desktop, and Web. Write your CT configuration once in shared 
            code and it works on all platforms automatically.</p>
          </div>

          <div className={styles.featureCard}>
            <h4>OkHttp Integration</h4>
            <p>Drop-in network interceptor for OkHttp. Add CT verification to your existing 
            Android or JVM Desktop networking stack with a single line of code.</p>
          </div>

          <div className={styles.featureCard}>
            <h4>Ktor Plugin</h4>
            <p>Native Ktor plugin for multiplatform HTTP clients. Works with OkHttp 
            engine on Android and JVM Desktop, Darwin engine on iOS, and browser-native CT on Web.</p>
          </div>

          <div className={styles.featureCard}>
            <h4>Configurable Policies</h4>
            <p>Support for Chrome and Apple CT policies. Configure host inclusion/exclusion 
            patterns and choose fail-open or fail-closed behavior.</p>
          </div>

          <div className={styles.featureCard}>
            <h4>Production Ready</h4>
            <p>Built with fail-open defaults for safety. Comprehensive logging support and 
            configurable error handling for production environments.</p>
          </div>
        </div>
      </section>

      {/* Quickstart Section */}
      <section className={styles.quickstart}>
        <h2>Quick Install</h2>
        <p>
          Add Seal to your Kotlin Multiplatform project with version catalog configuration:
        </p>
        <CodeBlock code={versionCatalogConfig} language="bash" title="libs.versions.toml" />
        
        <div className={styles.callout}>
          <div>
            <strong>Tip:</strong> Use <code>seal-ktor</code> for multiplatform projects — it provides 
            a unified API that works on Android, iOS, JVM Desktop, and Web.
          </div>
        </div>
      </section>

      {/* Show Me The Code */}
      <section>
        <h2 id="code-example">Show Me The Code!</h2>
        <p style={{ marginBottom: '2rem' }}>
          Get Certificate Transparency verification running in minutes:
        </p>
        
        <div className={styles.steps}>
          <div className={styles.step}>
            <h3>Option 1: OkHttp (Android / JVM Desktop)</h3>
            <p>Add a network interceptor to your OkHttp client:</p>
            <CodeBlock code={okhttpBasicExample} language="kotlin" title="OkHttp Setup" />
          </div>

          <div className={styles.step}>
            <h3>Option 2: Ktor (Multiplatform)</h3>
            <p>Install the CT plugin on your Ktor client:</p>
            <CodeBlock code={ktorBasicExample} language="kotlin" title="Ktor Setup" />
          </div>
        </div>
        
        <div className={styles.calloutSuccess}>
          <div>
            <strong>That's it!</strong> Your HTTPS connections are now verified against 
            Certificate Transparency logs. Seal handles SCT extraction, log verification, 
            and policy enforcement automatically.
          </div>
        </div>
      </section>

      {/* Resources */}
      <section>
        <h2 id="resources">Resources</h2>
        <ul>
          <li><Link to="/why-ct">Why Certificate Transparency?</Link> — Understanding CT and its advantages</li>
          <li><Link to="/getting-started">Getting Started Guide</Link> — Installation and basic setup</li>
          <li><Link to="/guides/okhttp">OkHttp Integration</Link> — Full OkHttp interceptor guide</li>
          <li><Link to="/guides/ktor">Ktor Integration</Link> — Multiplatform Ktor plugin guide</li>
          <li><Link to="/guides/configuration">Configuration</Link> — All configuration options</li>
          <li><Link to="/demo">Demo Application</Link> — See Seal in action</li>
          <li><a href="https://github.com/jermeyyy/seal" target="_blank" rel="noopener noreferrer">GitHub Repository</a> — Source code and issues</li>
        </ul>
      </section>
    </article>
  )
}
