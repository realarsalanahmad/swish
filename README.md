<div align="center">
  <img src="branding/logo.png" alt="Swish logo" width="180">
  <h1>Swish</h1>
  <p><strong>Your videos. Your music. Your taste.</strong></p>
  <p>An independent Android video and music client, maintained by <a href="https://github.com/realarsalanahmad">ArsDevs</a>.</p>
  <p><strong>Android 8.0+</strong> &nbsp; · &nbsp; <strong>Kotlin + Compose</strong> &nbsp; · &nbsp; <strong>GPL-3.0</strong></p>
  <p><a href="#get-started">Get started</a> · <a href="#features">Features</a> · <a href="#build-it-yourself">Build</a> · <a href="privacy.md">Privacy</a> · <a href="CONTRIBUTING.md">Contribute</a></p>
</div>

---

Swish brings video playback, a dedicated music player, Shorts and a personal
library into one app. Its recommendation engines learn on your device. An
optional Swish account can save your learned interests for later restoration.

## Features

| Explore | Play | Keep |
| :--- | :--- | :--- |
| Personalized video Home | Video quality and audio-track selection | Watch history and subscriptions |
| Dedicated music discovery | Music queues, lyrics and playback controls | Likes, playlists and saved Shorts |
| Vertical Shorts feed | Background playback and picture-in-picture | Downloads and local media |
| Takeout import from Settings | Playback speed, captions and gestures | Backup, restore and device sync |

Optional integrations include SponsorBlock, DeArrow, Return YouTube Dislike,
song recognition and Discord presence. Their providers receive the requests
described in the [privacy policy](privacy.md).

## Get started

1. Install a Swish APK that you built or obtained from this repository's releases.
2. Browse without an account, or open **Settings → Swish account** to sign in.
3. Open **Settings → Backup** to import Google Takeout ZIPs. Select all parts of
   a split export together; more exports can be added later.
4. Watch and listen to teach the local engines. Use **Your taste** to inspect
   interests, hide unwanted content or reset personalization.

### Bring your YouTube interests

[Google Takeout](https://takeout.google.com/) exports a snapshot of your data.
Select **YouTube and YouTube Music**, include history/subscriptions/playlists,
choose JSON history when available, then export ZIPs and import them in Swish.

Import before signing in or while signed in. Swish keeps an encrypted pending
profile on the device and attempts to save the learned video, Shorts and music
interests to your account. On sign-in, a pending import takes precedence over an
older saved profile. An unsuccessful upload keeps the pending copy for a later
sign-in or manual save. The account screen also has **Save personalization** and
**Restore saved personalization**. The full ZIPs, local library and complete
history are not uploaded by this save.

**Reset saved personalization** replaces the local and saved account taste.
It does not delete the account, library or downloads. If the cloud update fails,
the previous local profile is restored. See [privacy.md](privacy.md) for storage
and retention details.

### Discovery and viewing

Home balances recognizable content categories and gives recent uploads a boost
based on the channel's observed posting frequency. Unrequested horror-story
content and explicit AI horror bait are filtered using available metadata.
Takeout records explicitly marked as advertising activity are excluded from
imported watch history. Reimport the export to identify matching older ad entries.

While a Short plays, the overlay shows the channel name, like and comment actions.
Pause to see its title and other controls. The Home logo follows your selected
theme, and the Developer entry opens the maintainer's GitHub profile.

> Takeout is a snapshot, not a live YouTube connection. Swish's engines do not
> reproduce YouTube's private recommendation model. Content classification uses
> available metadata and can make mistakes.

## Build it yourself

You need **JDK 21**, **Android SDK platform 37.0**, **Build Tools 36.1.0** and the
included Gradle wrapper. The application ID is `dev.arsdevs1.swish`;
development builds append `.debug`.

```bash
git clone https://github.com/realarsalanahmad/Swish.git
cd Swish
# Copy local.properties.example to local.properties and set sdk.dir.
./gradlew :app:assembleGithubDebug
```

On Windows use `gradlew.bat` in PowerShell. APKs appear in
`app/build/outputs/apk/github/debug/`. The universal APK runs on every supported
ABI; a split APK is smaller and must match the device.

| Flavor | Command | Included integrations |
| :--- | :--- | :--- |
| `github` | `./gradlew :app:assembleGithubDebug` | App plus optional Discord integration |
| `foss` | `./gradlew :app:assembleFossDebug` | App without Discord integration |

The built-in release updater is currently disabled in both flavors. Release
signing requires your own keystore and credentials; debug APKs are development
builds and do not establish a production signing identity.

<details>
<summary><strong>Connect your own account backend</strong></summary>

The optional backend uses Node.js, Express and MongoDB. Backend setup is
documented in the companion service's README. Configure the app with:

```bash
./gradlew :app:assembleGithubDebug -PswishApiBaseUrl=https://YOUR_BACKEND_HOST
```

In GitHub Actions, set the repository variable `SWISH_API_BASE_URL` to your
HTTPS endpoint. With no endpoint configured, account actions are unavailable;
local playback and discovery still work. Database credentials belong only in
the backend environment, never in the app or repository.

</details>

## Development and checks

```bash
./gradlew ktlintCheck
./gradlew :app:testGithubDebugUnitTest :app:testFossDebugUnitTest
./gradlew :app:assembleGithubDebug :app:assembleFossDebug
```

The repository includes a workflow that runs checks and uploads development
APKs as Actions artifacts. It does not automatically publish a release.
See [CONTRIBUTING.md](CONTRIBUTING.md) and [SECURITY.md](SECURITY.md).

To create a source-only archive for GitHub or a release:

```bash
python scripts/prepare_github.py --output ../Swish-github-source.zip
```

## Privacy and rights

Recommendation processing happens on the device; optional account storage and
third-party network requests are explained in [Privacy](privacy.md).
Read [Terms](TERMS.md), [Copyright and notices](COPYRIGHT.md) and
[GPL-3.0](LICENSE). Swish is not affiliated with or endorsed by YouTube or Google.

**Maintained by ArsDevs** · [GitHub](https://github.com/realarsalanahmad) ·
[YouTube](https://www.youtube.com/@ArsDevS1)
