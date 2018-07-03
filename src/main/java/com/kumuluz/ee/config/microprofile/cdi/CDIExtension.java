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
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.*;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

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
    private static final Map<Type, Type> ALTERNATIVE_TYPES = new HashMap<>();

    static {
        // producers for following types are defined in com.kumuluz.ee.config.microprofile.cdi.ConfigInjectionProducer
        IGNORED_TYPES.add(Optional.class);
        IGNORED_TYPES.add(List.class);
        IGNORED_TYPES.add(Set.class);

        // ignore primitive types, since CDI already correctly maps them
        // this ensures that producers don't overlap (eg. one for boolean.class and one for Boolean.class)
        ALTERNATIVE_TYPES.put(boolean.class, Boolean.class);
        ALTERNATIVE_TYPES.put(int.class, Integer.class);
        ALTERNATIVE_TYPES.put(long.class, Long.class);
        ALTERNATIVE_TYPES.put(float.class, Float.class);
        ALTERNATIVE_TYPES.put(double.class, Double.class);
    }

    private Set<InjectionPoint> injectionPoints = new HashSet<>();

    public void collectConfigProducer(@Observes ProcessInjectionPoint<?, ?> pip) {
        ConfigProperty configProperty = pip.getInjectionPoint().getAnnotated().getAnnotation(ConfigProperty.class);
        if (configProperty != null) {
            injectionPoints.add(pip.getInjectionPoint());
        }
    }

    public void validateInjectionPoint(@Observes ProcessInjectionPoint<?, ?> pip) {

        InjectionPoint ip = pip.getInjectionPoint();
        ConfigProperty configPropertyAnnotation = ip.getAnnotated().getAnnotation(ConfigProperty
                .class);

        if (configPropertyAnnotation != null) {
            try {
                if (Class.class.isInstance(ip.getType())) {
                    ConfigPropertyProducer.getGenericProperty(ip);
                }
            } catch (Throwable t) {
                Class failingClass;
                Bean bean = ip.getBean();
                if (bean == null) {
                    failingClass = ip.getMember().getDeclaringClass();
                } else {
                    failingClass = ip.getBean().getBeanClass();
                }
                pip.addDefinitionError(new DeploymentException("Deploment Failure for ConfigProperty " +
                        configPropertyAnnotation.name() + " in class " + failingClass.getCanonicalName() + " Reason "
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

                HashSet<Type> types = new HashSet<>();
                for (InjectionPoint ip : injectionPoints) {
                    Type t = ip.getType();
                    if (t instanceof ParameterizedType) {
                        t = ((ParameterizedType) t).getActualTypeArguments()[0];
                    }
                    if (IGNORED_TYPES.contains(t)) {
                        continue;
                    }
                    // if mapping to alternative type is present, use alternative instead
                    types.add(ALTERNATIVE_TYPES.getOrDefault(t, t));
                }

                for (final Type converterType : types) {

                    Bean<?> bean = bm.createBean(new TypesBeanAttributes<Object>(beanAttributes) {

                        @Override
                        public Set<Type> getTypes() {
                            HashSet<Type> result = new HashSet<>();
                            result.add(converterType);
                            return result;
                        }
                    }, ConfigPropertyProducer.class, bm.getProducerFactory(annotatedMethod, null));
                    event.addBean(bean);
                }
            }
        }
    }
}
