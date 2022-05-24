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
package com.kumuluz.ee.config.microprofile.cdi;

import com.kumuluz.ee.config.microprofile.ConfigImpl;
import com.kumuluz.ee.config.microprofile.annotations.OptionalCollectionIP;
import com.kumuluz.ee.config.microprofile.utils.AlternativeTypesUtil;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.ConfigValue;
import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.*;
import javax.enterprise.util.AnnotationLiteral;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Supplier;

/**
 * Extension validates if all @{@link ConfigProperty} values are present at startup and initializes ConfigProperty
 * producer bean for all types, found in @{@link ConfigProperty} injection points.
 *
 * @author Urban Malc
 * @author Jan Meznarič
 * @since 1.1
 */
public class CDIExtension implements Extension {

    private static final Set<Type> IGNORED_TYPES = new HashSet<>();
    private static final Set<Class<?>> SUPPORTED_COLLECTION_TYPES = new HashSet<>();
    private static final List<DeploymentException> DEPLOYMENT_EXCEPTIONS = new ArrayList<>();

    static {
        // producers for following types are defined in com.kumuluz.ee.config.microprofile.cdi.ConfigInjectionProducer
        IGNORED_TYPES.add(Optional.class);
        IGNORED_TYPES.add(ConfigValue.class);
        IGNORED_TYPES.add(Supplier.class);
        IGNORED_TYPES.add(List.class);
        IGNORED_TYPES.add(Set.class);

        SUPPORTED_COLLECTION_TYPES.add(List.class);
        SUPPORTED_COLLECTION_TYPES.add(Set.class);
    }

    private final Set<InjectionPoint> injectionPoints = new HashSet<>();
    private final Set<InjectionPoint> injectionPointsProperties = new HashSet<>();

    public void collectConfigProducer(@Observes ProcessInjectionPoint<?, ?> pip) {
        ConfigProperty configProperty = pip.getInjectionPoint().getAnnotated().getAnnotation(ConfigProperty.class);
        if (configProperty == null) {
            return;
        }
        // add annotation if injection type is Optional<List<?>> or Optional<Set<?>>
        if (isOptionalCollection(pip.getInjectionPoint().getType())) {
            pip.configureInjectionPoint().addQualifier(new AnnotationLiteral<OptionalCollectionIP>() {});
        }
        injectionPoints.add(pip.getInjectionPoint());
    }

    public void collectConfigProperties(@Observes ProcessInjectionPoint<?, ?> pip) {

        ConfigProperties configProperties = pip.getInjectionPoint().getAnnotated().getAnnotation(ConfigProperties.class);

        if (configProperties == null) {
            return;
        }

        injectionPointsProperties.add(pip.getInjectionPoint());
    }

    public void vetoConfigPropertiesBeans(@Observes ProcessBeanAttributes<?> pba) {

        ConfigProperties configProperties = pba.getAnnotated().getAnnotation(ConfigProperties.class);
        if (configProperties == null) {
            return;
        }

        if (!ConfigPropertiesProducer.class.equals(pba.getAnnotated().getBaseType())) {

            // veto existing beans, because dynamic producers are added by this extension
            pba.veto();

            // validate bean
            try {
                ConfigPropertiesProducer.getGenericPropertiesObject((Class<?>) pba.getAnnotated().getBaseType(),
                        Optional.empty());
            } catch (Throwable t) {
                Class<?> failingClass = (Class<?>) pba.getAnnotated().getBaseType();
                DEPLOYMENT_EXCEPTIONS.add(new DeploymentException("Deploment Failure for ConfigProperties with prefix '" +
                        configProperties.prefix() + "' in class " + failingClass.getCanonicalName() + " Reason "
                        + t.getMessage(), t));
            }
        }
    }

