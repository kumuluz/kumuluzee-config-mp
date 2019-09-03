package com.kumuluz.ee.config.microprofile.tests;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test
public class ArrayInjectionTest extends Arquillian {
    @Deployment
    public static JavaArchive deploy() {
        JavaArchive testJar = ShrinkWrap
                .create(JavaArchive.class, "arrayInjectionTest.jar")
                .addClasses(ArrayInjectionTest.class)
                .addAsResource("config.yaml")
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .as(JavaArchive.class);
            return testJar;
    }

	@Inject
	@ConfigProperty(name="parameter.arrayParameter")
	private List<String> listParam;

	@Inject
	@ConfigProperty(name="parameter.arrayParameter")
	private String[] arrayParam;

	@Inject
	@ConfigProperty(name="parameter.arrayParameter")
	private Optional<String[]> arrayParamOpt;

	@Inject
	@ConfigProperty(name="parameter.stringParameter")
	private Optional<String> stringParam;

	private static final String[] TEST_TARGET =
			new String[]{"one ", " two", " [three, four] "};
	
    @Test
    public void testNotParsingDelimeters() {
		Assert.assertEquals("[here be,  dragons]", stringParam.get());
    }

    @Test
    public void testArrayInjection() {
		Assert.assertEquals(TEST_TARGET, arrayParamOpt.get());
		Assert.assertEquals(TEST_TARGET, arrayParam);
    }
    
    @Test
    public void testListInjection() {
		Assert.assertEquals(Arrays.asList(TEST_TARGET), listParam);
    }
}
