# Research: Sample Storage Management

**Date**: 2025-10-30  
**Feature**: Sample Storage Management POC  
**Branch**: 001-sample-storage

## 1. Hibernate XML Mapping Pattern

**Examined Files**:

- `src/main/resources/hibernate/hbm/Person.hbm.xml`
- `src/main/resources/hibernate/hbm/Patient.hbm.xml`
- `src/main/resources/hibernate/hbm/Sample.hbm.xml`
- `src/main/resources/hibernate/hbm/ElectronicOrder.hbm.xml`

**Pattern Identified**:

```xml
<?xml version="1.0"?>
<!DOCTYPE hibernate-mapping PUBLIC "-//Hibernate/Hibernate Mapping DTD 3.0//EN"
"http://www.hibernate.org/dtd/hibernate-mapping-3.0.dtd">

<hibernate-mapping>
    <class name="org.openelisglobal.{module}.valueholder.{Entity}"
        table="{TABLE_NAME}" optimistic-lock="version" dynamic-update="true">

        <!-- ID with StringSequenceGenerator -->
        <id name="id"
            type="org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType">
            <column name="ID" precision="10" scale="0" />
            <generator
                class="org.openelisglobal.hibernate.resources.StringSequenceGenerator">
                <param name="sequence_name">{table_name}_seq</param>
            </generator>
        </id>

        <!-- Optimistic locking -->
        <version name="lastupdated" column="LASTUPDATED"
            type="timestamp" access="field" />

        <!-- Properties -->
        <property name="{fieldName}" type="java.lang.String">
            <column name="{COLUMN_NAME}" />
        </property>

        <!-- Many-to-One relationships -->
        <many-to-one name="{relationName}"
            class="org.openelisglobal.{module}.valueholder.{RelatedEntity}"
            fetch="select" lazy="false">
            <column name="{FOREIGN_KEY_COLUMN}" precision="10" scale="0" not-null="true" />
        </many-to-one>

        <!-- Enum types -->
        <property name="{enumField}" column="{COLUMN_NAME}">
            <type name="org.hibernate.type.EnumType">
                <param name="enumClass">org.openelisglobal.{module}.valueholder.{EnumClass}</param>
                <param name="useNamed">true</param>
            </type>
        </property>
    </class>
</hibernate-mapping>
```

**Key Observations**:

- **ID Generation**: Custom `StringSequenceGenerator` with
  `LIMSStringNumberUserType` converter
- **Optimistic Locking**: `version` field on `lastupdated` column with
  `access="field"`
- **Dynamic Updates**: `dynamic-update="true"` generates SQL with only changed
  fields
- **Column Naming**: Uppercase convention (e.g., `LAST_NAME`,
  `ACCESSION_NUMBER`)
- **Table Naming**: Uppercase, often singular (e.g., `PERSON`, `SAMPLE`,
  `PATIENT`)
- **Lazy Loading**: Explicitly disabled on many-to-one relationships
  (`lazy="false"`)

**Application to Storage Entities**:

For `StorageRoom.hbm.xml`:

```xml
<hibernate-mapping>
    <class name="org.openelisglobal.storage.valueholder.StorageRoom"
        table="STORAGE_ROOM" optimistic-lock="version" dynamic-update="true">
        <id name="id"
            type="org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType">
            <column name="ID" precision="10" scale="0" />
            <generator class="org.openelisglobal.hibernate.resources.StringSequenceGenerator">
                <param name="sequence_name">storage_room_seq</param>
            </generator>
        </id>
        <version name="lastupdated" column="LASTUPDATED" type="timestamp" access="field" />

        <property name="fhirUuid" type="java.util.UUID">
            <column name="FHIR_UUID" not-null="true" unique="true" />
        </property>
        <property name="name" type="java.lang.String">
            <column name="NAME" length="255" not-null="true" />
        </property>
        <property name="code" type="java.lang.String">
            <column name="CODE" length="50" not-null="true" unique="true" />
        </property>
        <property name="description" type="java.lang.String">
            <column name="DESCRIPTION" />
        </property>
        <property name="active" type="java.lang.Boolean">
            <column name="ACTIVE" not-null="true" />
        </property>
    </class>
</hibernate-mapping>
```

Similar patterns apply to StorageDevice, StorageShelf, StorageRack with
many-to-one relationships to parent entities.

---

## 2. FHIR Location Resource Structure

**R4 Specification**: https://hl7.org/fhir/R4/location.html

**IHE mCSD Profile**: Mobile Care Services Discovery (mCSD) defines hierarchical
location structures for facility registries.

**FHIR R4 Location Resource Structure**:

```json
{
  "resourceType": "Location",
  "id": "{fhir_uuid}",
  "identifier": [{
    "system": "http://openelis.org/storage-location-code",
    "value": "{hierarchical_code}"
  }],
  "status": "active" | "inactive",
  "name": "{location_name}",
  "description": "{optional_description}",
  "mode": "instance",
  "type": [{
    "coding": [{
      "system": "http://terminology.hl7.org/CodeSystem/location-physical-type",
      "code": "ro" | "ve" | "co",
      "display": "Room" | "Vehicle" | "Container"
    }]
  }],
  "physicalType": {
    "coding": [{
      "system": "http://terminology.hl7.org/CodeSystem/location-physical-type",
      "code": "ro",
      "display": "Room"
    }]
  },
  "partOf": {
    "reference": "Location/{parent_fhir_uuid}",
    "display": "{parent_name}"
  },
  "extension": [{
    "url": "http://openelis.org/fhir/extension/storage-capacity",
    "valueInteger": 100
  }]
}
```

**Mapping Strategy for Storage Hierarchy**:

| OpenELIS Entity | FHIR Location Type          | physicalType Code        | Notes                                                    |
| --------------- | --------------------------- | ------------------------ | -------------------------------------------------------- |
| StorageRoom     | Location                    | `ro` (room)              | Top-level, no partOf reference                           |
| StorageDevice   | Location                    | `ve` (vehicle/equipment) | partOf = Room Location, type = freezer/fridge/cabinet    |
| StorageShelf    | Location                    | `co` (container)         | partOf = Device Location                                 |
| StorageRack     | Location                    | `co` (container)         | partOf = Shelf Location, extension for grid dimensions   |
| StoragePosition | N/A - not separate resource | N/A                      | Positions encoded in Rack extension[available-positions] |

