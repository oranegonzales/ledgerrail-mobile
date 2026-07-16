# Security policy

LedgerRail Mobile is a synthetic-data portfolio client. It must never store or transmit real financial data, credentials, personal data, or production secrets.

## Supported version

Security fixes are applied to the latest commit on `main`.

## Reporting a vulnerability

Please report vulnerabilities privately through the repository's **Security → Advisories → New draft security advisory** flow. Do not include secrets or exploit details in a public issue.

## Security boundaries

- The release client connects only to the fixed HTTPS LedgerRail Render endpoint.
- The app contains no API key or other secret.
- Cleartext network traffic and Android backups are disabled.
- Only the Internet permission is requested.
- All displayed transactions are synthetic portfolio data.

Automated tests, dependency review, Dependabot, and CodeQL supplement code review; they do not guarantee the absence of vulnerabilities.

