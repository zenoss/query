package org.zenoss.app.metricservice.api.remote;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.junit.Before;
import org.junit.Test;
import org.zenoss.app.AppConfiguration;
import org.zenoss.app.metricservice.api.MetricServiceAPI;
import org.zenoss.app.security.ZenossTenant;
import org.zenoss.app.zauthbundle.ZappSecurity;

import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MetricResourcesTest {

    AppConfiguration configuration;
    ZappSecurity security;
    MetricServiceAPI api;

    @Before
    public void setUp() {
        configuration = mock(AppConfiguration.class);
        security = mock(ZappSecurity.class);
        api = mock(MetricServiceAPI.class);
    }


    @Test
    public void testGetTagAuth() {
        when(configuration.isAuthEnabled()).thenReturn(true);

        Subject subject = mock( Subject.class);
        when(security.getSubject()).thenReturn( subject);

        PrincipalCollection collection = mock(PrincipalCollection.class);
        when(subject.getPrincipals()).thenReturn( collection);

        ZenossTenant tenant = new ZenossTenant( "1");
        when(collection.oneByType( ZenossTenant.class)).thenReturn(tenant);

        Map<String, List<String>> _tags = Maps.newHashMap();
        _tags.put( "zenoss_tenant_id", Lists.newArrayList( "1"));
        Optional<Map<String, List<String>>> tags = Optional.fromNullable(_tags);

        assertEquals(tags, new MetricResources(configuration, security, api).getTags(null));
    }

    @Test
    public void testGetTagNoAuth() {
        Optional<Map<String, List<String>>> tags = Optional.fromNullable(null);
        when(configuration.isAuthEnabled()).thenReturn(false);
        assertEquals(tags, new MetricResources(configuration, security, api).getTags(null));
    }
}
