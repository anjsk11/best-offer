# 자바 21이 설치된 가벼운 리눅스 환경
FROM eclipse-temurin:21-jre-jammy

# 빌드된 jar 파일을 컨테이너 내부로 복사
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar

# 컨테이너가 시작될 때 실행할 명령어
ENTRYPOINT ["java", "-jar", "/app.jar"]