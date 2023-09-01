FROM bellsoft/liberica-openjre-alpine:17.0.8
WORKDIR /opt
ENV PORT 8080
EXPOSE 8080
COPY target/banking-api-*.jar /opt/banking-api-*.jar
CMD ["java", "-jar", "/opt/banking-api-*.jar"]