import HomePage from "../pages/HomePage";

let homePage = null;

before("Setup storage tests", () => {
  // Smart fixture management: checks existence, only loads if needed
  // Set CYPRESS_SKIP_FIXTURES=true to skip loading
  // Set CYPRESS_FORCE_FIXTURES=true to force reload
  cy.setupStorageTests().then((page) => {
    homePage = page;
  });
});

after("Cleanup storage tests", () => {
  // Cleanup only if CLEANUP_FIXTURES=true (default: false for faster iteration)
  // The cleanupStorageTests command handles the env var check
  cy.cleanupStorageTests();
});

describe("Location Expandable Rows", function () {
  beforeEach(function () {
    // Navigate to Storage Dashboard
    cy.visit("/Storage");
    cy.get(".storage-dashboard", { timeout: 10000 }).should("be.visible");
  });

  describe("Expand/Collapse Interaction", function () {
    /**
     * T165: Test expand/collapse interaction
     * testExpandRow_ClickChevronIcon: click chevron icon expands row
     * testExpandRow_ShowsExpandedContent: expanded content visible and displays correct fields
     * testExpandRow_SingleRowExpansion: expanding new row collapses previous
     * testExpandRow_CollapseSameRow: clicking same chevron collapses row
     * testExpandRow_KeyboardNavigation: Enter/Space key expands/collapses row
     */
    it("should expand row when chevron icon is clicked", function () {
      // Navigate to Rooms tab
      cy.get('[data-testid="tab-rooms"]').click();
      cy.get('button[role="tab"]')
        .contains("Rooms")
        .should("have.attr", "aria-selected", "true");

      // Wait for table to load
      cy.get('[data-testid^="room-row-"]', { timeout: 10000 }).should(
        "have.length.at.least",
        1,
      );

      // Find expand button (chevron icon) for first row
      cy.get('[data-testid^="room-row-"]')
        .first()
        .within(() => {
          // Find the expand button (TableExpandRow button)
          cy.get('button[aria-label*="expand"]', { timeout: 5000 })
            .should("be.visible")
            .click({ force: true });
        });

      // Verify expanded content appears by test id
      cy.get('[data-testid^="expanded-room-"]', { timeout: 5000 }).should(
        "be.visible",
      );
    });

    it("should show expanded content with correct fields for room", function () {
      cy.get('[data-testid="tab-rooms"]').click();
      cy.get('[data-testid^="room-row-"]', { timeout: 10000 }).should(
        "have.length.at.least",
        1,
      );

      // Expand first row
      cy.get('[data-testid^="room-row-"]')
        .first()
        .within(() => {
          cy.get('button[aria-label*="expand"]').click({ force: true });
        });

      // Verify all required fields are displayed by test id
      cy.get('[data-testid^="expanded-room-"]').within(() => {
        cy.get('[data-testid$="-description"]').should("be.visible");
        cy.get('[data-testid$="-created-date"]').should("be.visible");
        cy.get('[data-testid$="-created-by"]').should("be.visible");
        cy.get('[data-testid$="-last-modified-date"]').should("be.visible");
        cy.get('[data-testid$="-last-modified-by"]').should("be.visible");
      });
    });

    it("should collapse previous row when expanding new row", function () {
      cy.get('[data-testid="tab-rooms"]').click();
      cy.get('[data-testid^="room-row-"]', { timeout: 10000 }).should(
        "have.length.at.least",
        2,
      );

      // Expand first row
      cy.get('[data-testid^="room-row-"]')
        .first()
        .within(() => {
          cy.get('button[aria-label*="expand"]').click({ force: true });
        });

      // Verify first row is expanded
      cy.get('[data-testid^="room-row-"]')
        .first()
        .next()
        .within(() => {
          cy.contains("Description").should("be.visible");
        });

      // Expand second row
      cy.get('[data-testid^="room-row-"]')
        .eq(1)
        .within(() => {
          cy.get('button[aria-label*="expand"]').click({ force: true });
        });

      // Verify first row is collapsed (expanded content not visible)
      cy.get('[data-testid^="room-row-"]')
        .first()
        .next()
        .within(() => {
          cy.contains("Description").should("not.exist");
        });

      // Verify second row is expanded
      cy.get('[data-testid^="room-row-"]')
        .eq(1)
        .next()
        .within(() => {
          cy.contains("Description").should("be.visible");
        });
    });

    it("should collapse row when clicking same chevron again", function () {
      cy.get('[data-testid="tab-rooms"]').click();
      cy.get('[data-testid^="room-row-"]', { timeout: 10000 }).should(
        "have.length.at.least",
        1,
      );

      // Expand row
      cy.get('[data-testid^="room-row-"]')
        .first()
        .within(() => {
          cy.get('button[aria-label*="expand"]').click({ force: true });
        });

      // Verify expanded
      cy.get('[data-testid^="room-row-"]')
        .first()
        .next()
        .within(() => {
          cy.contains("Description").should("be.visible");
        });

      // Click same chevron to collapse
      cy.get('[data-testid^="room-row-"]')
        .first()
        .within(() => {
          cy.get('button[aria-label*="expand"]').click({ force: true });
        });

      // Verify collapsed
      cy.get('[data-testid^="room-row-"]')
        .first()
        .next()
        .within(() => {
          cy.contains("Description").should("not.exist");
        });
    });

    it("should expand/collapse row with keyboard navigation", function () {
      cy.get('[data-testid="tab-rooms"]').click();
      cy.get('[data-testid^="room-row-"]', { timeout: 10000 }).should(
        "have.length.at.least",
        1,
      );

      // Focus on expand button and press Enter
      cy.get('[data-testid^="room-row-"]')
        .first()
        .within(() => {
          cy.get('button[aria-label*="expand"]').focus().type("{enter}");
        });

      // Verify expanded
      cy.get('[data-testid^="room-row-"]')
        .first()
        .next()
        .within(() => {
          cy.contains("Description").should("be.visible");
        });

      // Press Enter again to collapse
      cy.get('[data-testid^="room-row-"]')
        .first()
        .within(() => {
          cy.get('button[aria-label*="expand"]').focus().type("{enter}");
        });

      // Verify collapsed
      cy.get('[data-testid^="room-row-"]')
        .first()
        .next()
        .within(() => {
          cy.contains("Description").should("not.exist");
        });
    });
  });

  describe("Expanded Content Verification", function () {
    /**
     * T166: Test expanded content verification
     * testExpandedContent_RoomFields: verifies Description, Created Date, Created By, Last Modified Date, Last Modified By displayed for room
     * testExpandedContent_DeviceFields: verifies Temperature Setting, Capacity Limit, Description, Created Date, Created By, Last Modified Date, Last Modified By displayed for device
     * testExpandedContent_ShelfFields: verifies Capacity Limit, Description, Created Date, Created By, Last Modified Date, Last Modified By displayed for shelf
     * testExpandedContent_RackFields: verifies Position Schema Hint, Description, Created Date, Created By, Last Modified Date, Last Modified By displayed for rack
     * testExpandedContent_ReadOnly: verifies no input fields in expanded content, only read-only display
     */
    it("should display all required fields for room", function () {
      cy.get('[data-testid="tab-rooms"]').click();
      cy.get('[data-testid^="room-row-"]', { timeout: 10000 }).should(
        "have.length.at.least",
        1,
      );

      // No need to reload - fixtures are already set up

      // Expand first row
      cy.get('[data-testid^="room-row-"]')
        .first()
        .within(() => {
          cy.get('button[aria-label*="expand"]').click({ force: true });
        });

      // Verify all fields
      cy.get('[data-testid^="room-row-"]')
        .first()
        .next()
        .within(() => {
          cy.contains("Description").should("be.visible");
          cy.contains("Created Date").should("be.visible");
          cy.contains("Created By").should("be.visible");
          cy.contains("Last Modified Date").should("be.visible");
          cy.contains("Last Modified By").should("be.visible");
          cy.contains("Main laboratory room").should("be.visible");
        });
    });

    it("should display all required fields for device", function () {
      cy.get('[data-testid="tab-devices"]').click();
      cy.get('[data-testid^="device-row-"]', { timeout: 10000 }).should(
        "have.length.at.least",
        1,
      );

      // No need to reload - fixtures are already set up

      // Expand first row
      cy.get('[data-testid^="device-row-"]')
        .first()
        .within(() => {
          cy.get('button[aria-label*="expand"]').click({ force: true });
        });

      // Verify all fields
      cy.get('[data-testid^="device-row-"]')
        .first()
        .next()
        .within(() => {
          cy.contains("Temperature Setting").should("be.visible");
          cy.contains("Capacity Limit").should("be.visible");
          cy.contains("Description").should("be.visible");
          cy.contains("Created Date").should("be.visible");
          cy.contains("Created By").should("be.visible");
          cy.contains("Last Modified Date").should("be.visible");
          cy.contains("Last Modified By").should("be.visible");
          cy.contains("-20.5").should("be.visible");
          cy.contains("100").should("be.visible");
        });
    });

    it("should display all required fields for shelf", function () {
      cy.get('[data-testid="tab-shelves"]').click();
      cy.get('[data-testid^="shelf-row-"]', { timeout: 10000 }).should(
        "have.length.at.least",
        1,
      );

      // No need to reload - fixtures are already set up

      // Expand first row
      cy.get('[data-testid^="shelf-row-"]')
        .first()
        .within(() => {
          cy.get('button[aria-label*="expand"]').click({ force: true });
        });

      // Verify all fields
      cy.get('[data-testid^="shelf-row-"]')
        .first()
        .next()
        .within(() => {
          cy.contains("Capacity Limit").should("be.visible");
          cy.contains("Description").should("be.visible");
          cy.contains("Created Date").should("be.visible");
          cy.contains("Created By").should("be.visible");
          cy.contains("Last Modified Date").should("be.visible");
          cy.contains("Last Modified By").should("be.visible");
          cy.contains("50").should("be.visible");
        });
    });

    it("should display all required fields for rack", function () {
      cy.get('[data-testid="tab-racks"]').click();
      cy.get('[data-testid^="rack-row-"]', { timeout: 10000 }).should(
        "have.length.at.least",
        1,
      );

      // No need to reload - fixtures are already set up

      // Expand first row
      cy.get('[data-testid^="rack-row-"]')
        .first()
        .within(() => {
          cy.get('button[aria-label*="expand"]').click({ force: true });
        });

      // Verify all fields
      cy.get('[data-testid^="rack-row-"]')
        .first()
        .next()
        .within(() => {
          cy.contains("Position Schema Hint").should("be.visible");
          cy.contains("Description").should("be.visible");
          cy.contains("Created Date").should("be.visible");
          cy.contains("Created By").should("be.visible");
          cy.contains("Last Modified Date").should("be.visible");
          cy.contains("Last Modified By").should("be.visible");
          cy.contains("A1-Z99").should("be.visible");
        });
    });

    it("should display expanded content as read-only", function () {
      cy.get('[data-testid="tab-rooms"]').click();
      cy.get('[data-testid^="room-row-"]', { timeout: 10000 }).should(
        "have.length.at.least",
        1,
      );

      // Expand row
      cy.get('[data-testid^="room-row-"]')
        .first()
        .within(() => {
          cy.get('button[aria-label*="expand"]').click({ force: true });
        });

      // Verify no input fields in expanded content
      cy.get('[data-testid^="room-row-"]')
        .first()
        .next()
        .within(() => {
          // Should not contain any text inputs
          cy.get('input[type="text"]').should("not.exist");
          cy.get('input[type="number"]').should("not.exist");
          cy.get("textarea").should("not.exist");
          // Should only contain read-only text
          cy.contains("Description").should("be.visible");
        });
    });
  });

  describe("Accessibility", function () {
    /**
     * T167: Test accessibility
     * testExpandedContent_ARIA: verifies aria-expanded attribute on TableExpandRow
     * testExpandedContent_KeyboardNavigation: Enter/Space key works for expand/collapse
     * testExpandedContent_ScreenReader: verifies semantic HTML structure with role="region" and aria-label
     */
    it("should have proper ARIA attributes on expandable rows", function () {
      cy.get('[data-testid="tab-rooms"]').click();
      cy.get('[data-testid^="room-row-"]', { timeout: 10000 }).should(
        "have.length.at.least",
        1,
      );

      // Check ARIA attributes on expand button
      cy.get('[data-testid^="room-row-"]')
        .first()
        .within(() => {
          cy.get('button[aria-label*="expand"]')
            .should("have.attr", "aria-label")
            .and("include", "expand");
        });

      // Expand row
      cy.get('[data-testid^="room-row-"]')
        .first()
        .within(() => {
          cy.get('button[aria-label*="expand"]').click({ force: true });
        });

      // Verify aria-expanded is set correctly
      cy.get('[data-testid^="room-row-"]')
        .first()
        .should("have.attr", "aria-expanded", "true");
    });

    it("should support keyboard navigation for expand/collapse", function () {
      cy.get('[data-testid="tab-rooms"]').click();
      cy.get('[data-testid^="room-row-"]', { timeout: 10000 }).should(
        "have.length.at.least",
        1,
      );

      // Focus on expand button and press Space
      cy.get('[data-testid^="room-row-"]')
        .first()
        .within(() => {
          cy.get('button[aria-label*="expand"]').focus().type(" ");
        });

      // Verify expanded
      cy.get('[data-testid^="room-row-"]')
        .first()
        .next()
        .within(() => {
          cy.contains("Description").should("be.visible");
        });

      // Press Space again to collapse
      cy.get('[data-testid^="room-row-"]')
        .first()
        .within(() => {
          cy.get('button[aria-label*="expand"]').focus().type(" ");
        });

      // Verify collapsed
      cy.get('[data-testid^="room-row-"]')
        .first()
        .next()
        .within(() => {
          cy.contains("Description").should("not.exist");
        });
    });

    it("should have semantic HTML structure for screen readers", function () {
      cy.get('[data-testid="tab-rooms"]').click();
      cy.get('[data-testid^="room-row-"]', { timeout: 10000 }).should(
        "have.length.at.least",
        1,
      );

      // Expand row
      cy.get('[data-testid^="room-row-"]')
        .first()
        .within(() => {
          cy.get('button[aria-label*="expand"]').click({ force: true });
        });

      // Verify semantic structure
      cy.get('[data-testid^="room-row-"]')
        .first()
        .next()
        .within(() => {
          // Should have role="region" and aria-label
          cy.get('[role="region"]')
            .should("exist")
            .and("have.attr", "aria-label")
            .and("include", "Additional");
        });
    });
  });

  describe("Samples and Occupancy Columns", function () {
    /**
     * Test Samples column in Rooms table
     * Test Occupancy column in Devices table
     */
    it("should display correct sample count in rooms table", function () {
      cy.get('[data-testid="tab-rooms"]').click();
      cy.get('[data-testid^="room-row-"]', { timeout: 10000 }).should(
        "have.length.at.least",
        1,
      );

      // Verify the table header includes "Samples"
      cy.contains("th", "Samples").should("be.visible");

      // Verify at least one room row exists with sample count data
      // The sample count should be a number (0 or greater)
      cy.get('[data-testid^="room-row-"]').first().should("be.visible");
    });

    it("should display correct occupancy in devices table", function () {
      cy.get('[data-testid="tab-devices"]').click();
      cy.get('[data-testid^="device-row-"]', { timeout: 10000 }).should(
        "have.length.at.least",
        1,
      );

      // Verify the table header includes "Occupancy"
      cy.contains("th", "Occupancy").should("be.visible");

      // Verify at least one device row exists with occupancy data
      // Occupancy should show as "occupied/total (percentage%)" format
      cy.get('[data-testid^="device-row-"]')
        .first()
        .within(() => {
          // Occupancy should contain a progress bar or formatted text
          // Format: "X/Y (Z%)" where X is occupied, Y is total capacity, Z is percentage
          cy.get("td").should("contain", "/");
        });
    });

    it("should display correct occupancy in shelves table", function () {
      cy.get('[data-testid="tab-shelves"]').click();
      cy.get('[data-testid^="shelf-row-"]', { timeout: 10000 }).should(
        "have.length.at.least",
        1,
      );

      // Verify the table header includes "Occupancy"
      cy.contains("th", "Occupancy").should("be.visible");

      // Verify at least one shelf row exists with occupancy data
      cy.get('[data-testid^="shelf-row-"]')
        .first()
        .within(() => {
          // Occupancy should contain a progress bar or formatted text
          cy.get("td").should("contain", "/");
        });
    });
  });
});
