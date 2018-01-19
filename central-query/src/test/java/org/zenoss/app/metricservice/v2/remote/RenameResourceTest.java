package org.zenoss.app.metricservice.v2.remote;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.zenoss.app.metricservice.api.impl.OpenTSDBMetricStorage;
import org.zenoss.app.metricservice.api.impl.OpenTSDBSuggest;
import org.zenoss.app.metricservice.api.impl.OpenTSDBRename;
import org.zenoss.app.metricservice.api.impl.OpenTSDBRenameResult;
import org.zenoss.app.metricservice.api.impl.Utils;
import org.zenoss.app.metricservice.v2.impl.QueryServiceImpl;
import org.zenoss.app.security.ZenossTenant;
import org.zenoss.app.zauthbundle.ZappSecurity;
import com.sun.jersey.api.client.ClientResponse;
import com.yammer.dropwizard.validation.InvalidEntityException;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RenameResourceTest extends ResourceTest {
    private static final ObjectMapper MAPPER = Utils.getObjectMapper();
    private static final int MOCK_PORT = 4242;
    private static final String URL_PATH = "/api/v2/performance/rename";
    private static final String OTSDB_RENAME_PATH = "/api/uid/rename";
    private static final String OTSDB_SUGGEST_PATH = "/api/suggest";

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
    public void testMetricWhole() throws IOException, JSONException {
        String expectedResultFile = "/rename/metricwhole/result.json";
        String metricRequestFile = "/rename/metricwhole/request.json";
        String suggestInteraction = "/rename/metricwhole/suggestInteraction.json";
        String otsdbInteraction = "/rename/metricwhole/otsdbInteraction.json";

        testRename(expectedResultFile, metricRequestFile, suggestInteraction, otsdbInteraction);
    }

    @Test
    public void testMetricWholeNameNotFound() throws IOException, JSONException {
        String metricRequestFile = "/rename/metricwholenamenotfound/request.json";
        String suggestInteraction = "/rename/metricwholenamenotfound/suggestInteraction.json";

        testNoResult(metricRequestFile, suggestInteraction);
    }

    @Test
    public void testMetricWholeNewNameAlreadyAssigned() throws IOException, JSONException {
        String expectedResultFile =
            "/rename/metricwholenewnamealreadyassigned/result.json";
        String metricRequestFile =
            "/rename/metricwholenewnamealreadyassigned/request.json";
        String suggestInteraction =
            "/rename/metricwholenewnamealreadyassigned/suggestInteraction.json";
        String otsdbInteraction =
            "/rename/metricwholenewnamealreadyassigned/otsdbInteraction.json";

        testBadRequest(expectedResultFile, metricRequestFile, suggestInteraction, otsdbInteraction);
    }

    @Test
    public void testTagvWhole() throws IOException, JSONException {
        String expectedResultFile = "/rename/tagvwhole/result.json";
        String metricRequestFile = "/rename/tagvwhole/request.json";
        String suggestInteraction = "/rename/tagvwhole/suggestInteraction.json";
        String otsdbInteraction = "/rename/tagvwhole/otsdbInteraction.json";

        testRename(expectedResultFile, metricRequestFile, suggestInteraction, otsdbInteraction);
    }

    @Test
    public void testTagvWholeNameNotFound() throws IOException, JSONException {
        String metricRequestFile = "/rename/tagvwholenamenotfound/request.json";
        String suggestInteraction = "/rename/tagvwholenamenotfound/suggestInteraction.json";

        testNoResult(metricRequestFile, suggestInteraction);
    }

    @Test
    public void testTagvWholeNewNameAlreadyAssigned() throws IOException, JSONException {
        String expectedResultFile =
            "/rename/tagvwholenewnamealreadyassigned/result.json";
        String metricRequestFile =
            "/rename/tagvwholenewnamealreadyassigned/request.json";
        String suggestInteraction =
            "/rename/tagvwholenewnamealreadyassigned/suggestInteraction.json";
        String otsdbInteraction =
            "/rename/tagvwholenewnamealreadyassigned/otsdbInteraction.json";

        testBadRequest(expectedResultFile, metricRequestFile, suggestInteraction, otsdbInteraction);
    }

    @Test
    public void testMetricPrefix() throws IOException, JSONException {
        String expectedResultFile = "/rename/metricprefix/result.json";
        String metricRequestFile = "/rename/metricprefix/request.json";
        String suggestInteraction = "/rename/metricprefix/suggestInteraction.json";
        String rename1Interaction = "/rename/metricprefix/rename1Interaction.json";
        String rename2Interaction = "/rename/metricprefix/rename2Interaction.json";

        testRenamePrefix(
            expectedResultFile,
            metricRequestFile,
            suggestInteraction,
            rename1Interaction,
            rename2Interaction
        );
    }

    @Test
    public void testMetricPrefixNotFound() throws IOException, JSONException {
        String metricRequestFile = "/rename/metricprefixnotfound/request.json";
        String suggestInteraction = "/rename/metricprefixnotfound/suggestInteraction.json";

        testNoResult(
            metricRequestFile,
            suggestInteraction
        );
    }

    @Test
    public void testTagvPrefix() throws IOException, JSONException {
        String expectedResultFile = "/rename/tagvprefix/result.json";
        String metricRequestFile = "/rename/tagvprefix/request.json";
        String suggestInteraction = "/rename/tagvprefix/suggestInteraction.json";
        String rename1Interaction = "/rename/tagvprefix/rename1Interaction.json";
        String rename2Interaction = "/rename/tagvprefix/rename2Interaction.json";

        testRenamePrefix(
            expectedResultFile,
            metricRequestFile,
            suggestInteraction,
            rename1Interaction,
            rename2Interaction
        );
    }

    @Test(expected = InvalidEntityException.class)
    public void testBadPatternType() throws IOException, JSONException {
        String metricRequestFile = "/rename/badpatterntype/request.json";

        testUnprocessableEntity(
            metricRequestFile
        );
    }

    @Test(expected = InvalidEntityException.class)
    public void testBadType() throws IOException, JSONException {
        String metricRequestFile = "/rename/badtype/request.json";

        testUnprocessableEntity(
            metricRequestFile
        );
    }

    @Test(expected = InvalidEntityException.class)
    public void testMissingType() throws IOException, JSONException {
        String metricRequestFile = "/rename/missingtype/request.json";

        testUnprocessableEntity(
            metricRequestFile
        );
    }

    @Test(expected = InvalidEntityException.class)
    public void testMissingPatternType() throws IOException, JSONException {
        String metricRequestFile = "/rename/missingpatterntype/request.json";

        testUnprocessableEntity(
            metricRequestFile
        );
    }

    /**
     * posts a whole metric rename request and verifies results.  OpenTSDB interaction needs to "mocked" out in infiles
     *
     * @param expectedResultFile    File that contains the expected json
     * @param metricRequestFile     File that contains the metric request to make
     * @param otsdbInteractionFiles File(s) that contains the opentsdb request that is expected to be made and the mock result.
     *                              See {@link OtsdbInteraction} class.
     * @throws IOException          Thrown by CharStreams.toString() and Utils.getObjectMapper()
     * @throws JSONException        Thrown by JSONCompare
     */
    private void testRename(String expectedResultFile, String metricRequestFile, String... otsdbInteractionFiles) throws IOException, JSONException {
        InputStream input = this.getClass().getResourceAsStream(expectedResultFile);
        String expectedJSON = CharStreams.toString(new InputStreamReader(input));

        input = this.getClass().getResourceAsStream(metricRequestFile);
        String metricRequest = CharStreams.toString(new InputStreamReader(input));

        for (String otsdbInteraction : otsdbInteractionFiles) {
            input = this.getClass().getResourceAsStream(otsdbInteraction);
            String interactionJson = CharStreams.toString(new InputStreamReader(input));
            String otsdbRequest;
            String otsdbResponse;
            String path;
            try {
                OtsdbInteraction interaction = MAPPER.readValue(interactionJson, OtsdbInteraction.class);
                otsdbRequest = Utils.jsonStringFromObject(interaction.request);
                otsdbResponse = Utils.jsonStringFromObject(interaction.response);
                path = OTSDB_RENAME_PATH;
            } catch (JsonMappingException e) {
                SuggestInteraction interaction = MAPPER.readValue(interactionJson, SuggestInteraction.class);
                otsdbRequest = Utils.jsonStringFromObject(interaction.request);
                otsdbResponse = Utils.jsonStringFromObject(interaction.response);
                path = OTSDB_SUGGEST_PATH;
            }

            stubFor(post(urlEqualTo(path))
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

    /**
     * posts a metric prefix rename request and verifies results.  OpenTSDB interaction needs to "mocked" out in infiles
     *
     * @param expectedResultFile    File that contains the expected json
     * @param metricRequestFile     File that contains the metric request to make
     * @param otsdbInteractionFiles File(s) that contains the opentsdb request that is expected to be made and the mock result.
     *                              See {@link OtsdbInteraction} class.
     * @throws IOException          Thrown by CharStreams.toString() and Utils.getObjectMapper()
     * @throws JSONException        Thrown by JSONCompare
     */    private void testRenamePrefix(String expectedResultFile, String metricRequestFile, String... otsdbInteractionFiles) throws IOException, JSONException {
        InputStream input = this.getClass().getResourceAsStream(expectedResultFile);
        String expected = CharStreams.toString(new InputStreamReader(input));

        input = this.getClass().getResourceAsStream(metricRequestFile);
        String metricRequest = CharStreams.toString(new InputStreamReader(input));

        for (String otsdbInteraction : otsdbInteractionFiles) {
            input = this.getClass().getResourceAsStream(otsdbInteraction);
            String interactionJson = CharStreams.toString(new InputStreamReader(input));
            String otsdbRequest;
            String otsdbResponse;
            String path;
            try {
                OtsdbInteraction interaction = MAPPER.readValue(interactionJson, OtsdbInteraction.class);
                otsdbRequest = Utils.jsonStringFromObject(interaction.request);
                otsdbResponse = Utils.jsonStringFromObject(interaction.response);
                path = OTSDB_RENAME_PATH;
            } catch (JsonMappingException e) {
                SuggestInteraction interaction = MAPPER.readValue(interactionJson, SuggestInteraction.class);
                otsdbRequest = Utils.jsonStringFromObject(interaction.request);
                otsdbResponse = Utils.jsonStringFromObject(interaction.response);
                path = OTSDB_SUGGEST_PATH;
            }

            stubFor(post(urlEqualTo(path))
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
        assertEquals(expected, qr);
    }

    /**
     * posts a bad request to OpenTSDB and verifies results.  OpenTSDB interaction needs to "mocked" out in infiles
     *
     * @param expectedResultFile    File that contains the expected json
     * @param metricRequestFile     File that contains the metric request to make
     * @param otsdbInteractionFiles File(s) that contains the opentsdb request that is expected to be made and the mock result.
     *                              See {@link OtsdbInteraction} class.
     * @throws IOException          Thrown by CharStreams.toString() and Utils.getObjectMapper()
     * @throws JSONException        Thrown by JSONCompare
     */
    private void testBadRequest(String expectedResultFile, String metricRequestFile, String... otsdbInteractionFiles) throws IOException, JSONException {
        InputStream input = this.getClass().getResourceAsStream(expectedResultFile);
        String expectedJSON = CharStreams.toString(new InputStreamReader(input));

        input = this.getClass().getResourceAsStream(metricRequestFile);
        String metricRequest = CharStreams.toString(new InputStreamReader(input));

        for (String otsdbInteraction : otsdbInteractionFiles) {
            input = this.getClass().getResourceAsStream(otsdbInteraction);
            String interactionJson = CharStreams.toString(new InputStreamReader(input));
            String otsdbRequest;
            String otsdbResponse;
            String path;
            try {
                OtsdbInteraction interaction = MAPPER.readValue(interactionJson, OtsdbInteraction.class);
                otsdbRequest = Utils.jsonStringFromObject(interaction.request);
                otsdbResponse = Utils.jsonStringFromObject(interaction.response);
                path = OTSDB_RENAME_PATH;
            } catch (JsonMappingException e) {
                SuggestInteraction interaction = MAPPER.readValue(interactionJson, SuggestInteraction.class);
                otsdbRequest = Utils.jsonStringFromObject(interaction.request);
                otsdbResponse = Utils.jsonStringFromObject(interaction.response);
                path = OTSDB_SUGGEST_PATH;
            }

            stubFor(post(urlEqualTo(path))
                    .withHeader(HttpHeaders.CONTENT_TYPE, matching("application/json"))
                    .withRequestBody(equalToJson(otsdbRequest))
                    .willReturn(aResponse()
                            .withStatus(400)
                            .withHeader("Content-Type", "application/json")
                            .withBody(otsdbResponse)));
        }

        String qr = client().resource(URL_PATH)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .post(String.class, metricRequest);

        assertNotNull(qr);
        assertJsonEquals(expectedJSON, qr);
    }

    private void testNoResult(String metricRequestFile, String... otsdbInteractionFiles) throws IOException, JSONException {
        InputStream input = this.getClass().getResourceAsStream(metricRequestFile);
        String metricRequest = CharStreams.toString(new InputStreamReader(input));

        for (String otsdbInteraction : otsdbInteractionFiles) {
            input = this.getClass().getResourceAsStream(otsdbInteraction);
            String interactionJson = CharStreams.toString(new InputStreamReader(input));
            String otsdbRequest;
            String otsdbResponse;
            String path;
            try {
                OtsdbInteraction interaction = MAPPER.readValue(interactionJson, OtsdbInteraction.class);
                otsdbRequest = Utils.jsonStringFromObject(interaction.request);
                otsdbResponse = Utils.jsonStringFromObject(interaction.response);
                path = OTSDB_RENAME_PATH;
            } catch (JsonMappingException e) {
                SuggestInteraction interaction = MAPPER.readValue(interactionJson, SuggestInteraction.class);
                otsdbRequest = Utils.jsonStringFromObject(interaction.request);
                otsdbResponse = Utils.jsonStringFromObject(interaction.response);
                path = OTSDB_SUGGEST_PATH;
            }

            stubFor(post(urlEqualTo(path))
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

        assertEquals("", qr);
    }

    /**
     * posts unprocessable entity to central query and verifies results.  OpenTSDB interaction needs to "mocked" out in infiles
     *
     * @param expectedResultFile    File that contains the expected json
     * @param metricRequestFile     File that contains the metric request to make
     * @param otsdbInteractionFiles File(s) that contains the opentsdb request that is expected to be made and the mock result.
     *                              See {@link OtsdbInteraction} class.
     * @throws IOException          Thrown by CharStreams.toString() and Utils.getObjectMapper()
     * @throws JSONException        Thrown by JSONCompare
     */
    private void testUnprocessableEntity(String metricRequestFile) throws IOException, JSONException {
        InputStream input = this.getClass().getResourceAsStream(metricRequestFile);
        String metricRequest = CharStreams.toString(new InputStreamReader(input));

        ClientResponse qr = client().resource(URL_PATH)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .post(ClientResponse.class, metricRequest);
        assertNotNull(qr);
        assertEquals(qr.getStatus(), 422);
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
        OpenTSDBRename request;
        @JsonProperty
        OpenTSDBRenameResult response;
    }

    private static class SuggestInteraction {
        @JsonProperty
        OpenTSDBSuggest request;
        @JsonProperty
        ArrayList<String> response;
    }

    private static void assertJsonEquals(String expectedJson, String actualJson) throws IOException {
        // this json comparison method will provide information on expected and actual values
        // if the respective JsonNodes are not equal.
        JsonNode expectedNode = MAPPER.readTree(expectedJson);
        JsonNode actualNode = MAPPER.readTree(actualJson);
        assertEquals(expectedNode, actualNode);
    }
}
