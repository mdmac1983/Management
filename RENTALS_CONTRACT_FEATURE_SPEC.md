# Rentals — Contract-in-Booking & Analytics Card Feature Specification

Status: **Draft, not yet implemented.** This spec describes the two enhancements requested for the Phone Time Contract feature and is meant to be reviewed before the code changes are made.

## 1. Overview & Goals

Rentals already supports per-client Phone Time contracts (price, hours given, time used, time remaining), but a contract can currently only be created or changed from one place: Analytics → tap a client card → Contract tab. This spec adds two improvements:

1. **Set up or edit a contract directly from the booking form** (Booking tab's "+" and Schedule tab's edit flow), so the owner never has to leave what they're doing to manage a contract.
2. **Show contract status at a glance on the Analytics client card**, without needing to open the client's detail view, so the owner can scan all clients' contract standing in one screen.

## 2. Current State (for context)

- `CustomerEntity` already stores `hasContract`, `contractPrice`, `contractHours`, `contractStartEpochDay`, and `customCounterResetEpochDay`. No schema changes are needed for this feature.
- `RentalsRepository` already exposes `getPhoneTimeMinutes(customerId, sinceEpochDay)`, `setContract(customer, price, hours, startEpochDay)`, and `clearContract(customer)`.
- The only UI that reads or writes contract data today is `ui/analytics/ClientDetailDialog.kt`, specifically its private `ContractSetupDialog` composable and an inline "Clear Contract?" `AlertDialog`.
- `ui/booking/BookingFormDialog.kt` (shared by the Booking tab's new-booking flow and the Schedule tab's edit flow) has no contract UI and no direct reference to `RentalsRepository` — it's a "dumb" form driven entirely by callback parameters (`onAddCustomer`, `onAddService`, `onSave`, etc.).
- `ui/analytics/AnalyticsScreen.kt`'s `ClientProfiles` composable builds cards purely from the `bookings` list (grouped by customer); it never loads `CustomerEntity` rows, so it has no access to contract fields today.

## 3. Scope

**In scope:**
- Setting up a new contract for the customer selected in the booking form.
- Editing or clearing an existing contract for that customer, from the booking form.
- Displaying "time used" / "time remaining" (or "over by") on each Analytics client card when that client has a contract.

**Out of scope (unchanged by this spec):**
- The "User Defined" resettable counter (Custom tab) — no booking-form entry point is being added for it; it stays Analytics-only.
- Any change to how Phone Time itself is calculated (still: sum of timed "Phone" service bookings for that customer).
- Any change to the Breakdowns view in Analytics.

## 4. User Stories

- *As the owner, while booking a new job for a returning client, I want to see and edit their contract right there, so I don't have to switch tabs.*
- *As the owner, while typing up a brand-new client's first booking, I want to set up their contract immediately, so it's in place before I even save the booking.*
- *As the owner, scanning my client list in Analytics, I want to see who's near or over their contracted hours without tapping into each one.*

## 5. UX Design

### 5a. Booking form — Contract section

