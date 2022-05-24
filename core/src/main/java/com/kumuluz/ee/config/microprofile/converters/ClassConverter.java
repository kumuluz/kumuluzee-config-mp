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

import org.eclipse.microprofile.config.spi.Converter;

import javax.annotation.Priority;

/**
 * Class converter based on Class.forName.
 *
 * @author Urban Malc
 * @author Jan Meznarič
 * @since 1.2
 */
@Priority(1)
public class ClassConverter implements Converter<Class<?>> {

    public static final ClassConverter INSTANCE = new ClassConverter();

    @Override
    public Class<?> convert(String value) {

        if (value == null) {
            throw new NullPointerException();
        }

        try {
            return Class.forName(value);
        } catch (ClassNotFoundException ignored) {
        }

        return null;
    }
}
