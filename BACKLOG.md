# Alignation Feature Backlog

## Phase 1: Foundation (Database + Core)

### 1. Aligner Set Tracking
- Track which aligner set the user is currently on (e.g., Set 4 of 22)
- Record when each new set was started
- Optional notes per set change
- Show current set on home screen
- Set history view in settings

### 2. Full Audit Log
- Log all mutations (CREATE/UPDATE/DELETE) to alignment events and settings
- Store old and new values for each change
- Viewable audit log screen accessible from settings
- Helps debug data issues and provides accountability

## Phase 2: Settings + Alarms

### 3. Comprehensive Settings
- Configurable alarm sounds for each tier
- Enable/disable toggles for each alarm level
- Editable daily allowance, max allowance, and grace time via sliders
- Enable/disable grace time system

### 4. Tiered Alarm System
- 1h out: warning alarm (short loud sound, configurable)
- 1h45m out (15min before 2h soft limit): warning alarm
- 2h45m out (15min before 3h hard limit): warning alarm
- 2h55m out (5min before 3h final): long ongoing loud alarm

## Phase 3: Layout

### 5. Landscape Layout Fix
- Detect orientation on home screen
- Use side-by-side Row layout in landscape (ring + button)
- Prevent content from being cut off in landscape

## Phase 4: Photos + Reminders

### 6. Weekly Photo Reminder (Thursday 9:00)
- Scheduled notification every Thursday at 9:00 AM
- Reminds user to take progress photos

### 7. In-App Photo Capture
- Camera screens for front, left, and right photos
- Photos linked to current aligner set and date
- Photo gallery/history view

## Phase 5: Network Features (Back-end TBD)

### 8. Feedback Submission
- In-app feedback form
- API stub for future backend integration

### 9. Sharing Options
- Share progress via Android share intents
- Export tracking data (CSV or similar)