**Hierarchical Navigation via IHE mCSD**:

- Query all rooms: `GET /fhir/Location?physicalType=ro`
- Query devices in room: `GET /fhir/Location?partOf=Location/{room_uuid}`
- Include parent hierarchy:
  `GET /fhir/Location/{device_uuid}?_include=Location:partOf`

**Sample-to-Location Link via Specimen Resource**:

```json
{
  "resourceType": "Specimen",
  "id": "{sample_fhir_uuid}",
  "container": [
    {
      "identifier": {
        "value": "{hierarchical_location_path}"
      },
      "extension": [
        {
          "url": "http://openelis.org/fhir/extension/storage-rack",
          "valueReference": {
            "reference": "Location/{rack_fhir_uuid}"
          }
        },
        {
          "url": "http://openelis.org/fhir/extension/storage-position",
          "valueString": "{position_coordinate}"
        }
      ]
    }
  ]
}
```

**Decision**: Use FHIR Location resources for Room, Device, Shelf, Rack.
Positions tracked only in OpenELIS database (not synced to FHIR).
Sample-to-location link via Specimen.container extension.

---

## 3. Carbon Dropdown Cascading Pattern

**Component**: `@carbon/react` Dropdown component (v1.15.0)

**API Reference**:
https://react.carbondesignsystem.com/?path=/docs/components-dropdown--overview

**Pattern for Cascading Dropdowns**:

```javascript
import { Dropdown } from "@carbon/react";
import { useState, useEffect } from "react";

function CascadingLocationSelector({ onLocationChange }) {
  const [selectedRoom, setSelectedRoom] = useState(null);
  const [selectedDevice, setSelectedDevice] = useState(null);
  const [selectedShelf, setSelectedShelf] = useState(null);
  const [selectedRack, setSelectedRack] = useState(null);

  const [devices, setDevices] = useState([]);
  const [shelves, setShelves] = useState([]);
  const [racks, setRacks] = useState([]);

  // Fetch devices when room selected
  useEffect(() => {
    if (selectedRoom) {
      fetchDevices(selectedRoom.id).then(setDevices);
      setSelectedDevice(null); // Reset child selections
      setSelectedShelf(null);
      setSelectedRack(null);
    }
  }, [selectedRoom]);

  // Fetch shelves when device selected
  useEffect(() => {
    if (selectedDevice) {
      fetchShelves(selectedDevice.id).then(setShelves);
      setSelectedShelf(null);
      setSelectedRack(null);
    }
  }, [selectedDevice]);

  // Fetch racks when shelf selected
  useEffect(() => {
    if (selectedShelf) {
      fetchRacks(selectedShelf.id).then(setRacks);
      setSelectedRack(null);
    }
  }, [selectedShelf]);

  return (
    <>
      <Dropdown
        id="room-dropdown"
        titleText="Room"
        label="Select room"
        items={rooms}
        itemToString={(item) => item?.name || ""}
        onChange={({ selectedItem }) => setSelectedRoom(selectedItem)}
        selectedItem={selectedRoom}
      />

      <Dropdown
        id="device-dropdown"
        titleText="Device"
        label="Select device"
        items={devices}
        itemToString={(item) => item?.name || ""}
        onChange={({ selectedItem }) => setSelectedDevice(selectedItem)}
        selectedItem={selectedDevice}
        disabled={!selectedRoom}
      />

      <Dropdown
        id="shelf-dropdown"
        titleText="Shelf"
        label="Select shelf"
        items={shelves}
        itemToString={(item) => item?.label || ""}
        onChange={({ selectedItem }) => setSelectedShelf(selectedItem)}
        selectedItem={selectedShelf}
        disabled={!selectedDevice}
      />

      <Dropdown
        id="rack-dropdown"
        titleText="Rack"
        label="Select rack"
        items={racks}
        itemToString={(item) => item?.label || ""}
        onChange={({ selectedItem }) => setSelectedRack(selectedItem)}
        selectedItem={selectedRack}
        disabled={!selectedShelf}
      />
    </>
  );
}
```

**Key Points**:

- Controlled components with state for each level
- `useEffect` hooks trigger child data fetching on parent selection
- Reset child selections when parent changes
- `disabled` prop prevents selection until parent chosen
- `itemToString` prop formats display text for items

---

## 4. Barcode Scanner Browser Integration

**Event Type**: USB HID barcode scanners emit rapid keyboard events

**Detection Pattern**: Characters arrive within ~30-50ms interval (typical scan
gun speed)

**Implementation Strategy**:

```javascript
import { useEffect, useRef, useState } from "react";

function useBarcodeScanner(onScan, options = {}) {
  const {
    minLength = 3,
    timeout = 50, // ms between characters
  } = options;

  const bufferRef = useRef("");
  const timeoutIdRef = useRef(null);

  useEffect(() => {
    function handleKeyDown(event) {
      // Ignore if user is typing in an input field (unless it's our barcode input)
      if (
        event.target.tagName === "INPUT" &&
        !event.target.dataset.barcodeInput
      ) {
        return;
      }

      // Ignore modifier keys
      if (event.ctrlKey || event.altKey || event.metaKey) {
        return;
      }

      // Handle Enter key (scan complete)
      if (event.key === "Enter") {
        event.preventDefault();
        if (bufferRef.current.length >= minLength) {
          onScan(bufferRef.current);
        }
        bufferRef.current = "";
        return;
      }

      // Add character to buffer
      if (event.key.length === 1) {
        event.preventDefault();
        bufferRef.current += event.key;

        // Reset timeout
        if (timeoutIdRef.current) {
          clearTimeout(timeoutIdRef.current);
        }

        // Set new timeout to detect end of scan
        timeoutIdRef.current = setTimeout(() => {
          if (bufferRef.current.length >= minLength) {
            onScan(bufferRef.current);
          }
          bufferRef.current = "";
        }, timeout);
      }
    }

    window.addEventListener("keydown", handleKeyDown);

    return () => {
      window.removeEventListener("keydown", handleKeyDown);
      if (timeoutIdRef.current) {
        clearTimeout(timeoutIdRef.current);
      }
    };
  }, [onScan, minLength, timeout]);
}

// Usage in component:
function BarcodeScanMode({ onLocationScanned }) {
  const [scannedCode, setScannedCode] = useState("");

  useBarcodeScanner((barcode) => {
    setScannedCode(barcode);
    // Parse hierarchical barcode (e.g., "MAIN-FRZ01-SHA-RKR1")
    parseAndFetchLocation(barcode).then(onLocationScanned);
  });

  return (
    <TextInput
      id="barcode-input"
      data-barcode-input="true"
      labelText="Scan barcode or enter manually"
      value={scannedCode}
      onChange={(e) => setScannedCode(e.target.value)}
      placeholder="Scan barcode..."
    />
  );
}
```

