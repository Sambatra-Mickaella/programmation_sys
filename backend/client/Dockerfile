FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY lib/ lib/
COPY src/ src/
COPY views/ views/
COPY resources/ resources/
RUN rm -rf build \
  && mkdir -p build/assets build/pages build/WEB-INF/classes build/WEB-INF/lib \
  && find src -name "*.java" > sources.txt \
  && javac -cp ".:lib/*" -d build/WEB-INF/classes @sources.txt \
  && rm sources.txt \
  && cp -r views/pages/. build/pages/ \
  && cp -r views/assets/. build/assets/ \
  && cp -r views/*.xml build/WEB-INF/ \
  && cp -r resources/. build/WEB-INF/classes/ \
  && cp lib/json-simple-1.1.1.jar build/WEB-INF/lib/ \
  && (cd build && jar -cvf SmartDrive.war .)

FROM tomcat:10.1-jdk17
COPY --from=build /app/build/SmartDrive.war /usr/local/tomcat/webapps/SmartDrive.war
EXPOSE 8080
