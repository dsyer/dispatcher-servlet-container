#!/bin/bash

ARTIFACT=jafu-dispatcher
MAINCLASS=com.example.jafu.JafuApplication
VERSION=0.0.1-SNAPSHOT
FEATURE=${HOME}/.m2/repository/org/springframework/experimental/spring-graal-native-feature/0.6.0.BUILD-SNAPSHOT/spring-graal-native-feature-0.6.0.BUILD-SNAPSHOT.jar

GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'

cd `dirname $0`

rm -rf target
mkdir -p target/native-image

echo "Packaging $ARTIFACT with Maven"
../mvnw -DskipTests package > target/native-image/output.txt

JAR="$ARTIFACT-$VERSION.jar"
rm -f $ARTIFACT
echo "Unpacking $JAR"
cd target/native-image
jar -xvf ../$JAR >/dev/null 2>&1
cp -R META-INF BOOT-INF/classes

LIBPATH=`find BOOT-INF/lib | tr '\n' ':'`
CP=BOOT-INF/classes:$LIBPATH:$FEATURE

GRAALVM_VERSION=`native-image --version`
echo "Compiling $ARTIFACT with $GRAALVM_VERSION"
native-image \
  --no-server \
  --no-fallback \
  -H:+TraceClassInitialization \
  -H:Name=$ARTIFACT \
  -H:+PrintMethodHistogram \
  -H:+PrintAnalysisCallTree \
  -H:+PrintImageObjectTree \
  -H:+ReportExceptionStackTraces \
  --allow-incomplete-classpath \
  --report-unsupported-elements-at-runtime \
  --initialize-at-build-time=org.springframework.core.OrderComparator,org.springframework.web.servlet.config.annotation.InterceptorRegistry \
  -DremoveUnusedAutoconfig=true \
  -cp $CP $MAINCLASS >> output.txt

if [[ -f $ARTIFACT ]]; then
  printf "${GREEN}SUCCESS${NC}\n"
  mv ./$ARTIFACT ..
  exit 0
else
  printf "${RED}FAILURE${NC}: an error occurred when compiling the native-image.\n"
  exit 1
fi