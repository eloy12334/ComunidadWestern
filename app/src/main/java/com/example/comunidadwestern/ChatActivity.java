package com.example.comunidadwestern;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {
    private final ArrayList<ChatMessage> mensajes = new ArrayList<>();
    private ChatAdapter adapter;
    private DatabaseReference ref;
    private Query consulta;
    private ValueEventListener listener;
    private ListView lista;
    private EditText txtMensaje;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        lista = findViewById(R.id.listaMensajes);
        txtMensaje = findViewById(R.id.txtMensaje);
        adapter = new ChatAdapter();
        lista.setAdapter(adapter);
        lista.setDivider(null);
        lista.setDividerHeight(0);

        ref = FirebaseSupport.db().child("messages").child(AppConfig.COMMUNITY_ID);
        consulta = ref.orderByChild("createdAt").limitToLast(50);
        listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                mensajes.clear();
                String currentUid = FirebaseSupport.uid();

                for (DataSnapshot item : snapshot.getChildren()) {
                    String uid = item.child("uid").getValue(String.class);
                    String name = item.child("name").getValue(String.class);
                    String housing = item.child("housing").getValue(String.class);
                    String body = item.child("text").getValue(String.class);
                    Long time = item.child("createdAt").getValue(Long.class);

                    String fecha = time == null ? "" : new SimpleDateFormat(
                            "dd/MM HH:mm", Locale.getDefault()).format(new Date(time));

                    ChatMessage message = new ChatMessage();
                    message.uid = uid == null ? "" : uid;
                    message.name = name == null || name.trim().isEmpty() ? "Vecino" : name;
                    message.housing = housing == null || housing.trim().isEmpty() ? "-" : housing;
                    message.text = body == null ? "" : body;
                    message.date = fecha;
                    message.ownMessage = message.uid.equals(currentUid);
                    mensajes.add(message);
                }

                adapter.notifyDataSetChanged();
                if (!mensajes.isEmpty()) {
                    lista.post(() -> lista.setSelection(adapter.getCount() - 1));
                }
            }

            @Override
            public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
                Toast.makeText(ChatActivity.this, "No se pudo cargar el chat", Toast.LENGTH_SHORT).show();
            }
        };
        consulta.addValueEventListener(listener);

        ImageButton btnEnviar = findViewById(R.id.btnEnviarMensaje);
        ImageButton btnVolver = findViewById(R.id.btnVolverChat);
        btnEnviar.setOnClickListener(v -> enviar());
        btnVolver.setOnClickListener(v -> finish());

        txtMensaje.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                enviar();
                return true;
            }
            return false;
        });
    }

    private void enviar() {
        String contenido = txtMensaje.getText().toString().trim();
        if (contenido.isEmpty()) {
            txtMensaje.setError("Escribe un mensaje antes de enviar");
            return;
        }
        if (contenido.length() > 500) {
            txtMensaje.setError("Máximo 500 caracteres");
            return;
        }

        FirebaseSupport.db().child("profiles").child(FirebaseSupport.uid()).get()
                .addOnSuccessListener(profile -> {
                    Map<String, Object> message = new HashMap<>();
                    message.put("uid", FirebaseSupport.uid());
                    message.put("name", profile.child("name").getValue(String.class));
                    message.put("housing", profile.child("housing").getValue(String.class));
                    message.put("text", contenido);
                    message.put("createdAt", System.currentTimeMillis());

                    ref.push().setValue(message).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            txtMensaje.setText("");
                            txtMensaje.setError(null);
                        } else {
                            Toast.makeText(this, "No se pudo enviar el mensaje", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .addOnFailureListener(error -> Toast.makeText(
                        this, "No se pudo obtener tu perfil", Toast.LENGTH_SHORT).show());
    }

    private static class ChatMessage {
        String uid;
        String name;
        String housing;
        String text;
        String date;
        boolean ownMessage;
    }

    private class ChatAdapter extends BaseAdapter {
        private final LayoutInflater inflater = LayoutInflater.from(ChatActivity.this);

        @Override
        public int getCount() {
            return mensajes.size();
        }

        @Override
        public ChatMessage getItem(int position) {
            return mensajes.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            MessageHolder holder;
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.item_chat_message, parent, false);
                holder = new MessageHolder();
                holder.container = convertView.findViewById(R.id.contenedorMensaje);
                holder.sender = convertView.findViewById(R.id.txtNombreVivienda);
                holder.body = convertView.findViewById(R.id.txtTextoMensaje);
                holder.date = convertView.findViewById(R.id.txtFechaMensaje);
                convertView.setTag(holder);
            } else {
                holder = (MessageHolder) convertView.getTag();
            }

            ChatMessage message = getItem(position);
            holder.container.setBackgroundResource(message.ownMessage
                    ? R.drawable.chat_message_bubble_dark
                    : R.drawable.chat_message_bubble_light);
            holder.sender.setText(message.name + " · #" + message.housing);
            holder.body.setText(message.text);
            holder.date.setText(message.date.isEmpty() ? "" : "◷  " + message.date);
            holder.container.setContentDescription(message.name + ", vivienda "
                    + message.housing + ". " + message.text + ". " + message.date);
            return convertView;
        }
    }

    private static class MessageHolder {
        View container;
        TextView sender;
        TextView body;
        TextView date;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (consulta != null && listener != null) {
            consulta.removeEventListener(listener);
        }
    }
}
