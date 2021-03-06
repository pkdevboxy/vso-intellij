// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.services;

import com.microsoft.alm.plugin.idea.settings.TeamServicesSettingsService;
import com.microsoft.alm.plugin.services.PropertyService;

import java.util.Collections;
import java.util.Map;

public class PropertyServiceImpl implements PropertyService {
    private boolean restored = false;
    private Map<String, String> map;

    private static class Holder {
        private static PropertyServiceImpl INSTANCE = new PropertyServiceImpl();
    }

    /**
     * This constructor is protected to allow for testing
     */
    protected PropertyServiceImpl() {
    }

    public static PropertyServiceImpl getInstance() {
        return Holder.INSTANCE;
    }

    @Override
    public String getProperty(final String propertyName) {
        ensureRestored();
        return map.get(propertyName);
    }

    @Override
    public void setProperty(final String propertyName, final String value) {
        ensureRestored();
        map.put(propertyName, value);
    }

    public Map<String, String> getProperties() {
        if (map == null) {
            return null;
        }

        return Collections.unmodifiableMap(map);
    }

    private void ensureRestored() {
        if (!restored) {
            restored = true;
            map = TeamServicesSettingsService.getInstance().restoreProperties();
        }
    }
}
