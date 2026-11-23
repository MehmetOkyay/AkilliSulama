# AkilliSulama

ESP32 ile çalışan otomatik sulama sistemi. İki parçadan oluşur:

android-app/ : Sulamayı uzaktan yöneten Android uygulaması. Bekleme ve sulama süresini ayarlar, "Şimdi sula" ve "Durdur" komutlarını gönderir, bir sonraki sulamaya kalan süreyi gösterir.
esp32/ : Röleyi (GPIO 13) kontrol eden ESP32 yazılımı (PlatformIO). Sunucuyu 3 saniyede bir kontrol eder, vakit gelince veya uygulamadan komut gelince motoru çalıştırır.

Uygulama ve ESP32 aynı sunucudaki api.php adresiyle haberleşir. Sunucu kodu bu repoda yoktur.

## Kurulum

Kodlardaki yer tutucuları kendi bilgilerinle değiştir:

- `esp32/src/main.cpp`: `WIFI_ADI`, `WIFI_SIFRESI` ve `http://SUNUCU_IP/api.php?api_key=API_ANAHTARI`
- `android-app/app/src/main/java/com/example/akillisulama/MainActivity.java`: `http://SUNUCU_IP/api.php?api_key=API_ANAHTARI`
