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
package com.kumuluz.ee.config.microprofile.tests;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.*;

/**
 * Tests that check MP Config array/list injection from KumuluzEE's config.yaml
 *
 * @author Yog Sothoth
 * @since 1.4
 */
@Test
public class ArrayInjectionTest extends Arquillian {

    private static final String[] TEST_TARGET =
            new String[]{"one ", " two", " [three, four] "};

    @Inject
    @ConfigProperty(name = "parameter.arrayParameter")
    private List<String> listParam;

    @Inject
    @ConfigProperty(name = "parameter.arrayParameter")
    private Set<String> setParam;

    @Inject
    @ConfigProperty(name = "parameter.arrayParameter")
    private String[] arrayParam;

    @Inject
    @ConfigProperty(name = "parameter.arrayParameter")
    private Optional<String[]> arrayParamOpt;

    @Inject
    @ConfigProperty(name = "parameter.non-existent")
    private Optional<String[]> arrayParamEmptyOpt;

    @Inject
    @ConfigProperty(name = "parameter.arrayParameter")
    private Optional<List<String>> listParamOpt;

    @Inject
    @ConfigProperty(name = "parameter.non-existent")
    private Optional<List<String>> listParamEmptyOpt;

    @Inject
    @ConfigProperty(name = "parameter.arrayParameter")
    private Optional<Set<String>> setParamOpt;

    @Inject
    @ConfigProperty(name = "parameter.non-existent")
    private Optional<Set<String>> setParamEmptyOpt;

    @Inject
    @ConfigProperty(name = "parameter.stringParameter")
    private Optional<String> stringParam;

    @Inject
    @ConfigProperty(name = "parameter.arrayParameter[0]")
    private String arrayItemParam;

    @Deployment
    public static JavaArchive deploy() {
        return ShrinkWrap
                .create(JavaArchive.class, "arrayInjectionTest.jar")
                .addClasses(ArrayInjectionTest.class)
                .addAsResource("config.yaml")
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .as(JavaArchive.class);
    }

    @Test
    public void testNotParsingDelimeters() {
        Assert.assertEquals("[here be,  dragons]", stringParam.get());
    }

    @Test
    public void testArrayItemInjection() {
        Assert.assertEquals(TEST_TARGET[0], arrayItemParam);
    }

    @Test
    public void testArrayInjection() {
        Assert.assertTrue(arrayParamOpt.isPresent());
        Assert.assertEquals(TEST_TARGET, arrayParamOpt.get());
        Assert.assertEquals(TEST_TARGET, arrayParam);
    }

    @Test
    public void testListInjection() {
        Assert.assertEquals(Arrays.asList(TEST_TARGET), listParam);

        Assert.assertTrue(listParamOpt.isPresent());
        Assert.assertEquals(Arrays.asList(TEST_TARGET), listParamOpt.get());
    }

    @Test
    public void testSetInjection() {
        Assert.assertEquals(new HashSet<>(Arrays.asList(TEST_TARGET)), setParam);

        Assert.assertTrue(setParamOpt.isPresent());
        Assert.assertEquals(new HashSet<>(Arrays.asList(TEST_TARGET)), setParamOpt.get());
    }

    @Test
    public void testEmptyOptionalInjection() {
        Assert.assertFalse(arrayParamEmptyOpt.isPresent());
        Assert.assertFalse(listParamEmptyOpt.isPresent());
        Assert.assertFalse(setParamEmptyOpt.isPresent());
    }
}
