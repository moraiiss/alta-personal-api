# Imagem só para desenvolvimento local: o código é montado em /app pelo compose.yaml
FROM eclipse-temurin:25-jdk

ENV GRADLE_USER_HOME=/gradle-home \
    GRADLE_BUILD_DIR=/gradle-build

WORKDIR /app

COPY start.sh /usr/local/bin/start-dev
RUN chmod +x /usr/local/bin/start-dev

CMD ["start-dev"]
