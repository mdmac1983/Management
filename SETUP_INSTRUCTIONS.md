# Setting Up Management From Your Phone (No PC, No Termux, No Git)

This gets the project onto GitHub and building into an APK using only the GitHub mobile web site
(or app) and GitHub Actions. You never need to unzip anything on your device.

## 1. Create the repo

1. On github.com (mobile browser is fine), tap **+** > **New repository**.
2. Name it (e.g. `Rentals`), keep it **Private** if you want, and create it. Leave it empty (no
   README/license) - if it auto-adds a README, that's fine, just proceed.

## 2. Paste in the three workflow files

For each of the three files below, do: repo page > **Add file** > **Create new file** > type the
path exactly as shown (GitHub will auto-create the folders) > paste the contents > **Commit
changes** directly to `main`.

1. Path: `.github/workflows/unzip.yml` → paste the contents of the `unzip.yml` file from this
   project.
2. Path: `.github/workflows/build-apk.yml` → paste the contents of `build-apk.yml`.
3. Path: `.github/workflows/bootstrap.yml` → paste the contents of `bootstrap.yml`.

## 3. Upload the whole project as one zip

1. Repo page > **Add file** > **Upload files**.
2. Upload `project.zip` (the zip you were given alongside this document) - do NOT extract it
   first, just upload the single zip file itself, to the repo root.
3. Commit directly to `main`.

## 4. Run the Bootstrap workflow

1. Go to the **Actions** tab.
2. Click **Bootstrap Full Project** in the left sidebar.
3. Click **Run workflow** (top right) > **Run workflow** again to confirm.
4. Wait for it to finish (green check). It extracts `project.zip` into the repo, deletes the zip,
   commits the real project, and automatically starts the **Build APK** workflow for you.

## 5. Get your APK

1. Actions tab > **Build APK** > open the newest run.
2. Once it finishes, scroll down to **Artifacts** to download the APK directly, OR check the
   repo's **Releases** page (right sidebar on the repo home page) - every build also publishes a
   Release with the APK attached, which is usually the easier download on mobile.
3. Download the `.apk` and open it on your phone to install (you may need to allow "Install
   unknown apps" for your browser/Files app the first time).

## Making changes later

- **Small edits** (one or two files): use GitHub's web editor (pencil icon on any file, or
  **Add file > Create new file**) and commit directly to `main`. **Build APK** runs
  automatically on every push to `main`.
- **Bigger changes** (many files at once): zip them up on your phone, upload the zip to the repo
  root the same way as step 3 above, and commit. **Unzip Project** will detect the `.zip`,
  extract it, and automatically chain into **Build APK** - no need to run Bootstrap again
  (that one's only for the very first full setup, or if you ever want to replace the entire
  project wholesale).
- Every build bumps the version by `+0.1` and adds a line to the in-app changelog
  (Settings > Changelog) automatically - you don't need to do anything for that.

## Optional: real release signing

By default every build is debug-signed (fine for installing on your own devices). If you want a
real signed release APK later, generate a keystore, add a `keystore.properties` file at the repo
root (via the same "Create new file" method - though for real use you'd want to keep this out of
a public repo) with:

```
storeFile=your-keystore-file-name.jks
storePassword=...
keyAlias=...
keyPassword=...
```

and upload the actual `.jks` keystore file next to it. `app/build.gradle.kts` automatically picks
this up and signs release builds with it instead of falling back to debug signing.
