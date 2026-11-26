FROM eclipse-temurin:21-jdk AS build

WORKDIR /workspace/backend

# The Maven resources configuration reads Markdown from ../learning-content.
COPY backend/ ./
COPY learning-content/ /workspace/learning-content/

RUN chmod +x ./mvnw && ./mvnw -DskipTests package

FROM eclipse-temurin:21-jre AS runtime

WORKDIR /app

COPY --from=build /workspace/backend/target/*.jar /app/app.jar

EXPOSE 10000

CMD ["sh", "-c", "set -eu; secret_file=/etc/secrets/tidb-truststore.base64.txt; truststore=/tmp/tidb-truststore.p12; if [ ! -f \"$secret_file\" ]; then echo \"Required Render Secret File is missing: $secret_file\" >&2; exit 1; fi; if [ ! -s \"$secret_file\" ]; then echo \"Required Render Secret File is empty: $secret_file\" >&2; exit 1; fi; if ! base64 -d \"$secret_file\" > \"$truststore\"; then rm -f \"$truststore\"; echo \"Could not decode Render truststore Secret File: $secret_file\" >&2; exit 1; fi; if [ ! -s \"$truststore\" ]; then rm -f \"$truststore\"; echo \"Decoded truststore is empty: $truststore\" >&2; exit 1; fi; if ! chmod 600 \"$truststore\"; then rm -f \"$truststore\"; echo \"Could not set restrictive permissions on truststore: $truststore\" >&2; exit 1; fi; exec java -Dserver.port=${PORT:-10000} -jar /app/app.jar"]
