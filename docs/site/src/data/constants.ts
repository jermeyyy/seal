export const LIBRARY_VERSION = '0.1.0';

export const MAVEN_ARTIFACTS = {
  core: 'io.github.jermeyyy:seal-core',
  android: 'io.github.jermeyyy:seal-android',
  ios: 'io.github.jermeyyy:seal-ios',
  ktor: 'io.github.jermeyyy:seal-ktor',
} as const;

export const REPOSITORY_URLS = {
  github: 'https://github.com/jermeyyy/seal',
  mavenCentral: 'https://central.sonatype.com/search?q=io.github.jermeyyy',
  apiDocs: '/seal/api/index.html',
} as const;
