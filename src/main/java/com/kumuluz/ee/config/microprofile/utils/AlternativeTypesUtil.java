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

    private static final Map<Type, Type> ALTERNATIVE_TYPES = new HashMap<>();

    static {
        // ignore primitive types, since CDI already correctly maps them
        // this ensures that producers don't overlap (eg. one for boolean.class and one for Boolean.class)
        ALTERNATIVE_TYPES.put(boolean.class, Boolean.class);
        ALTERNATIVE_TYPES.put(int.class, Integer.class);
        ALTERNATIVE_TYPES.put(long.class, Long.class);
        ALTERNATIVE_TYPES.put(float.class, Float.class);
        ALTERNATIVE_TYPES.put(double.class, Double.class);
        ALTERNATIVE_TYPES.put(byte.class, Byte.class);
        ALTERNATIVE_TYPES.put(char.class, Character.class);
        ALTERNATIVE_TYPES.put(short.class, Short.class);
    }

    public static Optional<Type> getTypeFromPrimitive(Type primitive) {
        return Optional.ofNullable(ALTERNATIVE_TYPES.get(primitive));
    }
}
