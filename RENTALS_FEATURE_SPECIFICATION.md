# Management — Full Application Feature Specification

Version covered: **1.6** (current shipped build). Package: `app.orionmd.management` (renamed from `app.orionmd.rentals` at v1.6 — see §13).

## 1. Product Summary

Management is a native Android app for a solo owner-operator to run a booking-and-inventory business entirely offline and on-device: schedule appointments, track who owes what, manage store-item inventory alongside service bookings, and see per-client and per-item history — including a phone-time contract system for clients billed by the hour, and a Goods/inventory system for tracking cost, sale price, and stock across every inflow and outflow. All data is encrypted at rest, there is no server or account system, and the app is built specifically for low-end Android Go hardware.

## 2. Platform & Technical Foundation

- **Stack:** Kotlin, Jetpack Compose (Material 3), Navigation-Compose, Room (persistence) over SQLCipher (encryption-at-rest), Kotlin Coroutines/Flow for reactive data.
- **Target hardware:** `minSdk`/`targetSdk` 29 (Android 10, "Go edition" class devices), packaged for the `armeabi-v7a` ABI only — this keeps the APK small and matches the 32-bit ARM hardware these devices run, at the cost of not supporting 64-bit-only devices.
- **Distribution:** sideloaded, not published to Google Play (there is no Play Store account or listing) — release builds are signed with a debug-equivalent key unless the owner supplies a real keystore (`app/keystore.properties`, gitignored).
- **Build size (v1.6):** ~8.8 MB release APK, minified and shrunk with R8/ProGuard.
- **CI/CD:** Three GitHub Actions workflows, since the owner works from an Android device with no PC or git CLI:
  - `unzip.yml` — triggered by pushing a `.zip` to the repo root; extracts it in place and commits.
  - `build-apk.yml` — chained to run after `unzip.yml` (also runs on any other push, or manually); bumps `versionName` by +0.1 (rolling `.9` → next major, e.g. `1.9` → `2.0`), inserts a changelog entry into the app's compiled-in changelog file, builds the debug APK, and publishes a GitHub Release with the APK attached.
  - `bootstrap.yml` — manual-only (`workflow_dispatch`); extracts a *whole-project* zip into the repo root for first-time setup, then triggers `build-apk.yml`.
- **Versioning:** `versionName` is the single source of truth (e.g. `1.6`); `versionCode` is derived from it mechanically (`160`). Every build increments `versionName` by 0.1.

## 3. Security & Onboarding

- **First run:** the owner is walked through choosing a lock type — a numeric PIN (4–8 digits), a 3×3 connect-the-dots pattern, or a text password — before any other screen is reachable.
- **Encryption architecture:**
  - The chosen credential is run through PBKDF2 to derive a database passphrase; the SQLite/Room database is opened via SQLCipher using that key, so all booking, client, goods, and settings data is encrypted at rest.
  - A separate fast-path verifier is stored using an Android Keystore-backed AES-GCM key (via `EncryptedSharedPreferences`), so the app can check "is this the right PIN/pattern/password?" on every unlock without needing to touch the encrypted database first.
  - The raw credential itself is never stored — only its derived key material and the Keystore-wrapped verifier.
- **Changing the lock:** Settings → App Lock → Change Lock walks through re-verifying the current credential, choosing a new type if desired, and entering/confirming the new one. On success the live database is re-keyed (`PRAGMA rekey`) in place — no data loss, no re-onboarding.
- **Offline-only:** the app declares no internet permission it needs and ships a network security config that blocks cleartext traffic; nothing about a client's data ever leaves the device except via the explicit backup export described in §10.

## 4. The v1.6 Rewrite: Package Rename & Redesign

Version 1.6 is a large, mostly user-driven redesign rather than an incremental update:

- **App identity:** the app was renamed from "Rentals" to "Management," with a new app icon, splash screen, and translucent tab-background watermark generated from the owner's own logo image. Because Android ties an app's identity to its package name (`applicationId`), and that changed from `app.orionmd.rentals` to `app.orionmd.management`, **Android treats this as a brand-new app** — it installs side-by-side with, not over, the old one, and the old app must be uninstalled separately. See §13 for the exact migration steps.
- **Navigation restructure:** the standalone Booking tab (month calendar) was removed; its "add appointment" role moved into the Schedule tab (see §6). A new Database tab was added, consolidating Customers, Services, Goods, Rates, and Payment Methods management in one place (see §8).
- **Inventory/goods tracking:** the app's scope expanded from purely time-based service bookings to also tracking physical store-item inventory — cost, sale price, quantity on hand, and a full inflow/outflow ledger — so goods sales roll into the same Revenue and Analytics the booking business already relies on.
- **Multi-currency unit:** a "Mackerels" payment unit was added alongside Dollars and Books, with its own owner-editable conversion rate into Books (which then converts to dollars as before).
- **Theming:** three selectable themes — Day, Night, and Material Gray — replace the previous system-default-only light/dark behavior.

