# Management

A private, offline Android app for running a booking-and-inventory service business: appointment
scheduling, revenue tracking (Dollars/Books/Mackerels), goods/inventory management with
cost-and-profit reporting, and per-client analytics. Everything is encrypted at rest and stored
only on-device — nothing syncs anywhere.

Package: `app.orionmd.management` (renamed from `app.orionmd.rentals` at v1.6 — this is a fresh
install on top of that app, not an in-place update; see "Upgrading from Rentals" below).

## Features

### Schedule
- Three views — **Daily**, **Weekly** (default), and **Monthly** (full calendar grid) — switchable
  at the top of the tab.
- A floating **+** button, available from every view, opens a date picker and then the booking
  form to add a new appointment.
- Booking form: customer (dropdown or type-to-add), service, start/end time or "Not Applicable",
  rate type, payment method, payment amount in **Dollars, Books, or Mackerels**, and
  Paid/Partial/Unpaid status.
- Per-service time-conflict protection — overlapping times are blocked *within* the same service,
  but different services (e.g. two separate phone lines) can overlap freely.
- Inline Phone Time Contract setup/edit/clear directly from the booking form.

### Database (new)
- One tab, five sections: **Customers**, **Services**, **Goods**, **Rates**, **Payment Methods**.
- Customers/Services/Rates/Payment Methods are simple named lists — tap to rename, delete, or
  **+ Add** — and are the same lists the booking form reads from and writes to.
- **Goods** is a full inventory manager: track cost price, sale price, quantity on hand, and a
  reorder threshold per item; **Restock** and **Sell** actions record an immutable inflow/outflow
  ledger (so past sales keep the price/cost that applied at the time, even if you edit the item's
  current pricing later); low-stock items are flagged right in the list.

### Revenue
- Bottom-line totals for a specific date, the current week, or any custom range, combining
  appointment bookings and goods sales into one list, split into Paid vs. Unpaid.
- A **Goods Profit** card (revenue, cost, profit) for the selected period.
- Books and Mackerels are converted to dollars at editable rates (defaults: 1 Book = $8.00,
  4 Mackerels = 1 Book).
- **Save as PDF**, watermarked with your logo.

### Analytics
- **Clients** — per-client profiles (total spent, preferred payment method, services used, and
  phone-time contract status at a glance), sorted alphabetically. Tap a client for a full profile
  with All Time / Custom-resettable / Contract phone-time views.
- **Breakdowns** — revenue by Service, Payment Method, or Client, as percentage bar charts.
- **Inventory** (new) — Low Stock Alerts, Top-Selling Goods, Inventory Valuation (value at cost vs.
  sale price, potential profit), and overall Profit Margin across every recorded goods sale.
- **Save as PDF** for Clients and Breakdowns.

### Security
- Onboarding lets you set a PIN (4-8 digits), a pattern, or a password. That credential derives
  the encryption key for the entire on-device database (SQLCipher + a Keystore-backed verifier) -
  nothing is stored anywhere that could reveal it. Changeable anytime from Settings, which
  re-keys the live database in place with no data loss.

### Settings
- Books and Mackerels conversion rates.
- **Themes** (new): Day, Night, or Material Gray, switchable instantly.
- Language: English or Español, switching all in-app text instantly (your own typed data is never
  translated).
- App Lock credential management, encrypted backup export/import, and a **collapsible** (new)
  version changelog that shows just the latest entry by default.

## Upgrading from Rentals (pre-1.6)

v1.6 changed the Android package ID as part of the rename to "Management," which makes this a
brand-new app to Android rather than an update — the old app isn't touched or replaced
automatically. To move your data over:

1. In the old **Rentals** app: Settings → Backup → Export, and save the file somewhere reachable.
2. Install **Management** and complete first-run onboarding.
3. In **Management**: Settings → Backup → Import, and select that file.
4. Once confirmed, uninstall the old Rentals app.

The backup format doesn't embed a package name, so this import works cleanly across the rename as
long as you unlock with the same credential you exported with.

## Building

This project has no Android Studio dependency baked in - it's built entirely through GitHub
Actions. See `SETUP_INSTRUCTIONS.md` for how to get it running from an Android phone with no PC.

Locally (if you do have Android Studio or a full SDK):

```
./gradlew assembleDebug
```

The debug APK lands in `app/build/outputs/apk/debug/`.

## Project conventions

- Every CI build bumps `versionName` in `app/build.gradle.kts` by `+0.1` and appends an entry to
  `app/src/main/java/app/orionmd/management/Changelog.kt`, which is what Settings > Changelog
  displays in-app.
- Release signing is optional: add a `keystore.properties` file at the repo root (never commit
  it - see `.gitignore`) with `storeFile`, `storePassword`, `keyAlias`, `keyPassword` to sign
  release builds for real; without it, everything (including `assembleRelease`) falls back to
  debug signing.

## Documentation

- `RENTALS_FEATURE_SPECIFICATION.md` - full technical feature spec (architecture, data model,
  every tab's behavior in detail).
- `RENTALS_PRODUCT_GUIDE.md` - a plain-language walkthrough of the app for the person using it.
- `SETUP_INSTRUCTIONS.md` - how to get this project building from a phone with no PC.
