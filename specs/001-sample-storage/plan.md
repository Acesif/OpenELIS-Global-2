# Implementation Plan: Sample Storage Management

**Branch**: `001-sample-storage` | **Date**: 2025-10-30 | **Last Updated**: 2025-11-22 | **Spec**:
[spec.md](./spec.md)  
**Input**: Feature specification from `/specs/001-sample-storage/spec.md`

## Summary

Implement POC for Sample Storage Management to track physical location of
biological samples through a flexible storage hierarchy (Room → Device → Shelf →
Rack → Position). Positions can have 2-5 levels (minimum: room+device, maximum:
room+device+shelf+rack+position). POC scope includes core tracking workflows:
assignment (P1), search/retrieval (P2A), movement (P2B), and basic Storage
Dashboard (P4 - metrics cards, tabs, data tables). Defers disposal workflow (P3)
and advanced dashboard features (drill-down navigation, CSV export) to post-POC
iterations.

**Technical Approach**: Leverage existing OpenELIS infrastructure (5-layer
backend architecture, HAPI FHIR R4 server, Carbon Design System UI) to add
storage location tracking. Create reusable Storage Location Selector widget with
two-tier design (compact inline view + expanded modal) supporting cascading
dropdowns, type-ahead autocomplete, and quick-find search. Widget used in both
orders (SamplePatientEntry) and results (LogbookResults) workflows. Implement
samples table overflow menu with Manage Location (consolidates Move and View
Storage), Dispose, and View Audit (placeholder) actions. Create consolidated
Location Management Modal that handles both assignment (when no location exists)
and movement (when location exists) with dynamic wording and conditional fields.
Create Dispose modal matching Figma design. Implement full CRUD operations for
location tabs (Rooms, Devices, Shelves, Racks) with overflow menu actions (Edit,
Delete) - Edit modal allows editing all fields except Code and Parent (read-only),
Delete validates constraints (child locations, active samples) before deletion.
Map storage entities to FHIR Location resources for external interoperability.
**Note**: MoveSampleModal is largely implemented and can be used as a starting
point for the consolidated Location Management Modal.

