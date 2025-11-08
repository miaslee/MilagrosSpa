package com.devmiax.spamilagros.fragments;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.devmiax.spamilagros.LoginActivity;
import com.devmiax.spamilagros.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PerfilFragment extends Fragment {

    // UI
    private ImageView imgAvatar;
    private MaterialButton btnCambiarFoto, btnGuardar, btnLogout;
    private TextInputEditText etNombre, etTelefono;
    private TextView tvNombreCabecera, tvCorreoCabecera;

    // Firebase
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    // HTTP
    private OkHttpClient http;

    // Estado
    private Uri avatarUriLocal;
    private String avatarUrlRemoto;
    private String userDocId;

    private final ActivityResultLauncher<String> pickImage =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    avatarUriLocal = uri;
                    imgAvatar.setImageURI(uri);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_perfil, container, false);

        // Bind de vistas
        imgAvatar = view.findViewById(R.id.imgAvatar);
        btnCambiarFoto = view.findViewById(R.id.btnCambiarFoto);
        etNombre = view.findViewById(R.id.editNombrePerfil);
        etTelefono = view.findViewById(R.id.editTelefonoPerfil);
        btnGuardar = view.findViewById(R.id.btnGuardarPerfil);
        tvNombreCabecera = view.findViewById(R.id.tvNombreCabecera);
        tvCorreoCabecera = view.findViewById(R.id.tvCorreoCabecera);
        btnLogout = view.findViewById(R.id.btnCerrarSesion);

        // Inicializar Firebase y HTTP
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        http = new OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(40, TimeUnit.SECONDS)
                .writeTimeout(40, TimeUnit.SECONDS)
                .build();

        // Verificar sesión
        FirebaseUser u = auth.getCurrentUser();
        if (u == null) {
            startActivity(new Intent(requireContext(), LoginActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            requireActivity().finish();
            return view;
        }
        userDocId = u.getUid();

        // Listeners
        btnCambiarFoto.setOnClickListener(v -> pickImage.launch("image/*"));
        btnGuardar.setOnClickListener(v -> onGuardar());
        btnLogout.setOnClickListener(v -> {
            auth.signOut();
            startActivity(new Intent(requireContext(), LoginActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            requireActivity().finish();
        });

        // Cargar datos del perfil
        cargarPerfil();

        return view;
    }

    // ------------------------ CARGA PERFIL ------------------------
    private void cargarPerfil() {
        setLoading(true);
        db.collection("users").document(userDocId).get()
                .addOnSuccessListener(d -> {
                    setLoading(false);
                    if (d.exists()) {
                        String nombre = d.getString("nombre");
                        String tel = d.getString("telefono");
                        String mail = d.getString("correo");
                        avatarUrlRemoto = d.getString("avatarUrl");

                        etNombre.setText(nombre);
                        etTelefono.setText(tel);
                        tvNombreCabecera.setText(nombre);
                        tvCorreoCabecera.setText(mail);

                        if (!TextUtils.isEmpty(avatarUrlRemoto)) {
                            try {
                                Picasso.get()
                                        .load(avatarUrlRemoto)
                                        .placeholder(R.drawable.ic_person_24)
                                        .into(imgAvatar);
                            } catch (Throwable ignore) {}
                        }
                    } else {
                        Map<String, Object> base = new HashMap<>();
                        base.put("rol", "cliente");
                        db.collection("users").document(userDocId).set(base);
                    }
                })
                .addOnFailureListener(e -> setLoading(false));
    }

    // ------------------------ GUARDAR CAMBIOS ------------------------
    private void onGuardar() {
        String nombre = safeText(etNombre);
        String tel = safeText(etTelefono);

        if (nombre.isEmpty()) {
            toast("Ingresa tu nombre");
            return;
        }

        setLoading(true);

        if (avatarUriLocal != null) {
            subirAvatarRaw(avatarUriLocal, new UploadCb() {
                @Override public void ok(String url) {
                    avatarUrlRemoto = url;
                    guardarDocPerfil(nombre, tel, url);
                }
                @Override public void fail(String msg) {
                    setLoading(false);
                    toast("Error subiendo foto: " + msg);
                }
            });
        } else {
            guardarDocPerfil(nombre, tel, avatarUrlRemoto);
        }
    }

    private void guardarDocPerfil(String nombre, String tel, @Nullable String avatarUrl) {
        Map<String, Object> up = new HashMap<>();
        up.put("nombre", nombre);
        up.put("telefono", tel);
        if (avatarUrl != null) up.put("avatarUrl", avatarUrl);

        db.collection("users").document(userDocId)
                .set(up, SetOptions.merge())
                .addOnSuccessListener(v -> {
                    setLoading(false);
                    toast("Perfil actualizado");
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    toast("No se pudo guardar: " + e.getMessage());
                });
    }

    // ------------------------ SUBIDA AVATAR RAW ------------------------
    private interface UploadCb { void ok(String url); void fail(String msg); }

    private void subirAvatarRaw(Uri uri, UploadCb cb) {
        String serverUrl = "https://devmiasx.com/upload.php";

        byte[] bytes;
        try { bytes = compressImageUnder2MB(uri); }
        catch (Exception e) { cb.fail("No se pudo procesar la imagen: " + e.getMessage()); return; }

        RequestBody body = RequestBody.create(bytes, MediaType.parse("image/jpeg"));
        Request req = new Request.Builder()
                .url(serverUrl)
                .addHeader("Content-Type", "image/jpeg")
                .post(body)
                .build();

        http.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {
                requireActivity().runOnUiThread(() -> cb.fail(e.getMessage()));
            }
            @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String raw = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    requireActivity().runOnUiThread(() ->
                            cb.fail("HTTP " + response.code() + " — " + (raw.length()>200?raw.substring(0,200):raw)));
                    return;
                }
                String url = parseUrlFromPhpResponse(raw);
                final String finalUrl = url != null ? ensureAbsoluteUrl(url) : null;
                if (finalUrl == null) {
                    requireActivity().runOnUiThread(() -> cb.fail("Respuesta inesperada: " + raw));
                } else {
                    requireActivity().runOnUiThread(() -> cb.ok(finalUrl));
                }
            }
        });
    }

    // ------------------------ HELPERS ------------------------
    private String ensureAbsoluteUrl(String returned) {
        if (returned == null) return "";
        if (returned.startsWith("http://") || returned.startsWith("https://")) return returned;
        return "https://devmiasx.com/" + (returned.startsWith("/") ? returned.substring(1) : returned);
    }

    private String safeText(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }

    private void setLoading(boolean b) {
        btnGuardar.setEnabled(!b);
        btnCambiarFoto.setEnabled(!b);
        btnLogout.setEnabled(!b);
    }

    private void toast(String s) {
        Toast.makeText(requireContext(), s, Toast.LENGTH_LONG).show();
    }

    private byte[] compressImageUnder2MB(Uri uri) throws IOException {
        final int MAX_BYTES = 1_900_000;
        final int MAX_SIDE  = 1600;

        android.graphics.BitmapFactory.Options bounds = new android.graphics.BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream probe = requireContext().getContentResolver().openInputStream(uri)) {
            android.graphics.BitmapFactory.decodeStream(probe, null, bounds);
        }

        int w = bounds.outWidth, h = bounds.outHeight;
        int inSample = 1;
        while (w / inSample > MAX_SIDE || h / inSample > MAX_SIDE) inSample *= 2;

        android.graphics.BitmapFactory.Options opts = new android.graphics.BitmapFactory.Options();
        opts.inSampleSize = inSample;

        android.graphics.Bitmap bmp;
        try (InputStream in = requireContext().getContentResolver().openInputStream(uri)) {
            bmp = android.graphics.BitmapFactory.decodeStream(in, null, opts);
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, bos);
        return bos.toByteArray();
    }

    private @Nullable String parseUrlFromPhpResponse(String raw) {
        int idx = raw.lastIndexOf("/fotos/");
        if (idx == -1) return null;
        String tail = raw.substring(idx + "/fotos/".length()).replaceAll("[\\r\\n\\t ]", "");
        int end = tail.indexOf(".jpg");
        if (end != -1) return "/fotos/" + tail.substring(0, end + 4);
        return "/fotos/" + tail;
    }

    @Nullable
    private String getFileName(Uri uri) {
        if ("content".equals(uri.getScheme())) {
            try (Cursor c = requireContext().getContentResolver().query(uri, null, null, null, null)) {
                if (c != null && c.moveToFirst()) {
                    int idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (idx >= 0) return c.getString(idx);
                }
            }
        }
        return uri.getLastPathSegment();
    }
}
