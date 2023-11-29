/*
 *  Copyright (c) 2014-2021 Kumuluz and/or its affiliates
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

import jakarta.annotation.Priority;
import org.eclipse.microprofile.config.spi.Converter;

import java.util.OptionalLong;

/**
 * OptionalLong converter.
 *
 * @author Urban Malc
 * @since 2.0
 */
@Priority(1)
public class OptionalLongConverter implements Converter<OptionalLong> {

    public static final OptionalLongConverter INSTANCE = new OptionalLongConverter();

    @Override
    public OptionalLong convert(String value) throws IllegalArgumentException, NullPointerException {

        if (value == null) {
            throw new NullPointerException(); // intentionally, see NullConvertersTest
        }

        return OptionalLong.of(Long.parseLong(value));
    }
}