**Amendment (2025-11-07)**: Add expandable row functionality to location tables
(Rooms, Devices, Shelves, Racks) using Carbon DataTable expandable row pattern.
Expanded rows display additional entity fields (not visible in table columns) as
key-value pairs in read-only format. Only one row can be expanded at a time.
Expansion triggered by clicking chevron icon in dedicated first column. See
[research.md](./research.md#8-carbon-datatable-expandable-rows) for implementation
details.

**Amendment (2025-11-22)**: Add comprehensive barcode workflow implementation
following TDD approach. Includes unified input field (scan/type-ahead), 5-step
validation, debouncing (500ms), visual feedback, dual barcode auto-detection,
"last-modified wins" logic, label management (short code, printing, print
history), and error recovery. All barcode requirements from FR-023 through
FR-027f must be implemented. See Phase 10 below for detailed TDD workflow.

**Amendment (2025-01-15)**: Update capacity calculation logic to implement
two-tier system (per FR-062a, FR-062b, FR-062c). Devices and Shelves support
manual `capacity_limit` (static) or calculated capacity from children. If
`capacity_limit` is NULL, calculate from child locations (sum if all
children have defined capacities). If any child lacks defined capacity,
parent capacity cannot be determined and UI displays "N/A" with tooltip. Racks
always use calculated capacity (rows × columns). UI must visually distinguish
between manual and calculated capacities (badge, tooltip, or icon). See
updated capacity calculator implementation in Phase 2 below.

## Technical Context

**Language/Version**: Java 21 LTS (backend), React 17 (frontend)  
**Primary Dependencies**:

- Backend: Spring Boot 3.x, Hibernate 6.x, HAPI FHIR R4 (v6.6.2), JPA
- Frontend: @carbon/react v1.15.0, React Intl 5.20.12, Formik 2.2.9,
  getFromOpenElisServer/postToOpenElisServer utilities

**Storage**: PostgreSQL 14+ (existing OpenELIS database)  
**Testing**:

- Backend: JUnit 5 + Mockito (unit/integration)
- Frontend: Jest + React Testing Library (unit), Cypress 12.17.3 (E2E - existing
  OpenELIS framework)
- FHIR: Resource validation against R4 profiles

**Target Platform**: Web application (Linux server deployment, browser-based
UI)  
**Project Type**: Web (backend + frontend integration)  
**Performance Goals**: Reasonable response times for POC (few seconds for
searches/saves), no optimization required  
**Constraints**:

- POC scope only (P1, P2A, P2B user stories)
- > 70% test coverage per OpenELIS constitution
- FHIR R4 integration mandatory for storage entities

**Scale/Scope**:

- 5 storage entity types (Room, Device, Shelf, Rack, Position)
- 6 REST API endpoint groups (hierarchy CRUD with Edit/Delete, assignment, movement, search, barcode validation, label management)
- 1 reusable UI widget (Storage Location Selector with two-tier design)
- 6 modal components (Consolidated Location Management, Dispose, Edit Location, Delete Location confirmation, Label Management)
- 2 overflow menu components (samples table row actions, location table row actions)
- 2 integration points (SamplePatientEntry, LogbookResults)
- Barcode workflow components (unified input field, debouncing, visual feedback, label printing)

**Development Approach**: Test-Driven Development (TDD)

- Write tests BEFORE implementation code
- Order: API contracts → FHIR validation tests → Integration tests → Unit tests
  → Implementation → E2E tests
- All tests must pass before moving to next component
- Target >70% coverage per OpenELIS constitution

## Constitution Check

_GATE: Must pass before Phase 0 research. Re-check after Phase 1 design._

Verify compliance with
[OpenELIS Global 3.0 Constitution](../../.specify/memory/constitution.md):

- [x] **Configuration-Driven**: Position naming free-text (no validation),
      capacity thresholds configurable
- [x] **Carbon Design System**: UI uses @carbon/react exclusively (Tabs,
      DataTable with expandable rows, Modal, TextInput, Dropdown, OverflowMenu)
- [x] **FHIR/IHE Compliance**: All hierarchy levels (Room, Device, Shelf, Rack,
      Position) map to FHIR Location resources, sample links via
      Specimen.container. Positions can have 2-5 levels (minimum: room+device,
      maximum: room+device+shelf+rack+position).
- [x] **Layered Architecture**: Backend follows 5-layer pattern (StorageRoom
      valueholder → DAO → Service → Controller → Form)
- [x] **Test Coverage**: Unit + integration + Cypress E2E tests planned (>70%
      coverage goal per spec)
- [x] **Schema Management**: Liquibase changesets for 5 entity tables + junction
      tables, all with fhir_uuid columns
- [x] **Internationalization**: All UI strings use React Intl message keys (en,
      fr, sw minimum)
- [x] **Security & Compliance**: RBAC (Technicians/Managers/Admins), audit trail
      (sys_user_id, lastupdated), input validation

**Complexity Justification**: None required - plan fully compliant with
constitution.

## Project Structure

### Documentation (this feature)

```text
specs/001-sample-storage/
├── spec.md              # Feature specification (completed)
├── plan.md              # This file (/speckit.plan output)
├── research.md          # Phase 0 output (technology validation)
├── data-model.md        # Phase 1 output (entity schemas)
├── quickstart.md        # Phase 1 output (dev setup)
├── contracts/           # Phase 1 output (API specifications)
│   ├── storage-api.json # OpenAPI 3.0 spec for REST endpoints
│   └── fhir-mappings.md # FHIR Location resource mappings
└── tasks.md             # Phase 2 output (/speckit.tasks - deferred)
```

### Source Code (repository root)

```text
# Backend (Java) - OpenELIS Global existing structure
src/main/java/org/openelisglobal/storage/
├── valueholder/
│   ├── StorageRoom.java
│   ├── StorageDevice.java
│   ├── StorageShelf.java
│   ├── StorageRack.java
│   ├── StoragePosition.java
│   ├── SampleStorageAssignment.java
│   └── SampleStorageMovement.java
├── dao/
│   ├── StorageRoomDAO.java (+ impl)
│   ├── StorageDeviceDAO.java (+ impl)
│   ├── StorageShelfDAO.java (+ impl)
│   ├── StorageRackDAO.java (+ impl)
│   ├── StoragePositionDAO.java (+ impl)
│   ├── SampleStorageAssignmentDAO.java (+ impl)
│   └── SampleStorageMovementDAO.java (+ impl)
├── service/
│   ├── StorageLocationService.java (+ impl) - CRUD for hierarchy
│   ├── SampleStorageService.java (+ impl) - Assignment/movement logic
│   └── StorageSearchService.java (+ impl) - Search/filter operations
├── controller/
│   ├── StorageLocationRestController.java
│   ├── SampleStorageRestController.java
│   └── StorageSearchRestController.java
├── form/
│   ├── StorageLocationForm.java
│   ├── SampleAssignmentForm.java
│   └── SampleMovementForm.java
└── fhir/
    └── StorageLocationFhirTransform.java - FHIR Location mapping

src/main/resources/
├── liquibase/storage/
│   ├── 001-create-storage-tables.xml
│   ├── 002-create-assignment-tables.xml
│   └── 003-add-fhir-uuid-columns.xml
└── hibernate/hbm/
    ├── StorageRoom.hbm.xml
    ├── StorageDevice.hbm.xml
    ├── StorageShelf.hbm.xml
    ├── StorageRack.hbm.xml
    ├── StoragePosition.hbm.xml
    ├── SampleStorageAssignment.hbm.xml
    └── SampleStorageMovement.hbm.xml

src/test/java/org/openelisglobal/storage/
├── service/ - Service layer unit tests
├── controller/ - REST endpoint integration tests
└── fhir/ - FHIR transformation tests

# Frontend (React) - OpenELIS Global existing structure
frontend/src/components/storage/
├── StorageLocationSelector/
│   ├── StorageLocationSelector.jsx - Main reusable widget (two-tier: compact + modal)
│   ├── CompactLocationView.jsx - Compact inline view showing location path + expand button
│   ├── LocationSelectorModal.jsx - Expanded modal view with full assignment form
│   ├── QuickFindSearch.jsx - Quick-find search component (type-ahead autocomplete)
│   ├── CascadingDropdownMode.jsx - Cascading dropdowns for expanded modal
│   ├── AutocompleteMode.jsx - Type-ahead autocomplete for expanded modal
│   ├── UnifiedBarcodeInput.jsx - Unified input field supporting both barcode scan and type-ahead search
│   ├── BarcodeValidationService.js - Client-side barcode format validation and parsing
│   ├── BarcodeDebounceHook.js - Custom hook for 500ms debouncing logic
│   ├── BarcodeVisualFeedback.jsx - Visual feedback component (green checkmark, red X, ready state)
│   ├── StorageLocationSelector.test.jsx
│   └── index.js
├── StorageDashboard/
│   ├── StorageDashboard.jsx - Main dashboard component
│   ├── StorageLocationsMetricCard.jsx - Color-coded metric card showing location breakdown by type
│   ├── LocationFilterDropdown.jsx - Single location dropdown with autocomplete and tree view
│   ├── LocationTreeView.jsx - Hierarchical tree view component (expand/collapse)
│   ├── LocationAutocomplete.jsx - Autocomplete search component (flat list with full paths)
│   ├── LocationFilterDropdown.test.jsx
│   ├── StorageLocationsMetricCard.test.jsx
│   └── index.js
├── SampleStorage/
│   ├── LocationManagementModal.jsx - Consolidated modal for assignment and movement (replaces MoveSampleModal and ViewStorageModal)
│   ├── DisposeSampleModal.jsx - Dispose sample modal with reason/method/confirmation
│   ├── SampleActionsOverflowMenu.jsx - Overflow menu component for samples table rows (Manage Location, Dispose, View Audit)
│   ├── BulkMoveModal.jsx
│   └── index.js
├── LocationManagement/
│   ├── LocationActionsOverflowMenu.jsx - Overflow menu component for location table rows (Edit, Delete, Label Management)
│   ├── EditLocationModal.jsx - Modal for editing location entities (Room/Device/Shelf/Rack)
│   ├── DeleteLocationModal.jsx - Confirmation modal for deleting locations with constraint validation
│   ├── LabelManagementModal.jsx - Modal for short code management and label printing (Devices, Shelves, Racks)
│   ├── ShortCodeInput.jsx - Short code input with validation (max 10 chars, alphanumeric, auto-uppercase)
│   ├── PrintLabelButton.jsx - Print label button with PDF preview in new tab
│   ├── PrintHistoryDisplay.jsx - Read-only print history list component
│   └── index.js
└── hooks/
    ├── useStorageLocations.js - getFromOpenElisServer data fetching
    ├── useSampleStorage.js
    └── index.js

frontend/src/languages/
├── en.json - Add storage.* message keys
├── fr.json
└── sw.json

# Integration Points (modify existing files)
frontend/src/components/sample/SamplePatientEntry.jsx
frontend/src/components/logbook/LogbookResults.jsx

# E2E Tests (Cypress)
frontend/cypress/e2e/
├── storageAssignment.cy.js
├── storageSearch.cy.js
└── storageMovement.cy.js
```

**Structure Decision**: Follows existing OpenELIS monolithic repository
structure with clear module separation. Backend uses standard 5-layer pattern in
`org.openelisglobal.storage.*` package. Frontend components in
`frontend/src/components/storage/` with reusable widget design. Integration
points modify existing sample entry/search components to embed Storage Location
Selector widget.

## Test-Driven Development Workflow

**CRITICAL**: This POC follows **strict test-first development**. Tests are
written BEFORE implementation code.

### Development Order (Enforced)

**Phase 1: Contracts & Test Specifications**

1. ✅ API contracts (OpenAPI spec) - Define expected behavior
2. ✅ FHIR mappings documentation - Define FHIR resource structure
3. ✅ Data model documentation - Define entity relationships

**Phase 2: Test Creation (BEFORE any implementation code)**

1. **FHIR Validation Tests** - Write tests for FHIR Location resource
   creation/validation

   - Test: `StorageLocationFhirTransformTest.java`
   - Validates: Room/Device/Shelf/Rack/Position → Location resource structure
   - Validates: IHE mCSD profile compliance
   - Validates: Hierarchical partOf references correct

2. **Backend Unit Tests** - Write tests for service layer business logic
   - Test: `StorageLocationServiceImplTest.java`,
     `SampleStorageServiceImplTest.java`, `StorageSearchServiceImplTest.java`
   - Validates: Assignment validation logic (require Room and Device minimum 2
     levels, prevent inactive location, double-occupancy)
   - Validates: Capacity calculation and warning thresholds (80/90/100%) - includes two-tier capacity logic (manual vs calculated), hierarchical capacity calculation, and "N/A" handling when capacity cannot be determined
   - Validates: Hierarchical path construction
   - Validates: Bulk move auto-assignment logic
   - Validates: Audit trail creation on movements

2.5. **ORM Validation Tests** (Hibernate framework validation - ADDED based on
Phase 3 learnings)

- Test: `HibernateMappingValidationTest.java`
- Validates: All 7 storage entity Hibernate mappings load successfully
- Validates: SessionFactory builds without errors
- Validates: No JavaBean getter/setter conflicts (getActive vs isActive)
- Validates: Property names match between entities and .hbm.xml files
- Execution: <5 seconds, no database required
- **Purpose**: Catches ORM config errors before integration tests (fills gap
  between unit and integration)

3. **Backend Integration Tests** - Write tests for REST endpoints

   - Test: `StorageLocationRestControllerTest.java`,
     `SampleStorageRestControllerTest.java`,
     `StorageSearchRestControllerTest.java`
   - Validates: HTTP request/response contracts match OpenAPI spec
   - Validates: Database persistence after API calls
   - Validates: Error responses (400, 404, 409) for validation failures

4. **Frontend Unit Tests** - Write tests for React components
   - Test: `StorageLocationSelector.test.jsx`, `CompactLocationView.test.jsx`,
     `LocationSelectorModal.test.jsx`, `QuickFindSearch.test.jsx`,
     `CascadingDropdownMode.test.jsx`, etc.
   - Validates: Compact inline view displays location path correctly
   - Validates: Expand button opens modal
   - Validates: Quick-find search filters locations correctly

- Validates: Cascading dropdown state management
- Validates: Unified barcode input field accepts both scan and type-ahead
- Validates: Barcode format parsing (2-5 level hierarchical paths with hyphen delimiter)
- Validates: Debouncing logic (500ms cooldown, duplicate detection, different barcode warning)
- Validates: Visual feedback (ready state, success green checkmark, error red X)
- Validates: "Last-modified wins" behavior when both dropdowns and input field are used
- Validates: Pre-filling valid components when scan fails partially
- Validates: Hierarchical path display
- Validates: Validation requires Room and Device selection (minimum 2 levels),
  Shelf/Rack/Position optional
- Validates: API error handling and user feedback
- Test: `SampleActionsOverflowMenu.test.jsx`
- Validates: Menu renders with all three items (Manage Location, Dispose, View
  Audit placeholder)
- Validates: "View Audit" is disabled
- Test: `LocationManagementModal.test.jsx` (consolidated from MoveSampleModal and
  ViewStorageModal)
- Validates: Modal renders with correct title/button based on location existence
- Validates: Comprehensive sample information section (ID, Type, Status, Date
  Collected, Patient ID, Test Orders)
- Validates: Current Location section only appears when location exists
- Validates: Reason for Move field appears only when moving (location exists AND
  different location selected)
- Validates: Location selection updates preview
- Validates: Validation prevents moving to same location
- Validates: Condition Notes field always visible
- Test: `DisposeSampleModal.test.jsx`
- Validates: Modal renders with warning alert, sample info, disposal form fields
- Validates: "Confirm Disposal" button disabled until checkbox checked
- Validates: Validation requires reason and method selection

**Phase 3: Implementation (Make tests pass)**

1. **Backend Implementation** - Write code to pass tests

   - Liquibase changesets (schema)
   - Valueholder entities
   - DAO implementations
   - Service implementations
   - Controller implementations
   - FHIR transform service

2. **Frontend Implementation** - Write code to pass tests
   - Storage Location Selector widget
   - Integration into SamplePatientEntry, LogbookResults
   - Data fetching hooks

### Phase 3.1: Tab-Specific Search Functionality Implementation (FR-064(a))

### Objective

Implement tab-specific search functionality per FR-064 and FR-064a:

- **Samples tab**: Debounced live search (300-500ms) by sample ID, accession
  number type/prefix, and assigned location (full hierarchical path)
- **Rooms tab**: Search by name and code
- **Devices tab**: Search by name, code, and type
- **Shelves tab**: Search by name (label)
- **Racks tab**: Search by name (label)

All searches use case-insensitive partial/substring matching with OR logic
(matches any of the specified fields).

### TDD Approach

Following strict test-first development:

1. **Write failing tests** (Red phase)
2. **Implement minimal code to pass** (Green phase)
3. **Refactor while keeping tests green** (Refactor phase)

### Test Specifications

#### Backend Integration Tests (Write First)

**File**:
`src/test/java/org/openelisglobal/storage/controller/StorageSearchRestControllerTest.java`

**Test Cases**:

1. **Samples Search Tests**:

   - `testSearchSamples_BySampleId_ReturnsMatching()` - Search by exact sample
     ID
   - `testSearchSamples_ByAccessionPrefix_ReturnsMatching()` - Search by
     accession prefix (e.g., "S-2025" matches "S-2025-001")
   - `testSearchSamples_ByLocationPath_ReturnsMatching()` - Search by location
     path substring (e.g., "Freezer" matches "Main Laboratory > Freezer Unit 1 >
     ...")
   - `testSearchSamples_CombinedFields_OR_Logic()` - Search matches ANY of the
     three fields (OR logic)
   - `testSearchSamples_CaseInsensitive()` - "freezer" matches "Freezer Unit 1"
   - `testSearchSamples_PartialMatch()` - "S-202" matches "S-2025-001"
   - `testSearchSamples_EmptyQuery_ReturnsAll()` - Empty search returns all
     samples
   - `testSearchSamples_NoMatches_ReturnsEmpty()` - No matches returns empty
     array

2. **Rooms Search Tests**:

   - `testSearchRooms_ByName_ReturnsMatching()` - Search by name
     (case-insensitive partial)
   - `testSearchRooms_ByCode_ReturnsMatching()` - Search by code
     (case-insensitive partial)
   - `testSearchRooms_CombinedFields_OR_Logic()` - Matches name OR code

3. **Devices Search Tests**:

   - `testSearchDevices_ByName_ReturnsMatching()` - Search by name
   - `testSearchDevices_ByCode_ReturnsMatching()` - Search by code
   - `testSearchDevices_ByType_ReturnsMatching()` - Search by type (freezer,
     refrigerator, etc.)
   - `testSearchDevices_CombinedFields_OR_Logic()` - Matches name OR code OR
     type

4. **Shelves Search Tests**:

   - `testSearchShelves_ByLabel_ReturnsMatching()` - Search by label
     (case-insensitive partial)

5. **Racks Search Tests**:
   - `testSearchRacks_ByLabel_ReturnsMatching()` - Search by label
     (case-insensitive partial)

**Test Data Setup**:

- Use JDBC to insert test data (rooms, devices, shelves, racks, samples,
  assignments)
- Ensure test data covers:
  - Multiple samples with different accession prefixes
  - Samples in different locations (different hierarchical paths)
  - Entities with various names/codes/types for testing partial matches

**API Contract**:

- `GET /rest/storage/samples/search?q={searchTerm}` - Search samples
- `GET /rest/storage/rooms/search?q={searchTerm}` - Search rooms
- `GET /rest/storage/devices/search?q={searchTerm}` - Search devices
- `GET /rest/storage/shelves/search?q={searchTerm}` - Search shelves
- `GET /rest/storage/racks/search?q={searchTerm}` - Search racks

**Expected Response Format**:

- All endpoints return `List<Map<String, Object>>` matching existing API format
- Samples: Include sample ID, type, status, location (full path), assigned by,
  date
- Rooms: Include id, name, code, description, active
- Devices: Include id, name, code, type, roomId, roomName, active
- Shelves: Include id, label, deviceId, deviceName, roomId, roomName, active
- Racks: Include id, label, shelfId, shelfLabel, deviceId, deviceName, roomId,
  roomName, active

#### Backend Service Unit Tests (Write First)

**File**:
`src/test/java/org/openelisglobal/storage/service/StorageSearchServiceImplTest.java`

**Test Cases**:

1. **Sample Search Service Tests**:

   - `testSearchSamples_FiltersBySampleId()` - Filter samples by ID substring
   - `testSearchSamples_FiltersByAccessionPrefix()` - Filter by accession prefix
   - `testSearchSamples_FiltersByLocationPath()` - Filter by location path
     substring
   - `testSearchSamples_OR_Logic()` - Matches if ANY field matches
   - `testSearchSamples_CaseInsensitive()` - Case-insensitive matching
   - `testSearchSamples_EmptyQuery_ReturnsAll()` - Empty query returns all
   - `testSearchSamples_NullQuery_ReturnsAll()` - Null query returns all

2. **Room Search Service Tests**:

   - `testSearchRooms_FiltersByNameOrCode()` - Matches name OR code

3. **Device Search Service Tests**:

   - `testSearchDevices_FiltersByNameCodeOrType()` - Matches name OR code OR
     type

4. **Shelf Search Service Tests**:

   - `testSearchShelves_FiltersByLabel()` - Matches label

5. **Rack Search Service Tests**:
   - `testSearchRacks_FiltersByLabel()` - Matches label

**Mock Strategy**:

- Mock DAO calls to return test data
- Verify service calls DAO methods with correct filters
- Test search logic in isolation

#### Frontend Unit Tests (Write First)

**File**:
`frontend/src/components/storage/__tests__/StorageDashboardSearch.test.jsx`

**Test Cases**:

1. **Search Input Component Tests**:

   - `testSearchInput_RendersCorrectly()` - Renders search input with
     placeholder
   - `testSearchInput_UpdatesOnChange()` - Updates state on input change
   - `testSearchInput_DebouncedForSamples()` - Debounces input for samples tab
     (300-500ms)
   - `testSearchInput_ImmediateForOtherTabs()` - Immediate or submit-button for
     other tabs

2. **Search Results Tests**:

   - `testSearchResults_FiltersSamples()` - Filters samples by search term
   - `testSearchResults_FiltersRooms()` - Filters rooms by search term
   - `testSearchResults_FiltersDevices()` - Filters devices by search term
   - `testSearchResults_FiltersShelves()` - Filters shelves by search term
   - `testSearchResults_FiltersRacks()` - Filters racks by search term
   - `testSearchResults_CaseInsensitive()` - Case-insensitive matching
   - `testSearchResults_PartialMatch()` - Partial substring matching
   - `testSearchResults_EmptySearch_ShowsAll()` - Empty search shows all items

3. **Tab-Specific Search Tests**:
   - `testSamplesTab_SearchesByIdLocationPrefix()` - Samples tab searches by ID,
     accession prefix, location
   - `testRoomsTab_SearchesByNameCode()` - Rooms tab searches by name and code
   - `testDevicesTab_SearchesByNameCodeType()` - Devices tab searches by name,
     code, type
   - `testShelvesTab_SearchesByLabel()` - Shelves tab searches by label
   - `testRacksTab_SearchesByLabel()` - Racks tab searches by label

**Mock Strategy**:

- Mock API calls (`getFromOpenElisServer`)
- Use React Testing Library for component rendering
- Test user interactions (typing, debouncing)

#### Frontend E2E Tests (Write First)

**File**: `frontend/cypress/e2e/storageSearch.cy.js` (update existing)

**Test Cases**:

1. **Samples Tab Search E2E**:

   - `testSamplesSearch_BySampleId()` - Search by sample ID, verify results
   - `testSamplesSearch_ByAccessionPrefix()` - Search by accession prefix,
     verify results
   - `testSamplesSearch_ByLocationPath()` - Search by location path, verify
     results
   - `testSamplesSearch_Debounced()` - Verify debounced search (300-500ms delay)
   - `testSamplesSearch_CaseInsensitive()` - Verify case-insensitive matching
   - `testSamplesSearch_PartialMatch()` - Verify partial substring matching

2. **Rooms Tab Search E2E**:

   - `testRoomsSearch_ByName()` - Search rooms by name
   - `testRoomsSearch_ByCode()` - Search rooms by code

3. **Devices Tab Search E2E**:

   - `testDevicesSearch_ByName()` - Search devices by name
   - `testDevicesSearch_ByCode()` - Search devices by code
   - `testDevicesSearch_ByType()` - Search devices by type

4. **Shelves Tab Search E2E**:

   - `testShelvesSearch_ByLabel()` - Search shelves by label

5. **Racks Tab Search E2E**:
   - `testRacksSearch_ByLabel()` - Search racks by label

**Test Strategy**:

- Load test fixtures with known data
- Intercept API calls to verify search parameters
- Verify UI updates with filtered results
- Verify search persists across tab switches

### Implementation Tasks (After Tests Pass)

#### Backend Implementation

1. **Create Search Service Interface** (`StorageSearchService.java`):

   ```java
   List<Map<String, Object>> searchSamples(String query);
   List<StorageRoom> searchRooms(String query);
   List<StorageDevice> searchDevices(String query);
   List<StorageShelf> searchShelves(String query);
   List<StorageRack> searchRacks(String query);
   ```

2. **Implement Search Service** (`StorageSearchServiceImpl.java`):

   - Sample search: Query by sample ID, accession prefix, location path (OR
     logic)
   - Room search: Query by name OR code (case-insensitive LIKE)
   - Device search: Query by name OR code OR type (case-insensitive LIKE)
   - Shelf search: Query by label (case-insensitive LIKE)
   - Rack search: Query by label (case-insensitive LIKE)
   - All searches use case-insensitive substring matching

3. **Update REST Controllers**:

   - Add `GET /rest/storage/samples/search?q={term}` endpoint
   - Add `GET /rest/storage/rooms/search?q={term}` endpoint
   - Add `GET /rest/storage/devices/search?q={term}` endpoint
   - Add `GET /rest/storage/shelves/search?q={term}` endpoint
   - Add `GET /rest/storage/racks/search?q={term}` endpoint
   - All endpoints return JSON arrays matching existing API format

4. **Query Optimization**:
   - Use database indexes on searchable columns (name, code, type, label)
   - For samples location search: Use full-text search on hierarchical path or
     JOIN through hierarchy
   - Consider PostgreSQL `ILIKE` for case-insensitive matching

#### Frontend Implementation

1. **Update Search Component** (`StorageDashboard.jsx`):

   - Add search input field (already exists, update logic)
   - Implement debounced search for samples tab (300-500ms delay)
   - Implement search logic for each tab:
     - Samples: Search by ID, accession prefix, location path
     - Rooms: Search by name and code
     - Devices: Search by name, code, and type
     - Shelves: Search by label
     - Racks: Search by label
   - Use case-insensitive partial matching
   - Combine search with existing filters (AND logic)

2. **API Integration**:

   - Call search endpoints when search term is entered
   - For samples: Use debounced API calls (300-500ms delay)
   - For other tabs: Use debounced or submit-button search
   - Update table data with filtered results

3. **Search State Management**:
   - Store search term in component state
   - Clear search when switching tabs
   - Persist search term within tab (optional enhancement)

### Testing Checklist

- [ ] All backend integration tests pass
- [ ] All backend service unit tests pass
- [ ] All frontend unit tests pass
- [ ] All E2E tests pass
- [ ] Search works correctly for samples (ID, accession prefix, location)
- [ ] Search works correctly for rooms (name, code)
- [ ] Search works correctly for devices (name, code, type)
- [ ] Search works correctly for shelves (label)
- [ ] Search works correctly for racks (label)
- [ ] Case-insensitive matching verified
- [ ] Partial/substring matching verified
- [ ] Debounced search (300-500ms) verified for samples tab
- [ ] Empty search shows all items
- [ ] Search combines correctly with filters (AND logic)
- [ ] Performance acceptable (<2 seconds for 100,000+ records)

### Dependencies

- Existing filter functionality (FR-065) - search must work alongside filters
- Existing API endpoints for fetching data
- Existing table rendering components

### Success Criteria

- All tests pass (integration, unit, E2E)
- Search functionality works for all 5 tabs per FR-064
- Debounced search implemented for samples tab (300-500ms)
- Case-insensitive partial matching working
- Search combines with filters (AND logic)
- Performance meets requirements (<2 seconds)

---

**Phase 4: Dashboard Storage Locations Metric Card & E2E Tests**

### Dashboard Storage Locations Metric Card Implementation

**Objective**: Implement color-coded Storage Locations metric card with
breakdown by type and matching tab accents per FR-057 and FR-057a.

**Requirements**:

- Display formatted text list: "X rooms, Y devices, Z shelves, W racks" (active
  locations only)
- Color-code text using Carbon Design System tokens:
  - Rooms: `blue-70` (blue-70)
  - Devices: `teal-70` (teal-70)
  - Shelves: `purple-70` (purple-70)
  - Racks: `orange-70` (orange-70)
- Apply matching subtle accent colors to corresponding tab labels/backgrounds:
  - Rooms tab: subtle blue accent matching "X rooms" text color
  - Devices tab: subtle teal accent matching "Y devices" text color
  - Shelves tab: subtle purple accent matching "Z shelves" text color
  - Racks tab: subtle orange accent matching "W racks" text color
- Tab coloring must be very subtle (light background tint or border accent, not
  overpowering)

**Implementation Tasks**:

1. Update `StorageDashboard.jsx` to calculate active location counts by type
   (Room, Device, Shelf, Rack)
2. Create `StorageLocationsMetricCard.jsx` component displaying formatted
   breakdown with color-coded text
3. Apply Carbon color tokens to metric card text using Carbon `Text` component
   or inline styles
4. Update tab styling to include subtle accent colors matching metric card
   colors
5. Ensure colorblind accessibility (Carbon tokens are WCAG compliant)

**Testing**:

- Verify metric card displays correct counts for active locations only
- Verify color-coding matches Carbon Design System tokens
- Verify tab accent colors are subtle and match metric card colors
- Verify colorblind accessibility (test with colorblind simulation tools)

### E2E Tests - Validate complete workflows

1. **Cypress E2E Tests** - Test user scenarios end-to-end
   - Test: `storageAssignment.cy.js` (P1 user story)
   - Test: `storageSearch.cy.js` (P2A user story)
   - Test: `storageMovement.cy.js` (P2B user story)
   - Test: `storageDashboardFilter.cy.js` (Dashboard Samples tab filtering)
     - Test single location dropdown with autocomplete search
     - Test hierarchical browsing (tree view expand/collapse)
     - Test filtering by Room, Device, Shelf, and Rack levels
     - Verify downward inclusive filtering (selecting device shows all samples
       in child shelves/racks/positions)
     - Test inactive location display (visual distinction)
     - Test combination of location filter and status filter
     - Verify Position-level locations excluded from dropdown (only
       Room/Device/Shelf/Rack)
   - Test: `storageDashboardMetrics.cy.js` (Storage Locations metric card)
     - Verify metric card displays formatted breakdown: "X rooms, Y devices, Z
       shelves, W racks"
     - Verify color-coding matches specification (blue-70, teal-70, purple-70,
       orange-70)
     - Verify only active locations included in counts
     - Verify tab accent colors match metric card colors and are subtle
     - Verify colorblind accessibility

### Test-First Principles

**MANDATORY RULES**:

- ❌ **DO NOT** write implementation code before tests exist
- ❌ **DO NOT** skip test creation "to move faster"
- ✅ **DO** write failing tests first (Red phase)
- ✅ **DO** write minimal implementation to pass tests (Green phase)
- ✅ **DO** refactor after tests pass (Refactor phase)

**Benefits for POC**:

- Clear acceptance criteria (tests define "done")
- Prevents scope creep (only implement what tests require)
- Regression protection (catch breaks immediately)
- Living documentation (tests show how code should be used)

**Verification Gates**:

- After FHIR tests: All FHIR resources validate against R4 spec
- After integration tests: All API endpoints return correct responses per
  OpenAPI spec
- After unit tests: All business logic validated (assignment rules, capacity
  warnings, audit trails)
- After frontend tests: All UI components render correctly and handle user
  interactions
- After E2E tests: All user scenarios (P1, P2A, P2B) work end-to-end

## Complexity Tracking

No complexity violations - plan fully compliant with OpenELIS Global 3.0
Constitution.

---

## Phase 0: Outline & Research

**Objective**: Validate technology choices, resolve unknowns, document best
practices for OpenELIS patterns.

### Research Tasks

No NEEDS CLARIFICATION items in Technical Context - all technologies specified
per constitution. Research focuses on validating existing OpenELIS patterns and
best practices.

**Research Questions**:

1. **Hibernate XML Mapping Pattern**: How are existing OpenELIS entities mapped
   using Hibernate XML? (Examine existing .hbm.xml files for reference patterns)

2. **FHIR Location Resource Structure**: What is the correct FHIR R4 Location
   resource structure for hierarchical storage? (Validate against IHE mCSD
   profile requirements)

3. **Carbon Dropdown Cascading**: What is the best practice for implementing
   cascading dropdowns in Carbon Design System? (Check @carbon/react Dropdown
   component API)

4. **Barcode Scanner Integration**: How do USB HID barcode scanners emit
   keyboard input in browser context? (Research browser keyboard event handling
   for scan gun input)

5. **SWR Data Fetching Pattern**: How does existing OpenELIS frontend use SWR
   for API calls? (Examine existing hooks in `frontend/src/hooks/`)

6. **Cypress E2E Setup**: What is the OpenELIS Cypress configuration and test
   structure? (Examine existing cypress.config.js and test files in
   cypress/e2e/)

### Research Output Structure

Create `research.md` with sections:

```markdown
# Research: Sample Storage Management

## 1. Hibernate XML Mapping Pattern

- **Pattern**: [Describe existing OpenELIS pattern from .hbm.xml files]
- **Example**: [Reference to existing entity mapping]
- **Application**: [How to apply to StorageRoom, StorageDevice, etc.]

## 2. FHIR Location Resource Structure

- **R4 Specification**: [Link to HL7 FHIR R4 Location resource]
- **IHE mCSD Profile**: [Hierarchical location requirements]
- **Mapping Strategy**: [Room → Location, Device → Location.partOf, etc.]

## 3. Carbon Dropdown Cascading

- **Component**: [@carbon/react Dropdown API reference]
- **Pattern**: [Controlled component with onChange handlers]
- **Data Flow**: [Parent state management for cascading selection]

## 4. Barcode Scanner Browser Integration

- **Event Type**: [Keyboard events with rapid character input]
- **Detection**: [Timing-based detection (characters within ~50ms = scan)]
- **Implementation**: [useEffect hook with keydown listener]

## 5. SWR Data Fetching Pattern

- **Existing Pattern**: [Reference to OpenELIS hooks using SWR]
- **Caching Strategy**: [SWR cache key structure]
- **Mutation Pattern**: [useSWRMutation for POST/PUT/DELETE]

## 6. Cypress E2E Configuration

- **Status**: Existing Cypress 12.17.3 framework in OpenELIS
- **Configuration**: See enhanced configuration section below (per Constitution V.5)
- **Test Structure**: Page object pattern from existing tests
- **Best Practices**: See Test Refactoring Patterns section below
```

**Deliverable**: `specs/001-sample-storage/research.md` with all 6 questions
answered

---

## Phase 1: Design & Contracts (Test Specifications)

**Prerequisites**: research.md complete

**Objective**: Create design artifacts that serve as **test specifications**.
These documents define WHAT to test BEFORE writing any code.

### Task 1.1: Generate Data Model (Test Specification)

Create `data-model.md` documenting entity schemas, relationships, and validation
rules. This document serves as the specification for:

- **FHIR validation tests**: Verify entity → FHIR Location transformation
  correctness
- **Integration tests**: Verify database persistence matches schema
- **Unit tests**: Verify validation rules enforced

**Content**:

**Entities** (extract from spec.md Key Entities section):

1. **StorageRoom**

   - Fields: id (VARCHAR(36)), fhir_uuid (UUID), name (VARCHAR(255)), code
     (VARCHAR(50)), description (TEXT), active (BOOLEAN), sys_user_id (INT),
     lastupdated (TIMESTAMP)
   - Constraints: Unique (code), NOT NULL (name, code, active)
   - Relationships: One-to-Many with StorageDevice

2. **StorageDevice**

   - Fields: id, fhir_uuid, name, code, type (ENUM:
     freezer/refrigerator/cabinet/other), temperature_setting (DECIMAL),
     capacity_limit (INT), active, parent_room_id (FK), sys_user_id, lastupdated
   - Constraints: Unique (code within parent_room_id), NOT NULL (name, code,
     type, parent_room_id)
   - Relationships: Many-to-One with StorageRoom, One-to-Many with StorageShelf

3. **StorageShelf**

   - Fields: id, fhir_uuid, label, capacity_limit (INT), active,
     parent_device_id (FK), sys_user_id, lastupdated
   - Constraints: Unique (label within parent_device_id), NOT NULL (label,
     parent_device_id)
   - Relationships: Many-to-One with StorageDevice, One-to-Many with StorageRack

4. **StorageRack**

   - Fields: id, fhir_uuid, label, rows (INT), columns (INT),
     position_schema_hint (VARCHAR(50)), active, parent_shelf_id (FK),
     sys_user_id, lastupdated
   - Constraints: Unique (label within parent_shelf_id), NOT NULL (label,
     parent_shelf_id), CHECK (rows >= 0 AND columns >= 0)
   - Relationships: Many-to-One with StorageShelf, One-to-Many with
     StoragePosition
   - Calculated: capacity = rows \* columns (or 0 if no grid)

5. **StoragePosition**

   - Fields: id, fhir_uuid (UUID), coordinate (VARCHAR(50), optional), row_index
     (INT, optional), column_index (INT, optional), occupied (BOOLEAN DEFAULT
     false), parent_device_id (FK, required), parent_shelf_id (FK, optional),
     parent_rack_id (FK, optional), sys_user_id, lastupdated
   - Constraints: NOT NULL (parent_device_id), UNIQUE (fhir_uuid), coordinate
     optional (only for 5-level positions), CHECK (if parent_rack_id NOT NULL
     then parent_shelf_id NOT NULL), CHECK (if coordinate NOT NULL then
     parent_rack_id NOT NULL), coordinate allows duplicates within same rack
     (flexible storage)
   - Relationships: Many-to-One with StorageDevice (required), Many-to-One with
     StorageShelf (optional), Many-to-One with StorageRack (optional),
     One-to-One with SampleStorageAssignment (current)
   - Note: Position represents the lowest level in hierarchy for a sample
     assignment. Can be at device level (2 levels), shelf level (3 levels), rack
     level (4 levels), or position level (5 levels). Minimum requirement is
     device level (room + device); cannot be just a room. Maps to FHIR Location
     resource with occupancy extension.

6. **SampleStorageAssignment**

   - Fields: id, sample_id (FK to Sample), location_id (numeric, NOT NULL),
     location_type (VARCHAR(20), NOT NULL), position_coordinate (VARCHAR(50),
     nullable), assigned_by_user_id (FK to SystemUser), assigned_date
     (TIMESTAMP), notes (TEXT)
   - Constraints: NOT NULL (sample_id, location_id, location_type,
     assigned_by_user_id), Unique (sample_id) - one current location per sample
   - CHECK constraint: `location_type` must be one of: 'device', 'shelf', 'rack'
     (position is just text coordinate, not entity)
   - Relationships: Many-to-One with Sample, Polymorphic relationship to
     StorageDevice/StorageShelf/StorageRack via location_id + location_type,
     Many-to-One with SystemUser
   - Note: Represents CURRENT location. Supports flexible assignment to any
     hierarchy level (device/shelf/rack) via simplified polymorphic
     relationship. Position is represented as optional text field
     (`position_coordinate`), not a separate entity reference. Historical moves
     tracked in SampleStorageMovement.

7. **SampleStorageMovement**
   - Fields: id, sample_id (FK), previous_position_id (FK), new_position_id
     (FK), moved_by_user_id (FK), movement_date (TIMESTAMP), reason (TEXT)
   - Constraints: NOT NULL (sample_id, moved_by_user_id, movement_date),
     previous_position_id OR new_position_id can be NULL (initial assignment or
     removal)
   - Relationships: Many-to-One with Sample, Many-to-One with StoragePosition
     (previous), Many-to-One with StoragePosition (new), Many-to-One with
     SystemUser
   - Note: Immutable audit log. Insertion only, no updates/deletes.

**Validation Rules** (from spec.md Functional Requirements):

- Require minimum 2 levels for valid location: Room and Device MUST be selected
  (FR-033a). Shelf, Rack, and Position are optional. Position can be at device
  level (2 levels), shelf level (3 levels), rack level (4 levels), or position
  level (5 levels). Minimum requirement is device level (room + device); cannot
  be just a room.
- Prevent assignment to inactive location (FR-035)
- Prevent double-occupancy unless rack allows duplicates (FR-034)
- Capacity warnings at 80%, 90%, 100% (FR-036) - no hard block
- Position coordinate free text, max 50 chars (FR-010)
- Hierarchical barcode uniqueness (FR-004)

**State Transitions**:

- Sample: No location → Assigned → Moved (multiple times) → [Disposed - deferred
  to P3]
- Position: Empty (occupied=false) → Occupied (occupied=true) → Empty (on sample
  move/disposal)
- Location hierarchy: Active → Inactive (deactivation requires no active samples
  or warning)

### Task 1.2: Generate API Contracts (Test Specification)

Create `/contracts/storage-api.json` (OpenAPI 3.0) specification. This document
serves as the contract for:

- **Backend integration tests**: Verify REST endpoints match request/response
  schemas
- **Frontend component tests**: Mock API responses match contract
- **E2E tests**: Verify complete request/response flow

**Endpoints**:

**Storage Hierarchy Management**:

- `GET /rest/storage/rooms` - List all rooms (with optional filters)
- `POST /rest/storage/rooms` - Create room
- `GET /rest/storage/rooms/{id}` - Get room details
- `PUT /rest/storage/rooms/{id}` - Update room
- `DELETE /rest/storage/rooms/{id}` - Delete room (if no children)
- `GET /rest/storage/devices` - List devices (filterable by room)
- `POST /rest/storage/devices` - Create device
- [... similar CRUD for shelves, racks, positions]

**Sample Storage Assignment**:

- `POST /rest/storage/samples/assign` - Assign sample to location
  - Request:
    `{ sample_id, location_id, location_type, position_coordinate?, notes }`
  - Response: `{ assignment_id, hierarchical_path, assigned_date }`
  - Validation:
    - `location_id` and `location_type` are required (NOT NULL)
    - `location_type` must be one of: 'device', 'shelf', 'rack' (position is
      just text coordinate, not entity)
    - If `location_type = 'device'`: Minimum 2 levels (room + device per
      FR-033a)
    - If `location_type = 'shelf'`: 3 levels (room + device + shelf)
    - If `location_type = 'rack'`: 4 levels (room + device + shelf + rack)
    - Location must be active (check entire hierarchy: room, device, shelf,
      rack)
    - `position_coordinate` is optional text (max 50 chars) for any
      location_type to provide specific position information

**Sample Search**:

- `GET /rest/storage/samples/search?sample_id={id}` - Search by sample ID
  - Response:
    `{ sample_id, type, status, location: { room, device, shelf, rack, position, hierarchical_path }, assigned_by, assigned_date }`
- `GET /rest/storage/samples?location_id={id}&location_type={room|device|shelf|rack}&status={active}` -
  Filter samples by location (single location dropdown) and status
  - `location_id`: ID of selected location (Room, Device, Shelf, or Rack)
  - `location_type`: Hierarchy level of selected location (determines downward
    inclusive filtering)
  - Filter behavior: Returns all samples within selected location's hierarchy
    (downward inclusive)
  - Example:
    `GET /rest/storage/samples?location_id=123&location_type=device&status=active`
    returns all samples in device 123 and all its child shelves/racks/positions

**Sample Movement**:

- `POST /rest/storage/samples/move` - Move sample to new location
  - Request:
    `{ sample_id, location_id, location_type, position_coordinate?, reason }`
  - Response: `{ movement_id, previous_location, new_location, moved_date }`
  - Validation:
    - `location_id` and `location_type` are required (NOT NULL)
    - `location_type` must be one of: 'device', 'shelf', 'rack' (position is
      just text coordinate, not entity)
    - If `location_type = 'device'`: Minimum 2 levels (room + device per
      FR-033a)
    - If `location_type = 'shelf'`: 3 levels (room + device + shelf)
    - If `location_type = 'rack'`: 4 levels (room + device + shelf + rack)
    - Location must be active (check entire hierarchy: room, device, shelf,
      rack)
    - `position_coordinate` is optional text (max 50 chars) for any
      location_type to provide specific position information
- `POST /rest/storage/samples/bulk-move` - Bulk move samples
  - Request:
    `{ sample_ids: [], target_rack_id, position_assignments: [{sample_id, position_coordinate}] }`
  - Response: `{ movement_ids: [], summary: { total, successful, failed } }`

**Sample Disposal**:

- `POST /rest/storage/samples/dispose` - Dispose sample
  - Request: `{ sample_id, reason, method, notes, date_time }`
  - Response: `{ disposal_id, sample_id, disposed_date, location_at_disposal }`
  - Validation: Requires authorization (role-based permission check), reason and
    method required
  - Note: Disposal workflow deferred to post-POC (P3), but endpoint structure
    defined

**Location Quick-Find Search**:

- `GET /rest/storage/locations/search?q={term}` - Search locations at any
  hierarchy level
  - Query parameter: `q` - search term (location name or code)
  - Response: `[{ id, name, code, type, hierarchical_path, level }]` - Array of
    matching locations with full hierarchical paths
  - Behavior: Case-insensitive partial matching across Room, Device, Shelf, and
    Rack levels
  - Example: `GET /rest/storage/locations/search?q=freezer` returns devices
    matching "freezer" with full paths like "Main Laboratory > Freezer Unit 1"

**Barcode Generation**: ✅ Implemented in Phase 10 (barcode validation, label management, printing)

### Task 1.3: Generate FHIR Mappings (Test Specification)

Create `/contracts/fhir-mappings.md` documenting FHIR resource structure. This
document serves as the specification for:

- **FHIR validation tests**: Verify transform service outputs match FHIR R4
  Location spec
- **IHE mCSD compliance tests**: Verify hierarchical queries work correctly
- **Integration tests**: Verify FHIR sync occurs after entity persistence

**Content**:

**FHIR R4 Location Resource Mapping**:

```markdown
# FHIR Location Mappings for Storage Entities

## Room → FHIR Location

- `Location.id` = StorageRoom.fhir_uuid
- `Location.name` = StorageRoom.name
- `Location.identifier.value` = StorageRoom.code
- `Location.status` = StorageRoom.active ? "active" : "inactive"
- `Location.description` = StorageRoom.description
- `Location.mode` = "instance"
- `Location.physicalType.coding.code` = "ro" (room)

## Device → FHIR Location

- `Location.id` = StorageDevice.fhir_uuid
- `Location.name` = StorageDevice.name
- `Location.identifier.value` = "ROOM_CODE-DEVICE_CODE" (hierarchical)
- `Location.status` = active/inactive
- `Location.mode` = "instance"
- `Location.physicalType.coding.code` = "ve" (vehicle/equipment)
- `Location.type.coding.code` = StorageDevice.type (freezer/fridge/cabinet)
- `Location.partOf.reference` = "Location/{parent_room_fhir_uuid}"

## Shelf → FHIR Location

- Similar to Device
- `Location.partOf.reference` = "Location/{parent_device_fhir_uuid}"
- `Location.physicalType.coding.code` = "co" (container)

## Rack → FHIR Location

- Similar to Shelf
- `Location.partOf.reference` = "Location/{parent_shelf_fhir_uuid}"
- Custom extension for rows/columns: `extension[grid-dimensions]`

## Position → FHIR Location (with Extensions)

- Maps to FHIR R4 `Location` resource (child of parent location)
- `Location.id` = StoragePosition.fhir_uuid
- `Location.identifier.value` = hierarchical code based on position level:
  - Device level: "{room_code}-{device_code}"
  - Shelf level: "{room_code}-{device_code}-{shelf_label}"
  - Rack level: "{room_code}-{device_code}-{shelf_label}-{rack_label}"
  - Position level:
    "{room_code}-{device_code}-{shelf_label}-{rack_label}-{coordinate}"
- `Location.name` = coordinate (if position level) or device/shelf/rack label
  (if lower level)
- `Location.partOf.reference` = "Location/{parent_fhir_uuid}" (parent device,
  shelf, or rack depending on position level)
- `Location.extension[position-occupancy].valueBoolean` = occupied status
- `Location.extension[position-grid-row].valueInteger` = row index (optional)
- `Location.extension[position-grid-column].valueInteger` = column index
  (optional)

## Sample-to-Location Link

- `Specimen.container.identifier.value` = full hierarchical path
- `Specimen.container.extension[storage-position-location].valueReference` =
  "Location/{position_fhir_uuid}"
- `Specimen.extension[storage-assigned-date].valueDateTime` = assignment
  timestamp
```

**IHE mCSD Compliance**:

- All Location resources queryable via `GET /fhir/Location?partOf={parent_id}`
- Hierarchical queries supported: `GET /fhir/Location?_include=Location:partOf`
- Position availability queries:
  `GET /fhir/Location?partOf={rack_fhir_uuid}&extension=position-occupancy|false`

**FHIR Sync Strategy**:

- All entities (Room, Device, Shelf, Rack, Position): Sync immediately on entity
  create/update via @PostPersist/@PostUpdate hooks
- Uses existing OpenELIS FHIR sync pattern (FhirTransformService +
  FhirPersistanceService)
- Specimen: Update container extension on sample assignment/movement

**Inline Location Creation**:

- Uses same REST endpoints (POST /storage/rooms, POST /storage/devices, etc.)
- Frontend manages state to immediately show newly created location in selector
  dropdown
- No special "inline" endpoints needed - standard CRUD operations

**SamplePatientEntry Integration** (Orders Workflow):

- **Integration Point**: Below "Collector" field in sample collection section
- **Widget Placement**: After collector dropdown, before sample collection time
- **Behavior**: Optional assignment (can be left blank and assigned later)
- **Widget Structure**: Compact inline view showing selected location path (or
  "Not assigned") with "Expand" button
- **Expanded Modal**: Opens full location assignment modal matching View Storage
  modal structure (sample info, current location, full assignment form)
- **Component**: Embeds
  `<StorageLocationSelector workflow="orders" optional={true} />`

**LogbookResults Integration** (Results Workflow):

- **Integration Point**: Below existing referral/test result fields in expanded
  sample details
- **Widget Structure**: Compact inline view with quick-find search input
  (type-ahead autocomplete) for rapidly finding existing locations + "Expand"
  button
- **Quick-Find Search**: Matches location names/codes at any hierarchy level
  (Room, Device, Shelf, or Rack) using case-insensitive partial matching,
  displays full hierarchical path in results
- **Expanded Modal**: Opens full location assignment modal matching View Storage
  modal structure (sample info, current location, full assignment form)
- **Behavior**: Shows current location (read-only or editable based on
  permissions), allows Move action via overflow menu
- **Component**: Embeds
  `<StorageLocationSelector workflow="results" showQuickFind={true} />`

### Task 1.4: Generate Quickstart (Test-First Development Guide)

Create `quickstart.md` documenting test-first development workflow and
environment setup. This guide emphasizes running tests BEFORE writing
implementation code.

````markdown
# Quickstart: Sample Storage Management POC

## Prerequisites

- OpenELIS Global 3.0 development environment running (see
  [dev_setup.md](../../../docs/dev_setup.md))
- PostgreSQL 14+ database accessible
- Java 21, Maven 3.8+, Node.js 16+

## Backend Setup

1. **Database Migration**
   ```bash
   # Liquibase changesets auto-run on application startup
   # Verify migration: psql -U clinlims -d clinlims -c "\dt storage_*"
   ```
````

2. **Build Backend**

   ```bash
   cd /Users/pmanko/code/OpenELIS-Global-2
   mvn clean install -DskipTests -Dmaven.test.skip=true
   ```

3. **Run Tests**

   ```bash
   # Unit tests
   mvn test -Dtest="org.openelisglobal.storage.**"

   # Integration tests (requires DB)
   mvn verify -Dtest="org.openelisglobal.storage.controller.**"
   ```

## Frontend Setup

1. **Install Dependencies** (if not already done)

   ```bash
   cd frontend
   npm install
   ```

2. **Add Internationalization Keys**

   - Edit `frontend/src/languages/en.json`, `fr.json`, `sw.json`
   - Add storage.\* message keys (see translations section in this doc)

3. **Run Frontend Dev Server**

   ```bash
   npm start
   # Access at https://localhost/
   ```

4. **Run Frontend Tests**

   ```bash
   # Unit tests
   npm test -- components/storage

   # E2E tests (Cypress) - Run individually per Constitution V.5
   npm run cy:run -- --spec "cypress/e2e/storageAssignment.cy.js"
   npm run cy:run -- --spec "cypress/e2e/storageSearch.cy.js"
   npm run cy:run -- --spec "cypress/e2e/storageMovement.cy.js"
   # Full suite only in CI/CD: npm run cy:run
   ```

## FHIR Validation

1. **Access FHIR Server**

   ```
   https://fhir.openelis.org:8443/fhir/
   ```

2. **Query Storage Locations**

   ```bash
   # Get all rooms
   curl https://fhir.openelis.org:8443/fhir/Location?physicalType=ro

   # Get devices in a specific room
   curl https://fhir.openelis.org:8443/fhir/Location?partOf=Location/{room_fhir_uuid}
   ```

3. **Validate FHIR Resource**
   ```bash
   # POST new Location resource and check response
   curl -X POST https://fhir.openelis.org:8443/fhir/Location \
     -H "Content-Type: application/fhir+json" \
     -d @test-location.json
   ```

## Testing User Scenarios

### P1: Basic Storage Assignment

1. Navigate to Sample Patient Entry
2. Complete sample accessioning
3. In Storage Location Selector widget:
   - Verify compact inline view shows "Not assigned" initially
   - Click "Expand" button to open full location assignment modal
   - In expanded modal, try cascading dropdown mode (Room → Device → Shelf →
     Rack → Position)
   - Try type-ahead autocomplete in expanded modal
   - Verify modal shows sample info box and current location section
   - Select location and verify hierarchical path updates in compact view
4. Verify assignment saved with hierarchical path

### P2A: Sample Search/Retrieval

1. Navigate to Logbook Results
2. Search for sample ID
3. Verify location displays in compact inline view (with quick-find search
   input)
4. Test quick-find search: Type location name/code, verify autocomplete results
   show full hierarchical paths
5. Select location from quick-find results, verify compact view updates
6. Click "Expand" button to open full location assignment modal
7. Navigate to Storage Dashboard, Samples tab
8. Test single location dropdown filter:
   - Open location dropdown
   - Test autocomplete search (type "Freezer" to find devices)
   - Test hierarchical browsing (expand/collapse tree view)
   - Select a Room → verify all samples in that room (downward inclusive)
   - Select a Device → verify all samples in that device and its children
   - Verify inactive locations are visually distinguished
   - Test combination: location filter + status filter

### P2B: Sample Movement

1. Navigate to Storage Dashboard, Samples tab
2. Find sample with assigned location
3. Click overflow menu (⋮) in Actions column
4. Verify menu shows: Manage Location, Dispose, View Audit (placeholder/disabled)
5. Click "Manage Location" from overflow menu
6. Verify Location Management modal opens (titled "Move Sample" since location
   exists) with:
   - Comprehensive sample information section (Sample ID, Type, Status, Date
     Collected, Patient ID, Test Orders)
   - Current location displayed in gray box
   - Downward arrow icon separator
   - Location selector in bordered box (Room/Device/Shelf/Rack/Position
     selectors)
   - Condition Notes textarea
   - "Selected Location" preview box (shows "Not selected" initially)
   - Cancel and "Confirm Move" buttons
7. Select new location, verify "Selected Location" preview updates
8. Verify "Reason for Move" field appears (since location exists and different
   location selected)
9. Enter reason (optional)
10. Click "Confirm Move"
11. Verify audit trail updated

### P2B Extended: Location Management Modal - Assignment Mode

1. Navigate to Storage Dashboard, Samples tab
2. Find sample without assigned location
3. Click overflow menu (⋮) in Actions column
4. Click "Manage Location"
5. Verify Location Management modal opens (titled "Assign Storage Location" since
   no location exists) with:
   - Comprehensive sample information section (Sample ID, Type, Status, Date
     Collected, Patient ID, Test Orders)
   - No Current Location section (location doesn't exist)
   - Horizontal line separator
   - Location selector in bordered box (Room/Device/Shelf/Rack/Position
     selectors)
   - Condition Notes textarea
   - "Selected Location" preview box
   - Cancel and "Assign" buttons
6. Select location assignment using form
7. Verify "Reason for Move" field does NOT appear (no existing location)
8. Click "Assign"
9. Verify assignment saved

### P3: Sample Disposal (Deferred to Post-POC)

1. Navigate to Storage Dashboard, Samples tab
2. Find sample to dispose
3. Click overflow menu (⋮) in Actions column
4. Click "Dispose"
5. Verify Dispose modal opens with:
   - Modal title "Dispose Sample" with subtitle
   - Red warning alert at top ("This action cannot be undone")
   - Sample information section (Sample ID, Type, Status) in gray box
   - Current Storage Location section with location pin icon
   - Disposal instructions info box
   - Required "Disposal Reason" dropdown
   - Required "Disposal Method" dropdown
   - Optional "Additional Notes" textarea
   - Confirmation checkbox ("I confirm...")
   - Cancel and "Confirm Disposal" button (red/destructive styling, disabled
     until checkbox checked)
6. Select reason and method
7. Check confirmation checkbox
8. Click "Confirm Disposal"
9. Verify sample marked as disposed

## Troubleshooting

- **Liquibase migration fails**: Check `liquibase/storage/*.xml` syntax
- **FHIR sync fails**: Verify FHIR server running at configured URI
- **Frontend widget not appearing**: Check React Intl message keys loaded
- **Barcode scanner not detected**: Verify USB HID scanner emitting keyboard
  events

````

### Task 1.5: Update Agent Context

Run agent context update script:

```bash
cd /Users/pmanko/code/OpenELIS-Global-2
.specify/scripts/bash/update-agent-context.sh cursor-agent
````

This script will:

- Detect Cursor AI agent context file
- Add new technology references from this plan:
  - Storage module package structure
  - FHIR Location resource mapping (flexible hierarchy: positions can have 2-5
    levels)
  - Carbon Design System Storage Location Selector widget
  - Cypress E2E testing patterns (existing OpenELIS framework)
- Preserve existing OpenELIS patterns and manual additions

**Deliverables**:

- `data-model.md` - Entity schemas, relationships, validation rules
- `/contracts/storage-api.json` - OpenAPI 3.0 REST endpoints
- `/contracts/fhir-mappings.md` - FHIR R4 Location resource mappings
- `quickstart.md` - Developer setup instructions
- Updated agent context file (Cursor-specific)

---

## Phase 2: Task Breakdown (Deferred)

**Not executed by `/speckit.plan` command.**

Use `/speckit.tasks` command to generate detailed task breakdown from this plan.
Tasks will be organized by user story (P1, P2A, P2B) with dependency ordering
and parallel execution markers.

---

## Post-Phase 1 Constitution Re-Check

_Re-verify compliance after design artifacts generated:_

- [x] **Configuration-Driven**: Position coordinates remain free-text, no
      hardcoded validation
- [x] **Carbon Design System**: Storage Location Selector uses @carbon/react
      Dropdown, TextInput, Button components
- [x] **FHIR/IHE Compliance**: All hierarchy levels (Room, Device, Shelf, Rack,
      Position) map to FHIR Location resources, IHE mCSD hierarchy supported.
      Positions can have 2-5 levels (minimum: room+device, maximum:
      room+device+shelf+rack+position).
- [x] **Layered Architecture**: All 5 layers present (Valueholder → DAO →
      Service → Controller → Form)
- [x] **Test Coverage**: Unit/integration/Cypress E2E test structure defined in
      quickstart
- [x] **Schema Management**: Liquibase changesets planned in
      `liquibase/storage/` with fhir_uuid columns
- [x] **Internationalization**: Message keys documented for en/fr/sw in
      quickstart
- [x] **Security & Compliance**: RBAC enforced in controllers, audit fields in
      all entities

**Final Verdict**: ✅ Plan fully compliant with OpenELIS Global 3.0 Constitution

---

---

## Phase 5: Overflow Menu and Consolidated Location Management Modal Implementation

### Objective

Implement samples table row overflow menu with three actions (Manage Location,
Dispose, View Audit placeholder) and consolidated Location Management Modal that
handles both assignment and movement workflows. The consolidated modal replaces
the previous separate Move and View Storage modals. **Note**: MoveSampleModal is
largely implemented and can be used as a starting point for consolidation.

### Overflow Menu Component

**Component**: `SampleActionsOverflowMenu.jsx`

**Requirements**:

- Uses Carbon Design System `OverflowMenu` component
- Displays three menu items:
  1. **Manage Location** - Opens consolidated Location Management Modal (replaces
     previous Move and View Storage actions)
  2. **Dispose** - Opens Dispose modal
  3. **View Audit** - Placeholder (disabled or with visual indicator)
- Accessible via keyboard navigation and screen readers
- Integrated into samples table Actions column

**Implementation**:

```jsx
import { OverflowMenu, OverflowMenuItem } from "@carbon/react";

<OverflowMenu>
  <OverflowMenuItem
    itemText="Manage Location"
    onClick={() => openLocationManagementModal(sample)}
  />
  <OverflowMenuItem
    itemText="Dispose"
    onClick={() => openDisposeModal(sample)}
  />
  <OverflowMenuItem itemText="View Audit" disabled />
</OverflowMenu>;
```

**Testing**:

- Unit test: Menu renders with all three items
- Unit test: "View Audit" is disabled
- E2E test: Clicking "Manage Location" opens consolidated modal

### Consolidated Location Management Modal Component

**Component**: `LocationManagementModal.jsx` (consolidates previous MoveSampleModal
and ViewStorageModal)

**Starting Point**: Use existing `MoveSampleModal.jsx` as foundation and extend it
to support both assignment and movement workflows.

**Requirements** (per FR-038 through FR-044, consolidated from previous Move and
View Storage modals):

- **Dynamic Title and Button Wording**:
  - If no location assigned: Modal title "Assign Storage Location", button text
    "Assign"
  - If location exists: Modal title "Move Sample" with subtitle "Move sample
    [Sample ID] to a new storage location", button text "Confirm Move"
- **Comprehensive Sample Information** section: Highlighted/background box showing
  Sample ID, Type, Status, Date Collected, Patient ID, Test Orders
- **Current Location** section (conditional - only if location exists): Full
  hierarchical path (Room > Device > Shelf > Rack > Position) in highlighted gray
  background box. If no location exists, this section MUST NOT be displayed
- **Visual Separator**: Downward-pointing arrow icon if location exists, or
  horizontal line if no location
- **Location Selection Form** in bordered box:
  - Unified barcode input field (Quick Assign) - supports both barcode scan and type-ahead search (implemented in Phase 10)
  - Room dropdown selector (required, marked with \*)
  - Device dropdown selector
  - Shelf dropdown selector
  - Rack/Box dropdown selector
  - Position text input field (optional, with format hint)
  - Condition Notes textarea (optional)
- **Selected Location** preview section: Gray background box showing selected
  hierarchical path (displays "Not selected" until location chosen)
- **Reason for Move** field (conditional): Textarea field that appears ONLY when:
  (1) sample has existing location AND (2) user selects a different location.
  Field is optional (not required) and labeled "Reason for Move (optional)"
- Footer buttons: Cancel and action button with dynamic text ("Assign" if no
  location, "Confirm Move" if location exists). Action button uses primary/dark
  styling
- Uses Carbon Design System `Modal` component with proper accessibility
  attributes

**Implementation Notes**:

- **Refactor Strategy**: Start with existing `MoveSampleModal.jsx` and extend:
  1. Add logic to detect if sample has location (determines modal mode)
  2. Add comprehensive sample details section (Date Collected, Patient ID, Test
     Orders)
  3. Make Current Location section conditional (only show if location exists)
  4. Add Condition Notes field (always visible)
  5. Make Reason for Move field conditional (only show when moving)
  6. Update title and button text based on location existence
  7. Update API call to handle both assignment and movement (use same endpoint
     with different payloads)
- Reuses `LocationSelectorModal` component for location selection
- Updates "Selected Location" preview in real-time as user selects location
- Validates new location is different from current location (when moving)
- Calls `POST /rest/storage/samples/assign` for assignment or
  `POST /rest/storage/samples/move` for movement
- Handles both initial assignment and location changes in single unified
  interface

**Testing**:

- Unit test: Modal renders with correct title/button based on location existence
- Unit test: Sample information section shows comprehensive details
- Unit test: Current Location section only appears when location exists
- Unit test: Reason for Move field appears only when moving (location exists AND
  different location selected)
- Unit test: Location selection updates preview
- Unit test: Validation prevents moving to same location
- Unit test: Condition Notes field always visible
- E2E test: Complete assignment workflow (no location → assign)
- E2E test: Complete movement workflow (location exists → move with reason)

### Dispose Sample Modal Component

**Component**: `DisposeSampleModal.jsx`

**Requirements** (per FR-051a through FR-051k):

- Modal title: "Dispose Sample" with subtitle "Permanently dispose of sample
  [Sample ID]"
- Red warning alert box at top: "This action cannot be undone. The sample will
  be marked as disposed and removed from storage."
- **Sample Information** section: Gray background box showing Sample ID, Type,
  Status
- **Current Storage Location** section:
  - Location pin icon
  - Full hierarchical path in gray background box
  - Helper text: "Sample will be removed from this location upon disposal"
- Disposal instructions info box (blue/info styling) with sample-specific
  instructions
- Horizontal separator line
- Required fields:
  - "Disposal Reason \*" dropdown (required, marked with asterisk, initially
    shows "Select reason..." placeholder)
  - "Disposal Method \*" dropdown (required, marked with asterisk, initially
    shows "Select method..." placeholder)
- Optional "Additional Notes (optional)" textarea field
- Confirmation checkbox: "I confirm that I want to permanently dispose of this
  sample. This action cannot be undone."
- Footer buttons:
  - Cancel button
  - "Confirm Disposal" button:
    - Red/destructive action styling (e.g., rgba(231,0,11,0.6) background)
    - Disabled until confirmation checkbox checked
- Uses Carbon Design System `Modal` component with proper accessibility
  attributes

**Implementation Notes**:

- Dropdown values:
  - Disposal Reason: Expired, Contaminated, Patient Request, Testing Complete,
    Other
  - Disposal Method: Biohazard Autoclave, Chemical Neutralization, Incineration,
    Other
- Date/Time auto-set to current timestamp (editable for backdating if needed)
- Authorization check via role-based permissions
- Calls `POST /rest/storage/samples/dispose` endpoint on confirm

**Testing**:

- Unit test: Modal renders with all required sections
- Unit test: "Confirm Disposal" button disabled until checkbox checked
- Unit test: Validation requires reason and method selection
- E2E test: Complete disposal workflow with audit trail verification

**Note**: View Storage Modal functionality is now consolidated into Location
Management Modal. The consolidated modal handles both viewing current location and
editing/changing assignment in a single unified interface.

### Storage Location Selector Widget Structure Updates

**Component Updates**: `StorageLocationSelector.jsx`, `CompactLocationView.jsx`,
`LocationSelectorModal.jsx`, `QuickFindSearch.jsx`

**Two-Tier Design**:

1. **Compact Inline View** (`CompactLocationView.jsx`):

   - Displays selected location hierarchical path (or "Not assigned" if no
     location)
   - Shows "Expand" or "Edit" button
   - Results workflow: Includes quick-find search input (`QuickFindSearch.jsx`)
   - Quick-find: Type-ahead autocomplete matching Room/Device/Shelf/Rack levels,
     displays full hierarchical paths in results

2. **Expanded Modal View** (`LocationSelectorModal.jsx`):
   - Matches View Storage modal structure
   - Sample information section
   - Current location display
   - Full assignment form (barcode scan input, Room/Device/Shelf/Rack/Position
     selectors, condition notes)
   - Cancel and action buttons

**Implementation Notes**:

- Same widget component used in both SamplePatientEntry (orders) and
  LogbookResults (results)
- Quick-find search only shown in results workflow (`showQuickFind={true}` prop)
- Quick-find calls `GET /rest/storage/locations/search?q={term}` endpoint
- Unified barcode input field implemented in Phase 10 (supports both barcode scan and type-ahead search)

**Testing**:

- Unit test: Compact view displays location path correctly
- Unit test: Expand button opens modal
- Unit test: Quick-find search filters locations correctly
- Unit test: Modal structure matches View Storage modal
- E2E test: Complete assignment workflow in both orders and results contexts

## Phase 6: Location CRUD Operations Implementation

### Objective

Implement full CRUD operations for location tabs (Rooms, Devices, Shelves, Racks) with overflow menu actions (Edit, Delete) per FR-037f through FR-037v. Each location entity can be edited via modal dialog and deleted with validation constraints.

### Requirements Summary

- **Overflow Menu**: Each location table row (Rooms, Devices, Shelves, Racks) has overflow menu with Edit and Delete actions
- **Edit Modal**: Opens modal dialog with full form for editing location fields (Code and Parent fields are read-only)
- **Delete Operation**: Validates constraints (child locations, active samples) before deletion, shows confirmation dialog
- **Field Constraints**: Code and Parent relationship fields are read-only to prevent structural changes

### Components to Create

**Frontend Components**:

1. **LocationActionsOverflowMenu.jsx** - Overflow menu component for location table rows
   - Similar to `SampleActionsOverflowMenu.jsx`
   - Displays two menu items: Edit, Delete
   - Uses Carbon Design System `OverflowMenu` component

2. **EditLocationModal.jsx** - Modal for editing location entities
   - Generic component that adapts to Room/Device/Shelf/Rack entity types
   - Displays editable fields based on entity type
   - Code and Parent fields are read-only (disabled)
   - Validates code uniqueness and parent-child relationships
   - Uses Carbon Design System `Modal` component

3. **DeleteLocationModal.jsx** - Confirmation modal for deleting locations
   - Validates constraints before showing confirmation
   - Displays error message if constraints exist
   - Shows confirmation dialog with warning if no constraints
   - Uses Carbon Design System `Modal` component with destructive styling

**Backend Updates**:

1. **Update REST Controllers** - Add/update endpoints for Edit and Delete operations
   - `PUT /rest/storage/rooms/{id}` - Update room (already exists, may need validation updates)
   - `DELETE /rest/storage/rooms/{id}` - Delete room with constraint validation
   - Similar for devices, shelves, racks

2. **Update Service Layer** - Add constraint validation methods
   - `validateDeleteConstraints(LocationEntity)` - Check for child locations and active samples
   - `canDeleteLocation(LocationEntity)` - Returns boolean with reason if false
   - `getDeleteConstraintMessage(LocationEntity)` - Returns user-friendly error message

### Test Specifications

#### Backend Integration Tests

**File**: `src/test/java/org/openelisglobal/storage/controller/StorageLocationRestControllerTest.java`

**New Test Cases**:

1. **Edit Location Tests**:
   - `testUpdateRoom_UpdatesEditableFields()` - Update room name, description, status
   - `testUpdateRoom_CodeReadOnly()` - Attempt to update code, verify rejected or ignored
   - `testUpdateDevice_UpdatesEditableFields()` - Update device name, type, temperature, capacity
   - `testUpdateDevice_ParentReadOnly()` - Attempt to change parent room, verify rejected
   - `testUpdateShelf_UpdatesEditableFields()` - Update shelf label, capacity, status
   - `testUpdateRack_UpdatesEditableFields()` - Update rack label, dimensions, status
   - `testUpdateLocation_CodeUniquenessValidation()` - Attempt duplicate code, verify error
   - `testUpdateLocation_InvalidData_Returns400()` - Invalid field values return 400

2. **Delete Location Tests**:
   - `testDeleteRoom_WithChildDevices_ReturnsError()` - Cannot delete room with devices
   - `testDeleteRoom_WithActiveSamples_ReturnsError()` - Cannot delete room with active samples
   - `testDeleteRoom_NoConstraints_DeletesSuccessfully()` - Delete room with no children/samples
   - `testDeleteDevice_WithChildShelves_ReturnsError()` - Cannot delete device with shelves
   - `testDeleteDevice_WithActiveSamples_ReturnsError()` - Cannot delete device with active samples
   - `testDeleteShelf_WithChildRacks_ReturnsError()` - Cannot delete shelf with racks
   - `testDeleteRack_WithActiveSamples_ReturnsError()` - Cannot delete rack with active samples
   - `testDeleteLocation_ReturnsConstraintMessage()` - Error message includes specific reason
   - `testDeleteLocation_ConfirmationRequired()` - Successful deletion requires confirmation (handled in frontend)

**Test Data Setup**:
- Create test locations with various constraint scenarios
- Create test samples assigned to locations
- Verify constraint checking logic

#### Backend Service Unit Tests

**File**: `src/test/java/org/openelisglobal/storage/service/StorageLocationServiceImplTest.java`

**New Test Cases**:

1. **Constraint Validation Tests**:
   - `testValidateDeleteConstraints_RoomWithDevices_ReturnsFalse()` - Room with devices cannot be deleted
   - `testValidateDeleteConstraints_RoomWithActiveSamples_ReturnsFalse()` - Room with samples cannot be deleted
   - `testValidateDeleteConstraints_DeviceWithShelves_ReturnsFalse()` - Device with shelves cannot be deleted
   - `testValidateDeleteConstraints_LocationNoConstraints_ReturnsTrue()` - Location with no constraints can be deleted
   - `testGetDeleteConstraintMessage_RoomWithDevices_ReturnsMessage()` - Error message for room with devices
   - `testGetDeleteConstraintMessage_DeviceWithSamples_ReturnsMessage()` - Error message for device with samples

2. **Update Validation Tests**:
   - `testUpdateLocation_CodeUniquenessCheck()` - Verify code uniqueness validation
   - `testUpdateLocation_ReadOnlyFieldsIgnored()` - Code and Parent fields not updated even if provided

#### Frontend Unit Tests

**File**: `frontend/src/components/storage/__tests__/LocationActionsOverflowMenu.test.jsx`

**Test Cases**:
- `testOverflowMenu_RendersEditAndDelete()` - Menu renders with Edit and Delete items
- `testOverflowMenu_EditOpensModal()` - Clicking Edit opens EditLocationModal
- `testOverflowMenu_DeleteOpensModal()` - Clicking Delete opens DeleteLocationModal
- `testOverflowMenu_KeyboardAccessible()` - Menu accessible via keyboard navigation

**File**: `frontend/src/components/storage/__tests__/EditLocationModal.test.jsx`

**Test Cases**:
- `testEditModal_RendersForRoom()` - Modal renders with Room fields
- `testEditModal_RendersForDevice()` - Modal renders with Device fields
- `testEditModal_CodeFieldReadOnly()` - Code field is disabled/read-only
- `testEditModal_ParentFieldReadOnly()` - Parent field is disabled/read-only
- `testEditModal_EditableFieldsEnabled()` - Name, description, status fields are editable
- `testEditModal_ValidationErrors()` - Displays validation errors for duplicate code
- `testEditModal_SaveCallsAPI()` - Save button calls PUT endpoint
- `testEditModal_CancelClosesModal()` - Cancel button closes modal without saving

**File**: `frontend/src/components/storage/__tests__/DeleteLocationModal.test.jsx`

**Test Cases**:
- `testDeleteModal_WithConstraints_ShowsError()` - Shows error message if constraints exist
- `testDeleteModal_NoConstraints_ShowsConfirmation()` - Shows confirmation dialog if no constraints
- `testDeleteModal_ConfirmationRequired()` - Confirm button disabled until user confirms
- `testDeleteModal_DeleteCallsAPI()` - Delete button calls DELETE endpoint
- `testDeleteModal_CancelClosesModal()` - Cancel button closes modal without deleting

#### Frontend E2E Tests

**File**: `frontend/cypress/e2e/storageLocationCRUD.cy.js` (new file)

**Test Cases**:

1. **Edit Location E2E**:
   - `testEditRoom_UpdatesNameAndDescription()` - Edit room name and description
   - `testEditDevice_UpdatesTypeAndCapacity()` - Edit device type and capacity
   - `testEditLocation_CodeReadOnly()` - Verify code field cannot be edited
   - `testEditLocation_ValidationErrors()` - Verify duplicate code validation

2. **Delete Location E2E**:
   - `testDeleteRoom_WithDevices_ShowsError()` - Attempt to delete room with devices
   - `testDeleteDevice_WithSamples_ShowsError()` - Attempt to delete device with samples
   - `testDeleteLocation_NoConstraints_Deletes()` - Delete location with no constraints
   - `testDeleteLocation_ConfirmationRequired()` - Verify confirmation dialog appears

#### Cypress Configuration (Per Constitution V.5)

**File**: `frontend/cypress.config.js`

**Configuration Requirements**:

```javascript
const { defineConfig } = require("cypress");

module.exports = defineConfig({
  video: false, // MUST be disabled by default (per Constitution V.5)
  screenshotOnRunFailure: true, // MUST be enabled (per Constitution V.5)
  defaultCommandTimeout: 30000,
  viewportWidth: 1200,
  viewportHeight: 700,
  e2e: {
    setupNodeEvents(on, config) {
      // Browser console logging enabled by default (Cypress captures automatically)
      // Use on('task') to forward browser console to terminal if needed
      on('task', {
        log(message) {
          console.log(message);
          return null;
        }
      });
      return config;
    },
    baseUrl: "https://localhost",
    testIsolation: false, // Only if shared state needed
  },
});
```

**Key Configuration Points**:
- `video: false` - Disabled by default for performance (per Constitution V.5)
- `screenshotOnRunFailure: true` - Enabled for debugging (per Constitution V.5)
- Browser console logging automatically captured by Cypress
- Individual test execution during development (not full suite)

#### Test Refactoring Patterns

**Purpose**: Fix existing anti-patterns in Cypress E2E tests to align with best practices (per Constitution V.5).

**Pattern 1: Intercept Timing**

**Anti-Pattern**: Setting up intercepts after actions that trigger API calls
```javascript
// ❌ WRONG: Intercept after action
cy.get('[data-testid="storage-selector"]').click();
cy.intercept("GET", "**/rest/storage/rooms").as("getRooms");
cy.wait("@getRooms"); // May miss the request
```

**Correct Pattern**: Set up intercepts before actions
```javascript
// ✅ CORRECT: Intercept before action
cy.intercept("GET", "**/rest/storage/rooms").as("getRooms");
cy.get('[data-testid="storage-selector"]').click();
cy.wait("@getRooms");
```

**Pattern 2: Retry-Ability**

**Anti-Pattern**: Using `.then()` callbacks for state verification (no retry)
```javascript
// ❌ WRONG: No retry-ability
cy.get('[data-testid="modal"]').then(($el) => {
  expect($el).to.be.visible; // Fails immediately if not ready
});
```

**Correct Pattern**: Use `.should()` assertions that automatically retry
```javascript
// ✅ CORRECT: Retry-able assertions
cy.get('[data-testid="modal"]').should("be.visible");
cy.get('[data-testid="form-field"]').should("have.value", "expected");
```

**Pattern 3: Element Readiness**

**Anti-Pattern**: Missing element readiness checks before interaction
```javascript
// ❌ WRONG: No readiness check
cy.get('[data-testid="edit-modal"]').click(); // May not be ready
cy.get('[data-testid="name-field"]').type("new name"); // May fail
```

**Correct Pattern**: Wait for elements to be visible before interaction
```javascript
// ✅ CORRECT: Wait for readiness
cy.get('[data-testid="edit-modal"]').should("be.visible");
cy.wait("@getLocation");
cy.get('[data-testid="name-field"]').should("be.visible").and("not.be.empty");
```

**Pattern 4: State Verification**

**Anti-Pattern**: Using incorrect assertions for state verification
```javascript
// ❌ WRONG: Modal closure check
cy.get('[data-testid="modal"]').should("not.be.visible"); // May still exist in DOM
```

**Correct Pattern**: Use proper assertions for state changes
```javascript
// ✅ CORRECT: State verification
cy.get('[data-testid="modal"]').should("not.exist"); // Modal removed from DOM
cy.get(`[data-testid="row-${id}"]`).should("contain.text", newValue);
cy.get('div[role="status"]').should("be.visible").and("contain.text", "success");
```

**Pattern 5: Arbitrary Waits**

**Anti-Pattern**: Using fixed time delays instead of waiting for conditions
```javascript
// ❌ WRONG: Arbitrary wait
cy.wait(1000); // Fixed delay, may be too short or too long
```

**Correct Pattern**: Use Cypress's built-in waiting mechanisms
```javascript
// ✅ CORRECT: Wait for conditions
cy.intercept("GET", "**/rest/storage/rooms").as("getRooms");
cy.wait("@getRooms"); // Wait for API call
cy.get('[data-testid="dropdown"]').should("be.visible"); // Wait for element
```

**Refactoring Checklist** (for existing tests):
- [ ] Move all `cy.intercept()` calls to before actions that trigger them
- [ ] Replace `.then()` callbacks with `.should()` assertions for state verification
- [ ] Add visibility checks before all interactions (modals, form fields, buttons)
- [ ] Replace arbitrary `cy.wait(1000)` with proper waits (`cy.wait('@alias')` or `.should()`)
- [ ] Use proper assertions for state changes (`not.exist` instead of `not.be.visible`)
- [ ] Ensure tests can run individually (not dependent on full suite)

### API Contract Updates

**Update `/contracts/storage-api.json`**:

Add/update endpoints:

```json
{
  "paths": {
    "/rest/storage/rooms/{id}": {
      "put": {
        "summary": "Update room",
        "requestBody": {
          "content": {
            "application/json": {
              "schema": {
                "type": "object",
                "properties": {
                  "name": { "type": "string", "description": "Room name (editable)" },
                  "code": { "type": "string", "description": "Room code (read-only, ignored if provided)" },
                  "description": { "type": "string", "description": "Optional description (editable)" },
                  "active": { "type": "boolean", "description": "Active status (editable)" }
                },
                "required": ["name", "active"]
              }
            }
          }
        },
        "responses": {
          "200": { "description": "Room updated successfully" },
          "400": { "description": "Validation error (duplicate code, invalid data)" },
          "404": { "description": "Room not found" }
        }
      },
      "delete": {
        "summary": "Delete room",
        "responses": {
          "200": { "description": "Room deleted successfully" },
          "409": { 
            "description": "Cannot delete - constraints exist",
            "content": {
              "application/json": {
                "schema": {
                  "type": "object",
                  "properties": {
                    "error": { "type": "string" },
                    "message": { "type": "string", "description": "User-friendly constraint message" }
                  }
                }
              }
            }
          },
          "404": { "description": "Room not found" }
        }
      }
    }
  }
}
```

Similar endpoints for `/rest/storage/devices/{id}`, `/rest/storage/shelves/{id}`, `/rest/storage/racks/{id}`.

### Implementation Tasks

#### Backend Implementation

1. **Update Service Layer** (`StorageLocationServiceImpl.java`):
   - Add `validateDeleteConstraints(LocationEntity)` method
   - Add `canDeleteLocation(LocationEntity)` method
   - Add `getDeleteConstraintMessage(LocationEntity)` method
   - Update `update()` methods to ignore Code and Parent fields if provided

2. **Update REST Controllers**:
   - Ensure `PUT` endpoints validate editable fields only
   - Add `DELETE` endpoints with constraint validation
   - Return appropriate error messages (409 Conflict for constraints)

3. **Constraint Validation Logic**:
   ```java
   public boolean canDeleteRoom(StorageRoom room) {
       // Check for child devices
       if (deviceDAO.countByRoomId(room.getId()) > 0) {
           return false;
       }
       // Check for active samples in any child locations
       if (sampleStorageService.hasActiveSamplesInLocation(room.getId(), "room")) {
           return false;
       }
       return true;
   }
   ```

#### Frontend Implementation

1. **Create LocationActionsOverflowMenu Component**:
   - Similar structure to `SampleActionsOverflowMenu.jsx`
   - Two menu items: Edit, Delete
   - Props: `location` (entity object), `onEdit`, `onDelete` callbacks

2. **Create EditLocationModal Component**:
   - Generic component that adapts to entity type (Room/Device/Shelf/Rack)
   - Form fields based on entity type
   - Code and Parent fields disabled/read-only
   - Validation for code uniqueness
   - Calls `PUT /rest/storage/{entityType}/{id}` endpoint

3. **Create DeleteLocationModal Component**:
   - Checks constraints via API call before showing confirmation
   - Displays error message if constraints exist
   - Shows confirmation dialog if no constraints
   - Calls `DELETE /rest/storage/{entityType}/{id}` endpoint

4. **Update StorageDashboard.jsx**:
   - Replace placeholder action buttons with `LocationActionsOverflowMenu`
   - Add state management for Edit and Delete modals
   - Handle modal open/close and API calls

### Testing Checklist

- [ ] All backend integration tests pass
- [ ] All backend service unit tests pass
- [ ] All frontend unit tests pass
- [ ] All E2E tests pass
- [ ] Edit modal opens with correct fields for each entity type
- [ ] Code and Parent fields are read-only in Edit modal
- [ ] Edit saves changes correctly
- [ ] Delete validates constraints correctly
- [ ] Delete shows appropriate error messages
- [ ] Delete confirmation dialog appears when no constraints
- [ ] Delete successfully removes location when confirmed
- [ ] Table refreshes after Edit/Delete operations

### Dependencies

- Existing location REST endpoints (GET, POST)
- Existing location service layer
- Carbon Design System OverflowMenu and Modal components
- Existing table rendering in StorageDashboard

### Success Criteria

- All tests pass (integration, unit, E2E)
- Overflow menu appears on all location table rows
- Edit modal allows editing all editable fields
- Code and Parent fields are read-only
- Delete validates constraints and shows appropriate messages
- Delete confirmation dialog works correctly
- Table updates after Edit/Delete operations

---

## Phase 7: Expandable Row Functionality Implementation

**Date**: 2025-11-07  
**Status**: Planning  
**Spec Reference**: FR-059a through FR-059f  
**Research**: [research.md Section 8](./research.md#8-carbon-datatable-expandable-rows)

### Overview

Add expandable row functionality to location tables (Rooms, Devices, Shelves, Racks) in StorageDashboard component. Expanded rows display additional entity fields not visible in table columns, formatted as key-value pairs in read-only format. Only one row can be expanded at a time. Expansion triggered by clicking chevron icon in dedicated first column (Carbon DataTable standard pattern).

### Requirements Summary

- **FR-059a**: Location tables MUST support expandable rows using Carbon DataTable expandable row pattern
- **FR-059b**: Expandable rows MUST be triggered by clicking chevron/expand icon in dedicated column (first column)
- **FR-059c**: Expanded row content MUST display all entity fields not visible in table columns, formatted as key-value pairs in read-only format
- **FR-059d**: Only one row can be expanded at a time (expanding another automatically collapses the previous)
- **FR-059e**: Expanded row content MUST be read-only (Edit action remains in overflow menu)
- **FR-059f**: Expanded row MUST show entity-specific additional fields:
  - **Rooms**: Description, Created Date, Created By, Last Modified Date, Last Modified By
  - **Devices**: Temperature Setting, Capacity Limit, Description, Created Date, Created By, Last Modified Date, Last Modified By
  - **Shelves**: Capacity Limit, Description, Created Date, Created By, Last Modified Date, Last Modified By
  - **Racks**: Position Schema Hint, Description, Created Date, Created By, Last Modified Date, Last Modified By

### Technical Approach

**Frontend Changes**:

1. **Modify StorageDashboard.jsx**:
   - Add `expandableRows` prop to DataTable components for Rooms, Devices, Shelves, Racks tabs
   - Import `TableExpandHeader`, `TableExpandRow`, `TableExpandedRow` from `@carbon/react`
   - Add state management for expanded row ID (`useState` for `expandedRowId`)
   - Implement `handleRowExpand` function to manage single-row expansion
   - Create `renderExpandedContent` function for each location type (room, device, shelf, rack)
   - Update table structure to use `TableExpandHeader` in header row
   - Replace `TableRow` with `TableExpandRow` for data rows
   - Add `TableExpandedRow` after each `TableExpandRow` with expanded content

2. **Expanded Content Format**:
   - Use Carbon Grid/Column components for layout
   - Display fields as key-value pairs with labels from React Intl
   - Format dates using `intl.formatDate()`
   - Display "N/A" for missing optional fields
   - Read-only display (no input fields)

3. **State Management**:
   - Single `expandedRowId` state variable per tab (or shared across tabs)
   - Toggle logic: if same row clicked, collapse; if different row, expand new and collapse previous
   - Reset expanded state when switching tabs

**Backend Changes**: 
- Update `StorageLocationServiceImpl.getDevicesForAPI()` to include:
  - `totalCapacity` (calculated capacity when `capacityLimit` is null)
  - `capacityType` ("manual" if `capacityLimit` set, "calculated" if from children, null if cannot determine)
- Update `StorageLocationServiceImpl.getShelvesForAPI()` to include:
  - `totalCapacity` (calculated capacity when `capacityLimit` is null)
  - `capacityType` ("manual" if `capacityLimit` set, "calculated" if from children, null if cannot determine)
- Add `calculateDeviceCapacity()` and `calculateShelfCapacity()` methods per FR-062a, FR-062b

**API Changes**: 
- Device API response: Add `totalCapacity` (Integer, nullable) and `capacityType` (String: "manual" | "calculated" | null)
- Shelf API response: Add `totalCapacity` (Integer, nullable) and `capacityType` (String: "manual" | "calculated" | null)
- Rack API response: No changes (always uses calculated capacity from rows × columns)

### Test-Driven Development Plan

**Test Order** (TDD workflow):

1. **Unit Tests** (Jest + React Testing Library):
   - Test expanded state management (`handleRowExpand` function)
   - Test expanded content rendering for each location type
   - Test single-row expansion behavior (collapsing previous row)
   - Test missing field handling ("N/A" display)
   - Test date formatting in expanded content
   - Test tab switching resets expanded state

2. **E2E Tests** (Cypress):
   - Test expand row interaction (click chevron icon)
   - Test expanded content visibility and correctness
   - Test single-row expansion (expanding new row collapses previous)
   - Test expanded content is read-only (no edit capability)
   - Test expanded content for each location type (Rooms, Devices, Shelves, Racks)
   - Test keyboard navigation (Enter/Space to expand)
   - Test accessibility (ARIA attributes, screen reader support)

**Test Files**:

- Unit: `frontend/src/components/storage/StorageDashboard/StorageDashboard.test.jsx`
- E2E: `frontend/cypress/e2e/storageLocationExpandableRows.cy.js`

### Implementation Tasks

1. **Research & Design** ✅ (Complete - see research-expandable-rows.md)
2. **Unit Tests**: Write tests for expanded state management and content rendering
3. **Implementation**: Modify StorageDashboard.jsx to add expandable row functionality
4. **E2E Tests**: Write Cypress tests for expand/collapse interaction
5. **Accessibility Testing**: Verify ARIA attributes and keyboard navigation
6. **Integration Testing**: Test with real data from API

### Files to Modify

- `frontend/src/components/storage/StorageDashboard.jsx` - Add expandable row functionality, update occupancy display to handle "N/A" when capacity cannot be determined, add visual distinction for manual vs calculated capacities (per FR-062c)
- `frontend/src/components/storage/StorageDashboard/StorageDashboard.test.jsx` - Add unit tests for capacity calculation display, "N/A" handling, and capacity type badges
- `frontend/cypress/e2e/storageLocationExpandableRows.cy.js` - Add E2E tests (new file), add tests for capacity display (manual vs calculated, "N/A" tooltip)
- `frontend/src/languages/en.json` - Add message keys for expanded content labels, capacity type labels ("Manual Limit", "Calculated"), and "N/A" tooltip text
- `src/main/java/org/openelisglobal/storage/service/StorageLocationServiceImpl.java` - Add `calculateDeviceCapacity()` and `calculateShelfCapacity()` methods, update `getDevicesForAPI()` and `getShelvesForAPI()` to include `totalCapacity` and `capacityType`

### Dependencies

- **Carbon Design System v1.15**: `@carbon/react` with `TableExpandHeader`, `TableExpandRow`, `TableExpandedRow`
- **React Intl**: For internationalized field labels
- **Existing StorageDashboard**: Modify current table implementations

### Constitution Compliance

- ✅ **Carbon Design System First**: Uses Carbon DataTable expandable row pattern exclusively
- ✅ **Internationalization**: All field labels use React Intl message keys
- ✅ **Accessibility**: Carbon components provide ARIA attributes and keyboard navigation
- ✅ **Test Coverage**: Unit + E2E tests planned (>70% coverage goal)

### Success Criteria

- [ ] Expandable rows work for all location types (Rooms, Devices, Shelves, Racks)
- [ ] Only one row can be expanded at a time
- [ ] Expanded content displays all required fields as key-value pairs
- [ ] Expanded content is read-only (no edit capability)
- [ ] All unit tests pass
- [ ] All E2E tests pass
- [ ] Accessibility verified (ARIA attributes, keyboard navigation)
- [ ] Internationalization complete (all labels use React Intl)

## Phase 10: Barcode Workflow Implementation

**Note**: Research on existing OpenELIS barcode printing infrastructure completed (see `research.md` Section 9). Integration strategy documented. Some areas still need clarification during implementation (printer configuration, scanner hardware details).

### Objective

Implement comprehensive barcode workflow functionality per FR-023 through FR-027f:
- Unified input field supporting both barcode scanning and type-ahead search
- 5-step validation process (format, existence, hierarchy, activity, conflicts)
- Debouncing with 500ms cooldown period
- Visual feedback (ready state, success green checkmark, error red X)
- Dual barcode auto-detection (sample vs location barcodes)
- "Last-modified wins" logic when both dropdowns and input field are used
- Label management (short code, printing, print history)
- Error recovery with pre-filling valid components

### TDD Approach

Following strict test-first development with small, manageable iterations:

1. **Write failing tests** (Red phase)
2. **Implement minimal code to pass** (Green phase)
3. **Refactor while keeping tests green** (Refactor phase)

### Test-Driven Development Plan

#### Iteration 8.1: Backend Barcode Parsing and Validation

**Objective**: Implement server-side barcode parsing and 5-step validation process.

**Test Order** (TDD workflow):

1. **Backend Unit Tests** (Write First):
   - Test: `BarcodeParsingServiceTest.java`
   - Validates: Parse 2-level barcode (Room-Device format)
   - Validates: Parse 3-level barcode (Room-Device-Shelf format)
   - Validates: Parse 4-level barcode (Room-Device-Shelf-Rack format)
   - Validates: Parse 5-level barcode (Room-Device-Shelf-Rack-Position format)
   - Validates: Fixed hyphen delimiter parsing
   - Validates: Invalid delimiter rejection
   - Validates: Empty/null barcode handling

2. **Backend Service Tests** (Write Second):
   - Test: `BarcodeValidationServiceTest.java`
   - Validates: Step 1 - Format validation (parseable structure)
   - Validates: Step 2 - Location existence check (all codes exist in database)
   - Validates: Step 3 - Hierarchy validation (Shelf is child of Device, etc.)
   - Validates: Step 4 - Activity check (location is active, not decommissioned)
   - Validates: Step 5 - Conflict check (position not occupied if applicable)
   - Validates: Error messages for each failure type
   - Validates: Partial validation (some components valid, some invalid)
   - Validates: Pre-fill valid components in response

3. **Backend Integration Tests** (Write Third):
   - Test: `BarcodeValidationRestControllerTest.java`
   - Validates: `POST /rest/storage/barcode/validate` endpoint
   - Validates: Request/response format matches API contract
   - Validates: Database persistence after validation
   - Validates: Error responses (400, 404) for validation failures

**Implementation Tasks** (After Tests Pass):

1. Create `BarcodeParsingService.java` - Parse hierarchical barcode format
2. Create `BarcodeValidationService.java` - Implement 5-step validation
3. Create `BarcodeValidationRestController.java` - REST endpoint for validation
4. Update API contract in `contracts/storage-api.json`

#### Iteration 8.2: Frontend Unified Input Field

**Objective**: Create unified input field component that accepts both barcode scan and type-ahead search.

**Test Order** (TDD workflow):

1. **Frontend Unit Tests** (Write First):
   - Test: `UnifiedBarcodeInput.test.jsx`
   - Validates: Input field accepts keyboard input (manual typing)
   - Validates: Input field accepts rapid character input (barcode scan simulation)
   - Validates: Format-based detection (hyphens = barcode, no hyphens = type-ahead)
   - Validates: Enter key triggers validation
   - Validates: Field blur triggers validation
   - Validates: Visual feedback states (ready, success, error)
   - Validates: Auto-clear after successful population

2. **Frontend Integration Tests** (Write Second):
   - Test: `UnifiedBarcodeInput.integration.test.jsx`
   - Validates: API call to validation endpoint on Enter/blur
   - Validates: Success response populates location fields
   - Validates: Error response displays error message
   - Validates: Partial validation pre-fills valid components

**Implementation Tasks** (After Tests Pass):

1. Create `UnifiedBarcodeInput.jsx` - Unified input field component
2. Create `BarcodeVisualFeedback.jsx` - Visual feedback component
3. Integrate into `LocationSelectorModal.jsx`
4. Add React Intl message keys for barcode-related strings

#### Iteration 8.3: Debouncing Logic

**Objective**: Implement 500ms debouncing to prevent accidental double-scans.

**Test Order** (TDD workflow):

1. **Frontend Unit Tests** (Write First):
   - Test: `BarcodeDebounceHook.test.js`
   - Validates: Duplicate barcode within 500ms is ignored silently
   - Validates: Different barcode within 500ms shows warning and is ignored
   - Validates: Barcode after 500ms cooldown is processed normally
   - Validates: Cooldown timer resets after each scan
   - Validates: Multiple rapid scans handled correctly

**Implementation Tasks** (After Tests Pass):

1. Create `BarcodeDebounceHook.js` - Custom React hook for debouncing
2. Integrate into `UnifiedBarcodeInput.jsx`
3. Add warning message for different barcode within cooldown

#### Iteration 8.4: "Last-Modified Wins" Logic

**Objective**: Implement seamless switching between dropdown and input field modes.

**Test Order** (TDD workflow):

1. **Frontend Unit Tests** (Write First):
   - Test: `LocationSelectorModal.test.jsx` (update existing)
   - Validates: Dropdown selection then input field scan overwrites dropdowns
   - Validates: Input field scan then dropdown selection overwrites input
   - Validates: Visual feedback shows which method is active (highlight border/icon)
   - Validates: No error when switching between methods
   - Validates: Both methods visible simultaneously

**Implementation Tasks** (After Tests Pass):

1. Update `LocationSelectorModal.jsx` to track last-modified method
2. Add visual feedback (highlight border/icon) for active method
3. Implement overwrite logic based on last modification timestamp

#### Iteration 8.5: Label Management (Short Code and Printing)

**Objective**: Implement label management modal with short code input and print functionality.

**Test Order** (TDD workflow):

1. **Backend Unit Tests** (Write First):
   - Test: `ShortCodeValidationServiceTest.java`
   - Validates: Short code format (max 10 chars, alphanumeric, hyphen/underscore allowed)
   - Validates: Auto-uppercase conversion
   - Validates: Must start with letter or number (not hyphen/underscore)
   - Validates: Uniqueness within context (device/shelf/rack)
   - Validates: Warning when changing short code (affects printed labels)

2. **Backend Integration Tests** (Write Second):
   - Test: `LabelManagementRestControllerTest.java`
   - Validates: `PUT /rest/storage/{type}/{id}/short-code` endpoint
   - Validates: `POST /rest/storage/{type}/{id}/print-label` endpoint
   - Validates: Print history tracking (who, when, what)
   - Validates: PDF generation with system admin settings (label size, format, layout)

3. **Frontend Unit Tests** (Write Third):
   - Test: `LabelManagementModal.test.jsx`
   - Validates: Short code input with validation
   - Validates: Auto-uppercase on input
   - Validates: Warning dialog before short code change
   - Validates: Print label button opens PDF in new tab
   - Validates: Print history display (last printed, view history link)

**Implementation Tasks** (After Tests Pass):

1. Create `LabelManagementModal.jsx` - Label management modal
2. Create `ShortCodeInput.jsx` - Short code input with validation
3. Create `PrintLabelButton.jsx` - Print label button with PDF preview
4. Create `PrintHistoryDisplay.jsx` - Print history list component
5. Update `LocationActionsOverflowMenu.jsx` to include "Label Management" item
6. Create backend `ShortCodeValidationService.java`
7. Create backend `LabelManagementService.java` - Integrate with existing BarcodeLabelMaker (see research.md Section 9)
8. Create backend `StorageLocationLabel.java` - Extend Label class (see research.md Section 9)
9. Create backend `LabelManagementRestController.java` - REST endpoint for printing
10. Add database table for print history (Liquibase changeset)
11. Add `STORAGE_LOCATION_BARCODE_HEIGHT` and `STORAGE_LOCATION_BARCODE_WIDTH` to ConfigurationProperties (see research.md Section 9)

#### Iteration 8.6: E2E Tests

**Objective**: Validate complete barcode workflows end-to-end.

**Test Order** (TDD workflow):

1. **Cypress E2E Tests** (Write Last):
   - Test: `barcodeWorkflow.cy.js`
   - Validates: Scan 4-level barcode populates location fields correctly
   - Validates: Scan 2-level barcode (minimum) populates Room and Device only
   - Validates: Scan invalid barcode shows error message with parsed components
   - Validates: Debouncing prevents duplicate scans within 500ms
   - Validates: "Last-modified wins" when switching between dropdown and scan
   - Validates: Label management modal opens from overflow menu
   - Validates: Short code change shows warning dialog
   - Validates: Print label generates PDF and opens in new tab
   - Validates: Print history displays after printing

**Implementation Tasks** (After Tests Pass):

1. Create `frontend/cypress/e2e/barcodeWorkflow.cy.js`
2. Follow Constitution V.5 best practices:
   - Run tests individually during development
   - Browser console logging enabled and reviewed
   - Video recording disabled by default
   - Post-run review of console logs and screenshots

### Files to Create/Modify

**Backend**:
- `src/main/java/org/openelisglobal/storage/service/BarcodeParsingService.java` (new)
- `src/main/java/org/openelisglobal/storage/service/BarcodeValidationService.java` (new)
- `src/main/java/org/openelisglobal/storage/service/ShortCodeValidationService.java` (new)
- `src/main/java/org/openelisglobal/storage/service/LabelManagementService.java` (new)
- `src/main/java/org/openelisglobal/storage/controller/BarcodeValidationRestController.java` (new)
- `src/main/java/org/openelisglobal/storage/controller/LabelManagementRestController.java` (new)
- `src/test/java/org/openelisglobal/storage/service/BarcodeParsingServiceTest.java` (new)
- `src/test/java/org/openelisglobal/storage/service/BarcodeValidationServiceTest.java` (new)
- `src/test/java/org/openelisglobal/storage/service/ShortCodeValidationServiceTest.java` (new)
- `src/test/java/org/openelisglobal/storage/controller/BarcodeValidationRestControllerTest.java` (new)
- `src/test/java/org/openelisglobal/storage/controller/LabelManagementRestControllerTest.java` (new)
- `src/main/resources/liquibase/storage/004-create-print-history-table.xml` (new)

**Frontend**:
- `frontend/src/components/storage/StorageLocationSelector/UnifiedBarcodeInput.jsx` (new)
- `frontend/src/components/storage/StorageLocationSelector/BarcodeValidationService.js` (new)
- `frontend/src/components/storage/StorageLocationSelector/BarcodeDebounceHook.js` (new)
- `frontend/src/components/storage/StorageLocationSelector/BarcodeVisualFeedback.jsx` (new)
- `frontend/src/components/storage/LocationManagement/LabelManagementModal.jsx` (new)
- `frontend/src/components/storage/LocationManagement/ShortCodeInput.jsx` (new)
- `frontend/src/components/storage/LocationManagement/PrintLabelButton.jsx` (new)
- `frontend/src/components/storage/LocationManagement/PrintHistoryDisplay.jsx` (new)
- `frontend/src/components/storage/StorageLocationSelector/UnifiedBarcodeInput.test.jsx` (new)
- `frontend/src/components/storage/LocationManagement/LabelManagementModal.test.jsx` (new)
- `frontend/src/components/storage/StorageLocationSelector/LocationSelectorModal.jsx` (modify)
- `frontend/src/components/storage/LocationManagement/LocationActionsOverflowMenu.jsx` (modify)
- `frontend/cypress/e2e/barcodeWorkflow.cy.js` (new)

**API Contracts**:
- `specs/001-sample-storage/contracts/storage-api.json` (update with barcode endpoints)

**Internationalization**:
- `frontend/src/languages/en.json` (add barcode-related message keys)
- `frontend/src/languages/fr.json` (add barcode-related message keys)
- `frontend/src/languages/sw.json` (add barcode-related message keys)

### Dependencies

- **Carbon Design System v1.15**: `@carbon/react` TextInput, Modal, Button components
- **React Intl**: For internationalized error messages and labels
- **Existing OpenELIS utilities**: `getFromOpenElisServer`, `postToOpenElisServer`
- **PDF Generation**: Reuse existing iTextPDF via BarcodeLabelMaker (see research.md Section 9)

### Constitution Compliance

- ✅ **Carbon Design System First**: Uses Carbon TextInput, Modal, Button exclusively
- ✅ **Internationalization**: All barcode-related strings use React Intl message keys
- ✅ **Layered Architecture**: Backend follows 5-layer pattern (Service → Controller)
- ✅ **Test Coverage**: Unit + integration + E2E tests planned (>70% coverage goal)
- ✅ **Schema Management**: Print history table via Liquibase changeset
- ✅ **Security & Compliance**: Input validation, audit trail for print history

### Success Criteria

- [ ] Unified input field accepts both barcode scan and type-ahead search
- [ ] 5-step validation process works correctly for all barcode formats (2-5 levels)
- [ ] Debouncing prevents accidental double-scans (500ms cooldown)
- [ ] Visual feedback displays correctly (ready, success, error states)
- [ ] "Last-modified wins" logic works when switching between methods
- [ ] Label management modal accessible from overflow menu
- [ ] Short code validation and uniqueness checking works
- [ ] Print label generates PDF with system admin settings
- [ ] Print history tracks and displays correctly
- [ ] All unit tests pass
- [ ] All integration tests pass
- [ ] All E2E tests pass
- [ ] Internationalization complete (en, fr, sw)

## Implementation Enhancements

### Helper Methods (Service Layer)

**1. Hierarchical Path Builder**

```java
// In StorageLocationService
public String buildHierarchicalPath(StoragePosition position) {
    StringBuilder path = new StringBuilder();

    // Position always has parent_device (required), which has parent_room
    StorageDevice device = position.getParentDevice();
    StorageRoom room = device.getParentRoom();

    path.append(room.getName()).append(" > ").append(device.getName());

    // Add shelf if present (3+ level position)
    if (position.getParentShelf() != null) {
        StorageShelf shelf = position.getParentShelf();
        path.append(" > ").append(shelf.getLabel());

        // Add rack if present (4+ level position)
        if (position.getParentRack() != null) {
            StorageRack rack = position.getParentRack();
            path.append(" > ").append(rack.getLabel());

            // Add coordinate if present (5-level position)
            if (position.getCoordinate() != null && !position.getCoordinate().isEmpty()) {
                path.append(" > Position ").append(position.getCoordinate());
            }
        }
    }

    return path.toString();
}
```

**2. Capacity Calculator with Warnings**

**Capacity Determination Logic (per FR-062a, FR-062b)**:

- **Racks**: Capacity is ALWAYS calculated as `rows × columns` (per FR-017). If rows=0 OR columns=0, capacity=0 (no grid, rack-level assignment only)
- **Devices and Shelves**: Two-tier system:
  - If `capacity_limit` is set (static/manual limit), use that value as total capacity
  - If `capacity_limit` is NULL, calculate capacity from child locations:
    - If ALL child locations have defined capacities (either static `capacity_limit` set OR calculated capacity from their own children), sum those capacities
    - If ANY child location lacks a defined capacity, parent capacity cannot be determined (return null, UI shows "N/A")

```java
// In StorageLocationService
/**
 * Calculate total capacity for a device using two-tier logic (per FR-062a, FR-062b).
 * Returns null if capacity cannot be determined.
 */
public Integer calculateDeviceCapacity(StorageDevice device) {
    // Tier 1: Check if static capacity_limit is set
    if (device.getCapacityLimit() != null && device.getCapacityLimit() > 0) {
        return device.getCapacityLimit();
    }
    
    // Tier 2: Calculate from child shelves
    List<StorageShelf> shelves = storageShelfDAO.findByParentDeviceId(device.getId());
    if (shelves == null || shelves.isEmpty()) {
        return null; // No children, cannot determine capacity
    }
    
    int totalCapacity = 0;
    for (StorageShelf shelf : shelves) {
        Integer shelfCapacity = calculateShelfCapacity(shelf);
        if (shelfCapacity == null) {
            // Any child lacks defined capacity - cannot determine parent capacity
            return null;
        }
        totalCapacity += shelfCapacity;
    }
    
    return totalCapacity;
}

/**
 * Calculate total capacity for a shelf using two-tier logic (per FR-062a, FR-062b).
 * Returns null if capacity cannot be determined.
 */
public Integer calculateShelfCapacity(StorageShelf shelf) {
    // Tier 1: Check if static capacity_limit is set
    if (shelf.getCapacityLimit() != null && shelf.getCapacityLimit() > 0) {
        return shelf.getCapacityLimit();
    }
    
    // Tier 2: Calculate from child racks (racks always have defined capacity)
    List<StorageRack> racks = storageRackDAO.findByParentShelfId(shelf.getId());
    if (racks == null || racks.isEmpty()) {
        return null; // No children, cannot determine capacity
    }
    
    int totalCapacity = 0;
    for (StorageRack rack : racks) {
        // Racks always have defined capacity (rows × columns)
        int rackCapacity = rack.getRows() * rack.getColumns();
        totalCapacity += rackCapacity;
    }
    
    return totalCapacity;
}

/**
 * Calculate rack capacity (always rows × columns, per FR-017).
 */
public int calculateRackCapacity(StorageRack rack) {
    return rack.getRows() * rack.getColumns();
}

// In SampleStorageService
/**
 * Calculate capacity warning for a rack (per FR-036).
 * Racks always have defined capacity (rows × columns).
 */
public CapacityWarning calculateCapacity(StorageRack rack) {
    int totalCapacity = rack.getRows() * rack.getColumns();
    if (totalCapacity == 0) return null; // No grid

    int occupied = storageLocationService.countOccupied(rack.getId());
    int percentage = (occupied * 100) / totalCapacity;

    String warningMessage = null;
    if (percentage >= 100) {
        warningMessage = String.format("Rack %s is %d%% full. Consider using alternative storage.",
            rack.getLabel(), percentage);
    } else if (percentage >= 90) {
        warningMessage = String.format("Rack %s is %d%% full. Consider using alternative storage.",
            rack.getLabel(), percentage);
    } else if (percentage >= 80) {
        warningMessage = String.format("Rack %s is %d%% full. Consider using alternative storage.",
            rack.getLabel(), percentage);
    }

    return new CapacityWarning(occupied, totalCapacity, percentage, warningMessage);
}

/**
 * Calculate capacity warning for a device or shelf (per FR-036).
 * Returns null if capacity cannot be determined (per FR-062b).
 */
public CapacityWarning calculateCapacity(StorageDevice device) {
    Integer totalCapacity = storageLocationService.calculateDeviceCapacity(device);
    if (totalCapacity == null) {
        return null; // Capacity cannot be determined - UI will show "N/A"
    }
    
    int occupied = storageLocationService.countOccupiedInDevice(device.getId());
    int percentage = (occupied * 100) / totalCapacity;
    
    // Only show warnings if capacity is defined (per FR-036)
    String warningMessage = null;
    if (percentage >= 100) {
        warningMessage = String.format("Device %s is %d%% full. Consider using alternative storage.",
            device.getName(), percentage);
    } else if (percentage >= 90) {
        warningMessage = String.format("Device %s is %d%% full. Consider using alternative storage.",
            device.getName(), percentage);
    } else if (percentage >= 80) {
        warningMessage = String.format("Device %s is %d%% full. Consider using alternative storage.",
            device.getName(), percentage);
    }
    
    return new CapacityWarning(occupied, totalCapacity, percentage, warningMessage);
}

public CapacityWarning calculateCapacity(StorageShelf shelf) {
    Integer totalCapacity = storageLocationService.calculateShelfCapacity(shelf);
    if (totalCapacity == null) {
        return null; // Capacity cannot be determined - UI will show "N/A"
    }
    
    int occupied = storageLocationService.countOccupiedInShelf(shelf.getId());
    int percentage = (occupied * 100) / totalCapacity;
    
    String warningMessage = null;
    if (percentage >= 100) {
        warningMessage = String.format("Shelf %s is %d%% full. Consider using alternative storage.",
            shelf.getLabel(), percentage);
    } else if (percentage >= 90) {
        warningMessage = String.format("Shelf %s is %d%% full. Consider using alternative storage.",
            shelf.getLabel(), percentage);
    } else if (percentage >= 80) {
        warningMessage = String.format("Shelf %s is %d%% full. Consider using alternative storage.",
            shelf.getLabel(), percentage);
    }
    
    return new CapacityWarning(occupied, totalCapacity, percentage, warningMessage);
}
```

**3. Optimistic Locking Handler**

```java
// In SampleStorageService
@Transactional
public Map<String, Object> assignSampleWithLocation(String sampleId, String locationId,
        String locationType, String positionCoordinate, String notes) {
    // Validate location_id and location_type are provided
    if (locationId == null || locationType == null) {
        throw new ValidationException("location_id and location_type are required");
    }

    // Validate location_type is one of: 'device', 'shelf', 'rack'
    if (!Arrays.asList("device", "shelf", "rack").contains(locationType)) {
        throw new ValidationException("location_type must be one of: 'device', 'shelf', 'rack'");
    }

    // Load location entity based on locationType
    Object locationEntity = switch (locationType) {
        case "device" -> storageLocationService.get(Integer.parseInt(locationId), StorageDevice.class);
        case "shelf" -> storageLocationService.get(Integer.parseInt(locationId), StorageShelf.class);
        case "rack" -> storageLocationService.get(Integer.parseInt(locationId), StorageRack.class);
        default -> throw new ValidationException("Invalid location_type: " + locationType);
    };

    // Validate location has minimum 2 levels (room + device per FR-033a)
    validateLocationActiveForEntity(locationEntity, locationType);

    // Create assignment with location_id + location_type
    SampleStorageAssignment assignment = new SampleStorageAssignment();
    assignment.setSample(sampleDAO.get(sampleId).orElseThrow());
    assignment.setLocationId(Integer.parseInt(locationId));
    assignment.setLocationType(locationType);
    assignment.setPositionCoordinate(positionCoordinate);
    assignment.setAssignedByUserId(getCurrentUserId());
    assignment.setNotes(notes);

    assignmentDAO.insert(assignment);

    // Build hierarchical path
    String hierarchicalPath = buildHierarchicalPathForEntity(locationEntity, locationType, positionCoordinate);

    // Create audit log entry
    SampleStorageMovement movement = new SampleStorageMovement();
    movement.setSample(assignment.getSample());
    movement.setNewLocationId(Integer.parseInt(locationId));
    movement.setNewLocationType(locationType);
    movement.setMovedByUserId(getCurrentUserId());
    movement.setReason("Initial assignment");
    movementDAO.insert(movement);

    Map<String, Object> result = new HashMap<>();
    result.put("assignmentId", assignment.getId().toString());
    result.put("hierarchicalPath", hierarchicalPath);
    result.put("assignedDate", assignment.getAssignedDate());

    return result;
}
```

### Sequence Diagram: Sample Assignment Workflow

```
User (Browser)
    │
    │ 1. Select location via widget (cascading dropdowns)
    ├──────────> StorageLocationSelector.jsx
    │                 │
    │                 │ 2. GET /rest/storage/rooms
    │                 ├──────────> StorageLocationRestController
    │                 │                 │
    │                 │                 │ 3. getRooms()
    │                 │                 ├──────────> StorageLocationService
    │                 │                 │                 │
    │                 │                 │                 │ 4. Query DB
    │                 │                 │                 ├──────────> StorageRoomDAO
    │                 │                 │                 │
    │                 │                 │                 │ 5. Return rooms
    │                 │                 │                 <──────────┤
    │                 │                 │
    │                 │                 │ 6. Return rooms JSON
    │                 │                 <──────────┤
    │                 │
    │                 │ 7. Populate room dropdown
    │                 <──────────┤
    │
    │ ... (repeat for device, shelf, rack, position selection)
    │
    │ 8. Click "Save" with position selected
    ├──────────> SamplePatientEntry.jsx
    │                 │
    │                 │ 9. POST /rest/storage/samples/assign
    │                 │    { sampleId, locationId, locationType, positionCoordinate?, notes }
    │                 ├──────────> SampleStorageRestController
    │                 │                 │
    │                 │                 │ 10. assignSampleWithLocation()
    │                 │                 ├──────────> SampleStorageService
    │                 │                 │                 │
    │                 │                 │                 │ 11. Validate location_id and location_type provided
    │                 │                 │                 │ 12. Validate location_type is 'device', 'shelf', or 'rack'
    │                 │                 │                 │ 13. Load location entity based on location_type
    │                 │                 │                 │ 14. Validate location active (check entire hierarchy)
    │                 │                 │                 │ 15. Calculate capacity warning (if applicable)
    │                 │                 │                 │
    │                 │                 │                 │ 16. Create assignment with location_id + location_type
    │                 │                 │                 ├──────────> SampleStorageAssignmentDAO
    │                 │                 │                 │                 │
    │                 │                 │                 │                 │ 15. UPDATE storage_position
    │                 │                 │                 │                 │    (optimistic lock check)
    │                 │                 │                 │                 <──────────┤
    │                 │                 │                 │
    │                 │                 │                 │ 16. Create assignment record
    │                 │                 │                 ├──────────> SampleStorageAssignmentDAO
    │                 │                 │                 │                 │
    │                 │                 │                 │                 │ 17. INSERT assignment
    │                 │                 │                 │                 <──────────┤
    │                 │                 │                 │
    │                 │                 │                 │ 18. Create movement audit
    │                 │                 │                 ├──────────> SampleStorageMovementDAO
    │                 │                 │                 │                 │
    │                 │                 │                 │                 │ 19. INSERT movement
    │                 │                 │                 │                 <──────────┤
    │                 │                 │                 │
    │                 │                 │                 │ 20. Build hierarchical path
    │                 │                 │                 ├──────────> buildHierarchicalPath()
    │                 │                 │                 │
    │                 │                 │                 │ 21. Return assignment
    │                 │                 │                 <──────────┤
    │                 │                 │
    │                 │                 │ 22. Return assignment JSON
    │                 │                 <──────────┤
    │                 │
    │                 │ 23. Show success notification
    │                 <──────────┤
    │
    │ 24. Display location in UI
    <──────────┤

Automatic (via JPA hooks):
StoragePosition entity
    │
    │ 25. @PostUpdate hook triggered (occupied changed)
    ├──────────> StorageLocationFhirTransform.transformToFhirLocation(position)
    │                 │
    │                 │ 26. Build FHIR Location resource with position-occupancy extension
    │                 │
    │                 │ 27. POST/PUT to FHIR server
    │                 └──────────> FhirPersistanceService.save(location)
    │                                   │
    │                                   │ 28. Sync to HAPI FHIR Server
    │                                   └──────────> https://fhir.openelis.org:8443/fhir/

Specimen entity
    │
    │ 29. @PostUpdate hook triggered (assignment created)
    └──────────> Update Specimen.container.extension[storage-position-location] reference
```

### Sample Entity Integration

**Existing Sample Entity**: `org.openelisglobal.sample.valueholder.Sample`

- **No modifications required** - Sample entity remains unchanged
- **Integration via junction table**: SampleStorageAssignment links Sample to
  StoragePosition
- **Foreign key**: `SampleStorageAssignment.sample_id` → `Sample.id`
- **Query pattern**:
  `JOIN sample_storage_assignment ON sample.id = sample_storage_assignment.sample_id`

**Benefits**:

- ✅ No impact on existing Sample entity code
- ✅ Backward compatible (samples without location continue to work)
- ✅ Easy to query samples by location (JOIN on assignment table)
- ✅ Easy to query location for a sample (JOIN on assignment table)

### Task 1.4: Generate Quickstart (Test-First Development Guide)
