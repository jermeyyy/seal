import { Link } from 'react-router-dom'
import CodeBlock from '@components/CodeBlock/CodeBlock'
import styles from './Demo.module.css'

const cloneRunCode = `# Clone the repository
git clone https://github.com/jermeyyy/seal.git
cd seal

# Run the Android demo app
./gradlew :composeApp:installDebug

# Run on iOS (macOS only)
open iosApp/iosApp.xcodeproj`

const demoAppCode = `// The demo app showcases CT verification in action
val client = HttpClient(OkHttp) {
    install(CertificateTransparency) {
        // Verify all hosts
        +"*.*"
        
        // Log results
        logger = { host, result ->
            println("CT: $host -> $result")
        }
    }
}

// Make a request and see CT results in the logs
val response = client.get("https://www.google.com")
println("Status: \${response.status}")
println("CT verification passed!")`

export default function Demo() {
  return (
    <article className={styles.demo}>
      <h1>Demo Application</h1>
      <p className={styles.intro}>
        The Seal demo application showcases Certificate Transparency verification in action. 
        See how CT enforcement works on real HTTPS connections across Android and iOS.
      </p>

      <section>
        <h2 id="demo-features">Demo Features</h2>
        <div className={styles.featuresGrid}>
          <div className={styles.featureCard}>
            <h3>CT Verification</h3>
            <p>Real-time Certificate Transparency verification against live HTTPS endpoints.</p>
          </div>

          <div className={styles.featureCard}>
            <h3>Log Inspection</h3>
            <p>View SCT details, log IDs, and verification results for each connection.</p>
          </div>

          <div className={styles.featureCard}>
            <h3>Host Configuration</h3>
            <p>Configure inclusion and exclusion patterns and see how they affect verification.</p>
          </div>

          <div className={styles.featureCard}>
            <h3>Policy Comparison</h3>
            <p>Compare Chrome CT Policy vs Apple CT Policy results side by side.</p>
          </div>

          <div className={styles.featureCard}>
            <h3>Error Handling</h3>
            <p>See fail-open vs fail-closed behavior with different configurations.</p>
          </div>

          <div className={styles.featureCard}>
            <h3>Multiplatform</h3>
            <p>Same verification logic running on both Android (OkHttp) and iOS (Darwin).</p>
          </div>
        </div>
      </section>

      <section>
        <h2 id="platform-behavior">Platform-Specific Behavior</h2>
        
        <div className={styles.platformGrid}>
          <div className={styles.platformCard}>
            <h3>Android</h3>
            <ul>
              <li><strong>HTTP Client:</strong> OkHttp with network interceptor</li>
              <li><strong>SCT Source:</strong> Conscrypt TLS extension extraction</li>
              <li><strong>Log List:</strong> Fetched from Google CT log list</li>
              <li><strong>Verification:</strong> Pure Kotlin crypto verification</li>
            </ul>
          </div>

          <div className={styles.platformCard}>
            <h3>iOS</h3>
            <ul>
              <li><strong>HTTP Client:</strong> URLSession via Ktor Darwin engine</li>
              <li><strong>SCT Source:</strong> SecTrust API</li>
              <li><strong>Verification:</strong> Platform CT support via Security framework</li>
            </ul>
          </div>
        </div>
      </section>

      <section>
        <h2 id="running-demo">Running the Demo</h2>
        
        <h3>Prerequisites</h3>
        <ul>
          <li>JDK 17+ and Android SDK</li>
          <li>Android Studio or IntelliJ IDEA</li>
          <li>For iOS: macOS with Xcode installed</li>
        </ul>

        <h3>Clone and Run</h3>
        <CodeBlock code={cloneRunCode} language="bash" />

        <div className={styles.note}>
          <p><strong>💡 Tip:</strong> The demo application source code is in 
            <code>composeApp/src/</code>. Check the common main source set for 
            shared CT configuration code.</p>
        </div>
      </section>

      <section>
        <h2 id="code-example">Code Example from Demo</h2>
        
        <h3>Main CT Configuration</h3>
        <CodeBlock code={demoAppCode} language="kotlin" />
      </section>

      <section>
        <h2 id="explore-more">Explore More</h2>
        <ul>
          <li><a href="https://github.com/jermeyyy/seal/tree/main/composeApp" target="_blank" rel="noopener noreferrer">Demo Source Code</a> — Full implementation on GitHub</li>
          <li><Link to="/getting-started">Getting Started</Link> — Set up Seal in your project</li>
          <li><Link to="/guides/okhttp">OkHttp Guide</Link> — Detailed OkHttp integration</li>
          <li><Link to="/guides/ktor">Ktor Guide</Link> — Multiplatform Ktor setup</li>
        </ul>
      </section>
    </article>
  )
}
