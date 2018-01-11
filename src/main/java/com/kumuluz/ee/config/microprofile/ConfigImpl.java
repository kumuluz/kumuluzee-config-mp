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

import com.kumuluz.ee.config.microprofile.converters.*;
import com.kumuluz.ee.configuration.utils.ConfigurationUtil;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.Converter;

import javax.annotation.Priority;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URL;
import java.time.*;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Microprofile Config implementation that exposes KumuluzEE configuration framework.
 *
 * @author Urban Malc
 * @author Jan Meznarič
 * @since 1.1
 */
public class ConfigImpl implements Config, Serializable {

    private Map<Type, Converter> converters = new HashMap<>();
    private ConfigurationUtil configurationUtil = ConfigurationUtil.getInstance();

    private static final String ARRAY_SEPARATOR_REGEX = "(?<!\\\\)" + Pattern.quote(",");

    public ConfigImpl() {
        registerDefaultConverters();
        registerDiscoveredConverters();
    }

    private void registerDefaultConverters() {

        // build-in converters according to specification
        converters.put(Boolean.class, BooleanConverter.INSTANCE);
        converters.put(Integer.class, IntegerConverter.INSTANCE);
        converters.put(Long.class, LongConverter.INSTANCE);
        converters.put(Float.class, FloatConverter.INSTANCE);
        converters.put(Double.class, DoubleConverter.INSTANCE);
        converters.put(Duration.class, DurationConverter.INSTANCE);
        converters.put(LocalTime.class, LocalTimeConverter.INSTANCE);
        converters.put(LocalDate.class, LocalDateConverter.INSTANCE);
        converters.put(LocalDateTime.class, LocalDateTimeConverter.INSTANCE);
        converters.put(OffsetDateTime.class, OffsetDateTimeConverter.INSTANCE);
        converters.put(OffsetTime.class, OffsetTimeConverter.INSTANCE);
        converters.put(Instant.class, InstantConverter.INSTANCE);
        converters.put(Class.class, ClassConverter.INSTANCE);

        // additional converters
        converters.put(String.class, StringConverter.INSTANCE);
        converters.put(URL.class, URLConverter.INSTANCE);
    }

    private void registerDiscoveredConverters() {

        // load Converters
        ServiceLoader<Converter> serviceLoader = ServiceLoader.load(Converter.class);

        for (Converter converter : serviceLoader) {
            Type types[] = converter.getClass().getGenericInterfaces();
            for (Type type : types) {
                if (type instanceof ParameterizedType) {
                    Type args[] = ((ParameterizedType) type).getActualTypeArguments();
                    if (args.length == 1) {
                        Type cType = args[0];
                        Converter old = converters.get(cType);
                        if (old != null) {
                            if (getPriority(converter) > getPriority(old)) {
                                this.converters.put(cType, converter);
                            }
                        } else {
                            this.converters.put(cType, converter);
                        }
                    }
                }
            }
        }
    }

    private int getPriority(Converter converter) {

        int result = 100;
        Priority annotation = converter.getClass().getAnnotation(Priority.class);
        if (annotation != null) {
            result = annotation.value();
        }
        return result;
    }

    @Override
    public <T> Optional<T> getOptionalValue(String propertyName, Class<T> asType) {

        String value = configurationUtil.get(propertyName).orElse(null);

        return Optional.ofNullable(convert(value, asType));
    }

    @Override
    public <T> T getValue(String propertyName, Class<T> propertyType) {

        Optional<T> valueOpt = getOptionalValue(propertyName, propertyType);

        if (!valueOpt.isPresent()) {
            throw new NoSuchElementException("No configured value found for config key " + propertyName);
        }

        return valueOpt.get();
    }


    @SuppressWarnings("unchecked")
    public <T> T convert(String value, Class<T> asType) {

        if (value == null) {
            return null;
        }

        if (asType.isArray()) {
            Class<?> arrayType = asType.getComponentType();
            List a = convertList(value, arrayType);
            Object arr = Array.newInstance(arrayType, a.size());
            for (int i = 0; i < a.size(); i++) {
                Array.set(arr, i, a.get(i));
            }

            return (T)arr;
        }

        Converter<T> converter = getConverter(asType);
        return converter.convert(value);
    }

    public <T> List<T> convertList(String value, Class<T> listType) {

        String[] tokens = value.split(ARRAY_SEPARATOR_REGEX);

        Converter<T> converter = getConverter(listType);
        List<T> convertedList = new ArrayList<>(tokens.length);

        for (String token : tokens) {
            token = token.replace("\\,", ",");
            convertedList.add(converter.convert(token));
        }

        return convertedList;
    }

    @SuppressWarnings("unchecked")
    private <T> Converter<T> getConverter(Class asType) {

        if (asType.equals(boolean.class)) {
            asType = Boolean.class;
        } else if (asType.equals(double.class)) {
            asType = Double.class;
        } else if (asType.equals(float.class)) {
            asType = Float.class;
        } else if (asType.equals(int.class)) {
            asType = Integer.class;
        } else if (asType.equals(long.class)) {
            asType = Long.class;
        }

        Converter converter = converters.get(asType);

        if (converter == null) {
            // no registered converter, try to generate implicit converter
            converter = ImplicitConverter.getImplicitConverter(asType);
        }

        if (converter == null) {
            throw new IllegalArgumentException("No Converter registered for class " + asType);
        }

        return converter;
    }


    @Override
    public Iterable<String> getPropertyNames() {
        return null;
    }

    @Override
    public Iterable<ConfigSource> getConfigSources() {
        return null;
    }
}