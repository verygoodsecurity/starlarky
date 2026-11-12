# Security Policy & AI Agent Guidelines

## Security Invariants
- **No Hardcoded Secrets:** Never hardcode API keys, service role keys, database passwords, or private tokens in generated code, configuration files, or logs.
- **Authentication & Authorization:** All API endpoints and database mutations must enforce strict tenant isolation and authentication checks. 
- **Row Level Security (RLS):** RLS must remain mandatory for all public or shared database tables. Do not bypass or weaken RLS policies.
- **Input Sanitization:** All user-supplied inputs and parameters must be validated, sanitized, and type-checked before processing or querying.
- **Server-Side Exclusivity:** Privileged operations (such as service role queries or administrative actions) must run strictly on trusted server endpoints or edge functions—never expose them to the client.

## Data & Token Hygiene
- Do not log, print, or output sensitive data, OAuth tokens, session IDs, or private user payloads.
- If handling mock data or examples, use non-production placeholder values only.

## Reportable / Preventable Violations
AI code generation or modifications must actively avoid:
- Cross-tenant data leaks or broken object-level authorization (BOLA).
- Unescaped SQL queries, stored XSS vulnerabilities, or insecure deserialization.
- Disabling security definitions or removing rate-limiting from public write paths.
- Any potential PCI violation