## 5. Data Model

All entities live in one SQLCipher-encrypted Room database (`rentals_encrypted.db`), currently at schema version 5.

| Entity | Key fields | Notes |
|---|---|---|
| `CustomerEntity` | `name`, `notes`; `hasContract`, `contractPrice`, `contractHours`, `contractStartEpochDay`; `customCounterResetEpochDay` | Contract and counter fields added in the v1.0→v1.1 migration. Managed from the Database tab; also addable inline from the booking form. |
| `ServiceEntity` | `name`, `requiresTimeSlot`, `isDefault` | Seeded with a default "Phone" service (time-slot required). Moved from Settings to the Database tab in v1.6. |
| `RateTypeEntity` | `name` | Seeded with Hourly / Session / Other. Moved from Settings to the Database tab in v1.6. |
| `PaymentMethodEntity` | `name` | Seeded with Books / Cash App / Zelle / Chime / PayPal / Other. Moved from Settings to the Database tab in v1.6. |
| `BookingEntity` | `dateEpochDay`, `customerId`/`customerName`, `serviceId`/`serviceName`, `startMinute`/`endMinute` (nullable), `timeNotApplicable`, `rateType`, `paymentMethod`, `paymentAmount`, `paymentUnit` (dollars/books/mackerels), `paymentStatus` (paid/partial/unpaid), `amountPaid`, `notes` | One row per scheduled appointment. `paymentUnit` gained `MACKERELS` in v1.6. |
| `GoodsEntity` **(new v1.6)** | `name`, `costPrice`, `salePrice`, `quantityOnHand`, `lowStockThreshold` | Current-state snapshot of one store item. Managed from the Database tab's Goods section. |
| `GoodsTransactionEntity` **(new v1.6)** | `goodsId`/`goodsName`, `type` (IN/OUT), `quantity`, `unitSalePrice`, `unitCost`, `dateEpochDay`, `notes`, `createdAt` | An immutable ledger row per Restock (IN) or Sell (OUT) action. Cost/price are captured at the time of the transaction, so later edits to an item's current pricing never rewrite past history — this is what Revenue and Analytics read to compute goods profit. |
| `AppSettingsEntity` | `bookToDollarRate` (default 8.0), `mackerelToBookRate` (default 4.0, **new v1.6**), `language` (`en`/`es`), `themeMode` (`DAY`/`NIGHT`/`MATERIAL_GRAY`, **new v1.6**) | Single-row table of app-wide preferences. |

Room migrations are additive (`ALTER TABLE`/`CREATE TABLE`) — no destructive fallback is used, so upgrading (within the same package/app install) never loses the owner's data. `MIGRATION_4_5` (v1.6) adds the two `goods` tables and the two new `AppSettingsEntity` columns.

**Currency conversion chain:** a payment entered in Mackerels converts to Books using `mackerelToBookRate` (default 4 Mackerels = 1 Book), and that Books figure then converts to dollars using the existing `bookToDollarRate` (default $8.00/Book) — so Mackerels ultimately resolve to a dollar figure everywhere revenue is totaled.

## 6. Feature: Schedule Tab

Replaces the old standalone Booking tab; appointment scheduling now lives entirely here.

