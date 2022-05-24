package com.kumuluz.ee.config.microprofile.cdi;

import com.kumuluz.ee.config.microprofile.ConfigImpl;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.spi.DeploymentException;
import javax.enterprise.inject.spi.InjectionPoint;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Optional;

public class ConfigPropertiesProducer {

    @Dependent
    @ConfigProperties
    public static Object getGenericPropertiesObject(InjectionPoint ip) {

        Class<?> propertiesObjectType = (Class<?>) ip.getType();

        Optional<String> ipConfigPropertiesPrefixOverride = ip.getQualifiers().stream()
                .filter(q -> ConfigProperties.class.equals(q.annotationType()))
                .findAny()
                .map(a -> (ConfigProperties) a)
                .map(ConfigProperties::prefix)
                .filter(p -> !ConfigProperties.UNCONFIGURED_PREFIX.equals(p));

        return getGenericPropertiesObject(propertiesObjectType, ipConfigPropertiesPrefixOverride);
    }

    public static Object getGenericPropertiesObject(Class<?> propertiesObjectType,
                                                     Optional<String> configPropertiesPrefixOverride) {

        Optional<String> configPropertiesPrefix = configPropertiesPrefixOverride.or(
                () -> Optional.ofNullable(propertiesObjectType.getAnnotation(ConfigProperties.class))
                        .map(ConfigProperties::prefix)
                        .filter(p -> !ConfigProperties.UNCONFIGURED_PREFIX.equals(p))
        );

        try {
            Object propertiesObject = propertiesObjectType.getDeclaredConstructor().newInstance();

            processFields(propertiesObject, propertiesObject.getClass().getDeclaredFields(),
                    configPropertiesPrefix.orElse(null));

            return propertiesObject;

        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void processFields(Object propertiesObject, Field[] fields, String prefix)
            throws IllegalAccessException {

        ConfigImpl config = ConfigProvider.getConfig().unwrap(ConfigImpl.class);

        for (Field field : fields) {

            String actualPrefix = (prefix == null || prefix.isEmpty()) ? "" : prefix + ".";

            String fieldPropertyName = Optional.ofNullable(field.getAnnotation(ConfigProperty.class))
                    .map(ConfigProperty::name)
                    .orElse(field.getName());

            Optional<?> optionalValue = config.getOptionalValue(actualPrefix + fieldPropertyName,
                    field.getType());

            Object valueToAssign = null;

            if (optionalValue.isPresent()) {
                valueToAssign = optionalValue.get();
            } else {
                Optional<?> defaultValue = Optional.ofNullable(field.getAnnotation(ConfigProperty.class))
                        .map(ConfigProperty::defaultValue)
                        .filter(cp -> !ConfigProperty.UNCONFIGURED_VALUE.equals(cp))
                        .map(dv -> config.convert(dv, field.getType()));

                if (defaultValue.isPresent()) {
                    valueToAssign = defaultValue.get();
                }
            }

            if (valueToAssign != null) {

                if (!field.canAccess(propertiesObject)) {
                    field.setAccessible(true);
                }

                field.set(propertiesObject, valueToAssign);
            } else {

                // check if default value is set directly on field
                if (!field.canAccess(propertiesObject)) {
                    field.setAccessible(true);
                }

                if (field.get(propertiesObject) != null) {
                    // default value is already set
                    continue;
                }

                if (Optional.class.equals(field.getType())) {
                    field.set(propertiesObject, Optional.empty());
                } else {

                    throw new DeploymentException("Microprofile Config Property " + actualPrefix + fieldPropertyName +
                            " can not be found.");
                }
            }
        }
    }
}
