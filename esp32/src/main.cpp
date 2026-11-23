#include <Arduino.h>
#include <WiFi.h>
#include <HTTPClient.h>
#include <ArduinoJson.h>
#include "time.h"
#include "soc/soc.h"
#include "soc/rtc_cntl_reg.h"

const char* ssid = "WIFI_ADI";
const char* password = "WIFI_SIFRESI";
const String apiUrl = "http://SUNUCU_IP/api.php?api_key=API_ANAHTARI";

const int rolePin = 13;
const unsigned long API_ARALIK_MS = 3000;
const time_t GECERLI_ZAMAN_ESIGI = 1700000000;

struct SulamaDurumu {
  int beklemeDk;
  int sulamaDk;
  long sonSulamaZamani;
  int manuel;
};

bool sulamaAktif = false;
bool manuelIslendi = false;
bool saatBaslatildi = false;
unsigned long motorBaslamaZamani = 0;
unsigned long sulamaSuresiMilisaniye = 0;
unsigned long sonApiKontrol = 0;
long sonSulamaYerel = 0;

bool zamanGecerliMi() {
  return time(nullptr) > GECERLI_ZAMAN_ESIGI;
}

void saatiBaslat() {
  if (saatBaslatildi || WiFi.status() != WL_CONNECTED) return;
  configTime(3 * 3600, 0, "pool.ntp.org");
  saatBaslatildi = true;
  Serial.println("Saat internetten aliniyor...");
}

bool durumuOku(SulamaDurumu &d) {
  bool basarili = false;
  HTTPClient http;
  http.begin(apiUrl);
  http.setConnectTimeout(3000);
  http.setTimeout(3000);
  if (http.GET() == HTTP_CODE_OK) {
    StaticJsonDocument<512> doc;
    if (!deserializeJson(doc, http.getString())) {
      d.beklemeDk = doc["bekleme_suresi_dk"].as<int>();
      d.sulamaDk = doc["sulama_suresi_dk"].as<int>();
      d.sonSulamaZamani = doc["son_sulama_zamani"].as<long>();
      d.manuel = doc["manuel_calistir"].as<int>();
      basarili = true;
    }
  }
  http.end();
  return basarili;
}

void zamaniGuncelle() {
  if (!zamanGecerliMi()) return;

  long suAnkiSaat = (long)time(nullptr);
  sonSulamaYerel = suAnkiSaat;

  HTTPClient http;
  http.begin(apiUrl);
  http.setConnectTimeout(3000);
  http.setTimeout(3000);
  http.addHeader("Content-Type", "application/json");

  String requestBody = "{\"son_sulama_zamani\":" + String(suAnkiSaat) + "}";

  int responseCode = http.POST(requestBody);
  if (responseCode >= 200 && responseCode < 300) {
    Serial.println("Veritabani guncellendi!");
  } else {
    Serial.println("Veritabani guncellenemedi, kod: " + String(responseCode));
  }
  http.end();
}

void sulamayiBitir() {
  digitalWrite(rolePin, HIGH);
  sulamaAktif = false;
  zamaniGuncelle();
}

void durumuIsle(const SulamaDurumu &d) {
  long sonSulama = max(d.sonSulamaZamani, sonSulamaYerel);
  long gecenSaniye = (long)time(nullptr) - sonSulama;

  bool otomatikVaktiGeldi = zamanGecerliMi() && d.beklemeDk > 0 && sonSulama > 0 &&
                            gecenSaniye >= (long)d.beklemeDk * 60;

  if (d.manuel != 1) manuelIslendi = false;

  if (d.manuel == 2) {
    Serial.println("🛑 UYGULAMADAN DURDURULDU!");
    sulamayiBitir();
  } else if (!sulamaAktif && d.sulamaDk > 0 &&
             ((d.manuel == 1 && !manuelIslendi) || otomatikVaktiGeldi)) {
    Serial.println("💦 SULAMA BASLADI! (Motor devrede)");
    digitalWrite(rolePin, LOW);
    sulamaAktif = true;
    motorBaslamaZamani = millis();
    sulamaSuresiMilisaniye = (unsigned long)d.sulamaDk * 60UL * 1000UL;

    if (d.manuel == 1) {
      manuelIslendi = true;
      zamaniGuncelle();
    }
  }
}

void setup() {
  WRITE_PERI_REG(RTC_CNTL_BROWN_OUT_REG, 0);

  Serial.begin(115200);

  digitalWrite(rolePin, HIGH);
  pinMode(rolePin, OUTPUT);
  digitalWrite(rolePin, HIGH);

  Serial.print("WiFi'ye baglaniliyor: ");
  Serial.println(ssid);
  WiFi.begin(ssid, password);

  unsigned long baslangic = millis();
  while (WiFi.status() != WL_CONNECTED && millis() - baslangic < 30000) {
    delay(500);
    Serial.print(".");
  }

  if (WiFi.status() != WL_CONNECTED) {
    Serial.println("\nWiFi baglanamadi, arka planda denenmeye devam edilecek.");
    return;
  }
  Serial.println("\nWiFi Baglandi!");

  saatiBaslat();
  baslangic = millis();
  while (!zamanGecerliMi() && millis() - baslangic < 10000) {
    delay(200);
  }

  SulamaDurumu d;
  if (zamanGecerliMi() && durumuOku(d)) {
    long gecenSaniye = (long)time(nullptr) - d.sonSulamaZamani;

    if (d.sonSulamaZamani == 0 || gecenSaniye >= (long)d.beklemeDk * 60) {
      Serial.println("⚡ Cihaz açıldı ve sürenin dolmuş olduğunu gördü. Senkronize ediliyor...");
      zamaniGuncelle();
    }
  }
}

void loop() {
  unsigned long simdikiZaman = millis();

  if (sulamaAktif && (simdikiZaman - motorBaslamaZamani >= sulamaSuresiMilisaniye)) {
    Serial.println("🛑 SURE DOLDU, SULAMA BITTI!");
    sulamayiBitir();
  }

  saatiBaslat();

  if (WiFi.status() == WL_CONNECTED && (simdikiZaman - sonApiKontrol >= API_ARALIK_MS)) {
    sonApiKontrol = simdikiZaman;

    SulamaDurumu d;
    if (durumuOku(d)) {
      durumuIsle(d);
    }
  }
}
