# Management — Product Guide

Management is your appointment scheduling, revenue, inventory, and client-tracking app, built to run entirely on your phone with no internet connection and no account — everything is encrypted and stored only on your device. This guide walks through everything the app does, tab by tab (current version: 1.6).

> **Coming from the old "Rentals" app?** Management has a new package ID, so your phone treats it as a brand-new app rather than an update — see "Moving Over From Rentals" near the end of this guide before you uninstall anything.

## Getting Started

The first time you open Management, you'll set up a way to unlock it: a numeric PIN (4–8 digits), a connect-the-dots pattern, or a text password. Whichever you choose, it's what encrypts everything you store in the app — there's no "forgot password" recovery, since nobody but you ever has the key, so pick something you'll remember. You can change it anytime later from Settings.

## Schedule

This is where you manage every appointment. Three views sit at the top — **Daily**, **Weekly** (the default), and **Monthly** — and you can switch between them anytime. Monthly shows a full calendar grid; tap any date to see that day's bookings. Wherever you are, a **+** button is always available to add a new appointment — it'll ask you for a date, then open the booking form:

- **Customer** — pick someone you've booked before, or type a new name (they're added to your client list right away).
- **Phone Time Contract** — as soon as you've entered a customer, you'll see their contract status right here: "No contract set up" with a **Set Up Contract** button, or a summary (price, hours given, time remaining) with **Edit**/**Clear** buttons. Any change here takes effect immediately, whether or not you go on to save the booking.
- **Service** — what kind of appointment this is (defaults to "Phone"; manage your services from the Database tab).
- **Time Start/End** — pick a start and end time, or check **Not Applicable** for services that don't need a time slot.
- **Rate**, **Payment Method**, **Payment Amount** — now in **Dollars, Books, or Mackerels**, whichever the client is paying with — and **Payment Status** (Paid, Partial, or Unpaid).

If you try to book a time slot that overlaps another booking for the *same service* that day, the app stops you with a "time conflict" message so you can fix it before saving. Bookings under different services — say, "Phone" and a second line "Phone2" — are free to overlap, since they're independent lines.

## Database

Everything your business is built on lives here, in one tab with five sections you switch between at the top:

- **Customers** — your full client list. Tap a name to rename it, use the delete icon to remove one, or **+ Add** a new one.
- **Services** — the appointment types you offer (this used to be in Settings — it's here now, working the same way).
- **Rates** — your rate types (Hourly, Session, etc.) — also moved here from Settings.
- **Payment Methods** — how clients pay you — also moved here from Settings.
- **Goods** — new in this version: full tracking for anything you sell as physical stock — phones, accessories, store items, whatever you carry.
  - **+ Add** an item with its cost, sale price, and how many you're starting with.
  - Tap any item to **Update Prices**, **Restock** (add more units in), or **Sell** (record a sale — this automatically uses the item's current sale price and won't let you sell more than you have on hand).
  - Items running low show a warning badge right in the list, based on a reorder threshold you can set per item.

Anything you add here — a customer, a service, a rate, a payment method — shows up immediately in the Schedule tab's booking form, and vice versa. Goods sales are tracked here on their own, separate from appointment bookings, and flow into your Revenue and Analytics automatically.

## Revenue

Shows what you've earned — appointments and goods sales together. It defaults to the current week, but you can switch to a single date, a different week, or any custom date range. You'll see a **Bottom Line** — total revenue for that period, split into what's been paid and what's still owed — a **Goods Profit** card showing what you made selling inventory (revenue, cost, and profit), and a combined list of every appointment and goods sale that contributed to the total. Payments in Books or Mackerels are automatically converted to dollars using the rates you set in Settings.

At the bottom of the screen, **Save as PDF** exports whatever period you're currently viewing, with your logo watermarked across each page.

## Analytics

Your client and business overview, now in three views:

- **Clients** — a card for every client, listed alphabetically, showing how much they've spent, their preferred payment method, and the services they've used. Anyone with an active phone-time contract also shows a line right on the card — "Contract: 6h 15m used · 13h 45m remaining" — so you can see who's near or over their hours without opening each profile. Tap a card to open their full profile with Phone Time tracking (All Time, a Custom resettable counter, and Contract details).
- **Breakdowns** — your revenue broken down by Service, Payment Method, or Client, shown as bar charts.
- **Inventory** — new in this version, this is where the app surfaces the things worth watching in your stock: **Low Stock Alerts** for anything at or below its reorder point, **Top-Selling Goods** so you know what to keep stocked, an **Inventory Valuation** showing what your current stock is worth at cost versus at sale price (and what you'd profit if you sold it all), and your overall **Profit Margin** across every sale you've recorded.

Save as PDF is available for Clients and Breakdowns; Inventory is a live dashboard meant for checking in on, so it isn't included in the PDF export.

## Settings

Everything you can customize lives here:

- **Books Conversion Rate** and **Mackerels Conversion Rate** — set how many dollars a Book is worth, and how many Mackerels make up one Book.
- **Theme** — choose Day, Night, or Material Gray, and the whole app switches instantly.
- **Language** — switch between English and Español instantly. This only affects the app's own text — anything you've typed yourself, like client or item names, stays exactly as you wrote it.
- **App Lock** — change your PIN, pattern, or password anytime; you'll confirm your current one first.
- **Backup** — export a full, encrypted copy of everything (including your Goods inventory) to save wherever you like, and import it back later. Importing replaces what's currently in the app, so you'll need to unlock again afterward.
- **Changelog** — now collapsible, showing just the latest update by default with a button to see the full history if you want it.
- At the bottom, you'll find the app's version number and a Buy Me a Coffee link if you'd like to support development.

Services, Rates, and Payment Methods used to live here — they've moved to the new Database tab, alongside Customers and Goods.

## Moving Over From Rentals

Management replaces the old "Rentals" app, but because its package changed, your phone sees it as a completely different app — installing Management won't touch or upgrade your existing Rentals install, and it starts out empty. Here's how to bring your data over:

1. Open your **old Rentals app**, go to **Settings → Backup → Export**, and save the backup file somewhere you can get to later (Downloads, a cloud drive, wherever).
2. Install **Management** and go through the first-run setup (you can reuse the same PIN/pattern/password, or pick a new one).
3. In **Management**, go to **Settings → Backup → Import** and choose the file you saved in step 1. You'll need to unlock again once it's done.
4. Once you've confirmed everything looks right, you can safely uninstall the old Rentals app.

Your data — clients, bookings, goods, everything — is stored in an encrypted file that doesn't care what the app around it is called, so this import works cleanly.

## A Few Things Worth Knowing

- Everything is stored only on your phone and encrypted — there's no cloud sync and no account, so backing up (Settings → Backup) is the only way to move your data to a new device or protect against losing your phone.
- Adding a new customer, service, rate, or payment method while filling out a booking saves it immediately, even if you back out of that booking without finishing it.
- Goods and bookings are tracked separately, but both count toward the same Revenue and Analytics totals — nothing about your business's money is split across two places you have to add up yourself.
- The app is built to run smoothly on lower-end Android phones, so it stays fast and small even with a lot of history built up.

## Version History

- **1.0** — Booking calendar, Schedule, Revenue, and Analytics tabs; PIN/pattern/password app lock; editable Services/Rates/Payment Methods; encrypted backup export/import.
- **1.1** — Phone Time tracking on each client's Analytics profile.
- **1.2** — Spanish language option; Save as PDF on Schedule, Revenue, and Analytics; lighter dark-mode background.
- **1.3** — Time conflicts checked per service; contracts settable right from the booking form; Analytics client cards sort alphabetically and show contract time at a glance.
- **1.4** — Fixed a bug where changing your lock credential could, in a failure case, leave the app unable to unlock.
- **1.5** — Fixed a bug where adding a brand-new customer could occasionally create a duplicate entry.
- **1.6** — Renamed to Management with a new icon and look; replaced the Booking tab with an expanded Schedule (Daily/Weekly/Monthly, a + button everywhere); added a Database tab for Customers, Services, Goods, Rates, and Payment Methods; added full inventory tracking with cost/profit reporting; added a Mackerels payment option; added Day/Night/Material Gray themes; made the changelog collapsible.

The full text of every release lives in the app itself: Settings → Changelog.
