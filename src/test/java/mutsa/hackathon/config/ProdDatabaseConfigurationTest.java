package mutsa.hackathon.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.core.io.ClassPathResource;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ProdDatabaseConfigurationTest {

    @Test
    void MySQL_JDBC_드라이버가_런타임_클래스패스에_존재한다()
            throws ClassNotFoundException {

        Class<?> driverClass =
                Class.forName(
                        "com.mysql.cj.jdbc.Driver"
                );

        assertEquals(
                "com.mysql.cj.jdbc.Driver",
                driverClass.getName()
        );
    }

    @Test
    void 운영_프로필의_RDS_환경변수가_정상적으로_해석된다()
            throws Exception {

        Map<String, Object> environmentVariables =
                new LinkedHashMap<>();

        environmentVariables.put(
                "DB_HOST",
                "daybit-db.example.ap-northeast-2.rds.amazonaws.com"
        );

        environmentVariables.put(
                "DB_PORT",
                "3306"
        );

        environmentVariables.put(
                "DB_NAME",
                "daybit"
        );

        environmentVariables.put(
                "DB_USERNAME",
                "daybit_app"
        );

        environmentVariables.put(
                "DB_PASSWORD",
                "test-password"
        );

        MutablePropertySources propertySources =
                new MutablePropertySources();

        propertySources.addFirst(
                new MapPropertySource(
                        "test-environment",
                        environmentVariables
                )
        );

        YamlPropertySourceLoader loader =
                new YamlPropertySourceLoader();

        loader.load(
                "application-prod",
                new ClassPathResource(
                        "application-prod.yaml"
                )
        ).forEach(
                propertySources::addLast
        );

        PropertySourcesPropertyResolver resolver =
                new PropertySourcesPropertyResolver(
                        propertySources
                );

        assertEquals(
                "jdbc:mysql://daybit-db.example.ap-northeast-2.rds.amazonaws.com:3306/daybit"
                        + "?useUnicode=true"
                        + "&characterEncoding=UTF-8"
                        + "&serverTimezone=Asia/Seoul"
                        + "&sslMode=REQUIRED",
                resolver.getProperty(
                        "spring.datasource.url"
                )
        );

        assertEquals(
                "daybit_app",
                resolver.getProperty(
                        "spring.datasource.username"
                )
        );

        assertEquals(
                "test-password",
                resolver.getProperty(
                        "spring.datasource.password"
                )
        );

        assertEquals(
                "com.mysql.cj.jdbc.Driver",
                resolver.getProperty(
                        "spring.datasource.driver-class-name"
                )
        );

        assertEquals(
                "10",
                resolver.getProperty(
                        "spring.datasource.hikari.maximum-pool-size"
                )
        );

        assertEquals(
                "2",
                resolver.getProperty(
                        "spring.datasource.hikari.minimum-idle"
                )
        );

        assertEquals(
                "update",
                resolver.getProperty(
                        "spring.jpa.hibernate.ddl-auto"
                )
        );

        assertEquals(
                "false",
                resolver.getProperty(
                        "spring.jpa.open-in-view"
                )
        );

        assertNotNull(
                resolver.getProperty(
                        "spring.datasource.url"
                )
        );
    }
}