**Key Points**:

- Detect rapid character input (< 50ms between keys)
- Buffer characters until Enter key or timeout
- Prevent default to avoid input field focus issues
- Allow manual entry fallback in TextInput
- Parse hierarchical barcode format (ROOM-DEVICE-SHELF-RACK)

---

## 5. OpenELIS Frontend Data Fetching Pattern

**Discovery**: OpenELIS does **NOT** use SWR. Instead, uses custom
`getFromOpenElisServer` utility with `useState`/`useEffect`.

**Examined Files**:

- `frontend/src/components/patient/resultsViewer/useObstreeData.ts`
- `frontend/src/components/patient/resultsViewer/usePatientResultsData.ts`
- `frontend/src/components/layout/search/searchService.js`

**Existing Pattern**:

```javascript
import { useState, useEffect } from "react";
import { getFromOpenElisServer } from "../utils/Utils";

function useStorageLocations(parentId, type) {
  const [data, setData] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchLocations = (response) => {
    setData(response);
    setIsLoading(false);
  };

  const handleError = (error) => {
    setError(error);
    setIsLoading(false);
  };

  useEffect(() => {
    if (parentId && type) {
      setIsLoading(true);
      getFromOpenElisServer(
        `/rest/storage/${type}?parentId=${parentId}`,
        fetchLocations,
        handleError
      );
    }
  }, [parentId, type]);

  return { data, isLoading, error };
}
```

**Decision**: Follow existing OpenELIS pattern with `getFromOpenElisServer`
utility. Do NOT introduce SWR dependency.

**Mutation Pattern** (for POST/PUT/DELETE):

```javascript
import { postToOpenElisServer } from "../utils/Utils";

function useSampleAssignment() {
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState(null);

  const assignSample = async (assignmentData) => {
    setIsSubmitting(true);
    setError(null);

    return new Promise((resolve, reject) => {
      postToOpenElisServer(
        "/rest/storage/samples/assign",
        JSON.stringify(assignmentData),
        (response) => {
          setIsSubmitting(false);
          resolve(response);
        },
        (error) => {
          setIsSubmitting(false);
          setError(error);
          reject(error);
        }
      );
    });
  };

  return { assignSample, isSubmitting, error };
}
```

---

## 6. Cypress E2E Configuration

**Status**: OpenELIS uses **Cypress 12.17.3** for E2E tests.

**Current E2E Framework**: Cypress 12.17.3 (per constitution)

- Tests in `frontend/cypress/e2e/`
- Configuration: `frontend/cypress.config.js`
- Existing tests: patientEntry.cy.js, orderEntity.cy.js, validation.cy.js, etc.

**Cypress Configuration** (per Constitution V.5):

```javascript
// cypress.config.js
const { defineConfig } = require("cypress");

module.exports = defineConfig({
  video: false, // MUST be disabled by default (per Constitution V.5)
  screenshotOnRunFailure: true, // MUST be enabled (per Constitution V.5)
  defaultCommandTimeout: 30000,
  viewportWidth: 1200,
  viewportHeight: 700,
  watchForFileChanges: false,
  e2e: {
    setupNodeEvents(on, config) {
      // Browser console logging enabled by default (Cypress captures automatically)
      return config;
    },
    baseUrl: "https://localhost",
    testIsolation: false, // Only if shared state needed
    env: {
      STARTUP_WAIT_MILLISECONDS: 300000,
    },
  },
});
```

**Note**: For complete Cypress E2E testing guidelines, see Constitution Section V.5.
Key requirements:
- Run tests individually during development (not full suite)
- Browser console logging enabled by default
- Video recording disabled (`video: false`)
- Post-run review of console logs and screenshots required
- Follow intercept timing, retry-ability, and element readiness best practices

**Test Structure** (follow existing pattern):

```
frontend/cypress/e2e/
├── storageAssignment.cy.js (P1 - Storage Assignment)
├── storageSearch.cy.js (P2A - Sample Search/Retrieval)
└── storageMovement.cy.js (P2B - Sample Movement, including bulk)
```

**Example Test Pattern** (updated with best practices per Constitution V.5):

