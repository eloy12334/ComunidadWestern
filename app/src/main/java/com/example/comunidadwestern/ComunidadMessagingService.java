package com.example.comunidadwestern;

import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class ComunidadMessagingService extends FirebaseMessagingService {

    private static final String TAG = "ComunidadFCM";

    @Override
    public void onMessageReceived(RemoteMessage message) {
        super.onMessageReceived(message);

        Map<String, String> data = message.getData();
        if (data == null || data.isEmpty()) {
            Log.w(TAG, "Se recibió un mensaje FCM sin datos.");
            return;
        }

        String type = data.get("type");
        if (!"announcement".equals(type)) {
            Log.d(TAG, "Tipo de mensaje FCM no utilizado por esta aplicación.");
            return;
        }

        String announcementId = data.get("announcementId");
        if (announcementId == null || announcementId.trim().isEmpty()) {
            Log.w(TAG, "El mensaje FCM no contiene announcementId.");
            return;
        }

        String title = data.get("title");
        String body = data.get("body");

        if (title == null || title.trim().isEmpty()) {
            title = "Nuevo anuncio comunitario";
        }

        if (body == null || body.trim().isEmpty()) {
            body = "Hay información nueva en Comunidad Western.";
        }

        Log.i(TAG, "Notificación recibida para el anuncio " + announcementId);

        NotificationHelper.announcement(
                getApplicationContext(),
                title,
                body,
                announcementId
        );
    }

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);

        // La suscripción al topic se vuelve a solicitar cuando FCM renueva el token.
        FirebaseSupport.subscribeToCommunity();
        Log.i(TAG, "Token FCM renovado; se solicitó la suscripción al topic comunitario.");
    }
}
