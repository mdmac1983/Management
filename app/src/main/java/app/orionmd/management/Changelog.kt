package app.orionmd.management

/**
 * Static, shipped-with-the-APK changelog shown on Settings > Changelog. Each version bump adds
 * one entry here (build-apk.yml's version-bump step appends the new entry automatically as part
 * of every CI build - see .github/workflows/build-apk.yml).
 */
data class ChangelogItem(val version: String, val date: String, val notes: String)

object Changelog {
    // NEW ENTRIES GO DIRECTLY BELOW THIS LINE (build-apk.yml inserts new lines right after the
    // marker comment below - don't remove or reformat the marker or this list's opening brace).
    val entries: List<ChangelogItem> = listOf(
        // CHANGELOG_INSERT_MARKER
        ChangelogItem("1.6", "2026-09-20", "Renamed the app to Management (new icon, splash screen, and tab backgrounds from your logo; package ID changed, so this is a fresh install - export a backup from the old app and import it here). Removed the Booking tab; Schedule now defaults to Weekly with a new Monthly view and a + button to add appointments from any view. Added a Database tab (Customers, Services, Goods, Rates, Payment Methods), each with its own + to add entries. Goods/inventory tracking is new: track cost, sale price, and quantity on hand, with Restock and Sell actions and a running inflow/outflow ledger. Goods sales now flow into Revenue alongside bookings, with a Goods Profit summary. Analytics gained an Inventory view: low-stock alerts, top-selling goods, inventory valuation, and overall profit margin. Settings' Changelog is now collapsible (shows only the latest entry by default). Added a Mackerels payment unit with its own conversion rate to Books. Added Day, Night, and Material Gray themes (Settings > Theme). Removed Services/Rates/Payment Methods from Settings (moved to the new Database tab)."),
        ChangelogItem("1.5", "2026-09-09", "Fixed a bug (introduced in 1.3) where adding a brand-new customer from the booking form's \"+ Add new...\" could create two duplicate entries with the same name. Existing duplicates created by this bug are automatically merged on upgrade (their bookings are combined onto one entry)."),
        ChangelogItem("1.4", "2026-09-09", "Fixed a serious bug in Settings > Change Lock: a re-key failure could leave the app's stored credential out of sync with the actual encrypted database. The new credential is now only saved after the database has been successfully re-keyed to it, and any failure shows an error while your old PIN/pattern/password keeps working."),
        ChangelogItem("1.3", "2026-09-09", "Time conflicts are now checked per-service, so different services (e.g. two separate phone lines) can be booked at overlapping times. Contracts can now be set up, edited, or cleared directly from the booking form. Analytics client cards are sorted alphabetically and show time used/remaining for clients with an active contract."),
        ChangelogItem("1.2", "2026-09-09", "Added a Spanish language option (Settings > Language, translates the whole app instantly). Save as PDF on Schedule, Revenue, and Analytics, with a large translucent brand watermark on every page. Lightened the dark-mode background from near-black to gray."),
        ChangelogItem("1.1", "2026-09-09", "Added Phone Time tracking to each client's Analytics profile: All Time total, a resettable User Defined counter, and a Contract view (price, hours given, time used, time remaining). Tap any client card in Analytics to open it."),
        ChangelogItem("1.0", "2026-09-09", "Initial release: Booking calendar, Schedule, Revenue, and Analytics tabs. PIN/pattern/password app lock with on-device encryption (SQLCipher + Keystore-backed verifier). Editable Services/Rates/Payment Methods, Books-to-dollar conversion, and encrypted backup export/import.")
    )
}
