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

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;


/**
 * Microprofile Config provider.
 *
 * @author Urban Malc
 * @author Jan Meznarič
 * @since 1.1
 */
public class DefaultConfigProvider extends ConfigProviderResolver {

    private Config instance = null;

    @Override
    public Config getConfig() {
        return getConfig(null);
    }


    @Override
    public Config getConfig(ClassLoader forClassLoader) {

        if (instance == null) {
            instance = getBuilder().addDefaultSources().addDiscoveredSources().addDiscoveredConverters().build();
        }

        return instance;
    }

    @Override
    public void registerConfig(Config config, ClassLoader forClassLoader) {
        instance = config;
    }

    @Override
    public ConfigBuilder getBuilder() {
        return new ConfigBuilderImpl();
    }


    @Override
    public void releaseConfig(Config config) {

        if (instance == config) {
            instance = null;
        }

    }
}
