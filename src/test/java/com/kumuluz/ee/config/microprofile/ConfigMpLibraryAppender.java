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

import com.kumuluz.ee.config.microprofile.cdi.CDIExtension;
import org.jboss.arquillian.container.test.spi.client.deployment.CachedAuxilliaryArchiveAppender;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

/**
 * Packages KumuluzEE Config MicroProfile library as a ShrinkWrap archive and adds it to deployments.
 *
 * @author Urban Malc
 * @since 1.0.0
 */
public class ConfigMpLibraryAppender extends CachedAuxilliaryArchiveAppender {

    @Override
    protected Archive<?> buildArchive() {

        return ShrinkWrap.create(JavaArchive.class, "kumuluzee-config-mp.jar")
                .addPackages(true, MicroprofileConfigExtension.class.getPackage())
                .addAsServiceProvider(com.kumuluz.ee.common.ConfigExtension.class, MicroprofileConfigExtension.class)
                .addAsServiceProvider(javax.enterprise.inject.spi.Extension.class, CDIExtension.class)
                .addAsServiceProvider(org.eclipse.microprofile.config.spi.ConfigProviderResolver.class,
                        DefaultConfigProvider.class)
                .addAsResource("META-INF/beans.xml");
    }
}
