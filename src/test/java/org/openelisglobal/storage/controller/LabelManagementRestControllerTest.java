package org.openelisglobal.storage.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.storage.form.ShortCodeUpdateForm;
import org.openelisglobal.storage.service.LabelManagementService;
import org.openelisglobal.storage.service.ShortCodeValidationResult;
import org.openelisglobal.storage.service.ShortCodeValidationService;
import org.openelisglobal.storage.valueholder.StorageDevice;
import org.openelisglobal.storage.valueholder.StorageShelf;
import org.openelisglobal.storage.valueholder.StorageRack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Integration tests for LabelManagementRestController
 * Following TDD approach: Write tests BEFORE implementation
 * Tests cover short code updates, label generation, and print history
 */
public class LabelManagementRestControllerTest extends BaseWebContextSensitiveTest {

    private static final Logger logger = LoggerFactory.getLogger(LabelManagementRestControllerTest.class);

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private LabelManagementService labelManagementService;

    @Autowired
    private ShortCodeValidationService shortCodeValidationService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private JdbcTemplate jdbcTemplate;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        objectMapper = new ObjectMapper();
        jdbcTemplate = new JdbcTemplate(dataSource);
        cleanStorageTestData();
    }

    @After
    public void tearDown() throws Exception {
        cleanStorageTestData();
    }

    /**
     * Clean up storage-related test data
     */
    private void cleanStorageTestData() {
        try {
            jdbcTemplate.execute("DELETE FROM storage_location_print_history WHERE location_id::integer >= 1000");
            jdbcTemplate.execute("DELETE FROM storage_position WHERE id::integer >= 1000");
            jdbcTemplate.execute("DELETE FROM storage_rack WHERE id::integer >= 1000 OR label LIKE 'TEST-%'");
            jdbcTemplate.execute("DELETE FROM storage_shelf WHERE id::integer >= 1000 OR label LIKE 'TEST-%'");
            jdbcTemplate.execute("DELETE FROM storage_device WHERE id::integer >= 1000 OR code LIKE 'TEST-%'");
        } catch (Exception e) {
            logger.warn("Failed to clean storage test data: " + e.getMessage());
        }
    }

    /**
     * Helper: Create a test device and return its ID
     */
    private String createTestDevice() throws Exception {
        StorageDevice device = new StorageDevice();
        device.setCode("TEST-DEVICE-" + System.currentTimeMillis());
        device.setName("Test Device");
        device.setActive(true);
        // Save via service or DAO - using direct SQL for simplicity in test
        jdbcTemplate.update(
            "INSERT INTO storage_device (code, name, active, room_id) VALUES (?, ?, ?, 1)",
            device.getCode(), device.getName(), device.getActive());
        Integer id = jdbcTemplate.queryForObject(
            "SELECT id FROM storage_device WHERE code = ?", Integer.class, device.getCode());
        return String.valueOf(id);
    }

    /**
     * Test: PUT /rest/storage/{type}/{id}/short-code with valid short code
     * Expected: 200 OK with updated short code
     */
    @Test
    public void testPutShortCodeEndpoint_ValidCode_Returns200() throws Exception {
        // Given: Test device exists
        String deviceId = createTestDevice();
        ShortCodeUpdateForm form = new ShortCodeUpdateForm();
        form.setShortCode("FRZ01");

        // When: PUT /rest/storage/device/{id}/short-code
        // Then: Expect 200 OK with short code in response
        mockMvc.perform(put("/rest/storage/device/" + deviceId + "/short-code")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(form)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortCode").value("FRZ01"))
                .andExpect(jsonPath("$.message").exists());
    }

    /**
     * Test: PUT /rest/storage/{type}/{id}/short-code with invalid format
     * Expected: 400 Bad Request with error message
     */
    @Test
    public void testPutShortCodeEndpoint_InvalidFormat_Returns400() throws Exception {
        // Given: Test device exists
        String deviceId = createTestDevice();
        ShortCodeUpdateForm form = new ShortCodeUpdateForm();
        form.setShortCode("INVALID-CODE-TOO-LONG"); // Exceeds 10 characters

        // When: PUT /rest/storage/device/{id}/short-code
        // Then: Expect 400 Bad Request
        mockMvc.perform(put("/rest/storage/device/" + deviceId + "/short-code")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(form)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    /**
     * Test: PUT /rest/storage/{type}/{id}/short-code with location not found
     * Expected: 404 Not Found
     */
    @Test
    public void testPutShortCodeEndpoint_LocationNotFound_Returns404() throws Exception {
        // Given: Non-existent device ID
        ShortCodeUpdateForm form = new ShortCodeUpdateForm();
        form.setShortCode("FRZ01");

        // When: PUT /rest/storage/device/99999/short-code
        // Then: Expect 404 Not Found
        mockMvc.perform(put("/rest/storage/device/99999/short-code")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(form)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Location not found"));
    }

    /**
     * Test: POST /rest/storage/{type}/{id}/print-label generates PDF
     * Expected: 200 OK with PDF content type
     */
    @Test
    public void testPostPrintLabelEndpoint_GeneratesPdf_Returns200() throws Exception {
        // Given: Test device exists
        String deviceId = createTestDevice();

        // When: POST /rest/storage/device/{id}/print-label?shortCode=FRZ01
        // Then: Expect 200 OK with PDF content
        mockMvc.perform(post("/rest/storage/device/" + deviceId + "/print-label")
                .param("shortCode", "FRZ01"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().exists("Content-Disposition"))
                .andExpect(header().exists("Content-Disposition"));
    }

    /**
     * Test: POST /rest/storage/{type}/{id}/print-label tracks print history
     * Expected: Print history is recorded after label generation
     */
    @Test
    public void testPrintHistoryTracking_AfterLabelGeneration_Recorded() throws Exception {
        // Given: Test device exists
        String deviceId = createTestDevice();

        // When: POST /rest/storage/device/{id}/print-label
        mockMvc.perform(post("/rest/storage/device/" + deviceId + "/print-label")
                .param("shortCode", "FRZ01"))
                .andExpect(status().isOk());

        // Then: Print history record exists in database
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM storage_location_print_history WHERE location_id = ? AND location_type = 'device'",
            Integer.class, Integer.parseInt(deviceId));
        assertNotNull("Print history should be recorded", count);
        assertTrue("Print history count should be > 0", count > 0);
    }

    /**
     * Test: POST /rest/storage/{type}/{id}/print-label with invalid type
     * Expected: 400 Bad Request
     */
    @Test
    public void testPostPrintLabelEndpoint_InvalidType_Returns400() throws Exception {
        // Given: Invalid location type
        String deviceId = createTestDevice();

        // When: POST /rest/storage/room/{id}/print-label (room not supported)
        // Then: Expect 400 Bad Request
        mockMvc.perform(post("/rest/storage/room/" + deviceId + "/print-label"))
                .andExpect(status().isBadRequest());
    }

    /**
     * Test: GET /rest/storage/{type}/{id}/print-history returns history
     * Expected: 200 OK with list of print records
     */
    @Test
    public void testGetPrintHistory_ReturnsHistory_Returns200() throws Exception {
        // Given: Test device exists with print history
        String deviceId = createTestDevice();
        // Create print history record
        jdbcTemplate.update(
            "INSERT INTO storage_location_print_history (id, location_type, location_id, short_code, printed_by, printed_date, print_count) " +
            "VALUES (gen_random_uuid(), 'device', ?, 'FRZ01', '1', CURRENT_TIMESTAMP, 1)",
            Integer.parseInt(deviceId));

        // When: GET /rest/storage/device/{id}/print-history
        // Then: Expect 200 OK with print history list
        mockMvc.perform(get("/rest/storage/device/" + deviceId + "/print-history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    /**
     * Test: PDF generation uses system admin settings for dimensions
     * Expected: Label service uses ConfigurationProperties dimensions
     * Note: This test verifies integration with configuration, actual dimension
     * values are tested in LabelManagementService unit tests
     */
    @Test
    public void testPdfGenerationWithSystemAdminSettings_UsesConfigDimensions() throws Exception {
        // Given: Test device exists
        String deviceId = createTestDevice();

        // When: POST /rest/storage/device/{id}/print-label
        // Then: PDF is generated (dimensions are handled by LabelManagementService)
        // This test verifies the endpoint works, actual dimension testing is in service layer
        mockMvc.perform(post("/rest/storage/device/" + deviceId + "/print-label")
                .param("shortCode", "FRZ01"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"));
    }
}