```javascript
// storageAssignment.cy.js - UPDATED with best practices
import LoginPage from "../pages/LoginPage";

describe("Sample Storage Assignment (P1)", function () {
  before("Setup and login", () => {
    // Setup intercepts BEFORE any actions (best practice: intercept timing)
    cy.intercept("GET", "**/rest/storage/rooms").as("getRooms");
    cy.intercept("GET", "**/rest/storage/devices**").as("getDevices");
    cy.intercept("POST", "**/rest/storage/assignments").as("createAssignment");
    
    // Login
    const loginPage = new LoginPage();
    loginPage.visit();
    const homePage = loginPage.goToHomePage();
    homePage.goToSampleEntry();
  });

  it("should assign sample using cascading dropdowns", function () {
    cy.log("Starting assignment workflow");
    
    // Wait for storage selector to be ready (best practice: element readiness)
    cy.get('[data-testid="storage-location-selector"]').should("be.visible");
    
    // Open selector and wait for API call (best practice: intercept timing)
    cy.get('[data-testid="storage-location-selector"]').click();
    cy.wait("@getRooms");
    
    // Select room - wait for element readiness (best practice: retry-ability)
    cy.get('[data-testid="room-dropdown"]').should("be.visible").click();
    cy.contains("Main Laboratory").should("be.visible").click();
    
    // Select device - wait for API and element readiness
    cy.wait("@getDevices");
    cy.get('[data-testid="device-dropdown"]')
      .should("be.visible")
      .should("not.be.disabled")
      .click();
    cy.contains("Freezer Unit 1").should("be.visible").click();
    
    // Select shelf - same pattern
    cy.wait("@getDevices"); // May trigger again for shelf data
    cy.get('[data-testid="shelf-dropdown"]')
      .should("be.visible")
      .should("not.be.disabled")
      .click();
    cy.contains("Shelf-A").should("be.visible").click();
    
    // Select rack - same pattern
    cy.get('[data-testid="rack-dropdown"]')
      .should("be.visible")
      .should("not.be.disabled")
      .click();
    cy.contains("Rack R1").should("be.visible").click();
    
    // Enter position - wait for field to be ready
    cy.get('[data-testid="position-input"]')
      .should("be.visible")
      .type("A5");
    
    // Verify hierarchical path display (best practice: retry-able assertions)
    cy.get('[data-testid="location-path"]')
      .should("contain.text", "Main Laboratory > Freezer Unit 1 > Shelf-A > Rack R1 > Position A5");
    
    // Save assignment and verify
    cy.get('[data-testid="save-button"]').should("not.be.disabled").click();
    cy.wait("@createAssignment");
    cy.get('div[role="status"]')
      .should("be.visible")
      .and("contain.text", "assigned successfully");
  });
});
```

**Key Best Practices Demonstrated**:
- **Intercept Timing**: Set up `cy.intercept()` before actions that trigger API calls
- **Retry-Ability**: Use `.should()` assertions that automatically retry
- **Element Readiness**: Wait for elements to be visible before interaction
- **State Verification**: Use proper assertions (`contain.text`, `be.visible`)
- **No Arbitrary Waits**: Use `cy.wait('@alias')` instead of `cy.wait(1000)`

**Page Object Pattern** (follow existing structure):

```javascript
// cypress/pages/StorageAssignmentPage.js
class StorageAssignmentPage {
  getStorageLocationSelector() {
    return cy.get('[data-testid="storage-location-selector"]');
  }

  getRoomDropdown() {
    return cy.get('[data-testid="room-dropdown"]');
  }

  selectRoom(roomName) {
    this.getRoomDropdown().click();
    cy.contains(roomName).click();
    return this;
  }

  selectDevice(deviceName) {
    cy.get('[data-testid="device-dropdown"]').click();
    cy.contains(deviceName).click();
    return this;
  }

  enterPosition(coordinate) {
    cy.get('[data-testid="position-input"]').type(coordinate);
    return this;
  }

  clickSave() {
    cy.get('[data-testid="save-button"]').click();
    return this;
  }
}

export default StorageAssignmentPage;
```

**Run Commands** (per Constitution V.5):

```bash
# Run individual test file (RECOMMENDED during development)
npm run cy:run -- --spec "cypress/e2e/storageAssignment.cy.js"

# Run individual test case
npm run cy:run -- --spec "cypress/e2e/storageAssignment.cy.js" --grep "should assign sample"

# Open Cypress UI for interactive debugging (with console logging)
npx cypress open

# Run headed mode (see browser + console)
npx cypress run --headed

# Full suite (CI/CD only)
npm run cy:run
```

**Note**: Per Constitution V.5, tests MUST be run individually during development
(not full suite). Full suite runs are for CI/CD only. After each run, review
browser console logs and screenshots (especially on failures).

---

## 7. Certificate Architecture and Let's Encrypt Setup

**Date**: 2025-11-03  
**Context**: Infrastructure setup for `storage.openelis-global.org` subdomain

### Current Certificate Architecture

**Certificate Generation:**

- Project uses Docker container (`itechuw/certgen:main`) to generate
  **self-signed certificates** for development
- Certificates are generated in the `certs` service (lines 2-15 in
  `dev.docker-compose.yml`)
- Generated certificates include:
  - Self-signed certificate: `/etc/ssl/certs/apache-selfsigned.crt`
  - Private key: `/etc/ssl/private/apache-selfsigned.key`
  - Java keystore: `/etc/openelis-global/keystore` (PKCS12 format)
  - Java truststore: `/etc/openelis-global/truststore` (PKCS12 format)

**Certificate Details:**

- Subject: `CN=localhost`
- Subject Alternative Name: `DNS:*.openelis.org`
- Validity: 365 days
- Format: Self-signed X.509 certificate

**Certificate Distribution:** Certificates are distributed via Docker volumes:

- `key_trust-store-volume:/etc/openelis-global` - Java keystores/truststores
- `certs-vol:/etc/nginx/certs/` - Nginx certificates
- `keys-vol:/etc/nginx/keys/` - Nginx private keys

**Services Using Certificates:**

1. **Nginx Proxy** (`proxy` service):

   - Uses certificates from `certs-vol` and `keys-vol` volumes
   - Listens on ports 80 (HTTP) and 443 (HTTPS)
   - Current `nginx.conf` redirects all HTTP traffic to HTTPS
   - Uses generic `server_name __` (matches any hostname)

2. **OpenELIS Webapp** (`oe.openelis.org` service):

   - Mounts `key_trust-store-volume` for Java SSL communication
   - Uses keystore for outbound HTTPS connections
   - Uses truststore to validate peer certificates

3. **FHIR API** (`fhir.openelis.org` service):
   - Uses Java keystores/truststores via `JAVA_OPTS` environment variables
   - Configured for mutual TLS (mTLS) communication

### Security Considerations

1. **Certificate Storage:**

   - Never commit certificates to git - use `.gitignore` for certificate
     directories
   - Use Docker secrets for sensitive certificate passwords
   - Restrict file permissions on certificate files (600 for keys, 644 for
     certs)

