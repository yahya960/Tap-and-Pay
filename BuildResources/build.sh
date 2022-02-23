cd ..
sh gradlew app:clean
sh gradlew app:assembleDebug
mkdir BuildOutput
cd BuildOutput
mkdir TrainingApp
cd TrainingApp
mkdir debug
cd ..
cd ..
cp ./app/build/outputs/apk/debug/*.* ./BuildOutput/TrainingApp/debug

sh gradlew app:clean
sh gradlew app:assembleRelease
cd BuildOutput
cd TrainingApp
mkdir release
cd ..
cd ..
cp ./app/build/outputs/apk/release/*.* ./BuildOutput/TrainingApp/release
