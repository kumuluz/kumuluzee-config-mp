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
package com.kumuluz.ee.config.microprofile;

import com.kumuluz.ee.common.ConfigExtension;
import com.kumuluz.ee.common.config.EeConfig;
import com.kumuluz.ee.common.dependencies.EeExtensionDef;
import com.kumuluz.ee.common.dependencies.EeExtensionGroup;
import com.kumuluz.ee.common.wrapper.KumuluzServerWrapper;
import com.kumuluz.ee.config.microprofile.adapters.ConfigurationSourceAdapter;
import com.kumuluz.ee.configuration.ConfigurationSource;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;

import java.util.LinkedList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.logging.Logger;

/**
 * KumuluzEE Configuration Extension which discovers MicroProfile Config ConfigSource-s, converts them to
 * KumuluzEE ConfigurationSource and adds them to the framework.
 *
 * @author Urban Malc
 * @author Jan Meznarič
 * @since 1.1
 */
@EeExtensionDef(name = "MicroProfile", group = EeExtensionGroup.CONFIG)
public class MicroprofileConfigExtension implements ConfigExtension {

    private static final Logger log = Logger.getLogger(MicroprofileConfigExtension.class.getName());

    private List<ConfigurationSource> configurationSources;

    @Override
    public void init(KumuluzServerWrapper kumuluzServerWrapper, EeConfig eeConfig) {

        log.info("Initialising MicroProfile configuration sources.");

        configurationSources = new LinkedList<>();

        // load MicroProfile configuration sources
        ServiceLoader<ConfigSource> configSourceSL = ServiceLoader.load(ConfigSource.class);
        for (ConfigSource configSource : configSourceSL) {
            configurationSources.add(new ConfigurationSourceAdapter(configSource));
        }

        // load MicroProfile configuration source providers
        ServiceLoader<ConfigSourceProvider> configSourceProviderSL = ServiceLoader.load(ConfigSourceProvider.class);
        for (ConfigSourceProvider configSourceProvider : configSourceProviderSL) {
            for (ConfigSource configSource : configSourceProvider
                    .getConfigSources(MicroprofileConfigExtension.class.getClassLoader())) {
                configurationSources.add(new ConfigurationSourceAdapter(configSource));
            }
        }
    }

    @Override
    public List<ConfigurationSource> getConfigurationSources() {
        return configurationSources;
    }

    @Override
    public ConfigurationSource getConfigurationSource() {
        return null;
    }

    @Override
    public void load() {
    }
}
