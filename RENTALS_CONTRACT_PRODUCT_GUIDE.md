# Rentals — Phone Time Contracts: Product Guide

This guide describes how Phone Time Contracts will work once the changes in `RENTALS_CONTRACT_FEATURE_SPEC.md` are built. It's written for you as the app's owner/user, not as a technical document.

## What's a Phone Time Contract?

A contract is a standing agreement you set up with a client: they've paid a set price for a set number of hours of phone time, and the app tracks how much of that they've used as you log their bookings. Each client can have at most one active contract at a time. A contract has four parts:

- **Price** — what the client paid for the contract.
- **Hours given** — how many hours the contract covers.
- **Time used** — the total time from that client's timed "Phone" bookings, counted from the contract's start date forward.
- **Time remaining** — hours given minus time used (shown as "over" if they've gone past it).

## Setting up a contract while booking a job

You no longer have to leave the booking screen to do this. When you open a booking (either tapping "+" on a date in Booking, or opening an existing entry from Schedule) and fill in the Customer field, a new "Phone Time Contract" section appears right below it:

- If that client doesn't have a contract yet, you'll see **"No contract set up"** with a **Set Up Contract** button. Tapping it opens a small form: price, hours given, and a start date. Save it, and the contract is active immediately — you don't need to also save the booking for the contract to take effect.
- If that client already has a contract, you'll see a one-line summary — for example, *"Contract: $150 · 20h given · 6h 15m remaining"* — with **Edit** and **Clear** buttons right there.

This works the same way whether you're booking a totally new client (the app creates their client record as soon as you type their name, same as it already does for a new service or payment method) or a returning one.

## Editing or clearing a contract from a booking

- **Edit** reopens the same setup form, pre-filled with the current price, hours, and start date, so you can adjust any of them.
- **Clear** asks you to confirm, then removes the contract. Any time already logged under it isn't deleted — only the contract itself goes away, and the client goes back to "No contract set up."

Both actions take effect right away, independent of whatever booking you happen to have open at the time.

## Reading contract status in Analytics

Open the Analytics tab and switch to Clients. Any client with an active contract now shows an extra line on their card, so you can see where everyone stands without tapping into each one:

- **Under budget:** *"Contract: 6h 15m used · 13h 45m remaining"*
- **Over budget:** *"Contract: 22h used · 2h over"* — shown in red so it's easy to spot at a glance.

Clients without a contract look exactly as they do today — no extra line. Tapping a card still opens the full client detail view, which shows the complete contract breakdown (price, hours given, used, remaining, and the contract's start date) along with everything else already there (total spent, preferred payment method, services used).

## What happens if a client goes over their contracted hours?

Nothing is blocked — you can keep booking phone-time sessions for them as usual. The app simply shows "over" instead of "remaining," in red, both on their Analytics card and in their detail view, so you know to follow up (renew the contract, bill for the overage, or however you handle it) — that decision stays entirely yours.

## A few things worth knowing

- **Setting up a contract for a brand-new client and then backing out of the booking still keeps the contract.** The contract belongs to the client, not to that one booking, so it's saved the moment you tap Save on the contract form — cancelling the booking itself won't undo it.
- **A contract isn't tied to a specific service.** You can set one up for a client while booking them for any service — it's about their overall phone-time standing, not this one entry.
- **The separate "reset a counter yourself" option (Custom tab in the client detail view) is unaffected** — that's a different, simpler running total you can zero out anytime, and it isn't part of this change.
