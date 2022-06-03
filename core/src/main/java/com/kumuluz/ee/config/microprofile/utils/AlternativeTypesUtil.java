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
package com.kumuluz.ee.config.microprofile.utils;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Util for mapping non-primitive types to primitives.
 *
 * @author Urban Malc
 * @since 1.3
 */
public class AlternativeTypesUtil {

    private static final Map<Type, Type> TO_TYPE = new HashMap<>();
    private static final Map<Type, Type> TO_PRIMITIVE = new HashMap<>();

    static {
        // ignore primitive types, since CDI already correctly maps them
        // this ensures that producers don't overlap (eg. one for boolean.class and one for Boolean.class)
        TO_TYPE.put(boolean.class, Boolean.class);
        TO_TYPE.put(int.class, Integer.class);
        TO_TYPE.put(long.class, Long.class);
        TO_TYPE.put(float.class, Float.class);
        TO_TYPE.put(double.class, Double.class);
        TO_TYPE.put(byte.class, Byte.class);
        TO_TYPE.put(char.class, Character.class);
        TO_TYPE.put(short.class, Short.class);

        TO_PRIMITIVE.put(Boolean.class, boolean.class);
        TO_PRIMITIVE.put(Integer.class, int.class);
        TO_PRIMITIVE.put(Long.class, long.class);
        TO_PRIMITIVE.put(Float.class, float.class);
        TO_PRIMITIVE.put(Double.class, double.class);
        TO_PRIMITIVE.put(Byte.class, byte.class);
        TO_PRIMITIVE.put(Character.class, char.class);
        TO_PRIMITIVE.put(Short.class, short.class);
    }

    public static Optional<Type> getTypeFromPrimitive(Type primitive) {
        return Optional.ofNullable(TO_TYPE.get(primitive));
    }

    public static Optional<Type> getPrimitiveFromType(Type primitive) {
        return Optional.ofNullable(TO_PRIMITIVE.get(primitive));
    }
}
