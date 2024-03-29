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
package com.kumuluz.ee.config.microprofile.tests;

import org.eclipse.microprofile.config.spi.ConfigSource;

import java.util.Map;
import java.util.Set;

/**
 * Simple config source, returning one value.
 *
 * @author Urban Malc
 * @since 1.3
 */
public class SimpleConfigSource implements ConfigSource {

    @Override
    public Map<String, String> getProperties() {
        return null;
    }

    @Override
    public Set<String> getPropertyNames() {
        return Set.of("kumuluz.integration.test");
    }

    @Override
    public String getValue(String s) {
        if (s.equals("kumuluz.integration.test")) {
            return "ok";
        }

        return null;
    }

    @Override
    public String getName() {
        return "Integration test source";
    }

    @Override
    public int getOrdinal() {
        return 100;
    }
}
