package org.zenoss.app.metricservice.v2.remote;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.CharStreams;
import com.yammer.dropwizard.testing.ResourceTest;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.json.JSONException;
import org.junit.Rule;
import org.junit.Test;
import org.zenoss.app.metricservice.MetricServiceAppConfiguration;
import org.zenoss.app.metricservice.api.configs.MetricServiceConfig;
import org.zenoss.app.metricservice.api.impl.OpenTSDBMetricStorage;
import org.zenoss.app.metricservice.api.impl.OpenTSDBQuery;
import org.zenoss.app.metricservice.api.impl.OpenTSDBQueryResult;
import org.zenoss.app.metricservice.api.impl.Utils;
import org.zenoss.app.metricservice.v2.impl.QueryServiceImpl;
import org.zenoss.app.security.ZenossTenant;
import org.zenoss.app.zauthbundle.ZappSecurity;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ResourcesTest extends ResourceTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int MOCK_PORT = 42424;
    private static final String URL_PATH = "/api/v2/performance/query";
    private static final String OTSDB_QUERY_PATH = "/api/query";

    private MetricServiceAppConfiguration configuration;
    private ZappSecurity security;
    private Resources qsr;
    private ZenossTenant tenant;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(MOCK_PORT);

    @Override
    protected void setUpResources() throws Exception {
        configuration = mock(MetricServiceAppConfiguration.class);
        when(configuration.getMetricServiceConfig()).thenReturn(new MetricServiceConfig());
        configuration.getMetricServiceConfig().setIgnoreRateOption(false);
        configuration.getMetricServiceConfig().setOpenTsdbUrl("http://localhost:"+MOCK_PORT);
	
        security = mock(ZappSecurity.class);

        OpenTSDBMetricStorage otsdb = new OpenTSDBMetricStorage();
        otsdb.config = configuration;
        otsdb.startup();

        QueryServiceImpl backend = new QueryServiceImpl();
        backend.config = configuration;
        backend.metricStorage = otsdb;

        qsr = new Resources();
        qsr.configuration = configuration;
        qsr.security = security;
        qsr.api = backend;
        addResource(qsr);
    }

    @Test
    public void testTenant() {
        this.enableMockAuth();
        assertEquals(tenant.id(), this.qsr.getTenantId());

        Map<String, List<String>> _tags = Maps.newHashMap();
        _tags.put("zenoss_tenant_id", Lists.newArrayList("1"));
        assertEquals(_tags, qsr.addTenantId(null));
    }

    @Test
    public void testWildcardQuery() throws IOException, JSONException {
        String expectedResultFile = "/wildcardquery/query1Result.json";
        String metricRequestFile = "/wildcardquery/query1Request.json";

        String otsdbRequestFile = "/wildcardquery/query1OtsdbRequest.json";
        String otsdbRequestFile2 = "/wildcardquery/query1OtsdbRequest2.json";

        testQuery(expectedResultFile, metricRequestFile, otsdbRequestFile, otsdbRequestFile2);
    }

    @Test
    public void testSimpleQuery() throws IOException, JSONException {
        String expectedResultFile = "/simplequery/result.json";
        String metricRequestFile = "/simplequery/request.json";
        String otsdbInteraction = "/simplequery/otsdbInteraction.json";

        testQuery(expectedResultFile, metricRequestFile, otsdbInteraction);
    }

    @Test
    public void testRateOptionsQuery_c_z_v() throws IOException, JSONException {
        String expectedResultFile = "/rateoptionsquery/result.json";
        String metricRequestFile = "/rateoptionsquery/request-c-z-v.json";
        String otsdbInteraction = "/rateoptionsquery/otsdbInteraction-c-z-f.json";

        testQuery(expectedResultFile, metricRequestFile, otsdbInteraction);
    }

    @Test
    public void testRateOptionsQuery_c_n_v() throws IOException, JSONException {
        String expectedResultFile = "/rateoptionsquery/result.json";
        String metricRequestFile = "/rateoptionsquery/request-c-n-v.json";
        String otsdbInteraction = "/rateoptionsquery/otsdbInteraction-c-n-f.json";

        testQuery(expectedResultFile, metricRequestFile, otsdbInteraction);
    }

    @Test
    public void testIgnoreRateOption() throws IOException, JSONException {
        try {
            configuration.getMetricServiceConfig().setIgnoreRateOption(true);

            String expectedResultFile = "/ignorerateoptionquery/result.json";
            String metricRequestFile = "/ignorerateoptionquery/request.json";
            String otsdbInteraction = "/ignorerateoptionquery/otsdbInteraction.json";
            testQuery(expectedResultFile, metricRequestFile, otsdbInteraction);
        } finally {
            configuration.getMetricServiceConfig().setIgnoreRateOption(false);

        }
    }

    @Test
    public void testSpanCutoff() throws IOException, JSONException {
        try {
            configuration.getMetricServiceConfig().setIgnoreRateOption(true);
            configuration.getMetricServiceConfig().setRateOptionCutoffTs(1437520981);
            String expectedResultFile = "/spancutoff/result.json";
            String metricRequestFile = "/spancutoff/request.json";
            String pre_otsdbInteraction = "/spancutoff/precutoff-otsdbInteraction.json";
            String post_otsdbInteraction = "/spancutoff/postcutoff-otsdbInteraction.json";
            String gauge_otsdbInteraction = "/spancutoff/gauge-otsdbInteraction.json";

            testQuery(expectedResultFile, metricRequestFile, pre_otsdbInteraction, post_otsdbInteraction, gauge_otsdbInteraction);
        } catch (Exception e) {
	    e.printStackTrace();
        } finally {
            configuration.getMetricServiceConfig().setIgnoreRateOption(false);

        }
    }

    @Test
    public void testPreCutoff() throws IOException, JSONException {
        try {
            configuration.getMetricServiceConfig().setIgnoreRateOption(true);
            configuration.getMetricServiceConfig().setRateOptionCutoffTs(1437521231);
            String expectedResultFile = "/precutoff/result.json";
            String metricRequestFile = "/precutoff/request.json";
            String pre_otsdbInteraction = "/precutoff/precutoff-otsdbInteraction.json";
            String gauge_otsdbInteraction = "/precutoff/gauge-otsdbInteraction.json";

            testQuery(expectedResultFile, metricRequestFile, pre_otsdbInteraction, gauge_otsdbInteraction);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            configuration.getMetricServiceConfig().setIgnoreRateOption(false);

        }
    }

    @Test
    public void testRateOptionsQuery_c_v_v() throws IOException, JSONException {
        String expectedResultFile = "/rateoptionsquery/result.json";
        String metricRequestFile = "/rateoptionsquery/request-c-v-v.json";
        String otsdbInteraction = "/rateoptionsquery/otsdbInteraction-c-z-t.json";

        testQuery(expectedResultFile, metricRequestFile, otsdbInteraction);
    }

    @Test
    public void testRateOptionsQuery_c_n_t() throws IOException, JSONException {
        String expectedResultFile = "/rateoptionsquery/result.json";
        String metricRequestFile = "/rateoptionsquery/request-c-n-t.json";
        String otsdbInteraction = "/rateoptionsquery/otsdbInteraction-c-n-t.json";
        testQuery(expectedResultFile, metricRequestFile, otsdbInteraction);
    }

    @Test
    public void testLastQuery() throws IOException, JSONException {
        String expectedResultFile = "/lastquery/result.json";
        String metricRequestFile = "/lastquery/request.json";
        String otsdbInteraction = "/lastquery/otsdbInteraction.json";

        testQuery(expectedResultFile, metricRequestFile, otsdbInteraction);
    }

    @Test
    public void testExpressionQuery() throws IOException, JSONException {
        String expectedResultFile = "/expressionquery/result.json";
        String metricRequestFile = "/expressionquery/request.json";
        String otsdbInteraction = "/expressionquery/otsdbInteraction.json";

        testQuery(expectedResultFile, metricRequestFile, otsdbInteraction);
    }

    @Test
    public void testEmptyDataQuery() throws IOException, JSONException {
        String expectedResultFile = "/emptydataquery/query1Result.json";
        String metricRequestFile = "/emptydataquery/query1Request.json";
        String otsdbRequestFile = "/emptydataquery/query1OtsdbRequest.json";

        testQuery(expectedResultFile, metricRequestFile, otsdbRequestFile);
    }

    @Test
    public void testFilterQueryIgnoresTagsIfFiltersPresent() throws IOException, JSONException {
        String expectedResultFile = "/filterquery_ignores_tags/result.json";
        String metricRequestFile = "/filterquery_ignores_tags/request.json";
        String otsdbInteraction = "/filterquery_ignores_tags/otsdbInteraction.json";

        testQuery(expectedResultFile, metricRequestFile, otsdbInteraction);
    }

    @Test
    public void testFilterQueryWithGroupByFalse() throws IOException, JSONException {
        String expectedResultFile = "/filterquery_group_by_false/result.json";
        String metricRequestFile = "/filterquery_group_by_false/request.json";
        String otsdbInteraction = "/filterquery_group_by_false/otsdbInteraction.json";

        testQuery(expectedResultFile, metricRequestFile, otsdbInteraction);
    }

    @Test
    public void testFilterQueryWithGroupByTrue() throws IOException, JSONException {
        String expectedResultFile = "/filterquery_group_by_true/result.json";
        String metricRequestFile = "/filterquery_group_by_true/request.json";
        String otsdbInteraction = "/filterquery_group_by_true/otsdbInteraction.json";

        testQuery(expectedResultFile, metricRequestFile, otsdbInteraction);
    }

    /**
     * posts a metric query and verifies results.  OpenTSDB interaction needs to "mocked" out in infiles
     *
     * @param expectedResultFile    File that contains the expected json
     * @param metricRequestFile     File that contains the metric request to make
     * @param otsdbInteractionFiles File(s) that contains the opentsdb request that is expected to be made and the mock result.
     *                              See {@link OtsdbInteraction} class.
     * @throws IOException   Thrown by CharStreams.toString() and Utils.getObjectMapper()
     * @throws JSONException Thrown by JSONCompare
     */

    private void testQuery(String expectedResultFile, String metricRequestFile, String... otsdbInteractionFiles) throws IOException, JSONException {
        InputStream input = this.getClass().getResourceAsStream(expectedResultFile);
        String expectedJSON = CharStreams.toString(new InputStreamReader(input));

        input = this.getClass().getResourceAsStream(metricRequestFile);
        String metricRequest = CharStreams.toString(new InputStreamReader(input));

        for (String otsdbInteraction : otsdbInteractionFiles) {
            input = this.getClass().getResourceAsStream(otsdbInteraction);
            String interactionJson = CharStreams.toString(new InputStreamReader(input));
            OtsdbInteraction interaction = Utils.getObjectMapper().readValue(interactionJson, OtsdbInteraction.class);
            String otsdbRequest = Utils.jsonStringFromObject(interaction.request);
            String otsdbResponse = Utils.jsonStringFromObject(interaction.response);
            stubFor(post(urlEqualTo(OTSDB_QUERY_PATH))
                    .withHeader(HttpHeaders.CONTENT_TYPE, matching("application/json"))
                    .withRequestBody(equalToJson(otsdbRequest))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(otsdbResponse)));

        }

        String qr = client().resource(URL_PATH)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .post(String.class, metricRequest);

        assertNotNull(qr);
        assertJsonEquals(expectedJSON, qr);
    }

    private void enableMockAuth() {
        when(configuration.isAuthEnabled()).thenReturn(true);

        Subject subject = mock(Subject.class);
        when(security.getSubject()).thenReturn(subject);

        PrincipalCollection collection = mock(PrincipalCollection.class);
        when(subject.getPrincipals()).thenReturn(collection);

        tenant = ZenossTenant.get("1");
        when(collection.oneByType(ZenossTenant.class)).thenReturn(tenant);

    }

    private static class OtsdbInteraction {
        @JsonProperty
        OpenTSDBQuery request;
        @JsonProperty
        List<OpenTSDBQueryResult> response;
    }

    private static void assertJsonEquals(String expectedJson, String actualJson) throws IOException {
        // this json comparison method will provide information on expected and actual values
        // if the respective JsonNodes are not equal.
        JsonNode expectedNode = MAPPER.readTree(expectedJson);
        JsonNode actualNode = MAPPER.readTree(actualJson);
        assertEquals(expectedNode, actualNode);
    }
}
