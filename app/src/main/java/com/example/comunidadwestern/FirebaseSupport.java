package com.example.comunidadwestern;

import android.content.Context;
import android.widget.Toast;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;

public final class FirebaseSupport {
    private FirebaseSupport() { }
    public static boolean isReady(Context c) { try { return !FirebaseApp.getApps(c).isEmpty(); } catch(Exception e){ return false; } }
    public static String uid(){ return FirebaseAuth.getInstance().getCurrentUser().getUid(); }
    public static DatabaseReference db(){ return FirebaseDatabase.getInstance().getReference(); }
    public static void subscribeToCommunity(){
        FirebaseMessaging.getInstance().subscribeToTopic(AppConfig.FCM_TOPIC);
    }
    public static void showSetupMessage(Context c){
        Toast.makeText(c,"Falta configurar Firebase: agrega google-services.json y activa Authentication y Realtime Database. Revisa README.",Toast.LENGTH_LONG).show();
    }
}
