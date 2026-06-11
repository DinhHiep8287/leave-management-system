plugins {
    java
    id("org.springframework.boot") version "3.3.5"
    id("io.spring.dependency-management") version "1.1.6"
}

group = "com.peih68"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

extra["mapstructVersion"] = "1.5.5.Final"
extra["jjwtVersion"] = "0.12.6"
extra["springdocVersion"] = "2.6.0"

dependencies {
    // Web + Security + Persistence + Validation + Actuator
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Structured (JSON) logging for the prod profile (see logback-spring.xml)
    implementation("net.logstash.logback:logstash-logback-encoder:7.4")
    implementation("org.springframework.boot:spring-boot-starter-mail")

    // Database
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")

    // MapStruct
    implementation("org.mapstruct:mapstruct:${property("mapstructVersion")}")
    annotationProcessor("org.mapstruct:mapstruct-processor:${property("mapstructVersion")}")
    // Lombok + MapStruct binding
    annotationProcessor("org.projectlombok:lombok-mapstruct-binding:0.2.0")

    // JWT (chuẩn bị cho tuần 2)
    implementation("io.jsonwebtoken:jjwt-api:${property("jjwtVersion")}")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:${property("jjwtVersion")}")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:${property("jjwtVersion")}")

    // OpenAPI / Swagger
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:${property("springdocVersion")}")

    // Dev tools (only loaded in dev)
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    developmentOnly("org.springframework.boot:spring-boot-docker-compose")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    // Apache HttpClient5 — enables TestRestTemplate to handle 401 responses with
    // a request body (default JDK HttpURLConnection throws "cannot retry due to
    // server authentication" for streaming POSTs).
    testImplementation("org.apache.httpcomponents.client5:httpclient5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}

tasks.withType<Test> {
    useJUnitPlatform()
    maxHeapSize = "1g"
    jvmArgs("-XX:MaxMetaspaceSize=512m")
    // Override datasource so tests target a dedicated DB instead of the dev one.
    // Java system properties win over the SPRING_DATASOURCE_* env vars set by
    // docker-compose, which otherwise leak into tests.
    systemProperty("spring.datasource.url",
            System.getenv("SPRING_DATASOURCE_URL_TEST")
                ?: "jdbc:postgresql://postgres:5432/leave_management_test")
    systemProperty("spring.datasource.username",
            System.getenv("POSTGRES_USER") ?: "leave_admin")
    systemProperty("spring.datasource.password",
            System.getenv("POSTGRES_PASSWORD") ?: "changeme_in_local")
}

springBoot {
    mainClass.set("com.peih68.leave.LeaveApplication")
}
