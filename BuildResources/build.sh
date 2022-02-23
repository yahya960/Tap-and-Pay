cd ..
sh gradlew clean
sh gradlew assemble
mkdir -p ./BuildOutput/TrainingApp

cp -r ./app/build/outputs/apk/ ./BuildOutput/TrainingApp
