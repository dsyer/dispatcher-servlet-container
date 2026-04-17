## JVM Mode
To compile for JVM mode:
`./mvnw clean package`

To run in JVM Mode:
`java -jar target/spring-fruits-1.0.0-SNAPSHOT.jar`

## AOT on JVM mode
To compile for AOT mode:
`./mvnw clean compile spring-boot:process-aot package`

To run in AOT mode:
`java -Dspring.aot.enabled=true -jar target/spring-fruits-1.0.0-SNAPSHOT.jar`

## Native Mode
To compile for native mode:
`./mvnw clean native:compile -Pnative`

To run in native mode:
`target/spring-fruits`