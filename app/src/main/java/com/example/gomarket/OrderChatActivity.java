package com.example.gomarket;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gomarket.adapter.ChatMessageAdapter;
import com.example.gomarket.model.ChatMessage;
import com.example.gomarket.network.ApiClient;
import com.example.gomarket.network.ApiService;
import com.example.gomarket.util.SessionManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderChatActivity extends AppCompatActivity {

    private RecyclerView rvMessages;
    private EditText etInput;
    private ChatMessageAdapter adapter;
    private List<ChatMessage> messages = new ArrayList<>();

    private ApiService apiService;
    private SessionManager session;

    private long requestId;
    private long otherUserId;
    private String otherUserName;

    private Handler pollHandler;
    private Runnable pollRunnable;
    private long lastMessageId = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_chat);

        apiService = ApiClient.getApiService(this);
        session = new SessionManager(this);

        requestId = getIntent().getLongExtra("REQUEST_ID", -1);
        otherUserId = getIntent().getLongExtra("OTHER_USER_ID", -1);
        otherUserName = getIntent().getStringExtra("OTHER_USER_NAME");

        initViews();
        loadMessages();
        startPolling();
    }

    private void initViews() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        TextView tvTitle = findViewById(R.id.tvChatTitle);
        TextView tvSubtitle = findViewById(R.id.tvChatSubtitle);
        tvTitle.setText(otherUserName != null ? otherUserName : "Chat");

        if (requestId > 0) {
            tvSubtitle.setVisibility(View.VISIBLE);
            tvSubtitle.setText("Đơn #" + String.format("%03d", requestId));
        }

        // Order banner
        LinearLayout orderBanner = findViewById(R.id.orderBanner);
        TextView tvBanner = findViewById(R.id.tvOrderBannerText);
        if (requestId > 0) {
            orderBanner.setVisibility(View.VISIBLE);
            tvBanner.setText("📋 Đơn #" + String.format("%03d", requestId));
        }

        rvMessages = findViewById(R.id.rvChatMessages);
        etInput = findViewById(R.id.etChatInput);

        adapter = new ChatMessageAdapter(messages, session.getUserId());
        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        rvMessages.setLayoutManager(lm);
        rvMessages.setAdapter(adapter);

        findViewById(R.id.btnSendChat).setOnClickListener(v -> sendMessage());
    }

    private void sendMessage() {
        String text = etInput.getText().toString().trim();
        if (text.isEmpty() || requestId <= 0 || otherUserId <= 0) return;

        etInput.setText("");

        Map<String, Object> body = new HashMap<>();
        body.put("requestId", requestId);
        body.put("senderId", (long) session.getUserId());
        body.put("receiverId", otherUserId);
        body.put("message", text);

        apiService.sendChatMessage(body).enqueue(new Callback<ChatMessage>() {
            @Override
            public void onResponse(Call<ChatMessage> call, Response<ChatMessage> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ChatMessage msg = response.body();
                    messages.add(msg);
                    lastMessageId = msg.getId();
                    adapter.notifyItemInserted(messages.size() - 1);
                    rvMessages.smoothScrollToPosition(messages.size() - 1);
                }
            }

            @Override
            public void onFailure(Call<ChatMessage> call, Throwable t) {}
        });
    }

    private void loadMessages() {
        if (requestId <= 0) return;

        apiService.getChatMessages(requestId, null).enqueue(new Callback<List<ChatMessage>>() {
            @Override
            public void onResponse(Call<List<ChatMessage>> call, Response<List<ChatMessage>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    messages.clear();
                    messages.addAll(response.body());
                    if (!messages.isEmpty()) {
                        lastMessageId = messages.get(messages.size() - 1).getId();
                    }
                    adapter.notifyDataSetChanged();
                    if (!messages.isEmpty()) {
                        rvMessages.scrollToPosition(messages.size() - 1);
                    }
                }
            }

            @Override
            public void onFailure(Call<List<ChatMessage>> call, Throwable t) {}
        });
    }

    private void startPolling() {
        pollHandler = new Handler(Looper.getMainLooper());
        pollRunnable = new Runnable() {
            @Override
            public void run() {
                pollNewMessages();
                pollHandler.postDelayed(this, 3000);
            }
        };
        pollHandler.postDelayed(pollRunnable, 3000);
    }

    private void stopPolling() {
        if (pollHandler != null && pollRunnable != null) {
            pollHandler.removeCallbacks(pollRunnable);
        }
    }

    private void pollNewMessages() {
        if (requestId <= 0) return;

        apiService.getChatMessages(requestId, lastMessageId > 0 ? lastMessageId : null)
                .enqueue(new Callback<List<ChatMessage>>() {
                    @Override
                    public void onResponse(Call<List<ChatMessage>> call, Response<List<ChatMessage>> response) {
                        if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                            List<ChatMessage> newMsgs = response.body();
                            // Only add messages we don't already have (if polling with afterId)
                            if (lastMessageId > 0) {
                                messages.addAll(newMsgs);
                            } else {
                                messages.clear();
                                messages.addAll(newMsgs);
                            }
                            lastMessageId = messages.get(messages.size() - 1).getId();
                            adapter.notifyDataSetChanged();
                            rvMessages.smoothScrollToPosition(messages.size() - 1);
                        }
                    }

                    @Override
                    public void onFailure(Call<List<ChatMessage>> call, Throwable t) {}
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (pollHandler != null && pollRunnable != null) {
            pollHandler.postDelayed(pollRunnable, 3000);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopPolling();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopPolling();
    }
}
