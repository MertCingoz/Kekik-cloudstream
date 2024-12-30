```shell
cd ../../android-sdk
source bin/init.sh
cd ../stream/Kekik-cloudstream
./gradlew make makePluginsJson
```

```sh
cp ../Kekik-cloudstream/**/build/*.cs3 .
cp ../Kekik-cloudstream/build/plugins.json .
git add .
git commit -m "Update"
git push
```
