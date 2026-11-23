package com.example.akillisulama;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private TextView textMevcutBekleme, textMevcutSulama, textSayac;
    private EditText editBekleme, editSulama;
    private Button btnKaydet, btnSula, btnDurdur;
    private RequestQueue requestQueue;
    private CountDownTimer sayac;
    private final String API_URL = "http://SUNUCU_IP/api.php?api_key=API_ANAHTARI";

    private Handler otoGuncellemeHandler = new Handler();
    private Runnable otoGuncellemeRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        textMevcutBekleme = findViewById(R.id.textMevcutBekleme);
        textMevcutSulama = findViewById(R.id.textMevcutSulama);
        textSayac = findViewById(R.id.textSayac);
        editBekleme = findViewById(R.id.editBekleme);
        editSulama = findViewById(R.id.editSulama);
        btnKaydet = findViewById(R.id.btnKaydet);
        btnSula = findViewById(R.id.btnSula);
        btnDurdur = findViewById(R.id.btnDurdur);

        requestQueue = Volley.newRequestQueue(this);
        verileriCek();

        otoGuncellemeRunnable = new Runnable() {
            @Override
            public void run() {
                verileriCek();
                otoGuncellemeHandler.postDelayed(this, 10000);
            }
        };
        otoGuncellemeHandler.postDelayed(otoGuncellemeRunnable, 10000);

        btnKaydet.setOnClickListener(v -> {
            String yeniBekleme = editBekleme.getText().toString();
            String yeniSulama = editSulama.getText().toString();
            if (yeniBekleme.isEmpty() || yeniSulama.isEmpty()) {
                Toast.makeText(this, "Lütfen iki alanı da doldur!", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                JSONObject jsonBody = new JSONObject();
                jsonBody.put("islem", "ayarlari_guncelle");
                jsonBody.put("bekleme", Integer.parseInt(yeniBekleme));
                jsonBody.put("sulama", Integer.parseInt(yeniSulama));
                veriGonder(jsonBody, "Ayarlar güncellendi!");
            } catch (JSONException e) { e.printStackTrace(); }
        });

        btnSula.setOnClickListener(v -> {
            try {
                JSONObject jsonBody = new JSONObject();
                jsonBody.put("islem", "simdi_sula");
                veriGonder(jsonBody, "Sulama emri gönderildi!");
            } catch (JSONException e) { e.printStackTrace(); }
        });

        btnDurdur.setOnClickListener(v -> {
            try {
                JSONObject jsonBody = new JSONObject();
                jsonBody.put("islem", "sulamayi_durdur");
                veriGonder(jsonBody, "Sulama durduruldu! 🛑");
                new Handler().postDelayed(this::verileriCek, 3000);
            } catch (JSONException e) { e.printStackTrace(); }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sayac != null) sayac.cancel();
        otoGuncellemeHandler.removeCallbacks(otoGuncellemeRunnable);
    }

    private void veriGonder(JSONObject jsonBody, String basariMesaji) {
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, API_URL, jsonBody,
                response -> {
                    Toast.makeText(this, basariMesaji, Toast.LENGTH_SHORT).show();
                    editBekleme.setText(""); editSulama.setText("");
                    verileriCek();
                },
                error -> Toast.makeText(this, "Bağlantı hatası!", Toast.LENGTH_SHORT).show()
        );
        requestQueue.add(request);
    }

    private void verileriCek() {
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, API_URL, null,
                response -> {
                    try {
                        int beklemeDk = response.getInt("bekleme_suresi_dk");
                        int sulamaDk = response.getInt("sulama_suresi_dk");
                        long sonSulamaZamani = response.getLong("son_sulama_zamani");
                        int manuel = response.getInt("manuel_calistir");

                        textMevcutBekleme.setText("Mevcut: " + beklemeDk + " dk");
                        textMevcutSulama.setText("Mevcut: " + sulamaDk + " dk");

                        sayaciBaslat(sonSulamaZamani, beklemeDk, sulamaDk, manuel);
                    } catch (JSONException e) { textSayac.setText("Veri okuma hatası!"); }
                }, error -> textSayac.setText("Sunucuya bağlanılamadı!")
        );
        requestQueue.add(request);
    }

    private void sayaciBaslat(long sonSulamaZamani, int beklemeDk, int sulamaDk, int manuel) {
        if (sayac != null) sayac.cancel();

        long suAnkiZaman = System.currentTimeMillis() / 1000;
        long beklemeSaniye = beklemeDk * 60L;
        long sulamaSaniye = sulamaDk * 60L;

        long hedefBaslama = sonSulamaZamani + beklemeSaniye;
        long hedefBitis = hedefBaslama + sulamaSaniye;

        if (manuel == 1 || (suAnkiZaman >= hedefBaslama && suAnkiZaman < hedefBitis)) {
            long kalanSulama = (manuel == 1) ? sulamaSaniye : (hedefBitis - suAnkiZaman);
            if(kalanSulama < 0) kalanSulama = 0;

            sayac = new CountDownTimer(kalanSulama * 1000, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    long toplamSaniye = millisUntilFinished / 1000;
                    long dakika = toplamSaniye / 60;
                    long saniye = toplamSaniye % 60;
                    textSayac.setText("💦 Sulama Yapılıyor: " + dakika + " Dk " + saniye + " Sn");
                }
                @Override
                public void onFinish() {
                    textSayac.setText("Sulama bitti, güncelleniyor...");
                    new Handler().postDelayed(MainActivity.this::verileriCek, 2000);
                }
            }.start();
            return;
        }

        long kalanSaniye = hedefBaslama - suAnkiZaman;

        if (kalanSaniye <= 0) {
            textSayac.setText("⏳ Vakit geldi! (ESP32 bekleniyor...)");
            new Handler().postDelayed(this::verileriCek, 5000);
            return;
        }

        sayac = new CountDownTimer(kalanSaniye * 1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long toplamSaniye = millisUntilFinished / 1000;
                long gun = toplamSaniye / (24 * 3600);
                long saat = (toplamSaniye % (24 * 3600)) / 3600;
                long dakika = (toplamSaniye % 3600) / 60;
                long saniye = toplamSaniye % 60;

                String kalanMetin = "Sonraki Sulama: ";
                if (gun > 0) kalanMetin += gun + " Gün ";
                if (saat > 0) kalanMetin += saat + " Saat ";
                kalanMetin += dakika + " Dk " + saniye + " Sn";
                textSayac.setText(kalanMetin);
            }
            @Override
            public void onFinish() { verileriCek(); }
        }.start();
    }
}