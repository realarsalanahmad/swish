# Swish privacy policy

Last updated: **29 October 2026**. Maintainer: **ArsDevs**.

This policy describes the code in this repository. A separately deployed account
service or third-party provider may have additional terms and retention rules.

## Data on your device

Swish keeps watch/search/listening history, subscriptions, likes, playlists,
downloads, settings and learned recommendation profiles in local databases,
preferences or app files. The recommendation engines process their signals
locally. Playing online media still makes network requests to content providers.

Google Takeout is optional. Selecting ZIPs lets the app read supported history,
likes, search, subscription and playlist records and use them for your library
and interests. The account-profile save does not upload the original ZIP files,
full local history, downloads or complete playlists.

## Optional Swish accounts

If you create an account, the backend stores your name, normalized email,
account identifier, password hash and creation time. Passwords use salted
scrypt hashes; the database does not store them in plaintext. Session records
store a hash of the bearer token, the account identifier and expiration time.
Sessions currently expire after 30 days. The Android app encrypts its stored
session using Android Keystore-backed protection.
Pending imported recommendation profiles are also encrypted on the device.

When you save personalization, the backend stores serialized video and music
profiles under your account identifier with a save time. These can contain
learned topics, language preferences, recent imported recommendation seeds,
video/channel identifiers and personalization settings. Short-form interests
are included in the video profile. This is personal data even though it is
not a complete copy of your Takeout archive.

Swish attempts this profile save automatically when importing while signed in
or when signing in with a pending local import. A pending import is saved before
an older cloud profile is restored. Failed uploads retain the pending copy for
a later sign-in or manual save. A claimed pending import is associated with its
account so a different account cannot upload it. Without a compatible pending
import, signing in attempts to restore an existing saved profile. The account
screen also provides manual save and restore. There is no continuous cloud
synchronization after every watch or listen.

The supplied backend uses MongoDB; the configured deployment may use services
such as MongoDB Atlas and Vercel. They receive the requests needed to operate
the service, including network and operational information. Their policies and
the operator's deployment settings govern their infrastructure logs and backups.
Profile traffic uses the HTTPS backend endpoint configured by the distributor.

## Retention, reset and deletion

- Local data remains until you delete it, clear app storage or uninstall. Files
  exported or downloaded to shared storage can remain after uninstalling.
- Saved account profiles have no automatic expiration in the supplied backend.
  They remain until replaced by a save/reset or deleted by the service operator.
- **Reset saved personalization** resets selected recommendation profiles locally
  and replaces the saved account profile. It clears learned interests and imported
  recommendation seeds in those profiles; it does not delete the account or local
  library/history. If the cloud update fails, previous local personalization is
  restored and the app reports the failure.
- A successful personalization reset clears the pending import as well. Pending
  imports otherwise remain in local app storage until saved, reset or cleared
  with app storage. Signing out keeps this pending data and the local library.
- **Sign out** revokes the current server session and removes the local session
  when the request succeeds. It does not delete the saved profile or local data.
- Clearing app storage does not delete server-side account data. The current
  service does not provide an in-app account-deletion endpoint. For account-data
  deletion requests, contact the operator of the backend you used. For this
  project's operator/contact information, use the maintainer profile below.
- Android backup may back up selected settings, and device transfer may copy
  local library data according to the supplied backup rules and your OS settings.
  Exported backups and device sync are separate actions you initiate.

## Third-party requests

The app contacts YouTube/Google for streams, search, thumbnails, channels and
metadata. Depending on your enabled features, it may also contact:

| Feature | Data needed for the request | Provider examples |
| --- | --- | --- |
| Video playback fallback | Video identifier and extractor request | YouTube, PipePipe signature helper |
| Lyrics | Track/artist identifiers or search terms | LRCLIB and other configured lyrics providers |
| Song recognition | Audio fingerprint computed from a requested microphone sample | Shazam |
| SponsorBlock / DeArrow / dislikes | Video identifier | SponsorBlock, DeArrow, Return YouTube Dislike |
| Release information | Repository/release request and ordinary network metadata | GitHub |
| Discord presence | Playback title, creator, artwork and progress | Discord, when linked and enabled |
| Device sync / casting | Library transfer or media/local-network requests | Your chosen peer or cast device |

Providers normally receive your IP address and standard request metadata.
An explicitly configured proxy may also receive traffic and destination
information. Swish is not a VPN. Third-party providers follow their own policies.

## Permissions

Network and foreground-service permissions support online playback, downloads
and background audio. Optional permissions are requested for relevant features:
notifications; media/storage access for local files and downloads; camera for
device-sync QR codes; microphone for song recognition; popup overlays; and
battery-optimization exemption for background features. Network/Wi-Fi/multicast
access supports casting and device sync. The APK can also contain permissions
merged from its libraries. Check Android app settings to review or revoke them.

Swish does not declare location permission or use passive microphone/camera
monitoring. The inspected source does not include an advertising SDK or an
analytics/automatic crash-upload service. Ad-related entries inside a Takeout
export are source data, not app advertisements. User-initiated diagnostics,
exports and bug reports can contain personal information; review them before
sharing. No claim is made that every external provider is tracking-free.

## Contact and changes

Contact **ArsDevs** through
[github.com/realarsalanahmad](https://github.com/realarsalanahmad). Do not publish
passwords, tokens, Takeout archives or personal histories in public issues.
This policy will be updated when implemented storage or network behavior changes.