2. **Private Key Protection:**

   - Private keys should be stored in Docker volumes with restricted access
   - Consider using Docker secrets for keystore passwords
   - Rotate keys periodically

3. **Certificate Renewal:**
   - Set up monitoring for certificate expiration
   - Test renewal process before certificates expire
   - Have a fallback mechanism if renewal fails

### Testing the Configuration

**Verify DNS Resolution:**

```bash
dig storage.openelis-global.org
nslookup storage.openelis-global.org
```

**Test HTTP to HTTPS Redirect:**

```bash
curl -I http://storage.openelis-global.org
# Should return 301 redirect to HTTPS
```

**Verify Certificate:**

```bash
openssl s_client -connect storage.openelis-global.org:443 -servername storage.openelis-global.org
```

**Check Certificate Details:**

```bash
echo | openssl s_client -servername storage.openelis-global.org -connect storage.openelis-global.org:443 2>/dev/null | openssl x509 -noout -dates -subject
```

### Troubleshooting

**Common Issues:**

1. **ACME Challenge Fails:**

   - Ensure port 80 is accessible from the internet
   - Verify DNS points to correct IP
   - Check nginx is serving `.well-known/acme-challenge/` path

2. **Certificate Not Found:**

   - Verify certificate location:
     `/etc/letsencrypt/live/storage.openelis-global.org/`
   - Check nginx can read certificate files
   - Ensure proper file permissions

3. **Nginx Won't Start:**

   - Check nginx configuration syntax: `nginx -t`
   - Verify certificate paths in nginx.conf
   - Review Docker logs: `docker logs openelisglobal-proxy`

4. **Certificate Renewal Fails:**
   - Check Certbot logs: `docker logs openelisglobal-certbot-renew`
   - Verify nginx is running during renewal
   - Ensure challenge path is accessible

## 8. Carbon DataTable Expandable Rows

**Date**: 2025-11-07  
**Feature**: Expandable rows in location tables (Rooms, Devices, Shelves, Racks)

### Research Questions

#### Q1: How to implement Carbon DataTable expandable rows?

**Decision**: Use Carbon DataTable `expandableRows` prop with `TableExpandHeader`, `TableExpandRow`, and `TableExpandedRow` components.

**Rationale**: 
- Carbon Design System v1.15 provides built-in expandable row support via `expandableRows` prop
- Existing codebase pattern found in `EOrder.js` component demonstrates this pattern
- Follows constitution requirement (Principle II: Carbon Design System First)
- Provides accessibility support (ARIA labels, keyboard navigation)

**Implementation Pattern** (from EOrder.js):
```jsx
<DataTable
  rows={data}
  headers={headers}
  expandableRows
>
  {({ rows, headers, getHeaderProps, getRowProps, getTableProps }) => (
    <TableContainer>
      <Table>
        <TableHead>
          <TableRow>
            <TableExpandHeader aria-label="expand row" />
            {headers.map((header) => (
              <TableHeader {...getHeaderProps({ header })}>
                {header.header}
              </TableHeader>
            ))}
          </TableRow>
        </TableHead>
        <TableBody>
          {rows.map((row) => (
            <React.Fragment key={row.id}>
              <TableExpandRow {...getRowProps({ row })}>
                {row.cells.map((cell) => renderCell(cell, row))}
              </TableExpandRow>
              <TableExpandedRow colSpan={headers.length + 1}>
                {renderExpandedContent(row)}
              </TableExpandedRow>
            </React.Fragment>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  )}
</DataTable>
```

**Alternatives Considered**:
- ❌ Custom accordion component: Would violate Carbon Design System requirement
- ❌ Modal dialog: Would interrupt workflow, not inline
- ❌ Side panel: More complex, not standard Carbon pattern

**Reference**: 
- Carbon DataTable documentation: https://react.carbondesignsystem.com/?path=/docs/components-datatable--expandable
- Existing implementation: `frontend/src/components/eOrder/EOrder.js` (lines 290-340)

#### Q2: How to manage single-row expansion state?

**Decision**: Use React `useState` to track expanded row ID, with logic to collapse previous row when new row expands.

**Rationale**:
- Simple state management pattern
- Single source of truth for expanded state
- Easy to implement "only one expanded at a time" behavior
- No need for complex state management library

**Implementation Pattern**:
```jsx
const [expandedRowId, setExpandedRowId] = useState(null);

const handleRowExpand = (rowId) => {
  setExpandedRowId(expandedRowId === rowId ? null : rowId);
};

// In TableExpandRow:
<TableExpandRow
  {...getRowProps({ row })}
  isExpanded={expandedRowId === row.id}
  onExpand={() => handleRowExpand(row.id)}
>
```

**Alternatives Considered**:
- ❌ Multiple rows expanded: Violates spec requirement (FR-059d)
- ❌ Redux/Context: Overkill for simple local component state

#### Q3: What data format for expanded content?

**Decision**: Display additional fields as key-value pairs in a structured layout using Carbon Grid/Column components.

**Rationale**:
- Clear, scannable format
- Easy to implement with Carbon components
- Consistent with read-only requirement (FR-059e)
- Supports internationalization (key labels via React Intl)

**Implementation Pattern**:
```jsx
const renderExpandedContent = (row) => {
  const location = row.original; // Full location object
  return (
    <div style={{ padding: '1rem' }}>
      <Grid>
        <Column md={6}>
          <strong>Description:</strong> {location.description || 'N/A'}
        </Column>
        <Column md={6}>
          <strong>Created Date:</strong> {formatDate(location.createdDate)}
        </Column>
        {/* More key-value pairs */}
      </Grid>
    </div>
  );
};
```

**Alternatives Considered**:
- ❌ Plain text list: Less structured, harder to scan
- ❌ Nested table: Overkill for simple key-value display
- ❌ Card component: More visual weight than needed

#### Q4: How to handle missing/optional fields in expanded view?

**Decision**: Display "N/A" or empty string for missing optional fields, format dates/timestamps consistently.

**Rationale**:
- Prevents empty/blank spaces in UI
- Consistent user experience
- Clear indication when data is not available
- Follows existing OpenELIS patterns

