package com.example.gomarket;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gomarket.adapter.MessageAdapter;
import com.example.gomarket.model.MessageModel;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView rvMessages;
    private EditText edtMessage;
    private ImageView btnSend;
    private ImageView btnBack;
    private TextView tvShopperNameTitle;

    private List<MessageModel> messageList;
    private MessageAdapter adapter;
    private String currentUserId = "user123";
    private String shopperId;
    private String shopperName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        rvMessages = findViewById(R.id.rvMessages);
        edtMessage = findViewById(R.id.edtMessage);
        btnSend = findViewById(R.id.btnSend);
        btnBack = findViewById(R.id.btnBack);
        tvShopperNameTitle = findViewById(R.id.tvShopperNameTitle);

        shopperId = getIntent().getStringExtra("SHOPPER_ID");
        shopperName = getIntent().getStringExtra("SHOPPER_NAME");
        
        if (shopperName != null) {
            tvShopperNameTitle.setText("Chat với Shopper: " + shopperName);
        } else {
            tvShopperNameTitle.setText("Chat");
            shopperId = "s1"; // default mock
        }

        btnBack.setOnClickListener(v -> finish());

        messageList = new ArrayList<>();
        adapter = new MessageAdapter(this, messageList, currentUserId);
        
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);
        rvMessages.setAdapter(adapter);

        loadInitialMessages();

        btnSend.setOnClickListener(v -> {
            String text = edtMessage.getText().toString().trim();
            if (!text.isEmpty()) {
                sendMessage(text);
                edtMessage.setText("");
            }
        });
    }

    private void loadInitialMessages() {
        long now = System.currentTimeMillis();
        // Giả lập lịch sử chat
        messageList.add(new MessageModel("m1", "chat1", shopperId, "Chào bạn! Mình có thể giúp gì cho bạn?", now - 60000));
        adapter.notifyDataSetChanged();
        rvMessages.smoothScrollToPosition(messageList.size() - 1);
    }

    private void sendMessage(String text) {
        long now = System.currentTimeMillis();
        messageList.add(new MessageModel("m" + now, "chat1", currentUserId, text, now));
        adapter.notifyItemInserted(messageList.size() - 1);
        rvMessages.smoothScrollToPosition(messageList.size() - 1);

        // Giả lập Shopper rep lại sau 2 giây
        simulateShopperReply(text);
    }

    private void simulateShopperReply(String userMessage) {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            long now = System.currentTimeMillis();
            String reply = "Dạ vâng, mình đã ghi nhận và đang đi lấy đồ ạ.";
            if (userMessage.toLowerCase().contains("cảm ơn")) {
                reply = "Không có chi bạn nhé!";
            } else if (userMessage.toLowerCase().contains("thiếu")) {
                reply = "Bạn chờ mình báo lại xem ở siêu thị còn hàng không nha.";
            }

            messageList.add(new MessageModel("m" + now, "chat1", shopperId, reply, now));
            adapter.notifyItemInserted(messageList.size() - 1);
            rvMessages.smoothScrollToPosition(messageList.size() - 1);
        }, 2000);
    }
}
