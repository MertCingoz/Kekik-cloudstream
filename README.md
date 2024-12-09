```shell
cd ../android-sdk
source bin/init.sh
cd ../Kekik-cloudstream
./gradlew make makePluginsJson
```

```sh
cp ../Kekik-cloudstream/**/build/*.cs3 ../Kekik-cloudstream-builds
cp ../Kekik-cloudstream/build/plugins.json ../Kekik-cloudstream-builds
```
