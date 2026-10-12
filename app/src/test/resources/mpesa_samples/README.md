# M-Pesa sample messages

Real M-Pesa SMS bodies, anonymized. One message per line; lines starting with `#` describe the
variant the next message covers.

Anonymized: names, phone numbers, transaction codes, account numbers, balances, agent numbers,
small-business names and personal short links. Everything else, including spacing, punctuation
and capitalization quirks, is copied exactly from real messages, because those quirks are what
the parser must handle.

| File | Type |
|---|---|
| `sent.txt` | Send money to a person |
| `paybill.txt` | Pay bill ("sent to ... for account") |
| `paid.txt` | Buy goods / till |
| `received.txt` | Money received |
| `withdraw.txt` | Agent withdrawal |
| `airtime.txt` | Airtime purchase |
| `fuliza.txt` | Fuliza overdraft drawdown and repayment |
| `notices.txt` | Not transactions: failures, balance checks, Ratiba notices |
