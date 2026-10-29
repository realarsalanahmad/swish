# Swish security

Maintained by ArsDevs. Security fixes target the current source and latest
maintained release; support windows and response deadlines are not guaranteed.

## Report a vulnerability

Use this repository's GitHub **Security → Report a vulnerability** option when
private reporting is enabled. Otherwise contact the maintainer through
[GitHub](https://github.com/realarsalanahmad) to arrange a private channel.
Do not post credentials, session tokens, Takeout files or exploitable details in
public issues. Include the affected commit/build, steps and impact, with private
data removed. Please allow coordination before public disclosure.

## Protect credentials

Keep `.env`, `local.properties`, keystores, signing passwords, database URIs and
private exports out of Git. Use repository secrets for signing credentials and
backend deployment secrets for MongoDB. Configure an HTTPS account endpoint.
Do not embed database credentials in the Android app.

## Verify an APK

```bash
apksigner verify --print-certs swish.apk
```

Compare the reported certificate with the fingerprint published for the exact
Swish release. A production Swish signing fingerprint has not been established
in these documents. Debug builds use development keys; checksums verify a file's
integrity but do not independently prove who published it. Distribution and
signing identity must be established before treating a release as official.
