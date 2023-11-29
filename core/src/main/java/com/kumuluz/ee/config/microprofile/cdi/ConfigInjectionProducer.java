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
import com.kumuluz.ee.config.microprofile.ConfigValueImpl;
import com.kumuluz.ee.config.microprofile.annotations.OptionalCollectionIP;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.ConfigValue;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.InjectionPoint;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Supplier;

/**
 * Config injection producer.
 *
 * @author Urban Malc
 * @author Jan Meznarič
 * @since 1.1
 */
@ApplicationScoped
public class ConfigInjectionProducer {

    @SuppressWarnings({"unchecked"})
    private static <T> Class<T> getClassFromParameterizedType(ParameterizedType type) {
        return (Class<T>) type.getActualTypeArguments()[0];
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

            return ConfigProvider.getConfig().unwrap(ConfigImpl.class).getOptionalValue(annotation.name(), typeClass,
                    annotation.defaultValue());
        }

        return Optional.empty();
    }

    @Produces
    @ConfigProperty
    public ConfigValue getConfigValueProperty(InjectionPoint injectionPoint) {

        ConfigProperty annotation = injectionPoint.getAnnotated().getAnnotation(ConfigProperty.class);

        ConfigValue configValue = ConfigProvider.getConfig().getConfigValue(annotation.name());

        if (configValue.getSourceName() == null &&
                !ConfigProperty.UNCONFIGURED_VALUE.equals(annotation.defaultValue())) {

            // return default value
            return new ConfigValueImpl(configValue.getName(), annotation.defaultValue(), annotation.defaultValue(),
                    null, 0);
        }

        return configValue;
    }

    @Produces
    @ConfigProperty
    public <T> Supplier<T> getSupplierProperty(InjectionPoint injectionPoint) {

        ConfigProperty annotation = injectionPoint.getAnnotated().getAnnotation(ConfigProperty.class);
        Type type = injectionPoint.getType();
        if (type instanceof ParameterizedType) {
            Class<T> typeClass = getClassFromParameterizedType((ParameterizedType) type);

            return () -> ConfigProvider.getConfig().unwrap(ConfigImpl.class)
                    .getOptionalValue(annotation.name(), typeClass, annotation.defaultValue())
                    .orElse(null);
        }

        return () -> null;
    }

    private <T> List<T> getListProperty(String name, Type type, String defaultValue) {

        if (type instanceof ParameterizedType) {
            Class<T> typeClass = getClassFromParameterizedType((ParameterizedType) type);

            ConfigImpl config = ConfigProvider.getConfig().unwrap(ConfigImpl.class);

            String value = config.getOptionalValue(name, String.class, defaultValue)
                    .orElseThrow(() -> new NoSuchElementException("No configured value found for config key " + name));

            return config.convertList(value, typeClass);
        }

        return null;
    }

    @Produces
    @ConfigProperty
    public <T> List<T> getListProperty(InjectionPoint injectionPoint) {
        ConfigProperty annotation = injectionPoint.getAnnotated().getAnnotation(ConfigProperty.class);
        return getListProperty(annotation.name(), injectionPoint.getType(), annotation.defaultValue());
    }

    @Produces
    @ConfigProperty
    public <T> Set<T> getSetProperty(InjectionPoint injectionPoint) {
        List<T> values = getListProperty(injectionPoint);

        return new HashSet<>(values);
    }

    @Produces
    @ConfigProperty
    @OptionalCollectionIP // this qualifier is dynamically added to injection points with the right type
    public <T> Optional<List<T>> getOptionalListProperty(InjectionPoint injectionPoint) {

        ConfigProperty annotation = injectionPoint.getAnnotated().getAnnotation(ConfigProperty.class);
        if (!(injectionPoint.getType() instanceof ParameterizedType)) {
            return Optional.empty();
        }

        Type innerType = ((ParameterizedType) injectionPoint.getType()).getActualTypeArguments()[0];
        try {
            return Optional.ofNullable(getListProperty(annotation.name(), innerType, annotation.defaultValue()));
        } catch (NoSuchElementException e) {
            return Optional.empty();
        }
    }

    @Produces
    @ConfigProperty
    @OptionalCollectionIP // this qualifier is dynamically added to injection points with the right type
    public <T> Optional<Set<T>> getOptionalSetProperty(InjectionPoint injectionPoint) {

        ConfigProperty annotation = injectionPoint.getAnnotated().getAnnotation(ConfigProperty.class);
        if (!(injectionPoint.getType() instanceof ParameterizedType)) {
            return Optional.empty();
        }

        Type innerType = ((ParameterizedType) injectionPoint.getType()).getActualTypeArguments()[0];
        try {
            List<T> values = getListProperty(annotation.name(), innerType, annotation.defaultValue());
            if (values == null) {
                return Optional.empty();
            }

            return Optional.of(new HashSet<>(values));
        } catch (NoSuchElementException e) {
            return Optional.empty();
        }
    }
}
