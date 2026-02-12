FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY lib/ lib/
COPY src/ src/
RUN mkdir -p out \
  && javac -cp "lib/json-simple-1.1.1.jar" -d out src/*.java

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/out/ out/
COPY --from=build /app/lib/ lib/
EXPOSE 2100
ENTRYPOINT ["java","-cp","out:lib/json-simple-1.1.1.jar","LoadBalancer"]