Placement: a new "Phone Time Contract" section in `BookingFormDialog`, directly below the Customer field and above the Service field (it's tied to the customer, not the service — see §9).

States:
- **No customer chosen yet** (`customerName` blank): section is not shown at all.
- **Customer chosen, no contract on file**: a single line — "No contract set up" — with a text button "Set Up Contract".
- **Customer chosen, contract on file**: a compact one-line summary, e.g. `Contract: $150 · 20h given · 6h 15m remaining` (or `· 2h over` in the error color if exceeded), with two small text buttons: "Edit" and "Clear".

Behavior:
- As soon as the Customer field has a non-blank value (either picked from the dropdown or entered as a brand-new name), the form resolves that name to a real `CustomerEntity` via the existing `repository.getOrCreateCustomer(name)` call (the same idempotent lookup-or-insert already used elsewhere) and keeps it in local state. This re-resolves whenever the customer name changes.
- "Set Up Contract" / "Edit" open the same setup dialog used today in Analytics (price, hours given, start date fields) — see §7 for the code reuse plan.
- "Clear" opens the same confirmation dialog used today in Analytics ("This removes NAME's contract details...").
- Saving, editing, or clearing the contract writes to the database **immediately** via the repository — it does not wait for the booking form's own "Save" button, and it is not undone if the booking form is later cancelled. Rationale: a contract belongs to the *customer*, not to this one booking, exactly like the pattern the form already uses for "+ Add new..." on Service/Rate/Payment Method (those are created immediately too, independent of whether the booking itself is ultimately saved).

### 5b. Analytics client card

Each client card in the Clients view gains one more line, shown only when that client has an active contract, directly under the existing "Services: ..." line:

```
Contract: 6h 15m used · 13h 45m remaining
```

or, if over the contracted hours:

```
Contract: 22h used · 2h over
```
(rendered in the error color, matching the convention already used in the Contract tab of the client detail dialog).

Tapping the card still opens the full client detail dialog as it does today — that dialog's Contract tab already shows the complete breakdown (price, hours given, used, remaining, contract start date); the card is a summary only.

## 6. Data Model

No migration needed. Both features read/write the existing `CustomerEntity` contract columns (`has_contract`, `contract_price`, `contract_hours`, `contract_start_epoch_day`) added in the v1.0 → v1.1 phone-time-tracking work.

## 7. Architecture / Code Changes

1. **Extract shared contract UI** out of `ui/analytics/ClientDetailDialog.kt` into a new file, `ui/common/ContractComponents.kt`, so both the booking form and Analytics can use it without one screen package depending on another:
   - `ContractSetupDialog(initialPrice, initialHours, initialStart, onDismiss, onSave)` (currently private in `ClientDetailDialog.kt` — move as-is).
   - `ClearContractConfirmDialog(name, onConfirm, onDismiss)` (currently an inline `AlertDialog` block in `ClientDetailDialog.kt` — extract as its own composable so it isn't duplicated).
   - `millisToLocalDate(millis)` helper — move alongside.
   - `ClientDetailDialog.kt` is updated to import and call these instead of its own private copies; no behavior change there.

2. **`ui/booking/BookingFormDialog.kt`**:
   - Add a new parameter: `repository: RentalsRepository`.
   - Add local state: `resolvedCustomer: CustomerEntity?`, `showContractSetup: Boolean`, `showClearContractConfirm: Boolean`.
   - `LaunchedEffect(customerName) { if (customerName.isNotBlank()) resolvedCustomer = repository.getOrCreateCustomer(customerName) }`.
   - Render the Contract section (§5a) using `resolvedCustomer`.
   - Wire "Set Up Contract"/"Edit" → `ContractSetupDialog` → `repository.setContract(resolvedCustomer, price, hours, startEpochDay)`, then refresh `resolvedCustomer` (re-fetch via `repository.getCustomerById(id)`).
   - Wire "Clear" → `ClearContractConfirmDialog` → `repository.clearContract(resolvedCustomer)`, then refresh.
   - All of this runs in `rememberCoroutineScope()` launches, matching the existing pattern for `onAddCustomer`/`onAddService` callers.

3. **`ui/booking/BookingScreen.kt`** and **`ui/schedule/ScheduleScreen.kt`**: pass `repository = repository` into their existing `BookingFormDialog(...)` calls (both already hold a `repository` reference in scope, so this is a one-line addition at each call site).

4. **`ui/analytics/AnalyticsScreen.kt`**:
   - `AnalyticsScreen()` additionally observes `repository.observeCustomers()` and builds a `Map<Long, CustomerEntity>` keyed by id.
   - For customers where `hasContract == true`, compute used-minutes via `repository.getPhoneTimeMinutes(customerId, contractStartEpochDay)` inside a `LaunchedEffect` keyed on the customers+bookings lists, producing a `Map<Long, Long>` of customerId → usedMinutes.
   - `ClientProfiles(...)` takes these two maps as new parameters and, per card, looks up the customer by id; if `hasContract`, renders the new "Contract: … used · … remaining" line as described in §5b.

## 8. Localization

This app has an in-app English/Spanish string table (`util/Strings.kt`, `AppStrings` interface + `EnglishStrings`/`SpanishStrings`). New keys needed (both languages must be added, or the app won't compile — the interface enforces parity):

| Key | English | Spanish |
|---|---|---|
| `contractStatusNone` | "No contract set up" | "No hay contrato configurado" |
| `contractStatusPrefix` | "Contract: " | "Contrato: " |
| `contractCardUsedRemaining(used, remaining)` | "$used used · $remaining remaining" | "$used usado · $remaining restante" |
| `contractCardUsedOver(used, over)` | "$used used · $over over" | "$used usado · $over excedido" |

Existing keys are reused where possible: `setUpContractButton`, `editContractButton`, `clearContractButton`, `clearContractConfirmTitle`/`clearContractConfirmText`, `contractPriceLabel`, `hoursGivenLabel`, `save`, `cancel`.

## 9. Edge Cases & Decisions

- **Contract is scoped to the customer, not the booking's service.** The Contract section appears regardless of which Service is selected for this particular booking — a client can have a Phone Time contract and still book a one-off different service; the contract line is about their overall standing, not this booking.
- **New customer, contract set up, booking then cancelled.** The customer row and their contract are still saved (see §5a rationale). This matches existing behavior for services/rates/payment methods added inline mid-form.
- **Switching the Customer field mid-form** re-resolves and re-renders the Contract section for the newly selected/typed customer.
- **Contract exceeded.** Both the booking-form summary and the Analytics card use the "over by" phrasing and error color, matching the existing Contract tab in the client detail dialog.

## 10. Non-Goals

- No changes to the "User Defined" counter or its reset flow.
- No changes to how Phone Time minutes are calculated.
- No new database migration.

## 11. Rollout

Implementing this bumps the app version by +0.1 with a changelog entry (per project convention), e.g.:
> "Contracts can now be set up or edited directly from the booking form. Analytics client cards show time used/remaining at a glance for clients with an active contract."

## 12. Testing Checklist

- [ ] New booking, brand-new customer name: Contract section appears after typing the name; "Set Up Contract" creates the customer row immediately.
- [ ] New booking, existing customer with a contract: summary line shows correct used/remaining figures.
- [ ] Edit an existing contract from the booking form; confirm the change is reflected in Analytics' client detail dialog.
- [ ] Clear a contract from the booking form; confirm "No contract set up" reappears and the Analytics card line disappears.
- [ ] Set up a contract, then cancel the booking form without saving the booking: contract persists.
- [ ] Analytics: client with no contract shows no extra line; client with a contract under budget shows "remaining" in the normal color; client over budget shows "over" in the error color.
- [ ] Switch languages (Settings → Language) and confirm both new UI locations read correctly in Spanish.
- [ ] Full rebuild against the real Android SDK (debug + release) before shipping, per this project's established verification standard.
