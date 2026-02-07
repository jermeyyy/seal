package com.jermey.seal.core.loglist

/**
 * Embedded baseline CT log list data.
 * This is a bundled subset of the Google CT Log List V3 format, used as a fallback
 * when the network log list is unavailable and no cached version exists.
 *
 * This should be periodically updated when new library versions are released.
 * Last updated: 2025-01-01
 */
internal object EmbeddedLogListData {
    /**
     * The embedded log list JSON string in V3 format.
     * Contains a curated set of well-known, active CT logs.
     */
    val json: String = """
{
  "version": "3",
  "log_list_timestamp": "2025-01-01T00:00:00Z",
  "operators": [
    {
      "name": "Google",
      "email": ["google-ct-logs@googlegroups.com"],
      "logs": [
        {
          "description": "Google 'Argon2025' log",
          "log_id": "TnWjJ1yaEMM4W2zU3z9S6x3w4I4bjWnAsfpksWKaOd8=",
          "key": "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEIIKh+WdoHN9sI6wG4bMTkslz4sMnRNW/dkENaeH3bwMIX5KLCHCS0uo12gJh+BqdCzPILEK3GIpNE5U0rta6Cg==",
          "url": "https://ct.googleapis.com/logs/us1/argon2025/",
          "mmd": 86400,
          "state": {"usable": {"timestamp": "2024-01-01T00:00:00Z"}},
          "temporal_interval": {"start_inclusive": "2025-01-01T00:00:00Z", "end_exclusive": "2026-01-01T00:00:00Z"}
        },
        {
          "description": "Google 'Argon2026' log",
          "log_id": "EvFONL1TckyEBhnDjz96E/jntY+R6qIfCJ6HEelcrHo=",
          "key": "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEggJm1DgiRI55/3YRdSL5BTFxrdI1sW+kcpbbMGIuljwH2gwtclRvB0VWMfhU+JT+7Z82qpe79p3piTXxiwlrzA==",
          "url": "https://ct.googleapis.com/logs/us1/argon2026/",
          "mmd": 86400,
          "state": {"usable": {"timestamp": "2024-06-01T00:00:00Z"}},
          "temporal_interval": {"start_inclusive": "2026-01-01T00:00:00Z", "end_exclusive": "2027-01-01T00:00:00Z"}
        }
      ]
    },
    {
      "name": "Cloudflare",
      "email": ["ct-logs@cloudflare.com"],
      "logs": [
        {
          "description": "Cloudflare 'Nimbus2025' Log",
          "log_id": "zPsPaoVxCWX+lZtTzumyfCLphVwNl422qX5UwP5M3e0=",
          "key": "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEGoAaFRkZI3m0+qB5jo3VwYpvB3fFLEEW0qxJOOjaBkp3GXqBcSV9b+Wce4LDFhSjueIt0m3lEoMEoI/xbpl8A==",
          "url": "https://ct.cloudflare.com/logs/nimbus2025/",
          "mmd": 86400,
          "state": {"usable": {"timestamp": "2024-01-01T00:00:00Z"}},
          "temporal_interval": {"start_inclusive": "2025-01-01T00:00:00Z", "end_exclusive": "2026-01-01T00:00:00Z"}
        }
      ]
    },
    {
      "name": "Let's Encrypt",
      "email": ["sre@letsencrypt.org"],
      "logs": [
        {
          "description": "Let's Encrypt 'Oak2025' log",
          "log_id": "ouMK5EXvva2bfjjtR2d3U9eCW4SU1yteGyzEuVCkR+c=",
          "key": "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEKeBr1FWjEHFi9IVBQOhSzCMExSfDhMONPNh+g9Gze6FCahCMp3bYSlWjFMAHNNjhCFiW0dVpERoXs3gR5mz0mQ==",
          "url": "https://oak.ct.letsencrypt.org/2025/",
          "mmd": 86400,
          "state": {"usable": {"timestamp": "2024-01-01T00:00:00Z"}},
          "temporal_interval": {"start_inclusive": "2025-01-01T00:00:00Z", "end_exclusive": "2026-01-01T00:00:00Z"}
        }
      ]
    },
    {
      "name": "DigiCert",
      "email": ["ctops@digicert.com"],
      "logs": [
        {
          "description": "DigiCert Yeti2025 Log",
          "log_id": "fVkeEuF4KnscYWd8iol7tkS+C0lzmEcqVj9xsXCWqXA=",
          "key": "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE35UAXhDBAIpRa5TsmK0M+AvJ55UbkAR5qcG3i0BO7MbWGCahFPgNbFDbMNri7cXb2wtDF+VqJkv4htRIWlT+0g==",
          "url": "https://yeti2025.ct.digicert.com/log/",
          "mmd": 86400,
          "state": {"usable": {"timestamp": "2024-01-01T00:00:00Z"}},
          "temporal_interval": {"start_inclusive": "2025-01-01T00:00:00Z", "end_exclusive": "2026-01-01T00:00:00Z"}
        }
      ]
    }
  ]
}
""".trimIndent()
}
