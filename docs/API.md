# API Documentation

## SleepEntry Data Model

### Fields

| Field | Type | Description | Example |
|-------|------|-------------|---------|
| `id` | Long | Primary key, auto-generated | 1 |
| `date` | String | Sleep date in M-d-yy format | "8-1-26" |
| `sleepTime` | String | Bedtime in 12-hour format | "11:39pm" |
| `fallAsleepMin` | String | Time to fall asleep | "30min" |
| `wakeTime` | String | Wake time in 12-hour format | "6:45am" |
| `totalSleep` | String | Calculated total sleep duration | "7h 6m" |

### Data Format

The app uses a specific text format for bulk input:

```
date-----sleep_time+time_to_fall_asleep-------wake_time
```

#### Examples
```
8-1-26-----11:39pm+30min-------6:45am
8-1-27-----12:15am+15min-------7:30am
8-1-28-----10:45pm+45min-------6:00am
```

## Parsing Logic

### Date Format
- Pattern: `M-d-yy`
- Examples: `8-1-26`, `12-25-24`
- Month and day can be 1 or 2 digits
- Year should be 2 digits

### Time Format
- Pattern: `H:mmam/pm` or `Ham/pm`
- Examples: `11:39pm`, `6:45am`, `12am`, `11pm`
- Case insensitive (AM/PM or am/pm)
- Minutes are optional

### Duration Format
- Pattern: `Xmin` or `Xh Ymin`
- Examples: `30min`, `1h 15min`, `45min`
- Space between hours and minutes is optional

## Sleep Duration Calculation

### Algorithm
1. Parse sleep time to minutes since midnight
2. Parse fall-asleep duration to minutes
3. Parse wake time to minutes since midnight
4. Calculate actual sleep start: `sleepTime + fallAsleepDuration`
5. Calculate total sleep: `wakeTime - actualSleepStart`
6. Handle overnight: if negative, add 24 hours
7. Format as "Xh Ym"

### Example Calculation
```
Sleep: 11:39pm (23:39) = 23*60 + 39 = 1419 minutes
Fall asleep: 30min = 30 minutes
Wake: 6:45am (06:45) = 6*60 + 45 = 405 minutes

Actual sleep start: 1419 + 30 = 1449 minutes
Total sleep: 405 - 1449 = -1044 minutes
Add 24 hours: -1044 + 1440 = 396 minutes

Format: 396 minutes = 6h 36m
```

## Database Schema

### Table: sleep_entries
```sql
CREATE TABLE sleep_entries (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    date TEXT NOT NULL,
    sleepTime TEXT NOT NULL,
    fallAsleepMin TEXT NOT NULL,
    wakeTime TEXT NOT NULL,
    totalSleep TEXT NOT NULL
);
```

### Indexes
- Primary key on `id`
- Date index for chronological ordering

## Validation Rules

### Required Fields
- All fields are required
- Date must match M-d-yy pattern
- Time must match 12-hour format
- Duration must be positive

### Business Logic
- Sleep time and wake time can be the same (24-hour sleep)
- Fall-asleep duration cannot be negative
- Total sleep is calculated automatically
- Entries are sorted by date (newest first)

## Error Handling

### Parsing Errors
- Invalid format returns `null` for bulk parsing
- Individual entries show validation errors
- Empty fields are rejected

### Edge Cases
- Midnight crossing (sleep before midnight, wake after)
- Same day sleep (sleep and wake same day)
- Very long/short durations
- Invalid time combinations

## Export Formats

### CSV Format
```csv
date,sleepTime,fallAsleepMin,wakeTime,totalSleep
8-1-26,11:39pm,30min,6:45am,7h 6m
8-1-27,12:15am,15min,7:30am,7h 15m
```

### JSON Format
```json
[
  {
    "id": 1,
    "date": "8-1-26",
    "sleepTime": "11:39pm",
    "fallAsleepMin": "30min",
    "wakeTime": "6:45am",
    "totalSleep": "7h 6m"
  }
]
```