**Implementation Pattern**:
```jsx
const formatField = (value, formatter) => {
  if (value === null || value === undefined || value === '') {
    return intl.formatMessage({ id: 'common.not.available', defaultMessage: 'N/A' });
  }
  return formatter ? formatter(value) : value;
};
```

**Alternatives Considered**:
- ❌ Hide missing fields: Inconsistent row heights, confusing
- ❌ Show empty: Looks like a bug

#### Q5: How to ensure expanded content is accessible?

**Decision**: Use Carbon's built-in ARIA attributes from `TableExpandRow` and `TableExpandedRow`, add semantic HTML structure.

**Rationale**:
- Carbon components provide accessibility out of the box
- ARIA labels automatically handled by `TableExpandHeader` and `TableExpandRow`
- Keyboard navigation supported (Enter/Space to expand)
- Screen reader friendly with proper heading structure

**Implementation Pattern**:
- Carbon `TableExpandRow` automatically handles:
  - `aria-expanded` attribute
  - `aria-controls` linking to expanded content
  - Keyboard navigation (Enter/Space)
- Use semantic HTML in expanded content:
  ```jsx
  <TableExpandedRow>
    <div role="region" aria-label="Additional location details">
      {/* Content */}
    </div>
  </TableExpandedRow>
  ```

**Alternatives Considered**:
- ❌ Custom ARIA implementation: Carbon already handles this
- ❌ No accessibility: Violates WCAG 2.1 AA requirement

### Technical Decisions Summary

| Decision | Choice | Rationale |
|----------|--------|-----------|
| UI Pattern | Carbon DataTable expandable rows | Constitution compliance, existing pattern |
| State Management | React useState (single expandedRowId) | Simple, sufficient for requirement |
| Content Format | Key-value pairs in Grid layout | Clear, scannable, i18n-friendly |
| Missing Fields | Display "N/A" | Consistent UX, clear indication |
| Accessibility | Carbon built-in + semantic HTML | WCAG compliance, minimal custom work |

### Dependencies

- **Carbon Design System v1.15**: `@carbon/react` with `TableExpandHeader`, `TableExpandRow`, `TableExpandedRow`
- **React Intl**: For internationalized field labels
- **Existing StorageDashboard**: Modify current table implementations

### Implementation Notes

1. **Backend Changes**: None required - all fields already available in API responses
2. **API Changes**: None required - expanded view uses existing location data
3. **State Management**: Local component state sufficient (no global state needed)
4. **Testing**: 
   - Unit tests: Test expanded state management, content rendering
   - E2E tests: Test expand/collapse interaction, single-row behavior
5. **Performance**: Minimal impact - expanded content rendered on-demand, no additional API calls

---

## 9. Existing OpenELIS Barcode Printing Infrastructure

**Date**: 2025-11-22  
**Feature**: Integration with existing barcode label printing system

### Research Questions

#### Q1: What barcode printing infrastructure already exists in OpenELIS?

**Decision**: OpenELIS has a complete barcode printing system using iTextPDF library.

**Existing Infrastructure**:

1. **BarcodeLabelMaker.java** (`src/main/java/org/openelisglobal/barcode/BarcodeLabelMaker.java`):
   - Uses `com.itextpdf.text.pdf.Barcode128` for Code 128 barcodes
   - Uses `com.google.zxing` for QR codes
   - Generates PDF streams via `createLabelsAsStream()` method
   - Supports multiple label types: OrderLabel, SpecimenLabel, BlankLabel, BlockLabel, SlideLabel
   - Label dimensions configurable via `ConfigurationProperties`

2. **LabelMakerServlet.java** (`src/main/java/org/openelisglobal/common/servlet/barcode/LabelMakerServlet.java`):
   - Servlet endpoint: `/LabelMakerServlet`
   - Query parameters: `labNo`, `type`, `quantity`, `override`
   - Returns PDF stream with `Content-Type: application/pdf`
   - Frontend usage: `<iframe src="/LabelMakerServlet?labNo=...&type=...&quantity=..."/>`

3. **BarcodeConfigurationForm.java** (`src/main/java/org/openelisglobal/barcode/form/BarcodeConfigurationForm.java`):
   - System administration form for barcode settings
   - Configurable properties:
     - Label dimensions (height/width for each label type)
     - Maximum print limits (numMaxOrderLabels, numMaxSpecimenLabels, etc.)
     - Default print quantities
   - Stored in `SiteInformation` table via `BarcodeInformationService`

4. **BarcodeLabelInfo.java** (`src/main/java/org/openelisglobal/barcode/valueholder/BarcodeLabelInfo.java`):
   - Entity for tracking print history
   - Fields: `id`, `numPrinted`, `code`, `type`
   - Tracks how many times a label has been printed
   - Used for enforcing maximum print limits

5. **ConfigurationProperties.java** (`src/main/java/org/openelisglobal/common/util/ConfigurationProperties.java`):
   - Property enum values for barcode configuration:
     - `ORDER_BARCODE_HEIGHT`, `ORDER_BARCODE_WIDTH`
     - `SPECIMEN_BARCODE_HEIGHT`, `SPECIMEN_BARCODE_WIDTH`
     - `BLOCK_BARCODE_HEIGHT`, `BLOCK_BARCODE_WIDTH`
     - `SLIDE_BARCODE_HEIGHT`, `SLIDE_BARCODE_WIDTH`
     - `MAX_ORDER_PRINTED`, `MAX_SPECIMEN_PRINTED`
   - Properties stored in database (`site_information` table) or `SystemConfiguration.properties` file

**Pattern for Creating New Label Types**:

```java
// Example: OrderLabel extends Label
public class StorageLocationLabel extends Label {
    public StorageLocationLabel(StorageDevice device, String shortCode) {
        // Set dimensions from ConfigurationProperties
        width = Float.parseFloat(ConfigurationProperties.getInstance()
            .getPropertyValue(Property.STORAGE_LOCATION_BARCODE_WIDTH));
        height = Float.parseFloat(ConfigurationProperties.getInstance()
            .getPropertyValue(Property.STORAGE_LOCATION_BARCODE_HEIGHT));
        
        // Set barcode code (hierarchical path or short code)
        setCode(shortCode != null ? shortCode : buildHierarchicalPath(device));
        
        // Add fields above/below barcode
        aboveFields = new ArrayList<>();
        aboveFields.add(new LabelField("Location", device.getName(), 12));
        // ... more fields
    }
}
```

