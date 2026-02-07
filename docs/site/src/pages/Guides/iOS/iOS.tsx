import { Link } from 'react-router-dom'
import CodeBlock from '@components/CodeBlock/CodeBlock'
import styles from '../Guides.module.css'
import { iosUrlSessionExample } from '@data/codeExamples'

export default function IOS() {
  return (
    <article className={styles.guides}>
      <h1>iOS Integration</h1>
      <p className={styles.intro}>
        Learn about iOS-specific Certificate Transparency behavior, including 
        SecTrust integration and how the Ktor Darwin engine works with Seal.
      </p>

      <section>
        <h2 id="how-it-works">How CT Works on iOS</h2>
        <p>
          On iOS, Certificate Transparency verification leverages the platform's 
          native <code>SecTrust</code> API. When using the Ktor Darwin engine, Seal 
          hooks into the URLSession delegate to access the server trust object and 
          extract SCT information.
        </p>
        <ul>
          <li>SCTs are extracted via the <code>SecTrust</code> API during TLS handshake</li>
          <li>Verification uses the Security framework's built-in CT support</li>
          <li>Works with URLSession-based networking (Ktor Darwin engine)</li>
        </ul>
      </section>

      <section>
        <h2 id="ktor-darwin">Ktor Darwin Engine Setup</h2>
        <p>
          The recommended way to use Seal on iOS is through the Ktor Darwin engine:
        </p>
        <CodeBlock code={iosUrlSessionExample} language="kotlin" title="iOS Ktor Setup" />
      </section>

      <section>
        <h2 id="automatic-features">What Works Automatically</h2>
        <p>
          When you install the <code>CertificateTransparency</code> plugin with the Darwin engine:
        </p>
        <ul>
          <li>SCT extraction from the TLS handshake is handled automatically</li>
          <li>The CT log list is fetched and cached</li>
          <li>Verification runs against the configured CT policy</li>
          <li>Results are reported through the logger callback</li>
        </ul>

        <div className={styles.note}>
          <p><strong>💡 Note:</strong> iOS has built-in CT support at the OS level 
          (App Transport Security). Seal provides additional control over CT enforcement 
          with configurable policies and host patterns.</p>
        </div>
      </section>

      <section>
        <h2 id="platform-considerations">Platform Considerations</h2>
        <ul>
          <li>
            <strong>Minimum iOS version:</strong> iOS 13+ is required for full SecTrust CT support
          </li>
          <li>
            <strong>App Transport Security:</strong> ATS provides baseline security. 
            Seal adds explicit CT verification on top of ATS.
          </li>
          <li>
            <strong>Background networking:</strong> CT verification works with background 
            URLSession tasks.
          </li>
        </ul>
      </section>

      <section>
        <h2 id="next-steps">Next Steps</h2>
        <ul>
          <li><Link to="/guides/ktor">Ktor Integration</Link> — Full multiplatform Ktor guide</li>
          <li><Link to="/guides/configuration">Configuration Reference</Link> — All available options</li>
          <li><Link to="/guides/custom-policies">Custom Policies</Link> — Chrome vs Apple policies</li>
        </ul>
      </section>
    </article>
  )
}
