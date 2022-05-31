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
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

public class ConfigPropertiesProducer {

    @Dependent
    @ConfigProperties
    public static Object getGenericPropertiesObjectIP(InjectionPoint ip) {

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
            throw new DeploymentException("Could not instantiate a @ConfigProperties object " + propertiesObjectType, e);
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

                setField(field, propertiesObject, valueToAssign);
            } else {

                // check if default value is set directly on field
                if (getField(field, propertiesObject) != null) {
                    // default value is already set
                    continue;
                }

                if (Optional.class.equals(field.getType())) {
                    setField(field, propertiesObject, Optional.empty());
                } else if (OptionalInt.class.equals(field.getType())) {
                    setField(field, propertiesObject, OptionalInt.empty());
                } else if (OptionalLong.class.equals(field.getType())) {
                    setField(field, propertiesObject, OptionalLong.empty());
                } else if (OptionalDouble.class.equals(field.getType())) {
                    setField(field, propertiesObject, OptionalDouble.empty());
                } else {

                    throw new DeploymentException("Microprofile Config Property " + actualPrefix + fieldPropertyName +
                            " can not be found.");
                }
            }
        }
    }

    private static Object getField(Field field, Object fieldObject) throws IllegalAccessException {

        try {
            String getterName = "get" + field.getName().substring(0, 1).toUpperCase() + field.getName().substring(1);
            Method getter = fieldObject.getClass().getDeclaredMethod(getterName);

            if (getter.getReturnType().equals(field.getType())) {
                return getter.invoke(fieldObject);
            }
        } catch (Exception ignored) {
        }

        // access via getter failed, try directly via field
        if (!field.canAccess(fieldObject)) {
            field.setAccessible(true);
        }
        return field.get(fieldObject);
    }

    private static void setField(Field field, Object fieldObject, Object value) throws IllegalAccessException {

        try {
            String setterName = "set" + field.getName().substring(0, 1).toUpperCase() + field.getName().substring(1);
            Method setter = fieldObject.getClass().getDeclaredMethod(setterName, field.getType());

            setter.invoke(fieldObject, value);
        } catch (Exception e) {

            // access via setter failed, try directly via field
            if (!field.canAccess(fieldObject)) {
                field.setAccessible(true);
            }
            field.set(fieldObject, value);
        }
    }
}
