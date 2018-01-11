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
package com.kumuluz.ee.config.microprofile.cdi;

import com.kumuluz.ee.config.microprofile.ConfigImpl;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.spi.AnnotatedMember;
import javax.enterprise.inject.spi.DeploymentException;
import javax.enterprise.inject.spi.InjectionPoint;
import java.util.Optional;

/**
 * Config property producer produces config values from config instance.
 *
 * @author Urban Malc
 * @author Jan Meznarič
 * @since 1.1
 */
public class ConfigPropertyProducer {

    @Dependent
    @ConfigProperty
    public static Object getGenericProperty(InjectionPoint ip) {
        ConfigProperty configPropertyAnnotation = ip.getAnnotated().getAnnotation(ConfigProperty.class);

        // get config key
        String configurationPropertyKey = configPropertyAnnotation.name();
        if (configurationPropertyKey.isEmpty()) {

            if (ip.getAnnotated() instanceof AnnotatedMember) {
                AnnotatedMember member = (AnnotatedMember) ip.getAnnotated();
                if (member.getDeclaringType() != null) {
                    configurationPropertyKey = member.getDeclaringType().getJavaClass().getCanonicalName() + "." +
                            member.getJavaMember().getName();
                }
            }
        }

        // get config value
        ConfigImpl config = (ConfigImpl) ConfigProvider.getConfig();
        Class<?> configurationPropertyType = (Class<?>) ip.getType();
        Optional resultOpt = config.getOptionalValue(configurationPropertyKey, configurationPropertyType);

        Object configurationPropertyValue;
        if (resultOpt.isPresent()) {
            configurationPropertyValue = resultOpt.get();
        } else {
            // no value found, try default value
            configurationPropertyValue = config.convert(configPropertyAnnotation.defaultValue(),
                    configurationPropertyType);

            if (configPropertyAnnotation.defaultValue().equals(ConfigProperty.UNCONFIGURED_VALUE) ||
                    configurationPropertyValue == null) {
                throw new DeploymentException("Microprofile Config Property " + configPropertyAnnotation.name() +
                        " can not be found.");
            }
        }
        return configurationPropertyValue;
    }
}
