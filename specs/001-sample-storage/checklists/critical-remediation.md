# Critical Remediation Checklist

**Purpose**: MANDATORY fixes required BEFORE `/speckit.implement`
**Created**: 2025-11-09
**Based on**: /speckit.analyze analysis report
**Priority**: CRITICAL - These issues violate constitution or block implementation

---

## Constitution Violations

### CHK001 - Fix JUnit Version Specification in plan.md

- **Issue**: Plan.md line 72-75 incorrectly specifies "JUnit 5 + Mockito" but constitution MANDATES JUnit 4 (4.13.1) as non-negotiable standard
- **Evidence**:
  - Code inspection shows actual implementation uses JUnit 4:
    - `import org.junit.Test` (JUnit 4, NOT `org.junit.jupiter.api.Test`)
    - `import org.junit.Assert.*` (JUnit 4)
    - `import org.junit.runner.RunWith` (JUnit 4)
    - `import org.mockito.junit.MockitoJUnitRunner` (JUnit 4 compatible)
  - Constitution (.specify/memory/constitution.md) Section V.1 explicitly mandates JUnit 4
  - All existing test files in src/test/java/org/openelisglobal/storage/ use JUnit 4
- **File**: specs/001-sample-storage/plan.md
- **Location**: Line 72 (Testing framework specification)
- **Action**: Replace `JUnit 5 + Mockito` with `JUnit 4 (4.13.1) + Mockito 2.21.0`
- **Exact Change**:
  ```markdown
  # BEFORE (line 72-75):
  **Testing**:

  - Backend: JUnit 5 + Mockito (unit/integration)
  - Frontend: Jest + React Testing Library (unit), Cypress 12.17.3 (E2E - existing OpenELIS framework)

  # AFTER:
  **Testing**:

  - Backend: JUnit 4 (4.13.1) + Mockito 2.21.0 (unit/integration)
  - Frontend: Jest + React Testing Library (unit), Cypress 12.17.3 (E2E - existing OpenELIS framework)
  ```
- **Verification**: Search plan.md for any other JUnit 5 references and replace with JUnit 4
- **Status**: ✅ FIXED

---

## Test Strategy Issues

### CHK002 - Refine E2E Test Scope to Happy Path Only (Constitution V.5 Compliance)

- **Issue**: Spec E2E Test Scenarios section (spec.md:L782-831) lists 15+ test scenarios including edge cases, contradicting Constitution V.5 mandate of "Maximum 5-10 test cases per execution during development"
- **Evidence**:
  - Current implementation has 105 E2E tests across 13 files:
    - storageMovement.cy.js: 21 tests (TOO MANY)
    - storageSearch.cy.js: 18 tests (TOO MANY)
    - storageLocationExpandableRows.cy.js: 16 tests (TOO MANY)
    - storageFilters.cy.js: 13 tests (TOO MANY)
    - Plus 9 more files with 6-5 tests each
  - Constitution V.5 Section "Test Execution Workflow" states:
    - "Individual Execution: E2E tests MUST be executed individually in small, manageable chunks during development"
    - "Maximum 5-10 test cases per execution during development"
    - "Full test suite runs are for CI/CD only"
  - Spec contradicts itself: Lists many edge case tests (L796-831) but notes "Edge cases... are covered by unit/integration tests, not E2E tests" (L805-806)
- **Files**:
  - specs/001-sample-storage/spec.md (lines 782-831)
  - specs/001-sample-storage/plan.md (lines 790-793 if TDD approach section exists)
- **Actions Required**:
  1. **Update spec.md E2E Test Scenarios section** (L782-831):
     - Rename section to "E2E Test Requirements" (clarify these are test objectives, not literal test files)
     - Limit to 5-10 happy path tests per user story:
       - **US P1 (Assignment)**: 3 tests (one per input mode: cascading dropdown, type-ahead, barcode)
       - **US P2A (Search)**: 2 tests (search by sample ID, filter by location)
       - **US P2B (Movement)**: 2-3 tests (single move, bulk move)
     - Add note: "Implemented as individual Cypress E2E test files per Constitution V.5. Edge cases, validation errors, and concurrent access scenarios are covered by unit/integration tests."
  2. **Remove edge case tests from E2E scope**:
     - Move to unit/integration test section: "Position occupied" errors, "Inactive location" errors, "Disposed sample movement", "Concurrent access conflicts"
     - Keep only happy path user workflows in E2E
  3. **Add execution guidance**:
     - Note: "During development, run tests individually: `npm run cy:run -- --spec 'cypress/e2e/{feature}.cy.js'`"
     - Note: "Full suite execution only for CI/CD or pre-merge validation"
