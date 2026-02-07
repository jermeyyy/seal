import { Link } from 'react-router-dom'
import CodeBlock from '@components/CodeBlock/CodeBlock'
import styles from '../Guides.module.css'
import { configurationFullExample } from '@data/codeExamples'

export default function Configuration() {
  return (
    <article className={styles.guides}>
      <h1>Configuration</h1>
      <p className={styles.intro}>
        Complete reference for all Seal configuration options. Control host patterns, 
        CT policies, log list sources, caching, and failure behavior.
      </p>

      <section>
        <h2 id="full-example">Full Configuration Example</h2>
        <p>
          Here's a complete example showing all available configuration options:
        </p>
        <CodeBlock code={configurationFullExample} language="kotlin" title="Full Configuration" />
      </section>

      <section>
        <h2 id="host-patterns">Host Patterns</h2>
        <p>
          Control which hosts undergo CT verification using inclusion and exclusion operators:
        </p>
        <table>
          <thead>
            <tr>
              <th>Pattern</th>
              <th>Description</th>
              <th>Example</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td><code>+"*.example.com"</code></td>
              <td>Include all subdomains</td>
              <td>Matches <code>www.example.com</code>, <code>api.example.com</code></td>
            </tr>
            <tr>
              <td><code>+"www.example.com"</code></td>
              <td>Include specific host</td>
              <td>Matches only <code>www.example.com</code></td>
            </tr>
            <tr>
              <td><code>-"internal.example.com"</code></td>
              <td>Exclude specific host</td>
              <td>Skips CT for this host</td>
            </tr>
            <tr>
              <td>(no patterns)</td>
              <td>Include all hosts</td>
              <td>All hosts are verified</td>
            </tr>
          </tbody>
        </table>

        <div className={styles.note}>
          <p><strong>💡 Note:</strong> Exclusions always take precedence over inclusions. 
          If a host matches both an include and exclude pattern, it will be excluded.</p>
        </div>
      </section>

      <section>
        <h2 id="ct-policy">CT Policy</h2>
        <p>
          Set the Certificate Transparency policy to use for verification:
        </p>
        <ul>
          <li><code>ChromeCtPolicy()</code> — Default. Uses Google Chrome's CT requirements.</li>
          <li><code>AppleCtPolicy()</code> — Uses Apple's CT requirements.</li>
        </ul>
      </section>

      <section>
        <h2 id="failure-modes">Failure Modes</h2>
        <table>
          <thead>
            <tr>
              <th>Setting</th>
              <th>Behavior</th>
              <th>Use Case</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td><code>failOnError = false</code></td>
              <td>Log failure, allow request (fail-open)</td>
              <td>Initial deployment, monitoring</td>
            </tr>
            <tr>
              <td><code>failOnError = true</code></td>
              <td>Throw exception, block request (fail-closed)</td>
              <td>Strict enforcement, security-critical apps</td>
            </tr>
          </tbody>
        </table>
      </section>

      <section>
        <h2 id="log-list">Log List Configuration</h2>
        <p>
          Seal fetches the list of trusted CT logs to verify SCTs against. You can 
          configure the source and caching:
        </p>
        <ul>
          <li>
            <code>logListUrl</code> — URL to fetch the CT log list from. 
            Defaults to Google's public log list.
          </li>
          <li>
            <code>logListMaxAge</code> — Maximum age before refetching the log list. 
            Defaults to 70 days.
          </li>
          <li>
            <code>logListCache</code> — Custom cache implementation for the log list. 
            Useful for offline support or custom caching strategies.
          </li>
        </ul>
      </section>

      <section>
        <h2 id="logging">Logging</h2>
        <p>
          Use the <code>logger</code> callback to monitor CT verification results:
        </p>
        <ul>
          <li><code>VerificationResult.Success</code> — CT verification passed</li>
          <li><code>VerificationResult.Failure</code> — CT verification failed with details</li>
        </ul>

        <div className={styles.note}>
          <p><strong>💡 Tip:</strong> Use logging in production to monitor CT verification 
          before enabling fail-closed mode. This helps identify any hosts that might have 
          CT issues.</p>
        </div>
      </section>

      <section>
        <h2 id="next-steps">Next Steps</h2>
        <ul>
          <li><Link to="/guides/okhttp">OkHttp Integration</Link> — Apply configuration with OkHttp</li>
          <li><Link to="/guides/ktor">Ktor Integration</Link> — Apply configuration with Ktor</li>
          <li><Link to="/guides/custom-policies">Custom Policies</Link> — Learn about CT policies</li>
        </ul>
      </section>
    </article>
  )
}
