package com.devmiax.spamilagros;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Request;
import okhttp3.Response;

public class confirmacionCita extends AppCompatActivity {

    // UI
    private MaterialToolbar toolbar;
    private MaterialTextView tvConfServicio, tvConfFechaHora, tvConfProfesional;
    private MaterialTextView tvMontoTotal, tvDeposito, tvYapeNumero;
    private ImageView imgVoucherPrev;
    private MaterialButton btnAdjuntarVoucher, btnConfirmarReserva;
    private LinearProgressIndicator progressConfirm;

    // Datos recibidos
    private String servicioId, servicioNombre, fecha, hora, staffId, staffNombre;
    private double precio, deposito;

    // Firebase
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    // HTTP
    private OkHttpClient http;

    // Imagen seleccionada
    private Uri voucherUri;

    private final ActivityResultLauncher<String> pickImage =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    voucherUri = uri;
                    imgVoucherPrev.setImageURI(uri);
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_confirmacion_cita);

        // ---- UI ----
        toolbar           = findViewById(R.id.toolbarConfirmacion);
        tvConfServicio    = findViewById(R.id.tvConfServicio);
        tvConfFechaHora   = findViewById(R.id.tvConfFechaHora);
        tvConfProfesional = findViewById(R.id.tvConfProfesional);
        tvMontoTotal      = findViewById(R.id.tvMontoTotal);
        tvDeposito        = findViewById(R.id.tvDeposito);
        tvYapeNumero      = findViewById(R.id.tvYapeNumero);
        imgVoucherPrev    = findViewById(R.id.imgVoucherPrev);
        btnAdjuntarVoucher= findViewById(R.id.btnAdjuntarVoucher);
        btnConfirmarReserva= findViewById(R.id.btnConfirmarReserva);
        progressConfirm   = findViewById(R.id.progressConfirm);

        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // ---- Extras ----
        Intent in = getIntent();
        servicioId     = in.getStringExtra("servicioId");
        servicioNombre = in.getStringExtra("servicioNombre");
        precio         = in.getDoubleExtra("precio", 0);
        fecha          = in.getStringExtra("fecha");
        hora           = in.getStringExtra("hora");
        staffId        = in.getStringExtra("staffId");
        staffNombre    = in.getStringExtra("staffNombre");

        deposito = round2(precio * 0.5);

        tvConfServicio.setText(nz(servicioNombre));
        tvConfFechaHora.setText(nz(fecha) + " • " + nz(hora));
        tvConfProfesional.setText(staffNombre != null && !staffNombre.isEmpty() ? "Con " + staffNombre : "Profesional a asignar");
        tvMontoTotal.setText("Total: " + soles(precio));
        tvDeposito.setText("Deposita: " + soles(deposito));
        tvYapeNumero.setText("Yape: 999 999 999"); // TODO: número real

        // ---- Firebase / HTTP ----
        auth = FirebaseAuth.getInstance();
        db   = FirebaseFirestore.getInstance();
        http = new OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(40, TimeUnit.SECONDS)
                .writeTimeout(40, TimeUnit.SECONDS)
                .build();

        btnAdjuntarVoucher.setOnClickListener(v -> pickImage.launch("image/*"));
        btnConfirmarReserva.setOnClickListener(v -> onConfirmar());
    }

    private void onConfirmar() {
        FirebaseUser u = auth.getCurrentUser();
        if (u == null) { toast("Debes iniciar sesión"); return; }
        if (voucherUri == null) { toast("Adjunta la captura del pago de 50%"); return; }

        setLoading(true);
        setLoading(true);
        subirVoucherRawADevmias(voucherUri, new UploadCallback() {
            @Override public void onSuccess(String fullUrl) { crearReservaFirestore(u.getUid(), fullUrl); }
            @Override public void onError(String msg) { setLoading(false); toast("Error subiendo voucher: " + msg); }
        });
    }

    // ============================== SUBIDA A TU SERVIDOR ==============================

    private interface UploadCallback { void onSuccess(String url); void onError(String msg); }

    // Sube la imagen como bytes crudos (lo que espera php://input)
    private void subirVoucherRawADevmias(Uri uri, UploadCallback cb) {
        String serverUrl = "https://devmiasx.com/upload.php";

        // 1) Lee y comprime a < ~2MB (mismo helper que te pasé antes)
        byte[] bytes;
        try { bytes = compressImageUnder2MB(uri); }
        catch (Exception e) { cb.onError("No se pudo procesar la imagen: " + e.getMessage()); return; }

        okhttp3.RequestBody body = okhttp3.RequestBody.create(bytes, okhttp3.MediaType.parse("image/jpeg"));
        okhttp3.Request req = new okhttp3.Request.Builder()
                .url(serverUrl)
                .addHeader("Content-Type", "image/jpeg")
                .post(body)
                .build();

        http.newCall(req).enqueue(new okhttp3.Callback() {
            @Override public void onFailure(okhttp3.Call call, java.io.IOException e) {
                runOnUiThread(() -> cb.onError(e.getMessage()));
            }
            @Override public void onResponse(okhttp3.Call call, okhttp3.Response response) throws java.io.IOException {
                String raw = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> cb.onError("HTTP " + response.code() + " — " + (raw.length()>200?raw.substring(0,200):raw)));
                    return;
                }
                // Tu PHP devuelve un TEXTO tipo:
                // "Imagen recibida y guardada correctamente en /ruta/servidor/fotos/20251005_153012.jpg"
                String fileName = parseFileNameFromResponse(raw);
                if (fileName == null) {
                    runOnUiThread(() -> cb.onError("Respuesta inesperada: " + raw));
                    return;
                }
                String fullUrl = "https://devmiasx.com/fotos/" + fileName; // expón /fotos en tu hosting
                runOnUiThread(() -> cb.onSuccess(fullUrl));
            }
        });
    }

    // Comprime a JPG por debajo de ~1.9 MB reduciendo tamaño y calidad.
