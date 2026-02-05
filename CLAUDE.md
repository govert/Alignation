# Alignation Project Notes

## Critical Rules

- **NEVER lose user history on updates** - Database migrations must preserve all existing alignment events. Always use Room migrations (not destructive recreation) when schema changes.

## App Concept

The app tracks aligner wear compliance with a **budget mindset**:
- User has a **2-hour daily allowance** for aligners being out (3 hours absolute max)
- Goal: minimize days exceeding 2 hours out across the 16-week treatment
- Grace system: wearing >22 hours (up to 30 mins extra) carries over as extra allowance next day (no further accumulation)

## Alert Tiers (when aligners are out)
- 30 minutes: gentle alert
- 45 minutes: more serious alert
- 1 hour: alarm

## Treatment Timeline
- 16 weeks total
- Track progress toward end date
- Count days with >2 hours out as "problem days" to minimize
