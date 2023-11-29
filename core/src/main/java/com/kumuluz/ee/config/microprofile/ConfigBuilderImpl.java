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

import com.kumuluz.ee.config.microprofile.adapters.ConfigSourceAdapter;
import com.kumuluz.ee.config.microprofile.converters.*;
import com.kumuluz.ee.config.microprofile.utils.AlternativeTypesUtil;
import com.kumuluz.ee.config.microprofile.utils.ConverterWithOrdinal;
import com.kumuluz.ee.configuration.ConfigurationSource;
import com.kumuluz.ee.configuration.sources.EnvironmentConfigurationSource;
import com.kumuluz.ee.configuration.sources.FileConfigurationSource;
import com.kumuluz.ee.configuration.sources.SystemPropertyConfigurationSource;
import com.kumuluz.ee.configuration.utils.ConfigurationUtil;
import jakarta.annotation.Priority;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.Converter;

import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

/**
 * Implementation of {@link ConfigBuilder}.
 *
 * @author Urban Malc
 * @since 1.3
 */
public class ConfigBuilderImpl implements ConfigBuilder {

    private static final List<Class<? extends ConfigurationSource>> DEFAULT_SOURCES = List.of(
            SystemPropertyConfigurationSource.class,
            EnvironmentConfigurationSource.class,
            FileConfigurationSource.class
    );
    private static final ConfigurationUtil CONFIGURATION_UTIL = ConfigurationUtil.getInstance();

    private final Map<Type, ConverterWithOrdinal<?>> converters;
    private final List<ConfigSource> configSources;

    public ConfigBuilderImpl() {

        this.configSources = new ArrayList<>();
        this.converters = new HashMap<>();

        converters.put(Boolean.class, getConverterWithOrdinalAnnotation(BooleanConverter.INSTANCE));
        converters.put(Integer.class, getConverterWithOrdinalAnnotation(IntegerConverter.INSTANCE));
        converters.put(Long.class, getConverterWithOrdinalAnnotation(LongConverter.INSTANCE));
        converters.put(Float.class, getConverterWithOrdinalAnnotation(FloatConverter.INSTANCE));
        converters.put(Double.class, getConverterWithOrdinalAnnotation(DoubleConverter.INSTANCE));
        converters.put(Class.class, getConverterWithOrdinalAnnotation(ClassConverter.INSTANCE));
        converters.put(Character.class, getConverterWithOrdinalAnnotation(CharacterConverter.INSTANCE));
        converters.put(Byte.class, getConverterWithOrdinalAnnotation(ByteConverter.INSTANCE));
        converters.put(Short.class, getConverterWithOrdinalAnnotation(ShortConverter.INSTANCE));
        converters.put(OptionalDouble.class, getConverterWithOrdinalAnnotation(OptionalDoubleConverter.INSTANCE));
        converters.put(OptionalInt.class, getConverterWithOrdinalAnnotation(OptionalIntConverter.INSTANCE));
        converters.put(OptionalLong.class, getConverterWithOrdinalAnnotation(OptionalLongConverter.INSTANCE));
    }

    @Override
    public ConfigBuilder addDefaultSources() {
        CONFIGURATION_UTIL.getConfigurationSources().stream()
                .filter((cs) -> DEFAULT_SOURCES.contains(cs.getClass()))
                .map(ConfigSourceAdapter::new)
                .forEach(configSources::add);

        return this;
    }

    @Override
    public ConfigBuilder addDiscoveredSources() {
        CONFIGURATION_UTIL.getConfigurationSources().stream()
                .filter((cs) -> !DEFAULT_SOURCES.contains(cs.getClass()))
                .map(ConfigSourceAdapter::new)
                .forEach(configSources::add);

        return this;
    }

    @Override
    public ConfigBuilder addDiscoveredConverters() {
        // load Converters
        for (Converter<?> converter : ServiceLoader.load(Converter.class)) {
            withConverters(converter);
        }

        return this;
    }

    @Override
    public ConfigBuilder forClassLoader(ClassLoader classLoader) {
        return this;
    }

    @Override
    public ConfigBuilder withSources(ConfigSource... configSources) {
        this.configSources.addAll(Arrays.asList(configSources));

        return this;
    }

    @Override
    public ConfigBuilder withConverters(Converter<?>... converters) {
        for (Converter<?> converter : converters) {
            Type[] types = converter.getClass().getGenericInterfaces();
            for (Type type : types) {
                if (type instanceof ParameterizedType) {
                    Type[] args = ((ParameterizedType) type).getActualTypeArguments();
                    if (args.length == 1) {
                        Type cType = args[0];
                        int ordinal = getConverterOrdinal(converter);
                        withConverter((Class) cType, ordinal, converter);
                    }
                }
            }
        }

        return this;
    }

    @Override
    public <T> ConfigBuilder withConverter(Class<T> cType, int newOrdinal, Converter<T> converter) {
        ConverterWithOrdinal<?> old = converters.get(cType);
        if (old == null || newOrdinal > old.getOrdinal()) {
            this.converters.put(cType, new ConverterWithOrdinal<>(converter, newOrdinal));
        }

        return this;
    }

    @Override
    public Config build() {
        this.configSources.sort(Comparator.comparingInt(ConfigSource::getOrdinal).reversed());

        Map<Type, Converter<?>> actualConverters = new HashMap<>();
        this.converters.forEach((key, value) -> {
            actualConverters.put(key, value.getConverter());
            actualConverters.put(Array.newInstance((Class<?>) key, 0).getClass(), new ArrayConverter<>(value.getConverter(), (Class) key));
            AlternativeTypesUtil.getPrimitiveFromType(key).ifPresent(primitive ->
                    actualConverters.put(Array.newInstance((Class<?>) primitive, 0).getClass(),
                            new ArrayConverter<>(value.getConverter(), (Class) primitive)));
        });

        return new ConfigImpl(this.configSources, actualConverters);
    }

    private <T> ConverterWithOrdinal<T> getConverterWithOrdinalAnnotation(Converter<T> converter) {
        return new ConverterWithOrdinal<>(converter, getConverterOrdinal(converter));
    }

    private int getConverterOrdinal(Converter<?> converter) {

        int result = 100;
        Priority annotation = converter.getClass().getAnnotation(Priority.class);
        if (annotation != null) {
            result = annotation.value();
        }
        return result;
    }
}
