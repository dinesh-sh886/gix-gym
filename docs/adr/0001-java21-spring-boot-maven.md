# ADR 0001: Java 21, Spring Boot, and Maven

**Status:** proposed
**Owner:** dinesh-sh886
**Date:** 2026-10-10

## Context

The project requires a robust, long-term supported technology stack for building microservices. The team needs:

- Long-term support (LTS) for the Java runtime
- A mature, opinionated framework for rapid service development
- A build tool with strong dependency management and multi-module support
- Alignment with industry standards and broad ecosystem compatibility

## Decision

- **Java 21 LTS** as the language runtime (current LTS, supported until September 2031)
- **Spring Boot 3.3.x** as the application framework (requires Java 17+, aligned with Spring 6)
- **Maven 3.9+** as the build tool with a parent POM for centralized dependency management

The parent POM (`pom.xml`) defines:
- Centralized dependency versions via `dependencyManagement`
- Plugin configurations for compiler, test, code quality (JaCoCo, SpotBugs, Checkstyle), and enforcement
- Java 21 toolchain enforcement via `maven-enforcer-plugin`
- Profiles for `local` (lenient) and `ci` (strict) quality gates

## Consequences

### Positive
- Java 21 provides virtual threads, pattern matching, records, and sealed classes — all valuable for microservices
- Spring Boot 3.x offers native GraalVM support, observability (Micrometer), and Jakarta EE 9+ namespace
- Maven multi-module structure supports independent service builds and a monorepo workflow
- Centralized versions reduce version drift across services

### Negative
- Java 21 requires team familiarity with newer language features
- Spring Boot 3.x migration from 2.x is not applicable (greenfield), but third-party library compatibility must be verified
- Maven XML verbosity vs. Gradle Kotlin DSL; team chose Maven for broader CI/CD compatibility

## Verification

- `mvn enforcer:enforce` passes on Java 21+
- `mvn verify` executes compilation, tests, and quality checks
- Parent POM loads without unresolved dependencies

## References

- [Java 21 Release Notes](https://openjdk.org/projects/jdk/21/)
- [Spring Boot 3.3 Reference](https://docs.spring.io/spring-boot/docs/3.3.x/reference/html/)
- [Maven Enforcer Plugin](https://maven.apache.org/enforcer/maven-enforcer-plugin/)