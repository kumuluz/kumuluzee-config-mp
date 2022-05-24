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
import java.util.Optional;

public class OptionalDefaultValueInjectionTest extends Arquillian {

    @Deployment
    public static JavaArchive deploy() {
        return ShrinkWrap
                .create(JavaArchive.class, "optionalDefaultValueInjectionTest.jar")
                .addClasses(OptionalDefaultValueInjectionTest.class)
                .addAsResource("config.yaml")
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .as(JavaArchive.class);
    }

    @Inject
    @ConfigProperty(name = "nonexistent.property", defaultValue = "providedDefault")
    private Optional<String> defaultOptionalValue;

    @Test
    public void optionalDefaultValueTest() {

        if (defaultOptionalValue.isEmpty()) {
            Assert.fail("defaultOptionalValue is empty");
        }

        Assert.assertEquals("providedDefault", defaultOptionalValue.get());
    }
}
