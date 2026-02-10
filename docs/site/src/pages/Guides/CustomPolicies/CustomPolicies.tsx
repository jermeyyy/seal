import { Link } from 'react-router-dom'
import CodeBlock from '@components/CodeBlock/CodeBlock'
import styles from '../Guides.module.css'
import { customPolicyExample, customPolicyCreationExample, customPolicyUsageExample } from '@data/codeExamples'

export default function CustomPolicies() {
  return (
    <article className={styles.guides}>
      <h1>Custom Policies</h1>
      <p className={styles.intro}>
        Seal supports multiple Certificate Transparency policies. Learn about 
        the available policies and how to configure them for your needs.
      </p>

      <section>
        <h2 id="what-is-ct-policy">What is a CT Policy?</h2>
        <p>
          A CT Policy defines the rules for determining whether a certificate has 
          sufficient Certificate Transparency coverage. Different vendors (Google, Apple) 
          have different requirements for how many SCTs (Signed Certificate Timestamps) 
          a certificate needs and from which logs they must come.
        </p>
      </section>

      <section>
        <h2 id="chrome-ct-policy">Chrome CT Policy</h2>
        <p>
          The Chrome CT Policy is the default policy used by Seal. It mirrors the 
          requirements that Google Chrome uses for CT enforcement:
        </p>
        <ul>
          <li>Requires SCTs from multiple independent CT logs</li>
          <li>SCTs must come from logs that are currently trusted</li>
          <li>Number of required SCTs depends on certificate lifetime</li>
          <li>At least one SCT must be from a Google-operated log and one from a non-Google log</li>
        </ul>
      </section>

      <section>
        <h2 id="apple-ct-policy">Apple CT Policy</h2>
        <p>
          The Apple CT Policy follows Apple's requirements for Certificate Transparency. 
          Apple has its own set of trusted CT logs and verification rules:
        </p>
        <ul>
          <li>Used by Safari and Apple platforms</li>
          <li>Has its own log diversity requirements</li>
          <li>May have different SCT count requirements than Chrome</li>
        </ul>
      </section>

      <section>
        <h2 id="usage">Using Policies</h2>
        <CodeBlock code={customPolicyExample} language="kotlin" title="Policy Configuration" />

        <div className={styles.note}>
          <p><strong>💡 Note:</strong> The <code>ChromeCtPolicy</code> is the default. 
          You only need to explicitly set a policy if you want to use 
          <code>AppleCtPolicy</code> or a custom implementation.</p>
        </div>
      </section>

      <section>
        <h2 id="custom-policy">Creating a Custom Policy</h2>
        <p>
          <code>CTPolicy</code> is a <code>fun interface</code> with a single <code>evaluate</code> method, 
          making it easy to create your own policy — either as a class or a lambda.
        </p>
        <p>
          The <code>evaluate</code> method receives two parameters:
        </p>
        <ul>
          <li><code>certificateLifetimeDays</code> — the lifetime of the certificate in days, useful for scaling SCT requirements</li>
          <li><code>sctResults</code> — a list of <code>SctVerificationResult</code> values, each either <code>Valid</code> (with the SCT and log operator) or an <code>Invalid</code> subtype such as <code>FailedVerification</code>, <code>LogNotTrusted</code>, <code>LogExpired</code>, <code>LogRejected</code>, or <code>SignatureMismatch</code></li>
        </ul>
        <p>
          The method returns a <code>VerificationResult</code>: either <code>Success.Trusted</code> with the 
          valid SCTs, or a <code>Failure</code> variant such as <code>NoScts</code>, <code>TooFewSctsTrusted</code>, 
          <code>TooFewDistinctOperators</code>, or <code>LogServersFailed</code>.
        </p>

        <CodeBlock code={customPolicyCreationExample} language="kotlin" title="Custom Policy Implementation" />

        <div className={styles.note}>
          <p><strong>💡 Note:</strong> Because <code>CTPolicy</code> is a <code>fun interface</code>, 
          you can use a lambda expression for simple policies instead of creating a full class.</p>
        </div>

        <p>
          To use your custom policy, pass it to the <code>policy</code> property in the 
          configuration block:
        </p>

        <CodeBlock code={customPolicyUsageExample} language="kotlin" title="Using a Custom Policy" />
      </section>

      <section>
        <h2 id="policy-comparison">Policy Comparison</h2>
        <table>
          <thead>
            <tr>
              <th>Feature</th>
              <th>Chrome Policy</th>
              <th>Apple Policy</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>Default in Seal</td>
              <td>Yes</td>
              <td>No</td>
            </tr>
            <tr>
              <td>Log diversity</td>
              <td>Google + non-Google</td>
              <td>Apple requirements</td>
            </tr>
            <tr>
              <td>Lifetime-based SCT count</td>
              <td>Yes</td>
              <td>Yes</td>
            </tr>
          </tbody>
        </table>
      </section>

      <section>
        <h2 id="next-steps">Next Steps</h2>
        <ul>
          <li><Link to="/guides/configuration">Configuration Reference</Link> — All available options</li>
          <li><Link to="/guides/okhttp">OkHttp Integration</Link> — Apply policies with OkHttp</li>
          <li><Link to="/guides/ktor">Ktor Integration</Link> — Apply policies with Ktor</li>
        </ul>
      </section>
    </article>
  )
}
