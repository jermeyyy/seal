import { Link } from 'react-router-dom'
import CodeBlock from '@components/CodeBlock/CodeBlock'
import styles from '../Guides.module.css'
import {
  ktorBasicExample,
  ktorDslExample,
  ktorMultiplatformExample,
} from '@data/codeExamples'

export default function Ktor() {
  return (
    <article className={styles.guides}>
      <h1>Ktor Integration</h1>
      <p className={styles.intro}>
        Use the Seal Ktor plugin for Certificate Transparency verification in 
        Kotlin Multiplatform projects. Works on both Android and iOS with 
        the same configuration.
      </p>

      <section>
        <h2 id="basic-setup">Basic Setup</h2>
        <p>
          Install the <code>CertificateTransparency</code> plugin on your Ktor HTTP client:
        </p>
        <CodeBlock code={ktorBasicExample} language="kotlin" title="Basic Ktor Setup" />
      </section>

      <section>
        <h2 id="configuration">Configuration DSL</h2>
        <p>
          Configure host patterns, error handling, and logging:
        </p>
        <CodeBlock code={ktorDslExample} language="kotlin" title="Ktor Configuration" />
      </section>

      <section>
        <h2 id="multiplatform">Multiplatform Support</h2>
        <p>
          The same Ktor configuration works on both platforms. Seal automatically 
          uses the appropriate engine for each platform:
        </p>
        <CodeBlock code={ktorMultiplatformExample} language="kotlin" title="Multiplatform Configuration" />

        <table>
          <thead>
            <tr>
              <th>Platform</th>
              <th>Engine</th>
              <th>SCT Source</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>Android</td>
              <td>OkHttp</td>
              <td>Conscrypt TLS extension</td>
            </tr>
            <tr>
              <td>iOS</td>
              <td>Darwin</td>
              <td>SecTrust API</td>
            </tr>
          </tbody>
        </table>
      </section>

      <section>
        <h2 id="engine-selection">Engine Selection</h2>
        <p>
          When using Ktor on Android, make sure you're using the OkHttp engine. On iOS, 
          the Darwin engine is used automatically. The CT plugin integrates with each 
          engine's native TLS stack to extract SCTs.
        </p>

        <div className={styles.note}>
          <p><strong>💡 Note:</strong> On iOS, you can also use <code>HttpClient(Darwin)</code> 
          explicitly. The CT plugin hooks into the <code>URLSession</code> delegate 
          for TLS verification.</p>
        </div>
      </section>

      <section>
        <h2 id="next-steps">Next Steps</h2>
        <ul>
          <li><Link to="/guides/okhttp">OkHttp Integration</Link> — Direct OkHttp interceptor usage</li>
          <li><Link to="/guides/ios">iOS Integration</Link> — iOS-specific details</li>
          <li><Link to="/guides/configuration">Configuration Reference</Link> — All available options</li>
        </ul>
      </section>
    </article>
  )
}
