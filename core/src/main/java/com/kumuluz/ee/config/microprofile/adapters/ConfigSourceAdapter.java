/*
 *  Copyright (c) 2014-2017 Kumuluz and/or its affiliates
 *  and other contributors as indicated by the @author tags and
 *  the contributor list.
 *
 *  Licensed under the MIT License (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  https://opensource.org/licenses/MIT
 *
 *  The software is provided "AS IS", WITHOUT WARRANTY OF ANY KIND, express or
 *  implied, including but not limited to the warranties of merchantability,
 *  fitness for a particular purpose and noninfringement. in no event shall the
 *  authors or copyright holders be liable for any claim, damages or other
 *  liability, whether in an action of contract, tort or otherwise, arising from,
 *  out of or in connection with the software or the use or other dealings in the
 *  software. See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.kumuluz.ee.config.microprofile.adapters;

import com.kumuluz.ee.configuration.ConfigurationSource;
import org.eclipse.microprofile.config.spi.ConfigSource;

import java.util.*;

/**
 * Adapts KumuluzEE configuration framework {@link ConfigurationSource} to MicroProfile Config {@link ConfigSource}.
 *
 * @author Urban Malc
 * @author Yog Sothoth
 * @since 1.3
 */
public class ConfigSourceAdapter implements ConfigSource {

    private final ConfigurationSource configurationSource;

    public ConfigSourceAdapter(ConfigurationSource configurationSource) {
        this.configurationSource = configurationSource;
    }

    @Override
    public Map<String, String> getProperties() {

        if (configurationSource instanceof ConfigurationSourceAdapter) {
            return ((ConfigurationSourceAdapter) configurationSource).getConfigSource().getProperties();
        }

        return buildPropertiesMap();
    }

    @Override
    public Set<String> getPropertyNames() {

        if (configurationSource instanceof ConfigurationSourceAdapter) {
            return ((ConfigurationSourceAdapter) configurationSource).getConfigSource().getPropertyNames();
        }

        return getProperties().keySet();
    }

    @Override
    public int getOrdinal() {
        return configurationSource.getOrdinal();
    }

    @Override
    public String getValue(String s) {
        String val = configurationSource.get(s).orElse(null);

        if (val != null) {
            return val;
        } else {
            // try list
            Optional<Integer> listSize = this.configurationSource.getListSize(s);

            //this is a list or an array
            if (listSize.isPresent()) {
                //we ignore the returned value and build the array
                return buildArray(s, listSize.get());
            }
        }

        return null;
    }

    @Override
    public String getName() {

        if (configurationSource instanceof ConfigurationSourceAdapter) {
            return ((ConfigurationSourceAdapter) configurationSource).getConfigSource().getName();
        }

        return configurationSource.getClass().getName();
    }

    private Map<String, String> buildPropertiesMap() {
        Map<String, String> properties = new HashMap<>();
        buildPropertiesMap(properties, "");
        return properties;
    }

    private void buildPropertiesMap(Map<String, String> map, String prefix) {
        Optional<List<String>> mapKeys = this.configurationSource.getMapKeys(prefix);

        if (mapKeys.isPresent()) {
            String nextPrefix = (prefix.isEmpty()) ? "" : prefix + ".";
            for (String s : mapKeys.get()) {
                buildPropertiesMap(map, nextPrefix + s);
            }
        } else if (!prefix.isEmpty()) {
            Optional<Integer> listSize = this.configurationSource.getListSize(prefix);

            if (listSize.isPresent()) {
                for (int i = 0; i < listSize.get(); i++) {
                    buildPropertiesMap(map, prefix + "[" + i + "]");
                }
            } else {
                Optional<String> value = this.configurationSource.get(prefix);
                value.ifPresent(s -> map.put(prefix, s));
            }
        }
    }

    private String buildArray(String propertyName, int size) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < size; i++) {
            String prefix = String.format("%s[%d]", propertyName, i);
            Optional<List<String>> objectKeys = this.configurationSource.getMapKeys(prefix);

            if (objectKeys.isEmpty()) {
                Optional<String> item = this.configurationSource.get(String.format("%s[%d]", propertyName, i));
                if (i > 0) {
                    sb.append(',');
                }
                item.ifPresent(sb::append);
            } // else array item is an object, so we just omit it
        }

        return sb.toString();
    }
}