// - Reescala el lado mayor a máx 1600 px (ajustable)
// - Baja calidad progresivamente hasta 30% si hace falta
    private byte[] compressImageUnder2MB(Uri uri) throws IOException {
        final int MAX_BYTES = 1_900_000;   // ~1.9 MB
        final int MAX_SIDE  = 1600;        // px (ajusta si quieres)

        // 1) Leer dimensiones sin cargar en memoria
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream probe = getContentResolver().openInputStream(uri)) {
            BitmapFactory.decodeStream(probe, null, bounds);
        }
        int w = bounds.outWidth, h = bounds.outHeight;
        if (w <= 0 || h <= 0) throw new IOException("Imagen inválida");

        // 2) Calcular inSampleSize para no exceder MAX_SIDE
        int inSample = 1;
        while (w / inSample > MAX_SIDE || h / inSample > MAX_SIDE) inSample *= 2;

        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = inSample;

        Bitmap bmp;
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            bmp = BitmapFactory.decodeStream(in, null, opts);
        }
        if (bmp == null) throw new IOException("No se pudo decodificar la imagen");

        // 3) Comprimir bajando calidad
        byte[] data;
        int quality = 90;
        do {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.JPEG, quality, bos);
            data = bos.toByteArray();
            quality -= 10;
        } while (data.length > MAX_BYTES && quality >= 30);

        // 4) Si aún es grande, reducir dimensiones adicionalmente en pasos
        while (data.length > MAX_BYTES && (bmp.getWidth() > 800 || bmp.getHeight() > 800)) {
            int newW = Math.max(800, (int)(bmp.getWidth()  * 0.85f));
            int newH = Math.max(800, (int)(bmp.getHeight() * 0.85f));
            Bitmap scaled = Bitmap.createScaledBitmap(bmp, newW, newH, true);
            bmp.recycle();
            bmp = scaled;

            int q = Math.min(85, quality + 5); // reintenta con calidad media
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.JPEG, q, bos);
            data = bos.toByteArray();

            while (data.length > MAX_BYTES && q > 30) {
                bos.reset();
                q -= 5;
                bmp.compress(Bitmap.CompressFormat.JPEG, q, bos);
                data = bos.toByteArray();
            }
        }

        bmp.recycle();
        return data;
    }

    // Extrae "20251005_153012.jpg" de: ".../fotos/20251005_153012.jpg"
    private @Nullable String parseFileNameFromResponse(String raw) {
        int idx = raw.lastIndexOf("/fotos/");
        if (idx == -1) return null;
        String tail = raw.substring(idx + "/fotos/".length());
        // por si el server imprime algo más luego:
        tail = tail.replaceAll("[\\r\\n\\t ]", "");
        int sp = tail.indexOf(".jpg");
        if (sp != -1) return tail.substring(0, sp + 4);
        sp = tail.indexOf(".jpeg");
        if (sp != -1) return tail.substring(0, sp + 5);
        return tail; // fallback
    }


    private String ensureAbsoluteUrl(String returned) {
        if (returned == null) return "";
        if (returned.startsWith("http://") || returned.startsWith("https://")) return returned;
        // Igual que tu TS: prefija dominio si viene ruta relativa
        return "https://devmiasx.com/" + (returned.startsWith("/") ? returned.substring(1) : returned);
    }

    // ============================== FIRESTORE: CREAR RESERVA ==============================

    private void crearReservaFirestore(String uid, String voucherUrl) {
        Map<String, Object> data = new HashMap<>();
        data.put("uidCliente", uid);
        data.put("servicioId", servicioId);
        if (staffId != null) data.put("staffId", staffId);
        data.put("fecha", fecha);
        data.put("hora", hora);
        data.put("precio", precio);
        data.put("deposito", deposito);
        data.put("metodoPago", "yape");
        data.put("voucherUrl", voucherUrl);
        data.put("estado", "pendiente"); // admin/empleado la aceptan o rechazan
        data.put("createdAt", FieldValue.serverTimestamp());

        db.collection("reservas")
                .add(data)
                .addOnSuccessListener(ref -> {
                    String codigo = ref.getId().substring(0, 6).toUpperCase(Locale.getDefault());
                    ref.update("codigo", codigo);
                    setLoading(false);
                    toast("¡Reserva enviada! Código: " + codigo);
                    startActivity(new Intent(this, misCitas.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    toast("Error creando reserva: " + e.getMessage());
                });
    }

    // ============================== HELPERS ==============================

    private void setLoading(boolean b) {
        btnAdjuntarVoucher.setEnabled(!b);
        btnConfirmarReserva.setEnabled(!b);
        progressConfirm.setVisibility(b ? android.view.View.VISIBLE : android.view.View.GONE);
    }

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_LONG).show(); }

    private static double round2(double v) { return Math.round(v * 100.0) / 100.0; }
    private static String soles(double v) {
        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("es", "PE"));
        return nf.format(v);
    }
    private static String nz(String s) { return s == null ? "" : s; }

    private byte[] readAllBytes(Uri uri) throws IOException {
        try (InputStream in = getContentResolver().openInputStream(uri);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
            return bos.toByteArray();
        }
    }

    private @Nullable String getFileName(Uri uri) {
        if ("content".equals(uri.getScheme())) {
            try (Cursor c = getContentResolver().query(uri, null, null, null, null)) {
                if (c != null && c.moveToFirst()) {
                    int idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (idx >= 0) return c.getString(idx);
                }
            }
        }
        return uri.getLastPathSegment();
    }
}