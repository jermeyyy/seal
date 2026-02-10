import { Link } from 'react-router-dom'
import CodeBlock from '@components/CodeBlock/CodeBlock'
import styles from './GettingStarted.module.css'
import {
  versionCatalogConfig,
  gradleDependencies,
  androidVersionCatalogConfig,
  androidGradleDependencies,
  okhttpBasicExample,
  ktorBasicExample,
} from '@data/codeExamples'

export default function GettingStarted() {
  return (
    <article className={styles.gettingStarted}>
      <h1>Getting Started</h1>

      <p>
        This guide will walk you through setting up Seal in your Kotlin Multiplatform project.
        Follow these steps to get Certificate Transparency verification working in minutes.
      </p>

      <section>
        <h2 id="installation">Installation</h2>

        <p>
          Seal supports both Kotlin Multiplatform and native Android projects. Choose the setup that matches your project.
        </p>

        <h3 id="kmp-setup">Kotlin Multiplatform Setup</h3>
        <p>
          For KMP projects targeting Android and iOS, add all relevant Seal modules:
        </p>

        <h4 id="kmp-version-catalog">Version Catalog</h4>
        <CodeBlock code={versionCatalogConfig} language="bash" title="gradle/libs.versions.toml" />

        <h4 id="kmp-gradle-dependencies">Gradle Dependencies</h4>
        <CodeBlock code={gradleDependencies} language="kotlin" title="build.gradle.kts" />

        <h3 id="android-setup">Native Android Setup</h3>
        <p>
          For Android-only projects using OkHttp, you only need the core and Android modules:
        </p>

        <h4 id="android-version-catalog">Version Catalog</h4>
        <CodeBlock code={androidVersionCatalogConfig} language="bash" title="gradle/libs.versions.toml" />

        <h4 id="android-gradle-dependencies">Gradle Dependencies</h4>
        <CodeBlock code={androidGradleDependencies} language="kotlin" title="build.gradle.kts" />
      </section>

      <section>
        <h2 id="quick-start-okhttp">Quick Start with OkHttp</h2>
        <p>
          For Android projects using OkHttp, add the CT network interceptor:
        </p>
        <CodeBlock code={okhttpBasicExample} language="kotlin" title="OkHttp Setup" />

        <div className={styles.note}>
          <p><strong>💡 Important:</strong> You must use <code>addNetworkInterceptor()</code>, not 
          <code>addInterceptor()</code>. Network interceptors have access to the TLS connection 
          details needed for CT verification.</p>
        </div>
      </section>

      <section>
        <h2 id="quick-start-ktor">Quick Start with Ktor</h2>
        <p>
          For Kotlin Multiplatform projects using Ktor, install the CT plugin:
        </p>
        <CodeBlock code={ktorBasicExample} language="kotlin" title="Ktor Setup" />
      </section>

      <section>
        <h2 id="platform-requirements">Platform Requirements</h2>
        <table>
          <thead>
            <tr>
              <th>Platform</th>
              <th>Minimum Version</th>
              <th>HTTP Client</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>Android</td>
              <td>API 21+</td>
              <td>OkHttp / Ktor (OkHttp engine)</td>
            </tr>
            <tr>
              <td>iOS</td>
              <td>iOS 13+</td>
              <td>Ktor (Darwin engine)</td>
            </tr>
          </tbody>
        </table>
      </section>

      <section>
        <h2 id="modules">Available Modules</h2>
        <table>
          <thead>
            <tr>
              <th>Module</th>
              <th>Description</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td><code>seal-core</code></td>
              <td>Core CT verification logic, models, log list parsing, and policies</td>
            </tr>
            <tr>
              <td><code>seal-android</code></td>
              <td>Android-specific: OkHttp interceptor, Conscrypt SCT extraction</td>
            </tr>
            <tr>
              <td><code>seal-ios</code></td>
              <td>iOS-specific: SecTrust integration, Darwin networking support</td>
            </tr>
            <tr>
              <td><code>seal-ktor</code></td>
              <td>Ktor plugin for multiplatform CT verification</td>
            </tr>
          </tbody>
        </table>
      </section>

      <section>
        <h2 id="next-steps">Next Steps</h2>
        <p>
          Now that you have basic CT verification running, explore more:
        </p>
        <ul>
          <li>
            <Link to="/guides/okhttp">OkHttp Integration</Link> — Full guide for Android OkHttp setup
          </li>
          <li>
            <Link to="/guides/ktor">Ktor Integration</Link> — Multiplatform Ktor plugin guide
          </li>
          <li>
            <Link to="/guides/custom-policies">Custom Policies</Link> — Chrome and Apple CT policies
          </li>
          <li>
            <Link to="/guides/configuration">Configuration</Link> — All available configuration options
          </li>
          <li>
            <Link to="/guides/ios">iOS Integration</Link> — iOS-specific behavior and setup
          </li>
        </ul>
      </section>

      <div className={styles.proTip}>
        <h3>💡 Pro Tip</h3>
        <p>
          Start with fail-open mode (the default) so CT failures don't break your app.
          Once you're confident in your configuration, switch to fail-closed for maximum security.
        </p>
      </div>
    </article>
  )
}
