import { LIBRARY_VERSION, MAVEN_ARTIFACTS } from './constants';

// Helper for artifact coordinates with version
function getArtifactCoordinate(key: keyof typeof MAVEN_ARTIFACTS): string {
  return `${MAVEN_ARTIFACTS[key]}:${LIBRARY_VERSION}`;
}

// ============================================================================
// INSTALLATION EXAMPLES
// ============================================================================

export const versionCatalogConfig = `[versions]
seal = "${LIBRARY_VERSION}"

[libraries]
seal-core = { module = "${MAVEN_ARTIFACTS.core}", version.ref = "seal" }
seal-android = { module = "${MAVEN_ARTIFACTS.android}", version.ref = "seal" }
seal-ios = { module = "${MAVEN_ARTIFACTS.ios}", version.ref = "seal" }
seal-ktor = { module = "${MAVEN_ARTIFACTS.ktor}", version.ref = "seal" }`;

export const gradleDependencies = `kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("${getArtifactCoordinate('core')}")
            implementation("${getArtifactCoordinate('ktor')}")
        }
        androidMain.dependencies {
            implementation("${getArtifactCoordinate('android')}")
        }
        iosMain.dependencies {
            implementation("${getArtifactCoordinate('ios')}")
        }
    }
}`;

// ============================================================================
// OKHTTP EXAMPLES
// ============================================================================

export const okhttpBasicExample = `val client = OkHttpClient.Builder()
    .addNetworkInterceptor(
        certificateTransparencyInterceptor {
            // Include all hosts by default
        }
    )
    .build()

val response = client.newCall(
    Request.Builder()
        .url("https://www.google.com")
        .build()
).execute()`;

export const okhttpDslExample = `val client = OkHttpClient.Builder()
    .addNetworkInterceptor(
        certificateTransparencyInterceptor {
            // Include specific hosts
            +"*.google.com"
            +"*.github.com"
            
            // Exclude hosts
            -"internal.example.com"
            
            // Fail-open (default) or fail-closed
            failOnError = false
            
            // Log verification results
            logger = { host, result ->
                println("CT verification for \$host: \$result")
            }
        }
    )
    .build()`;

export const okhttpNetworkInterceptorNote = `// IMPORTANT: Must be a NETWORK interceptor, not an application interceptor
// Network interceptors have access to the TLS connection details needed for CT verification

// ✅ Correct
client.addNetworkInterceptor(certificateTransparencyInterceptor { })

// ❌ Wrong - won't have access to TLS details
client.addInterceptor(certificateTransparencyInterceptor { })`;

// ============================================================================
// KTOR EXAMPLES
// ============================================================================

export const ktorBasicExample = `val client = HttpClient(OkHttp) {  // or HttpClient(Darwin) on iOS
    install(CertificateTransparency) {
        // Include all hosts by default
    }
}

val response: HttpResponse = client.get("https://www.google.com")`;

export const ktorDslExample = `val client = HttpClient(OkHttp) {
    install(CertificateTransparency) {
        // Include specific hosts
        +"*.google.com"
        +"*.github.com"
        
        // Exclude hosts  
        -"internal.example.com"
        
        // Fail-open (default) or fail-closed
        failOnError = false
        
        // Log verification results
        logger = { host, result ->
            println("CT: \$host -> \$result")
        }
    }
}`;

export const ktorMultiplatformExample = `// commonMain - same code works on Android and iOS
val client = HttpClient {
    install(CertificateTransparency) {
        +"*.example.com"
    }
}

// The plugin automatically uses:
// - OkHttp engine on Android (with Conscrypt for SCT extraction)
// - Darwin engine on iOS (with SecTrust for CT verification)`;

// ============================================================================
// CUSTOM POLICIES EXAMPLES
// ============================================================================

export const customPolicyExample = `// Use Chrome's CT policy (default)
val chromePolicy = ChromeCtPolicy()

// Use Apple's CT policy
val applePolicy = AppleCtPolicy()

// Apply in configuration
certificateTransparencyInterceptor {
    policy = applePolicy
}`;

// ============================================================================
// iOS EXAMPLES
// ============================================================================

export const iosUrlSessionExample = `// iOS uses SecTrust for CT verification
// The Ktor Darwin engine handles this automatically

val client = HttpClient(Darwin) {
    install(CertificateTransparency) {
        +"*.example.com"
        failOnError = false
    }
}`;

// ============================================================================
// CONFIGURATION EXAMPLES
// ============================================================================

export const configurationFullExample = `certificateTransparencyInterceptor {
    // Host matching
    +"*.google.com"           // Include hosts matching pattern
    +"*.github.com"
    -"internal.example.com"   // Exclude specific hosts
    
    // CT Policy
    policy = ChromeCtPolicy() // or AppleCtPolicy()
    
    // Error handling
    failOnError = false       // false = fail-open (default), true = fail-closed
    
    // Logging
    logger = { host, result ->
        when (result) {
            is VerificationResult.Success -> 
                println("✅ CT passed for \$host")
            is VerificationResult.Failure -> 
                println("❌ CT failed for \$host: \$result")
        }
    }
    
    // Log list configuration
    logListUrl = "https://www.gstatic.com/ct/log_list/v3/log_list.json"
    logListMaxAge = 70.days
    
    // Optional custom cache
    logListCache = myCustomCache
}`;

// Type for code example keys
export type CodeExampleKey = keyof typeof codeExamples;

// Export as object for dynamic access
export const codeExamples = {
  versionCatalogConfig,
  gradleDependencies,
  okhttpBasicExample,
  okhttpDslExample,
  okhttpNetworkInterceptorNote,
  ktorBasicExample,
  ktorDslExample,
  ktorMultiplatformExample,
  customPolicyExample,
  iosUrlSessionExample,
  configurationFullExample,
} as const;
