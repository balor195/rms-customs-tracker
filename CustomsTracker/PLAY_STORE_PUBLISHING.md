# Publishing "Customs Tracker — RMS" to Google Play

`applicationId`: `com.rms.customs` · current version: `1.0.0` (versionCode `1`)

## Before you start: public or private?

This app tracks military/customs clearance data for Royal Medical Services. Before doing anything else, decide how it should be distributed — this changes several steps below.

| Option | Who can install it | Review/listing requirements | Best for |
|---|---|---|---|
| **Managed Google Play (private app)** | Only devices enrolled in your organization's Google Workspace / Android Enterprise | Minimal — no public listing, no content rating, no Data Safety form review by reviewers (still filled in, but not shown publicly) | **Recommended** for an internal RMS-only tool |
| **Closed testing track** | Specific testers you invite by email/group, capped list | Full store listing required, but app stays unsearchable | Pilot with a small group before wider rollout |
| **Public production listing** | Anyone on Google Play | Full listing, content rating, Data Safety, privacy policy, ongoing policy compliance | Only if RMS wants this discoverable by the public |

If you're unsure, start with **Internal testing** (fastest, up to 100 testers, almost no review) and move up a track later — you don't have to pick the final track today.

---

## 1. One-time account setup

1. Go to [play.google.com/console](https://play.google.com/console) and sign in with the Google account that should own the app (ideally an organizational account, not a personal one).
2. Pay the one-time **$25 USD registration fee**.
3. Complete identity verification (personal or organization). For an organization account you'll need a D-U-N-S number or equivalent business documentation — this can take a few days, so do it first if you haven't already.
4. Once verified, click **Create app** in the Play Console.
   - App name: `Customs Tracker — RMS` (matches `app_name` in `strings.xml`)
   - Default language: English (add Arabic later as a second listing language if needed — the app UI already supports RTL)
   - App or game: **App**
   - Free or paid: **Free**
   - Accept the Play Console declarations (US export laws, content guidelines).

---

## 2. Play App Signing (do this before your first upload)

Google Play re-signs your app for distribution. You upload your release build signed with **your** upload key (the keystore you already generated at `CustomsTracker/rms-customs-release.jks`), and Play wraps it with a Google-managed app signing key for the actual Play Store artifact.

1. In Play Console → your app → **Setup → App signing**.
2. Accept **Play App Signing** (the default and recommended path — Google securely stores the final signing key, so you can't permanently lose it even if your local keystore is lost).
3. Your local `rms-customs-release.jks` becomes the **upload key** — you still need it for every future release. **Back it up** (see the warning in `build_apk.bat`'s output and `keystore.properties` — neither is committed to git on purpose).

If `rms-customs-release.jks` is ever lost or compromised, you can request an upload-key reset from Google Play support since you're enrolled in Play App Signing — this is far less painful than losing a non-Play-App-Signing key, where the app would be permanently unable to receive updates.

---

## 3. Build an Android App Bundle (not an APK)

Google Play requires an **`.aab`** (Android App Bundle) for new app submissions, not the `.apk` that `build_apk.bat` currently produces. The same signing config you already set up (`keystore.properties`) applies automatically.

From the `CustomsTracker` folder:

```bat
gradlew.bat bundleRelease
```

Output: `app\build\outputs\bundle\release\app-release.aab`

This uses the exact same `signingConfigs.release` block in `app/build.gradle.kts` as the signed APK path, so it will only produce a signed bundle once `keystore.properties` exists (run `generate_keystore.bat` first if you haven't).

> Tip: if you want a one-click bundle build, ask to add a 3rd "Bundle (AAB)" option to `build_apk.bat` calling `bundleRelease` instead of `assembleRelease` — not included by default since Play Console upload is a manual step regardless.

---

## 4. Versioning every release

Play Console rejects re-uploads with a `versionCode` it has already seen. Before each new build you upload:

In `CustomsTracker/app/build.gradle.kts`:
```kotlin
defaultConfig {
    versionCode = 2          // must increase by at least 1 each upload, forever
    versionName = "1.0.1"    // human-readable, shown to users — your choice
    ...
}
```

`versionCode` is invisible to users and must strictly increase release after release (including testing tracks). `versionName` is what shows on the Play Store page.

---

## 5. Upload the release

1. Play Console → your app → pick a track:
   - **Testing → Internal testing** (fastest to set up, good first step), or
   - **Testing → Closed testing**, or
   - **Production**, or
   - **Setup → Advanced settings → Managed Google Play** if going the private/organization-only route.
2. Click **Create new release**.
3. Upload `app-release.aab`.
4. Add release notes (what changed in this version — required field).
5. Save → Review release → Start rollout.

For **Internal testing** specifically, you also need to add tester emails or a Google Group under that track's **Testers** tab, and share the opt-in link with them so the app shows up in their Play Store.

---

## 6. Store listing requirements (skip this section if going Managed Google Play private-only)

Required under **Grow → Store presence → Main store listing**:

| Asset | Spec |
|---|---|
| App icon | 512×512 PNG, 32-bit with alpha |
| Feature graphic | 1024×500 PNG/JPG |
| Phone screenshots | At least 2, 16:9 or 9:16, JPG/PNG |
| Short description | ≤ 80 characters |
| Full description | ≤ 4000 characters |
| Privacy policy URL | Required if you request any sensitive permission (this app requests CAMERA, notifications, etc. — so yes) |

Also required before you can roll out to production:

- **App content** section: content rating questionnaire, target audience & content, ads declaration (this app has none), Data Safety form (declare what data is collected — given Room/local DB + camera + future networking, be accurate about what's collected vs. stored locally only), government apps declaration if applicable.
- **App access**: if any part of the app is gated behind login (it is — `LoginScreen.kt`/`AdminSetupScreen.kt`), you must provide Google reviewers with test credentials or describe how to obtain access, otherwise review can be rejected as "unable to access full functionality."

---

## 7. Review & rollout

- Internal testing: usually live within minutes, no human review.
- Closed/open testing and Production: first-ever submission typically takes **up to a few days** for review; later updates are usually faster (hours).
- After approval, production rollout can be staged (e.g., 20% → 50% → 100%) under the release's rollout percentage setting — useful for catching issues before all users get the update.

---

## Quick reference: full release checklist

- [ ] Bump `versionCode` / `versionName` in `app/build.gradle.kts`
- [ ] `gradlew.bat bundleRelease` (uses `keystore.properties` — confirm it exists)
- [ ] Upload `.aab` to the chosen track in Play Console
- [ ] Write release notes
- [ ] (First release only) Complete store listing, content rating, Data Safety, privacy policy
- [ ] Roll out
