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

import com.kumuluz.ee.config.microprofile.converters.ArrayConverter;
import com.kumuluz.ee.config.microprofile.converters.ImplicitConverter;
import com.kumuluz.ee.config.microprofile.utils.AlternativeTypesUtil;
import com.kumuluz.ee.configuration.utils.ConfigurationInterpolationUtil;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigValue;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.Converter;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Microprofile Config implementation that exposes KumuluzEE configuration framework.
 *
 * @author Urban Malc
 * @author Jan Meznarič
 * @author Yog Sothoth
 * @since 1.1
 */
public class ConfigImpl implements Config, Serializable {

    private final Map<Type, Converter<?>> converters;
    private final List<ConfigSource> configSources;
    private Boolean resolveInterpolations = null;

    private List<String> configurationProfiles;

    public ConfigImpl(List<ConfigSource> configSources, Map<Type, Converter<?>> converters) {
        this.configSources = Collections.unmodifiableList(configSources);
        this.converters = converters;

        this.configurationProfiles = Collections.emptyList();
        this.configurationProfiles = getOptionalValue("kumuluzee.config.profile", String.class)
                .or(() -> getOptionalValue("mp.config.profile", String.class))
                .map(s -> s.split(","))
                .map(Arrays::asList)
                .orElse(Collections.emptyList());
        Collections.reverse(this.configurationProfiles);
    }

    public <T> Optional<T> getOptionalValue(String propertyName, Class<T> asType, String defaultValue) {

        String value;
        ConfigValue configValue = this.getConfigValue(propertyName);

        if (configValue.getSourceName() == null || configValue.getValue().isEmpty()) {
            if (ConfigProperty.UNCONFIGURED_VALUE.equals(defaultValue) || defaultValue == null ||
                    defaultValue.isEmpty()) {

                return Optional.empty();
            } else {
                value = defaultValue;
            }
        } else {
            value = configValue.getValue();
        }

        T convertedValue;
        try {
            convertedValue = convert(value, asType);
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not convert value " + value + " to type " + asType, e);
        }

        return Optional.ofNullable(convertedValue);
    }

    @Override
    public <T> Optional<T> getOptionalValue(String propertyName, Class<T> asType) {
        return getOptionalValue(propertyName, asType, null);
    }

    @Override
    public <T> T getValue(String propertyName, Class<T> propertyType) {

        Optional<T> valueOpt = getOptionalValue(propertyName, propertyType);

        if (valueOpt.isEmpty()) {
            throw new NoSuchElementException("No configured value found for config key " + propertyName);
        }

        return valueOpt.get();
    }

    @Override
    public <T> Optional<List<T>> getOptionalValues(String propertyName, Class<T> asType) {

        Optional<String> value = getOptionalValue(propertyName, String.class);

        if (value.isEmpty()) {
            return Optional.empty();
        }

        List<T> convertedValue;
        try {
            convertedValue = convertList(value.get(), asType);
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not convert value " + value + " to type " + asType, e);
        }

        if (convertedValue == null || convertedValue.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(convertedValue);
    }

    @Override
    public <T> List<T> getValues(String propertyName, Class<T> propertyType) {

        Optional<List<T>> valueOpt = getOptionalValues(propertyName, propertyType);

        if (valueOpt.isEmpty()) {
            throw new NoSuchElementException("No configured value found for config key " + propertyName);
        }

        return valueOpt.get();
    }

    public ConfigValue getConfigValue(String propertyName, boolean resolveInterpolations) {

        String rawValue = null;
        String value = null;
        String configSourceName = null;
        int configSourceOrdinal = 0;

        for (ConfigSource cs : this.configSources) {

            for (String profile : this.configurationProfiles) {
                rawValue = cs.getValue("%" + profile + "." + propertyName);

                if (rawValue != null) {
                    break;
                }
            }

            if (rawValue == null) {
                rawValue = cs.getValue(propertyName);
            }

            if (rawValue != null) {
                configSourceName = cs.getName();
                configSourceOrdinal = cs.getOrdinal();

                if (resolveInterpolations) {
                    value = ConfigurationInterpolationUtil.interpolateString(rawValue,
                            s -> Optional.ofNullable(this.getConfigValue(s, false).getValue())
                    );
                } else {
                    value = rawValue;
                }
                break;
            }
        }

        return new ConfigValueImpl(propertyName, value, rawValue, configSourceName, configSourceOrdinal);
    }

    @Override
    public ConfigValue getConfigValue(String propertyName) {

        if (this.resolveInterpolations == null) {
            this.resolveInterpolations = false;
            this.resolveInterpolations = this
                    .getOptionalValue("mp.config.property.expressions.enabled", boolean.class)
                    .orElse(true);
        }

        return getConfigValue(propertyName, this.resolveInterpolations);
    }

    @SuppressWarnings("unchecked")
    public <T> T convert(String value, Class<T> asType) {

        if (value == null) {
            throw new NullPointerException();
        }

        if (asType.isArray()) {
            Class<?> arrayType = asType.getComponentType();
            List<?> a = convertList(value, arrayType);

            if (a.isEmpty()) {
                return null;
            }

            Object arr = Array.newInstance(arrayType, a.size());
            for (int i = 0; i < a.size(); i++) {
                Array.set(arr, i, a.get(i));
            }

            return (T) arr;
        }

        Converter<T> converter = getConverter(asType)
                .orElseThrow(() -> new IllegalArgumentException("No Converter registered for class " + asType));

        return converter.convert(value);
    }

    public <T> List<T> convertList(String value, Class<T> listType) {

        if (value == null) {
            throw new NullPointerException();
        }

        String[] tokens = value.split(ArrayConverter.ARRAY_SEPARATOR_REGEX);

        Converter<T> converter = getConverter(listType)
                .orElseThrow(() -> new IllegalArgumentException("No Converter registered for class " + listType));
        List<T> convertedList = new ArrayList<>(tokens.length);

        for (String token : tokens) {
            token = token.replace("\\,", ",");
            if (!token.isEmpty()) {
                convertedList.add(converter.convert(token));
            }
        }

        return convertedList;
    }

    @Override
    public Iterable<String> getPropertyNames() {
        return this.configSources.stream().flatMap(e -> e.getPropertyNames().stream()).collect(Collectors.toSet());
    }

    @Override
    public Iterable<ConfigSource> getConfigSources() {
        return this.configSources;
    }

    @Override
    public <T> Optional<Converter<T>> getConverter(Class<T> aClass) {

        Class<?> asType = (Class<?>) AlternativeTypesUtil.getTypeFromPrimitive(aClass).orElse(aClass);

        Converter<?> converter = converters.get(asType);

        if (converter == null) {
            // no registered converter, try to generate implicit converter
            if (aClass.isArray()) {
                Class<?> elementClass = aClass.getComponentType();
                Converter<?> elementImplicitConverter = ImplicitConverter.getImplicitConverter(elementClass);
                converter = new ArrayConverter(elementImplicitConverter, elementClass);
            } else {
                converter = ImplicitConverter.getImplicitConverter(asType);
            }
        }

        if (converter == null) {
            return Optional.empty();
        }

        //noinspection unchecked
        return Optional.of((Converter<T>) converter);
    }

    @Override
    public <T> T unwrap(Class<T> aClass) {

        if (ConfigImpl.class.equals(aClass)) {
            //noinspection unchecked
            return (T) this;
        }

        throw new IllegalArgumentException("Not supported");
    }
}