**Integration Strategy**:

1. **Create StorageLocationLabel class** extending `Label`:
   - Use hierarchical path (`ROOM-DEVICE-SHELF-RACK`) or short code for barcode value
   - Read dimensions from `ConfigurationProperties` (add new properties: `STORAGE_LOCATION_BARCODE_HEIGHT`, `STORAGE_LOCATION_BARCODE_WIDTH`)
   - Display location name, code, hierarchical path on label

2. **Extend LabelMakerServlet** or create new endpoint:
   - Option A: Extend existing servlet with new `type=storage-location` parameter
   - Option B: Create REST endpoint `/rest/storage/{type}/{id}/print-label` (preferred for consistency with REST API pattern)
   - Return PDF stream same as existing servlet

3. **Add Configuration Properties**:
   - Add `STORAGE_LOCATION_BARCODE_HEIGHT` and `STORAGE_LOCATION_BARCODE_WIDTH` to `ConfigurationProperties.Property` enum
   - Add to `BarcodeConfigurationForm` for system admin UI
   - Store in `site_information` table via `BarcodeInformationService`

4. **Print History Tracking**:
   - Reuse existing `BarcodeLabelInfo` entity or create new `StorageLocationPrintHistory` entity
   - Track: location entity ID, short code (if used), printed by (user ID), printed date, print count
   - Store in database for audit trail

**Rationale**: Leveraging existing infrastructure reduces development effort and maintains consistency with OpenELIS patterns. iTextPDF is already in dependencies, label configuration system exists, and print history pattern is established.

**Alternatives Considered**:
- ❌ Custom PDF generation library: Would duplicate existing functionality
- ❌ Separate label printing system: Would create inconsistency and maintenance overhead
- ❌ Third-party label printing service: Would add external dependency and cost

#### Q2: How to configure default printer for label printing?

**Decision**: NEEDS CLARIFICATION - Research required on printer configuration in OpenELIS.

**Research Needed**:
- Does OpenELIS have system-wide default printer configuration?
- How do existing label printing workflows handle printer selection?
- Is printer selection handled by browser (user selects printer when PDF opens)?
- Or is there server-side printer configuration?

**Current Understanding**:
- LabelMakerServlet returns PDF stream to browser
- Browser PDF viewer handles printing (user selects printer)
- No evidence of server-side printer configuration in existing code

**Action Required**: Research printer configuration options:
1. Check if `ConfigurationProperties` has printer-related settings
2. Check if there's a printer selection dialog in frontend
3. Determine if "default printer" means browser default or system default
4. Document findings for implementation

#### Q3: What are the detailed requirements for USB HID barcode scanner integration?

**Decision**: Basic keyboard event handling is documented, but hardware-specific details need research.

**Current Research** (from Section 4):
- USB HID scanners emit rapid keyboard events (30-50ms between characters)
- Detection via character buffer with timeout
- Enter key indicates scan completion

**Additional Research Needed**:

1. **Scanner Configuration**:
   - Do scanners need any configuration (prefix/suffix characters)?
   - How to handle scanners that add Enter automatically vs manual Enter?
   - What happens if user types manually vs scans (detection method)?

2. **Browser Compatibility**:
   - Do all browsers handle USB HID scanners identically?
   - Any browser-specific quirks or limitations?
   - Mobile browser support (if applicable)?

3. **Error Handling**:
   - What if scanner malfunctions (partial scans, corrupted data)?
   - How to distinguish scanner input from normal typing?
   - Should we detect scan speed vs typing speed?

**Action Required**: Document hardware testing results and browser compatibility findings.

**Reference**: Existing research in Section 4 provides basic implementation pattern. Additional hardware testing recommended during implementation phase.

### Technical Decisions Summary

| Decision | Choice | Rationale |
|----------|--------|-----------|
| PDF Generation | Reuse existing iTextPDF via BarcodeLabelMaker | Already in dependencies, proven pattern |
| Label Type | Create StorageLocationLabel extending Label | Follows existing pattern, maintains consistency |
| Print Endpoint | REST endpoint `/rest/storage/{type}/{id}/print-label` | Consistent with REST API architecture |
| Configuration | Extend ConfigurationProperties and BarcodeConfigurationForm | Leverages existing system admin infrastructure |
| Print History | Create StorageLocationPrintHistory entity | Separate from sample labels, storage-specific audit trail |
| Printer Selection | Browser PDF viewer (user selects) | Matches existing pattern, no server-side printer config needed |

### Dependencies

- **iTextPDF**: Already in OpenELIS dependencies (`com.itextpdf:itextpdf`)
- **ZXing**: Already in dependencies for QR code support (`com.google.zxing:core`)
- **BarcodeLabelMaker**: Existing class, extend for storage locations
- **ConfigurationProperties**: Existing utility, add new properties

### Implementation Notes

1. **Backend Changes**:
   - Create `StorageLocationLabel.java` extending `Label`
   - Add `STORAGE_LOCATION_BARCODE_HEIGHT` and `STORAGE_LOCATION_BARCODE_WIDTH` to `ConfigurationProperties.Property` enum
   - Extend `BarcodeConfigurationForm` with storage location label dimensions
   - Create REST endpoint for label printing (or extend LabelMakerServlet)
   - Create `StorageLocationPrintHistory` entity and DAO/Service

2. **Frontend Changes**:
   - Label Management modal calls REST endpoint
   - PDF opens in new tab (browser handles printing)
   - Display print history from `StorageLocationPrintHistory` entity

3. **Database Changes**:
   - Add `storage_location_print_history` table (Liquibase changeset)
   - Add configuration properties to `site_information` table (via system admin UI)

4. **Testing**:
   - Unit tests for `StorageLocationLabel` class
   - Integration tests for print endpoint
   - E2E tests for label printing workflow

