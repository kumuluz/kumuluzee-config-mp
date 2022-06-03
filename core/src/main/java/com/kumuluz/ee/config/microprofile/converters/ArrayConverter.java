package com.kumuluz.ee.config.microprofile.converters;

import org.eclipse.microprofile.config.spi.Converter;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class ArrayConverter<T> implements Converter {

    public static final String ARRAY_SEPARATOR_REGEX = "(?<!\\\\)" + Pattern.quote(",");

    private final Converter<T> elementConverter;
    private final Class<T> elementType;

    public ArrayConverter(Converter<T> elementConverter, Class<T> elementType) {
        this.elementConverter = elementConverter;
        this.elementType = elementType;
    }

    @Override
    public Object convert(String value) throws IllegalArgumentException, NullPointerException {

        List<T> a = convertList(value);

        if (a.isEmpty()) {
            return null;
        }

        Object arr = Array.newInstance(elementType, a.size());
        for (int i = 0; i < a.size(); i++) {
            Array.set(arr, i, a.get(i));
        }

        return arr;
    }

    private List<T> convertList(String value) {

        if (value == null) {
            throw new NullPointerException();
        }

        String[] tokens = value.split(ARRAY_SEPARATOR_REGEX);

        List<T> convertedList = new ArrayList<>(tokens.length);

        for (String token : tokens) {
            token = token.replace("\\,", ",");
            if (!token.isEmpty()) {
                convertedList.add(this.elementConverter.convert(token));
            }
        }

        return convertedList;
    }
}
