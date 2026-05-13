# BUSINESS RULES

Stock can never go negative.

Inventory movement history is immutable.

Purchase orders validate stock entries.

Transfers require validation.

Invoices preserve accounting integrity.

POS sessions must remain historically accurate.

Pricing tiers must be preserved:
- Normal
- Loyal (-15%)
- Student (-15%)
- School (-20%)

Critical calculations must never be modified automatically.

Preserve:
- invoice synchronization
- warehouse relationships
- stock history
- supplier references