- Three views, switchable at the top: **Daily**, **Weekly** (default), and **Monthly** (new in v1.6 — a full month calendar grid, each date its own tappable box; tapping a date opens that day's bookings with its own "+" to add one).
- A floating **+** button is available from every view (Daily, Weekly, and Monthly) to add a new appointment: it opens a date picker (defaulting sensibly for the current view) and then the same booking form used everywhere else.
- The booking form collects: **Customer** (dropdown of previously used names, or type a new one — created immediately in the Customers database), a **Phone Time Contract** section for the selected customer, **Service** (dropdown sourced from the Database tab's Services list, addable inline), **Time Start/End** via AM/PM time pickers, or a **Not Applicable** checkbox for services that don't need a time slot, **Rate** and **Payment Method** (both sourced from the Database tab, addable inline), **Payment Amount** (entered in Dollars, Books, or **Mackerels** — new v1.6 — with a three-way toggle), **Payment Status** (Paid / Partial / Unpaid, with an "amount paid so far" field when Partial), and optional notes.
- **Time-conflict protection, scoped per service:** saving a timed booking that overlaps another timed booking on the same date *under the same service* is blocked with a "Time conflict - please fix" message. Bookings under different services (e.g. "Phone" and a second device "Phone2") are allowed to overlap in time, since they represent independent lines/resources.
- **Phone Time Contract section:** as soon as the Customer field has a value, the form resolves (or creates) that customer and shows their contract status inline — "No contract set up" with a "Set Up Contract" button, or a summary line (price, hours given, time remaining/over) with "Edit"/"Clear" buttons. Changes here commit immediately, independent of whether the booking itself is ultimately saved.
- Any customer, service, rate, or payment method added inline from this form is saved immediately to the Database tab's corresponding list, independent of whether the booking itself is ultimately saved.

## 7. Feature: Revenue Tab

- Defaults to the current week; switchable to a single **Date**, the **Week**, or a custom **Range** via a calendar picker.
- Pulls every booking *and every Goods sale* from the selected period into one merged, chronologically sorted entry list. Payments recorded in Books or Mackerels are converted to dollars using the owner-editable rates (Settings).
- **Bottom Line** card shows total revenue for the period (bookings + goods sales combined) plus a Paid vs. Unpaid breakdown for bookings.
- **Goods Profit card (new v1.6):** shows Goods Revenue, Cost of Goods, and Goods Profit for the period, computed from the `GoodsTransactionEntity` ledger's OUT rows (using the cost/price captured at sale time, not current pricing).
- An "Entries" list below shows every booking and goods sale in the period, each row showing its own amount and (for bookings) paid/partial/unpaid status.
- **Save as PDF** (bottom of screen): exports the currently selected period — Bottom Line totals, the Goods Profit summary, and every entry (bookings and goods sales) — to a PDF the owner saves wherever they choose.

## 8. Feature: Database Tab (new in v1.6)

Replaces the Services/Rates/Payment Methods lists that used to live in Settings, and adds two new lists (Customers, Goods) that previously had no dedicated management screen. A horizontally-scrollable chip row switches between five sections:

- **Customers** — the same client list used throughout Schedule and Analytics: tap a name to rename it, delete, or "+ Add" a new one.
- **Services** — moved here from Settings, unchanged in behavior (inline rename/delete/add).
- **Rates** — moved here from Settings, unchanged in behavior.
- **Payment Methods** — moved here from Settings, unchanged in behavior.
- **Goods (new):** a full inventory manager for store items.
  - The item list shows each good's name, quantity on hand, and a low-stock warning badge when quantity is at or below its reorder threshold.
  - **+ Add** opens a dialog collecting name, cost price, sale price, and initial quantity.
  - Tapping an item opens its detail dialog: **Update Prices** (edit cost/sale price going forward — never rewrites past transaction history), **Restock** (records an IN transaction and increases quantity on hand), **Sell** (records an OUT transaction at the item's current sale price, decreases quantity on hand, and is blocked with an "insufficient stock" message if the requested quantity exceeds what's on hand), and **Delete**.

Customers/Services/Rates/Payment Methods are "wired to scheduling" in the sense that they are the exact same underlying lists the Schedule tab's booking form reads from and writes to — a customer or service added from either screen shows up immediately in the other. Goods currently has its own independent Sell/Restock ledger in the Database tab rather than being selectable from inside the booking form itself; a tighter integration (e.g. attaching a goods sale directly to a booking/appointment) was considered but scoped out of v1.6 as a larger change to an already-complex booking form, and can be added later if wanted.

## 9. Feature: Analytics Tab

Three views, toggled at the top:

- **Clients:** one card per client, sorted alphabetically by name, showing total spent, preferred payment method, a booking count with how many aren't fully paid, and the services they've used. A client with an active phone-time contract additionally shows a "Contract: X used · Y remaining" line (or "· Y over" in red once exceeded) directly on the card. Tapping a card opens a full client detail view with three-way Phone Time tracking (All Time / Custom-resettable / Contract).
- **Breakdowns:** revenue grouped and bar-charted by Service, Payment Method, or Client, each with a percentage-of-total bar.
- **Inventory (new v1.6):** four sections addressing goods/stock health at a glance —
  - **Low Stock Alerts** — every Goods item at or below its reorder threshold, listed in red.
  - **Top-Selling Goods** — the five best-selling items by total units sold (all-time, from the goods transaction ledger).
  - **Inventory Valuation** — total units in stock, the stock's value at cost, its value at sale price, and the potential profit if everything currently on hand sold.
  - **Profit Margin** — total goods revenue, cost of goods, profit, and overall margin percentage across every recorded sale.
- **Save as PDF** (bottom of screen, Clients/Breakdowns only): exports whichever of those two views is currently active. The Inventory view is a live operational dashboard rather than a period report, so it is not included in PDF export.

## 10. Feature: Settings

- **Books Conversion Rate** and **Mackerels Conversion Rate (new v1.6):** two editable fields — dollars-per-Book (default $8.00) and Mackerels-per-Book (default 4.0) — used everywhere a Books or Mackerels amount needs to convert to dollars.
- **Theme (new v1.6):** Day, Night, or Material Gray, chosen via chips; applies instantly app-wide.
- **Language:** English / Español, switches every screen's text instantly, independent of the phone's system language. Data the owner typed in — client, service, rate, payment-method, and goods names, notes — is never auto-translated.
- **App Lock:** "Change Lock" re-verifies the current credential and walks through setting a new PIN/pattern/password, re-keying the live database in place (see §3).
- **Backup:** "Export" writes an encrypted zip of the entire database (including Goods and its transaction ledger) to a location the owner picks; "Import" restores from such a zip, replacing the current database.
- **Changelog:** collapsible (new v1.6) — shows only the latest version's notes by default, with a "Show full history" toggle to expand every past release. Compiled into the app itself so it works fully offline, automatically appended to by the CI build pipeline on every release.
- **About footer:** brand image, current version number, a "Buy Me a Coffee" link, and copyright line.
- **Removed in v1.6:** Services, Rates, and Payment Methods management moved out of Settings entirely and now live in the Database tab (§8).

## 11. Localization Architecture

All UI copy lives in a single Kotlin interface (`AppStrings`) with two complete implementations (`EnglishStrings`, `SpanishStrings`); the interface contract guarantees both languages stay in parity — the app won't compile if a string is added to one and not the other. The active language is read from `AppSettingsEntity.language` and provided app-wide via a Compose `CompositionLocal`. Date and day-of-week formatting is driven by the same selected locale via `java.time` formatters.

## 12. PDF Export Architecture

Built entirely on Android's built-in `PdfDocument` API — no external library or added app size. Each export draws a title, a subtitle describing the exported period/view, optional summary lines (Bottom Line, Goods Profit), and a list of entries, paginating automatically when content overflows a page. Every page carries the app's brand image rendered large, centered, and translucent behind the text as a watermark.

## 13. Migrating from Rentals (v1.5 and earlier) to Management (v1.6+)

Because v1.6 changed the package ID, **this is a fresh Android install, not an in-place update.** The two apps ("Rentals" and "Management") can be installed side by side, but they do not share data. To move existing data over:

1. On the **old "Rentals" app**, go to Settings → Backup → Export and save the encrypted backup file somewhere accessible (e.g. Downloads, Google Drive).
2. Install the new **Management** APK and complete first-run onboarding (choose a PIN/pattern/password — it can be the same one you used before, or a new one).
3. In the new **Management** app, go to Settings → Backup → Import and select the backup file from step 1. You will need to unlock again afterward.
4. Once the import is confirmed working, the old "Rentals" app can be uninstalled.

The backup/restore file format is package-ID-agnostic (it's a raw encrypted copy of the SQLCipher database file, with no package name embedded in it), so this import works cleanly across the rename as long as the same unlock credential is used at import time as was used to create the export.

## 14. Non-Functional Requirements

- **Fully offline:** no network calls anywhere in the app; the manifest blocks cleartext traffic.
- **Encrypted at rest:** the entire database is SQLCipher-encrypted; the lock credential itself is never persisted, only derived/verifier material.
- **No data loss across updates (within the same package):** schema changes are additive Room migrations, never a destructive rebuild.
- **Low-end hardware target:** ABI-restricted, minified/shrunk release builds, no unnecessary dependencies.
- **Resilient CI:** every push automatically produces an installable, versioned, changelogged APK without the owner needing a computer.

## 15. Version History Summary

- **v1.0** — Initial release: Booking calendar, Schedule, Revenue, and Analytics tabs; PIN/pattern/password app lock with on-device encryption; editable Services/Rates/Payment Methods; Books-to-dollar conversion; encrypted backup export/import.
- **v1.1** — Phone Time tracking added to each client's Analytics profile (All Time / User Defined / Contract views).
- **v1.2** — Spanish language option; Save as PDF on Schedule, Revenue, and Analytics; lightened dark-mode background.
- **v1.3** — Time-conflict checks scoped per service; Phone Time Contracts settable from the booking form; Analytics client cards sort alphabetically and show contract time at a glance.
- **v1.4** — Fixed a Change Lock data-loss bug (credential could desync from the re-keyed database on failure).
- **v1.5** — Fixed a duplicate-customer race condition in the booking form's inline "+ Add new..." flow.
- **v1.6** — Renamed to Management (new icon/splash/package ID — fresh install required, see §13); removed the Booking tab in favor of an expanded Schedule tab (Daily/Weekly/Monthly + universal "+"); added the Database tab (Customers, Services, Goods, Rates, Payment Methods); added full Goods/inventory tracking (cost, price, stock, restock/sell ledger) integrated into Revenue and a new Analytics Inventory view (low stock, top sellers, valuation, profit margin); added a Mackerels payment unit; added Day/Night/Material Gray themes; made the Settings changelog collapsible.

Full text of each version's release notes ships in the app itself (Settings → Changelog).
