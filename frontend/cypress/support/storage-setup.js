import LoginPage from "../pages/LoginPage";

/**
 * Common setup for all storage E2E tests
 * Consolidates login, fixture loading, and API readiness checks
 * Usage: cy.setupStorageTests()
 *
 * Environment variables (set via CYPRESS_* prefix):
 * - SKIP_FIXTURES=true: Skip fixture loading entirely (assumes fixtures exist)
 * - FORCE_FIXTURES=true: Force reload fixtures even if they exist
 * - CLEANUP_FIXTURES=true: Clean up fixtures after tests (default: false)
 *
 * Smart fixture management:
 * 1. If SKIP_FIXTURES=true: Skip loading (fastest, assumes fixtures exist)
 * 2. If FORCE_FIXTURES=true: Always load fixtures (reloads even if exist)
 * 3. Otherwise: Check if fixtures exist, only load if missing
 *
 * Examples:
 *   # Fast iteration (skip loading, skip cleanup)
 *   npm run cy:run -- --spec "cypress/e2e/storage*.cy.js"
 *
 *   # Force reload fixtures
 *   CYPRESS_FORCE_FIXTURES=true npm run cy:run -- --spec "cypress/e2e/storage*.cy.js"
 *
 *   # Clean up after tests
 *   CYPRESS_CLEANUP_FIXTURES=true npm run cy:run -- --spec "cypress/e2e/storage*.cy.js"
 */
Cypress.Commands.add("setupStorageTests", () => {
  // Wait for backend API to be available
  cy.waitForBackend("/rest/storage/samples");

  // Login
  const loginPage = new LoginPage();
  loginPage.visit();
  const homePage = loginPage.goToHomePage();

  // Smart fixture loading based on env vars and existence check
  const skipFixtures = Cypress.env("SKIP_FIXTURES") === true;
  const forceFixtures = Cypress.env("FORCE_FIXTURES") === true;

  if (skipFixtures) {
    cy.log(
      "Skipping fixture loading (SKIP_FIXTURES=true) - assuming fixtures exist",
    );
  } else if (forceFixtures) {
    cy.log(
      "Force loading fixtures (FORCE_FIXTURES=true) - reloading even if exist",
    );
    cy.loadStorageFixtures();
  } else {
    // Check if fixtures already exist before loading
    cy.checkStorageFixturesExist().then((fixturesExist) => {
      if (fixturesExist) {
        cy.log("Fixtures already exist - skipping load for faster iteration");
      } else {
        cy.log("Fixtures not found - loading test data");
        cy.loadStorageFixtures();
      }
    });
  }

  // Return homePage for tests that need it
  return cy.wrap(homePage);
});

/**
 * Cleanup after storage tests
 * Usage: cy.cleanupStorageTests()
 *
 * Only cleans up if CLEANUP_FIXTURES=true (default: false for faster iteration)
 * Set CYPRESS_CLEANUP_FIXTURES=true to enable cleanup
 */
Cypress.Commands.add("cleanupStorageTests", () => {
  const shouldCleanup = Cypress.env("CLEANUP_FIXTURES") === true;

  if (shouldCleanup) {
    cy.log("Cleaning up fixtures (CLEANUP_FIXTURES=true)");
    cy.cleanStorageFixtures();
  } else {
    cy.log(
      "Skipping fixture cleanup (CLEANUP_FIXTURES=false) - fixtures preserved for next run",
    );
  }
});

/**
 * Set up common API intercepts for storage tests
 * Usage: cy.setupStorageIntercepts()
 * Note: Using ** wildcard to match any query parameters
 */
Cypress.Commands.add("setupStorageIntercepts", () => {
  // Use more flexible patterns to match query parameters
  cy.intercept("GET", "**/rest/storage/samples**").as("getSamples");
  cy.intercept("GET", "**/rest/storage/rooms**").as("getRooms");
  cy.intercept("GET", "**/rest/storage/devices**").as("getDevices");
  cy.intercept("GET", "**/rest/storage/shelves**").as("getShelves");
  cy.intercept("GET", "**/rest/storage/racks**").as("getRacks");
  cy.intercept("GET", "**/rest/storage/locations/search**").as(
    "searchLocations",
  );
  cy.intercept("POST", "**/rest/storage/rooms**").as("createRoom");
  cy.intercept("POST", "**/rest/storage/devices**").as("createDevice");
  cy.intercept("POST", "**/rest/storage/shelves**").as("createShelf");
  cy.intercept("POST", "**/rest/storage/racks**").as("createRack");
  cy.intercept("POST", "**/rest/storage/samples/move**").as("moveSample");
});