    /**
     * Checks if type is {@literal Optional<List<?>>} or {@literal Optional<Set<?>>}
     */
    private boolean isOptionalCollection(Type type) {
        if (type instanceof ParameterizedType &&
                Optional.class.isAssignableFrom((Class<?>) ((ParameterizedType) type).getRawType())) {
            Type innerType = ((ParameterizedType) type).getActualTypeArguments()[0];
            if (innerType instanceof ParameterizedType) {
                for (Class<?> collectionClass : SUPPORTED_COLLECTION_TYPES) {
                    if (collectionClass.isAssignableFrom((Class<?>) ((ParameterizedType) innerType).getRawType())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public void validateInjectionPoint(@Observes ProcessInjectionPoint<?, ?> pip) {

        InjectionPoint ip = pip.getInjectionPoint();
        ConfigProperty configPropertyAnnotation = ip.getAnnotated().getAnnotation(ConfigProperty
                .class);

        if (configPropertyAnnotation != null) {
            try {
                if (ip.getType() instanceof Class && !ConfigValue.class.equals(ip.getType())) { // ignore ConfigValue, because it's always resolvable
                    ConfigPropertyProducer.getGenericProperty(ip);
                }
            } catch (Throwable t) {
                Class<?> failingClass;
                Bean<?> bean = ip.getBean();
                if (bean == null) {
                    failingClass = ip.getMember().getDeclaringClass();
                } else {
                    failingClass = ip.getBean().getBeanClass();
                }
                DEPLOYMENT_EXCEPTIONS.add(new DeploymentException("Deploment Failure for ConfigProperty " +
                        configPropertyAnnotation.name() + " in class " + failingClass.getCanonicalName() + " Reason "
                        + t.getMessage(), t));
            }
        }
    }

    protected void validate(@Observes AfterDeploymentValidation adv) {

        DEPLOYMENT_EXCEPTIONS.forEach(adv::addDeploymentProblem);
    }

    public void validateConfigPropertiesInjectionPoint(@Observes ProcessInjectionPoint<?, ?> pip) {

        InjectionPoint ip = pip.getInjectionPoint();
        ConfigProperties configPropertiesAnnotation = ip.getAnnotated().getAnnotation(ConfigProperties.class);

        if (configPropertiesAnnotation != null) {
            try {
                ConfigPropertiesProducer.getGenericPropertiesObject(ip);
            } catch (Throwable t) {
                Class<?> failingClass;
                Bean<?> bean = ip.getBean();
                if (bean == null) {
                    failingClass = ip.getMember().getDeclaringClass();
                } else {
                    failingClass = ip.getBean().getBeanClass();
                }
                DEPLOYMENT_EXCEPTIONS.add(new DeploymentException("Deploment Failure for ConfigProperties " +
                        configPropertiesAnnotation.prefix() + " in class " + failingClass.getCanonicalName() + " Reason "
                        + t.getMessage(), t));
            }
        }
    }

    public void addDynamicProducers(@Observes AfterBeanDiscovery event, BeanManager bm) {

        Config config = ConfigProvider.getConfig();

        if (config instanceof ConfigImpl) {

            AnnotatedType<ConfigPropertyProducer> annotatedType = bm.createAnnotatedType(ConfigPropertyProducer.class);
            BeanAttributes<?> beanAttributes = null;

            AnnotatedMethod<? super ConfigPropertyProducer> annotatedMethod = null;
            for (AnnotatedMethod<? super ConfigPropertyProducer> m : annotatedType.getMethods()) {
                if (m.getJavaMember().getName().equals("getGenericProperty")) {
                    beanAttributes = bm.createBeanAttributes(m);
                    annotatedMethod = m;
                    break;
                }
            }

            if (beanAttributes != null) {

                Set<Type> types = new HashSet<>();
                for (InjectionPoint ip : injectionPoints) {
                    Type t = ip.getType();
                    while (t instanceof ParameterizedType) {
                        t = ((ParameterizedType) t).getActualTypeArguments()[0];
                    }
                    if (IGNORED_TYPES.contains(t)) {
                        continue;
                    }
                    // ignore primitive types, since CDI already correctly maps them
                    // this ensures that producers don't overlap (e.g. one for boolean.class and one for Boolean.class)
                    types.add(AlternativeTypesUtil.getTypeFromPrimitive(t).orElse(t));
                }

                for (final Type converterType : types) {

                    Bean<?> bean = bm.createBean(new TypesBeanAttributes<>(beanAttributes) {

                        @Override
                        public Set<Type> getTypes() {
                            return Collections.singleton(converterType);
                        }
                    }, ConfigPropertyProducer.class, bm.getProducerFactory(annotatedMethod, null));
                    event.addBean(bean);
                }
            }
        }
    }

    public void addDynamicProducersProperties(@Observes AfterBeanDiscovery event, BeanManager bm) {

        Config config = ConfigProvider.getConfig();

        if (config instanceof ConfigImpl) {

            AnnotatedType<ConfigPropertiesProducer> annotatedType = bm.createAnnotatedType(ConfigPropertiesProducer.class);
            BeanAttributes<?> beanAttributes = null;

            AnnotatedMethod<? super ConfigPropertiesProducer> annotatedMethod = null;
            for (AnnotatedMethod<? super ConfigPropertiesProducer> m : annotatedType.getMethods()) {
                if (m.getJavaMember().getName().equals("getGenericPropertiesObject")) {
                    beanAttributes = bm.createBeanAttributes(m);
                    annotatedMethod = m;
                    break;
                }
            }

            if (beanAttributes != null) {

                Set<Type> types = new HashSet<>();
                for (InjectionPoint ip : injectionPointsProperties) {
                    Type t = ip.getType();
                    while (t instanceof ParameterizedType) {
                        t = ((ParameterizedType) t).getActualTypeArguments()[0];
                    }
                    if (IGNORED_TYPES.contains(t)) {
                        continue;
                    }
                    // ignore primitive types, since CDI already correctly maps them
                    // this ensures that producers don't overlap (e.g. one for boolean.class and one for Boolean.class)
                    types.add(AlternativeTypesUtil.getTypeFromPrimitive(t).orElse(t));
                }

                for (final Type propertiesObjectType : types) {

                    Bean<?> bean = bm.createBean(new TypesBeanAttributes<>(beanAttributes) {

                        @Override
                        public Set<Type> getTypes() {
                            return Collections.singleton(propertiesObjectType);
                        }
                    }, ConfigPropertiesProducer.class, bm.getProducerFactory(annotatedMethod, null));
                    event.addBean(bean);
                }
            }
        }
    }
}
