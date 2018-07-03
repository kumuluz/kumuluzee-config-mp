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
package com.kumuluz.ee.config.microprofile.converters;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Implicit converter based on static methods valueOf({@link String} s) and parse({@link CharSequence} cs).
 *
 * @author Urban Malc
 * @author Jan Meznarič
 * @since 1.2
 */
public class ImplicitMethodConverter<T> extends ImplicitConverter<T> {

    private Method method;

    private Method getConverterMethod(Class<T> tClass, String methodName, Class paramType) throws NoSuchMethodException {
        Method method = tClass.getMethod(methodName, paramType);
        if (!method.isAccessible()) {
            method.setAccessible(true);
        }
        if (Modifier.isStatic(method.getModifiers())) {
            return method;
        }

        throw new NoSuchMethodException();
    }

    public ImplicitMethodConverter(Class<T> tClass) throws NoSuchMethodException {
        method = null;

        try {
            method = getConverterMethod(tClass, "of", String.class);
        } catch (NoSuchMethodException ignored) {
        }

        if (method == null) {
            try {
                method = getConverterMethod(tClass, "valueOf", String.class);
            } catch (NoSuchMethodException ignored) {
            }
        }

        if (method == null) {
            try {
                method = getConverterMethod(tClass, "parse", CharSequence.class);
            } catch (NoSuchMethodException ignored) {
            }
        }

        if (method == null) {
            throw new NoSuchMethodException("Could not find appropriate converter method");
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public T convert(String value) {
        try {
            return (T) method.invoke(null, value);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
