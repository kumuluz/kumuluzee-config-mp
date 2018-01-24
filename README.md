# KumuluzEE MicroProfile Config
[![Build Status](https://img.shields.io/travis/kumuluz/kumuluzee-config-mp/master.svg?style=flat)](https://travis-ci.org/kumuluz/kumuluzee-config-mp)

> KumuluzEE Config MicroProfile extension provides you with a complete Eclipse MicroProfile Feature implementation for configuring your applications.

KumuluzEE MicroProfile Config 1.2 implements the 
[MicroProfile Config](https://microprofile.io/project/eclipse/microprofile-config) 1.2 API.

KumuluzEE MicroProfile Config contains APIs for accessing the configuration values from 
[KumuluzEE Configuration framework](https://github.com/kumuluz/kumuluzee/wiki/Configuration) and adding custom
MicroProfile `ConfigSource`s.

## Usage

MicroProfile Config API can be enabled by adding the following Maven dependency:

```xml
<dependency>
    <groupId>com.kumuluz.ee.config</groupId>
    <artifactId>kumuluzee-config-mp</artifactId>
    <version>1.2.0-SNAPSHOT</version>
</dependency>
```

### Configuration sources

KumuluzEE MicroProfile Config implementation exposes configuration from the
[KumuluzEE configuration framework](https://github.com/kumuluz/kumuluzee/wiki/Configuration).

Supported configuration sources (sorted by their priority) are:
- System properties (ordinal 400)
- Environment variables (ordinal 300)
- Configuration file (ordinal 100; `config.yml`, `config.yaml`, `config.properties` or
  `META-INF/microprofile-config.properties`; configuration file name can be overridden by specifying its name in the
  `com.kumuluz.ee.configuration.file` system property)

Additional configuration sources (including configuration servers such as etcd and Consul implemented in
`kumuluzee-config`) have the default ordinal 110. Ordinal value can be overridden for every configuration source by
setting the configuration key `config_ordinal` in the configuration source to the desired value.

### Accessing configuration values

There are a few possible approaches for getting `Config` instance and accessing configuration values, as described in
this section.

#### Programmatic access to the configuration object

You can access the `Config` object programmatically:

```java
Config config = ConfigProvider.getConfig();
String configValue = config.getValue("mp.test", String.class));
```

#### CDI injection of the configuration object

You can use CDI injection to get the `Config` object:

```java
@Inject
private Config config;

String configValue = config.getValue("mp.test", String.class));
```

#### CDI injection of the configuration values

You can get the configuration values directly with CDI injection:

```java
@Inject
@ConfigProperty(name = "mp.test", defaultValue="defaultTestValue")
private String injectedString;

@Inject
@ConfigProperty(name = "mp.test")
private Optional<String> injectedStringOpt;

@Inject
@ConfigProperty(name = "mp.test")
private Provider<String> stringProvider;
```

If the requested property does not exist, the injections in the first and third example throw `DeploymentException`.
Injection in the second example will return `Optional.empty()`, if the requested property does not exist.

### Converters

Since configuration sources return values of type `String`, converters are used to convert received values to the types
that were requested. All converters from the MicroProfile Config specification are implemented. They are as follows:

1. `boolean` and `Boolean` ("true", "1", "YES", "Y", "ON" are considered `true` (case insensitive), everything else
   `false`)
1. `int` and `Integer`
1. `long` and `Long`
1. `float` and `Float`
1. `double` and `Double`
1. `Duration`
1. `LocalTime`
1. `LocalDate`
1. `LocalDateTime`
1. `OffsetDateTime`
1. `OffsetTime`
1. `Instant`
1. `URL`
1. `Class` (based on the result of `Class.forName`)
1. Common sense converter (Tries to find class's constructor with a single string parameter or static methods
   `valueOf(String)` or `parse(CharSequence)`)

You can add custom converters by implementing
the `org.eclipse.microprofile.config.spi.Converter` interface and registering your implementation in the
`/META-INF/services/org.eclipse.microprofile.config.spi.Converter` file with the fully qualified class name. Example of
a custom converter is shown below:

```java
@Priority(500)
public class CustomerConverter implements Converter<Customer> {

    @Override
    public Customer convert(String s) {
        return Customer.parse(s);
    }
}
```

You can create multiple Converters for the same type. Converter with the highest priority will be used. Priority is
read from the `@Priority` annotation.

### Injection of arrays

Injection of arrays is supported. Arrays are defined as comma separated values in the configuration values. For example,
to define array in `microprofile-config.properties` file, add the following line:

```properties
mp.example-string-array=first,second,third\\,still_third
```

The commas can be escaped with the backslash character (in .properties files, the backslash character must be escaped
itself as shown above). Also note, that converters are applied to each array element, which means that array types are
not limited only to `String`.

To inject the property defined above, use the following code:

```java
@Inject
@ConfigProperty(name = "mp.example-string-array")
private String[] strings;
```

Injection of `Set` and `List` is also supported:

```java
@Inject
@ConfigProperty(name = "mp.example-string-array")
List<String> stringsAsList;

@Inject
@ConfigProperty(name = "mp.example-string-array")
Set<String> stringsAsSet;
```


### Adding custom configuration sources

Custom configuration sources can be added to extend the configuration framework.
You can add custom configuration source by implementing the `org.eclipse.microprofile.config.spi.ConfigSource`
interface and registering your implementation in the
`/META-INF/services/org.eclipse.microprofile.config.spi.ConfigSource` file with the fully qualified class name.
Example of a custom configuration source is shown below:

```java
public class ExampleConfigSource implements ConfigSource {
    @Override
    public Map<String, String> getProperties() {
        return null;
    }

    @Override
    public int getOrdinal() {
        return 120;
    }

    @Override
    public String getValue(String s) {
        if ("mp.custom-source-value".equals(s)) {
            return "Hello from custom ConfigSource";
        }

        return null;
    }

    @Override
    public String getName() {
        return "ExampleConfigSource";
    }
}
```

To dynamically add multiple configuration sources, implement the
`org.eclipse.microprofile.config.spi.ConfigSourceProvider` interface and register your implementation in the 
`/META-INF/services/org.eclipse.microprofile.config.spi.ConfigSourceProvider` file with the fully qualified class name.
The interface contains the method `Iterable<ConfigSource> getConfigSources(ClassLoader classLoader)` in which multiple
configuration sources can be registered programmatically.

## Changelog

Recent changes can be viewed on Github on the [Releases Page](https://github.com/kumuluz/kumuluzee-config-mp/releases)

## Contribute

See the [contributing docs](https://github.com/kumuluz/kumuluzee-config-mp/blob/master/CONTRIBUTING.md)

When submitting an issue, please follow the 
[guidelines](https://github.com/kumuluz/kumuluzee-config-mp/blob/master/CONTRIBUTING.md#bugs).

When submitting a bugfix, write a test that exposes the bug and fails before applying your fix. Submit the test 
alongside the fix.

When submitting a new feature, add tests that cover the feature.

## License

MIT
