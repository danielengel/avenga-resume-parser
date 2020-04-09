FROM openjdk:8-jdk-alpine
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar
COPY Resumes Resumes/
COPY GATEFiles GATEFiles/
COPY JAPEGrammars JAPEGrammars/
ENTRYPOINT ["java","-jar","/app.jar"]