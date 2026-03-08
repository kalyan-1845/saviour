# SARATHI Emergency - Beast Workflow Plan

## Problem Statement
The current system requires too many details from users during an emergency (registration, login, multiple form fields). For a person having a heart attack, this is unnecessary and wastes precious time.

## Proposed Solution
Simplify the emergency workflow to the absolute minimum - user enters only ONE thing (phone number), and the system handles everything else automatically. Also include complete database of Hyderabad & Secunderabad hospitals and police stations.

---

## Proposed Workflow

### Step 1: User Triggers Emergency (Simplest Flow)
- User clicks "Emergency SOS" button on homepage
- Page loads with ONLY:
  - Phone number input (auto-filled from device if available)
  - One-tap "SEND SOS" button
- **NO registration, NO login, NO forms**

### Step 2: Auto-Registration (Background)
- System checks if phone number exists in database
- If NOT registered → Auto-create account in background:
  - Phone number as identifier
  - Auto-generate temporary credentials
  - Save login state to localStorage
- User is now "logged in" automatically for future use

### Step 3: Auto-Location Tracking
- Request location permission immediately
- Auto-track live location using GPS
- No manual address entry needed

### Step 4: Instant Assignment (Target: Within 1 Minute)
- System automatically:
  1. Finds nearest hospital/police from database based on location
  2. Finds nearest available driver/ambulance
  3. Creates EmergencyTrip with status "assigned"
  4. Sends alert to driver with user location + destination
  5. Sends alert to hospital/police with: User phone + Live map location

### Step 5: Hospital/Police Gets Direct Info
- Hospital/Police receives:
  - User's phone number
  - Live map location (Google Maps link)
  - Emergency type (e.g., "Heart Attack", "Accident", "Crime")
  - ETA of ambulance/police vehicle

---

## Database Seeding - Hyderabad & Secunderabad

### Hospital Database (50+ Hospitals)
Include all major hospitals in Hyderabad and Secunderabad:
- Government Hospitals (Osmania, Gandhi, Nizam's, etc.)
- Private Hospitals (Apollo, Care, Yashoda, Sunshine, etc.)
- Specialty Hospitals (Cancer, Cardiac, Trauma)
- Each with: name, address, latitude, longitude, phone, specialties, bedsAvailable

### Police Database (All Police Stations)
Include all police stations in Hyderabad & Secunderabad:
- Police Commissionerate offices
- Circle Inspector offices
- Police Stations (each area)
- Each with: name, address, latitude, longitude, phone, jurisdiction

### Ambulance/Driver Database
- Pre-register ambulance drivers
- Track availability status
- Store vehicle details (ambulance type, equipment)

---

## Implementation Plan

### Phase 1: Database Seeding
- [ ] Create Hospital model with all Hyderabad/Secunderabad data
- [ ] Create PoliceStation model with all data
- [ ] Create seed script to populate database
- [ ] Run initial seeding

### Phase 2: Backend APIs
- [ ] Create `/api/hospitals` endpoint (get all hospitals)
- [ ] Create `/api/police` endpoint (get all police stations)
- [ ] Create `/api/emergency/sos` endpoint (main emergency handler)
- [ ] Create `/api/emergency/assign` endpoint (assign driver)
- [ ] Create `/api/drivers/available` endpoint (get available drivers)

### Phase 3: Frontend - Simplified SOS Page
- [ ] Create `app/public-sos/page.tsx` → New simplified emergency page
- [ ] Minimal UI: Phone input + Big SOS button
- [ ] Auto-location hook integration
- [ ] Show nearby hospitals/police on map
- [ ] Loading states and feedback

### Phase 4: Driver App Flow
- [ ] Driver receives emergency alert
- [ ] Driver accepts/rejects trip
- [ ] Navigation to pickup location
- [ ] Status updates (en route, arrived, completed)

### Phase 5: Hospital/Police Dashboard
- [ ] View incoming emergencies
- [ ] See user phone + location
- [ ] Track ambulance/police vehicle

---

## Files to Create/Modify

### New Files:
1. `models/Hospital.ts` - Hospital model
2. `models/PoliceStation.ts` - Police station model
3. `scripts/seed-db.ts` - Database seeding script
4. `app/api/hospitals/route.ts` - Get hospitals API
5. `app/api/police/route.ts` - Get police stations API
6. `app/api/emergency/sos/route.ts` - Main SOS API
7. `app/api/emergency/assign/route.ts` - Assign driver API
8. `app/api/drivers/available/route.ts` - Get available drivers
9. `app/public-sos/page.tsx` - Simplified SOS page

### Files to Modify:
1. `store/useEmergencyStore.ts` → Add emergency types (medical/police)
2. `models/EmergencyTrip.ts` → Add police station info
3. `app/page.tsx` → Link to simplified SOS

---

## Key Benefits
- ✅ User enters only PHONE NUMBER
- ✅ Auto-login after first use
- ✅ Live location auto-tracked
- ✅ Driver assigned within 1 minute
- ✅ Hospital/Police gets direct phone + location
- ✅ No unnecessary forms during emergency
- ✅ Complete Hyderabad/Secunderabad database
- ✅ Works for Medical + Police emergencies
