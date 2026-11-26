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

CMD ["sh", "-c", "java -Dserver.port=${PORT:-10000} -jar /app/app.jar"]
