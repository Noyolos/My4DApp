4DClerk (or 4DReceiptor / your final choice)

A lightweight Android app for 4D lottery clerks to quickly issue, reuse, and share tickets with unique serial numbers.

✨ Features

New Ticket Creation

Input numbers and bet amounts in a multiline format.

Automatic validation of syntax and bet format.

Default amount inheritance (enter once, reuse across numbers).

Unique Serial Numbers

Each issued ticket receives an auto-incremented sequence ID.

Counter resets daily at 20:00.

History & Reuse

All tickets are stored locally in a Room database.

Search by sequence ID or browse recent tickets.

One-click reuse: refill previous ticket input and generate a new ticket.

Formatted Output

Standardized ticket format with date, time, sequence ID, bet details, and total (GT).

Automatic calculation of grand totals.

Quick Sharing

Send formatted tickets directly to WhatsApp with a single tap.

Offline support — all functions work without internet.

Error Prevention

Input validation with clear error dialogs.

Prevents missing amounts or invalid ticket groups.

🛠 Tech Stack

Language: Kotlin

UI: Jetpack Compose + Material 3

Navigation: Jetpack Navigation

Data Persistence:

Room Database for ticket history

SharedPreferences for sequence counter

Integration: WhatsApp (via Android intent)

📖 Usage

Start a new ticket → Input numbers in groups, each group begins with -<place digit>.

-123
1234-2
5678-1-1


Confirm & Issue → App validates the format and generates an output ticket.

Send via WhatsApp → One tap to share.

Reuse a past ticket → Search by sequence ID in History and regenerate instantly.

📷 Screenshots (to add later)

Home screen

Input screen with validation

Output screen with WhatsApp button

History & reuse screen

🚀 Future Improvements

Automatic reset of sequence counter at 20:00 daily

Export history to CSV / PDF

Cloud sync for multi-device use

Notification reminders for draw times