---

## Summary of Research Findings

| Question                    | Answer                                                                                                                                                                                                                        | Source                                                          |
| --------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------- |
| Hibernate XML Mapping       | StringSequenceGenerator + LIMSStringNumberUserType, version on lastupdated, dynamic-update=true                                                                                                                               | Existing .hbm.xml files                                         |
| FHIR Location Structure     | R4 Location resource with partOf hierarchy, physicalType codes (ro/ve/co), IHE mCSD compliance                                                                                                                                | FHIR R4 spec + IHE mCSD                                         |
| Carbon Dropdown Cascading   | Controlled components, useEffect for child data fetching, disabled until parent selected                                                                                                                                      | @carbon/react Dropdown API                                      |
| Barcode Scanner Integration | USB HID keyboard events, character buffer with 50ms timeout, detect Enter key                                                                                                                                                 | Browser keyboard event handling                                 |
| Barcode Printing Infrastructure | Reuse existing iTextPDF/BarcodeLabelMaker, create StorageLocationLabel extending Label, REST endpoint for printing, extend ConfigurationProperties | Existing OpenELIS barcode printing system (BarcodeLabelMaker.java, LabelMakerServlet.java) |
| Frontend Data Fetching      | Custom `getFromOpenElisServer` utility with useState/useEffect (NOT SWR)                                                                                                                                                      | Existing OpenELIS hooks                                         |
| Cypress E2E Setup           | Use existing Cypress 12.17.3 framework, follow patientEntry.cy.js pattern                                                                                                                                                     | Existing OpenELIS E2E tests                                     |
| Certificate Architecture    | Self-signed certs via certgen container, distributed via Docker volumes to nginx/proxy and Java services. Let's Encrypt setup requires Certbot container, nginx ACME challenge handling, and subdomain-specific server blocks | dev.docker-compose.yml, nginx.conf, certificate-setup-report.md |
| Carbon DataTable Expandable Rows | Carbon DataTable expandableRows prop with TableExpandHeader/TableExpandRow/TableExpandedRow, React useState for single-row expansion, key-value pairs in Grid layout | Carbon DataTable docs, EOrder.js implementation |
| Capacity Calculation Logic | Two-tier system: manual `capacity_limit` (if set) OR calculated from children (sum if all children have defined capacities). Racks always use rows × columns. Show "N/A" if capacity cannot be determined. | Spec FR-062a, FR-062b, FR-062c, laboratory workflow analysis |

**Decisions Made**:

1. Follow existing Hibernate XML patterns for storage entities
2. Map Room/Device/Shelf/Rack/Position ALL to FHIR Location resources (positions
   as child locations with extensions)
3. Use Carbon Dropdown with cascading state management
4. Implement barcode scanner with keyboard event listener + character buffer
5. Use existing `getFromOpenElisServer` pattern (no SWR dependency)
6. Use existing Cypress framework for E2E tests (NOT Playwright)
7. Set up Let's Encrypt for `storage.openelis-global.org` subdomain
   incrementally, keeping self-signed certs for other services during
   development phase
8. Reuse existing OpenELIS barcode printing infrastructure (iTextPDF, BarcodeLabelMaker) for storage location labels
9. Implement two-tier capacity system: manual `capacity_limit` takes precedence, otherwise calculate from children (sum if all children have defined capacities). Display "N/A" with tooltip when capacity cannot be determined. Visually distinguish manual vs calculated capacities.

**Next Steps**: Proceed to Phase 1 design artifacts (data-model.md, contracts/,
quickstart.md)

---

## 9. Capacity Calculation Logic (2025-01-15)

**Question**: How should capacity be calculated for Devices and Shelves when `capacity_limit` is not set? How should the system handle cases where some children have defined capacities and others don't?

**Research Context**: 
- Spec requires occupancy display (FR-061, FR-062) showing fraction, percentage, and progress bar
- Devices and Shelves have optional `capacity_limit` field
- Racks always use calculated capacity (rows × columns per FR-017)
- Need to support both manual planning limits and dynamic calculation from hierarchy

**Decision**: Two-tier capacity system with hierarchical fallback

**Rationale**:
1. **Manual limits for planning**: Labs need to set capacity limits for procurement planning (e.g., "Freezer Unit 1 can hold 500 samples")
2. **Dynamic calculation for flexibility**: When limits aren't set, calculate from actual storage structure (sum of child capacities)
3. **Consistency requirement**: If ANY child lacks defined capacity, cannot reliably calculate parent (would show misleading data)
4. **User transparency**: Users must understand whether capacity is manual or calculated (visual distinction required)

**Implementation Pattern**:
- **Tier 1**: If `capacity_limit` is set, use that value (manual/static limit)
- **Tier 2**: If `capacity_limit` is NULL:
  - Calculate from child locations (shelves for devices, racks for shelves)
  - If ALL children have defined capacities (either static `capacity_limit` OR calculated from their own children), sum those capacities
  - If ANY child lacks defined capacity, return null (capacity cannot be determined)
- **Racks**: Always calculated (rows × columns), never use `capacity_limit` field

**UI Display**:
- When capacity is defined: Show "287/500 (57%)" with progress bar
- When capacity cannot be determined: Show "N/A" or "Unlimited" with tooltip explaining why
- Visual distinction: Badge, tooltip, or icon to indicate "Manual Limit" vs "Calculated"

**Alternatives Considered**:
- **Option B (Simplified)**: Only show occupancy when `capacity_limit` is set, otherwise show "Unlimited" - **Rejected**: Too restrictive, doesn't leverage rack capacity data
- **Option C (Always Calculate)**: Remove `capacity_limit` field, always calculate from children - **Rejected**: Labs need manual limits for planning purposes

**Dependencies**:
- Backend: `StorageLocationService` must implement `calculateDeviceCapacity()` and `calculateShelfCapacity()` methods
- API: Device/Shelf responses must include `totalCapacity` and `capacityType` fields
- Frontend: Occupancy display must handle null capacity and show visual distinction

**Reference**: Spec FR-062a, FR-062b, FR-062c, FR-061, FR-063