- **Example Refactored E2E Section**:
  ```markdown
  ## E2E Test Requirements

  **Purpose**: E2E tests validate complete user workflows end-to-end. Tests focus on happy path user journeys, NOT edge cases or validation errors (those are unit/integration tests).

  **Execution**: Per Constitution V.5, run tests individually during development (max 5-10 per execution). Full suite only in CI/CD.

  ### User Story P1 - Basic Storage Assignment (3 tests)

  - **E2E Test**: "Should assign sample via cascading dropdowns" (happy path)
  - **E2E Test**: "Should assign sample via type-ahead autocomplete" (happy path)
  - **E2E Test**: "Should assign sample via barcode scan" (happy path)

  **Edge Cases** (unit/integration tests, NOT E2E):
  - Position occupied errors → unit test in SampleStorageServiceTest
  - Inactive location errors → integration test in StorageLocationRestControllerTest
  - Validation errors → unit tests for each validation rule

  ### User Story P2A - Sample Search and Retrieval (2 tests)

  - **E2E Test**: "Should search samples by accession number" (happy path)
  - **E2E Test**: "Should filter samples by storage location" (happy path)

  **Edge Cases** (unit/integration tests, NOT E2E):
  - Search performance with 100k+ samples → integration test with database seeding
  - Empty search results → unit test

  ### User Story P2B - Sample Movement (2-3 tests)

  - **E2E Test**: "Should move single sample between locations" (happy path)
  - **E2E Test**: "Should move multiple samples with auto-assigned positions" (happy path)

  **Edge Cases** (unit/integration tests, NOT E2E):
  - Concurrent access conflicts → integration test with transaction isolation
  - Disposed sample movement → unit test in SampleStorageServiceTest
  - Insufficient capacity for bulk move → unit test

  **Execution Command** (development):
  ```bash
  # Run individual test file
  npm run cy:run -- --spec "cypress/e2e/storageAssignment.cy.js"

  # Full suite (CI/CD only)
  npm run cy:run
  ```
  ```
- **Verification**:
  - Count E2E tests in spec: Should be 7-10 total (not 15+)
  - Verify edge cases moved to unit/integration test sections
  - Confirm Constitution V.5 reference added
- **Status**: ✅ FIXED

---

## Scope Ambiguity

### CHK003 - Clarify POC Scope for US4 Dashboard Features

- **Issue**: Plan.md states "basic Storage Dashboard (P4 - metrics cards, tabs, data tables)" is IN POC scope (line 14), but spec.md "POC Scope" section (L271-310) lists US4 Dashboard as "Deferred to Post-POC". Ambiguous which P4 features are included vs excluded.
- **Evidence**:
  - Spec.md L286-288: "⏸️ **User Story 4 (P4)**: Storage Dashboard and Capacity Monitoring - Full dashboard with metrics cards, tabs, occupancy visualization, drill-down navigation"
  - Spec.md L290: "**Rationale**: POC focuses on demonstrating the core value proposition"
  - Plan.md L14: "POC scope includes core tracking workflows: assignment (P1), search/retrieval (P2A), movement (P2B), and basic Storage Dashboard (P4 - metrics cards, tabs, data tables)"
  - Inconsistency creates uncertainty: Are metrics cards in scope? Which tabs? Is drill-down navigation excluded?
- **Files**:
  - specs/001-sample-storage/spec.md (lines 271-310, POC Scope section)
  - specs/001-sample-storage/plan.md (line 14, Summary section)
- **Actions Required**:
  1. **Add explicit US4 POC Scope Matrix to spec.md** (after line 310):
     ```markdown
     ### User Story 4 (P4) POC Scope Breakdown

     **Included in POC**:
     - ✅ Metrics cards (Total Samples, Active, Disposed counts)
     - ✅ Storage Locations metric card (breakdown by type with color-coding)
     - ✅ 5 tabs (Rooms, Devices, Shelves, Racks, Samples)
     - ✅ Basic data tables (columns per tab as specified)
     - ✅ Basic filters per tab
     - ✅ Expandable rows (per Constitution V.7 amendment)

     **Deferred to Post-POC**:
     - ⏸️ Drill-down navigation (clicking location name to filter child levels)
     - ⏸️ CSV export functionality
     - ⏸️ Advanced occupancy color-coding (green/yellow/red)
     - ⏸️ Visual grid view for racks/positions

     **Rationale**: POC includes basic dashboard to validate that location data is captured correctly and can be displayed for management review. Advanced features (drill-down, export, visualization) deferred to ensure POC focuses on core tracking workflows.
     ```
  2. **Update plan.md Summary** (line 14) to reference spec POC Scope Matrix:
     ```markdown
     POC scope includes core tracking workflows: assignment (P1), search/retrieval (P2A), movement (P2B), and basic Storage Dashboard (P4 - see spec.md POC Scope Matrix for included/deferred features).
     ```
- **Verification**:
  - Spec.md and plan.md align on P4 scope
  - No ambiguity about which dashboard features are in POC
  - Implementers know exactly what to build
- **Status**: ✅ FIXED

---

## Summary

**Total Critical Items**: 3
**Completion Status**: 3/3 (100%) ✅

**Completed Actions**:
1. ✅ Fixed CHK001 (JUnit version) - Constitution compliance restored
2. ✅ Refined CHK002 (E2E test scope) - Constitution V.5 compliance achieved (7 tests, down from 13)
3. ✅ Clarified CHK003 (P4 scope) - POC scope matrix added to spec.md

**Before Proceeding to Implementation**:
- [X] All 3 critical items marked as FIXED
- [ ] Changes reviewed by feature owner
- [ ] Updated spec/plan committed to branch
- [ ] Ready for `/speckit.implement`
