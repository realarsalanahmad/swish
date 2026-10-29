# Swish development snapshot

29 October 2026 · Maintained by ArsDevs · App version unchanged.

## Improved

- Home balances recognizable content categories and favors recent uploads using
  a channel's observed posting frequency.
- Unrequested horror stories and explicit AI horror bait are filtered from Home.
- Shorts show channel name, like and comments during playback; pause reveals
  the remaining controls. Buttons are smaller and placed lower.
- The Home logo follows the selected theme. Developer opens ArsDevs on GitHub.
- Swish source, branding, build instructions and privacy/legal documents are
  organized for GitHub. Build automation produces development APK artifacts.

## Fixed

- Takeout interests imported before sign-in are saved when signing in, before an
  older account profile can overwrite them. Imports while signed in attempt an
  automatic save. Failed uploads retain an encrypted pending copy.
- Successful personalization reset replaces the account's saved taste and clears
  pending imported interests. A failed server reset restores local taste.
- Explicit Takeout advertising activity is excluded from imported history.
  Reimporting identifies matching older ad stubs while preserving actual playback
  history. Records without ad metadata cannot reliably be identified.
- Screenshots are permitted in the account dialog.

Content filters depend on metadata and can miss or misclassify videos. This
snapshot is a development build; it has not been published as a GitHub release.
