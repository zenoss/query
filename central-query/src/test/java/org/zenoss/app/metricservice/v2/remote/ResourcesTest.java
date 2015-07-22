package org.zenoss.app.metricservice.v2.remote;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.CharStreams;
import com.yammer.dropwizard.testing.ResourceTest;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.json.JSONException;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONCompare;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.JSONCompareResult;
import org.zenoss.app.metricservice.MetricServiceAppConfiguration;
import org.zenoss.app.metricservice.api.configs.MetricServiceConfig;
import org.zenoss.app.metricservice.api.impl.OpenTSDBPMetricStorage;
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
    private static final int MOCK_PORT = 4242;
    private static final String URL_PATH = "/api/v2/performance/query";
    private static final String OTSDB_QUERY_PATH = "/api/query";

    MetricServiceAppConfiguration configuration;
    ZappSecurity security;
    private Resources qsr;
    private ZenossTenant tenant;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(MOCK_PORT);


    @Override
    protected void setUpResources() throws Exception {
        configuration = mock(MetricServiceAppConfiguration.class);
        when(configuration.getMetricServiceConfig()).thenReturn(new MetricServiceConfig());

        security = mock(ZappSecurity.class);

        OpenTSDBPMetricStorage otsdb = new OpenTSDBPMetricStorage();
        otsdb.config = configuration;

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
        assertEquals(_tags, qsr.addTentanId(null));

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

    /**
     * posts a metric query and verifies results.  OpenTSDB interaction needs to "mocked" out in infiles
     *
     * @param expectedResultFile    file that contains the expected json
     * @param metricRequestFile     file that contains the metric request to make
     * @param otsdbInteractionFiles File(s) that contains the opentsdb request that is expected to be made and the mock result
     *                              see OtsdbInteraction class
     * @throws IOException
     * @throws JSONException
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

        JSONCompareResult result = JSONCompare.compareJSON(expectedJSON, qr, JSONCompareMode.NON_EXTENSIBLE);
        if (!result.passed()) {
            Assert.fail("test for " + metricRequestFile + " failed: " + result.getMessage());
        }
    }


    private void enableMockAuth() {
        when(configuration.isAuthEnabled()).thenReturn(true);

        Subject subject = mock(Subject.class);
        when(security.getSubject()).thenReturn(subject);

        PrincipalCollection collection = mock(PrincipalCollection.class);
        when(subject.getPrincipals()).thenReturn(collection);

        tenant = new ZenossTenant("1");
        when(collection.oneByType(ZenossTenant.class)).thenReturn(tenant);

    }

    private static class OtsdbInteraction {
        @JsonProperty
        OpenTSDBQuery request;
        @JsonProperty
        List<OpenTSDBQueryResult> response;
    }
}
