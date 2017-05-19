package org.zenoss.app.metricservice.v2.remote;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.yammer.dropwizard.testing.ResourceTest;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.zenoss.app.AppConfiguration;
import org.zenoss.app.metricservice.MetricServiceAppConfiguration;
import org.zenoss.app.metricservice.api.MetricServiceAPI;
import org.zenoss.app.metricservice.api.configs.MetricServiceConfig;
import org.zenoss.app.metricservice.api.impl.OpenTSDBMetricStorage;
import org.zenoss.app.metricservice.v2.impl.QueryServiceImpl;
import org.zenoss.app.metricservice.v2.remote.Resources;
import org.zenoss.app.security.ZenossTenant;
import org.zenoss.app.zauthbundle.ZappSecurity;

import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.junit.Assert.assertNotNull;

/**
 * Created by maya on 5/18/17.
 */
public class MetricRenameTest extends ResourceTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int MOCK_PORT = 4242;
    private static final String URL_PATH = "/api/v2/performance/rename";

    private MetricServiceAppConfiguration configuration;
    private ZappSecurity security;
    private Resources qsr;
    private ZenossTenant tenant;


    @Rule
    public WireMockRule wireMockRule = new WireMockRule(MOCK_PORT);

    @Test
    public void testChangeName() {
        String renameTest = "{\"oldId\": \"foo\", \"newId\": \"bar\"}";
        String qr = client().resource(URL_PATH)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .post(String.class, renameTest);
        assertNotNull(qr);
    }


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
}
