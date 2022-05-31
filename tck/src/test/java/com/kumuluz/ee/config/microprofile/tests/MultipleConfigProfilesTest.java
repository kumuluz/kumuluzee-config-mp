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

public class MultipleConfigProfilesTest extends Arquillian {

    @Deployment
    public static JavaArchive deploy() {
        return ShrinkWrap
                .create(JavaArchive.class, "multipleConfigProfilesTest.jar")
                .addClasses(MultipleConfigProfilesTest.class)
                .addAsResource("config-profiles.yaml", "config.yaml")
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .as(JavaArchive.class);
    }

    @Inject
    @ConfigProperty(name = "dockervalue")
    private Optional<String> dockervalue;

    @Inject
    @ConfigProperty(name = "prodvalue")
    private Optional<String> prodvalue;

    @Inject
    @ConfigProperty(name = "testvalue")
    private Optional<String> testvalue;

    @Inject
    @ConfigProperty(name = "override1")
    private Optional<String> override1;

    @Inject
    @ConfigProperty(name = "override2")
    private Optional<String> override2;

    @Inject
    @ConfigProperty(name = "override3")
    private Optional<String> override3;

    @Test
    public void multipleProfilesSimpleTest() {

        Assert.assertTrue(dockervalue.isPresent());
        Assert.assertEquals(dockervalue.get(), "docker");

        Assert.assertTrue(prodvalue.isPresent());
        Assert.assertEquals(prodvalue.get(), "prod");

        Assert.assertFalse(testvalue.isPresent());
    }

    @Test
    public void multipleProfilesOverrideTest() {

        Assert.assertTrue(override1.isPresent());
        Assert.assertTrue(override2.isPresent());
        Assert.assertTrue(override3.isPresent());

        // defined in both docker and prod, prod has priority because it is defined after docker
        Assert.assertEquals(override1.get(), "fromprod");

        // defined in docker
        Assert.assertEquals(override2.get(), "fromdocker");

        // defined in root
        Assert.assertEquals(override3.get(), "root");
    }
}
