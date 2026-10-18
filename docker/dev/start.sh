#!/bin/sh
# Hot reload: um Gradle em modo contínuo recompila a cada alteração em src/
# e o Spring DevTools (carregado pelo bootRun) reinicia o contexto.
set -e

# Cache do projeto fora do diretório montado, para não criar arquivos de root no host
GRADLE="./gradlew --project-cache-dir /gradle-project-cache"

$GRADLE classes
$GRADLE --continuous --quiet classes &
exec $GRADLE bootRun
