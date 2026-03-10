/**
 * Shared constants for analyzer E2E tests.
 *
 * Stable harness-backed tests use the minimal GeneXpert fixture from
 * src/test/resources/analyzer-minimal.sql (analyzer 2013 at 172.20.1.100:9600),
 * which matches projects/analyzer-harness/docker-compose.analyzer-test.yml.
 * Newly created analyzers are used only in the promotion-gate spec after
 * the fixture-backed lane is green.
 */
export const GENEXPERT_FIXTURE_ID = "2013";

/** Host/port for harness ASTM mock (same as fixture 2013). Used by promotion-gate. */
export const HARNESS_MOCK_HOST = "172.20.1.100";
export const HARNESS_MOCK_PORT = "9600";
