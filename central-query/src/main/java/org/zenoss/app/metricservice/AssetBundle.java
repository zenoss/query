package org.zenoss.app.metricservice;

import com.google.common.base.Optional;
import io.dropwizard.assets.AssetsBundle;
import org.zenoss.app.annotations.Bundle;
import org.zenoss.app.autobundle.AutoBundle;

@Bundle
public class AssetBundle implements AutoBundle {

    /*
     * (non-Javadoc)
     *
     * @see org.zenoss.app.autobundle.AutoBundle#getRequiredConfig()
     */
    @SuppressWarnings("rawtypes")
    @Override
    public Optional<Class> getRequiredConfig() {
        return Optional.absent();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.zenoss.app.autobundle.AutoBundle#getBundle()
     */
    @Override
    public io.dropwizard.Bundle getBundle() {
        return new AssetsBundle("/api/", "/static/performance/query/");
    }

}
