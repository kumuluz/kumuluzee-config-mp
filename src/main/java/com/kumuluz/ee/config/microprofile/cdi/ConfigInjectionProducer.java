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
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Config injection producer.
 *
 * @author Urban Malc
 * @author Jan Meznarič
 * @since 1.1
 */
@ApplicationScoped
public class ConfigInjectionProducer {

    @SuppressWarnings("unchecked")
    private static <T> Class<T> getClassFromParameterizedType(ParameterizedType type) {
        return (Class) type.getActualTypeArguments()[0];
    }

    private Config config;

    @PostConstruct
    void init() {
        config = ConfigProvider.getConfig();
    }

    @Produces
    @ApplicationScoped
    public Config createConfig() {
        return config;
    }

    @Produces
    @ConfigProperty
    public <T> Optional<T> getOptionalProperty(InjectionPoint injectionPoint) {
        ConfigProperty annotation = injectionPoint.getAnnotated().getAnnotation(ConfigProperty.class);
        Type type = injectionPoint.getType();
        if (type instanceof ParameterizedType) {
            Class<T> typeClass = getClassFromParameterizedType((ParameterizedType) type);

            return ConfigProvider.getConfig().getOptionalValue(annotation.name(), typeClass);
        }

        return Optional.empty();
    }

    @Produces
    @ConfigProperty
    public <T> List<T> getListProperty(InjectionPoint injectionPoint) {
        ConfigProperty annotation = injectionPoint.getAnnotated().getAnnotation(ConfigProperty.class);
        Type type = injectionPoint.getType();
        if (type instanceof ParameterizedType) {
            Class<T> typeClass = getClassFromParameterizedType((ParameterizedType) type);

            Config config = ConfigProvider.getConfig();

            String value = config.getValue(annotation.name(), String.class);

            if (config instanceof ConfigImpl) {
                return ((ConfigImpl) config).convertList(value, typeClass);
            }

        }

        return null;
    }

    @Produces
    @ConfigProperty
    public <T> Set<T> getSetProperty(InjectionPoint injectionPoint) {
        List<T> values = getListProperty(injectionPoint);

        return new HashSet<>(values);
    }

}
