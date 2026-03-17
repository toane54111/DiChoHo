package com.example.gomarket;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gomarket.adapter.ConversationAdapter;
import com.example.gomarket.network.ApiClient;
import com.example.gomarket.network.ApiService;
import com.example.gomarket.util.SessionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ConversationListActivity extends AppCompatActivity
        implements ConversationAdapter.OnConversationClickListener {

    private RecyclerView rvConversations;
    private LinearLayout emptyState;
    private ConversationAdapter adapter;
    private List<Map<String, Object>> conversations = new ArrayList<>();

    private ApiService apiService;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation_list);

        apiService = ApiClient.getApiService(this);
        session = new SessionManager(this);

        rvConversations = findViewById(R.id.rvConversations);
        emptyState = findViewById(R.id.emptyState);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        adapter = new ConversationAdapter(conversations, this);
        rvConversations.setLayoutManager(new LinearLayoutManager(this));
        rvConversations.setAdapter(adapter);

        loadConversations();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadConversations();
    }

    private void loadConversations() {
        apiService.getConversations(session.getUserId()).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    conversations.clear();
                    conversations.addAll(response.body());
                    adapter.notifyDataSetChanged();

                    if (conversations.isEmpty()) {
                        emptyState.setVisibility(View.VISIBLE);
                        rvConversations.setVisibility(View.GONE);
                    } else {
                        emptyState.setVisibility(View.GONE);
                        rvConversations.setVisibility(View.VISIBLE);
                    }
                }
            }

            @Override
            public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {}
        });
    }

    @Override
    public void onConversationClick(Map<String, Object> conversation) {
        Object reqIdObj = conversation.get("requestId");
        Object otherIdObj = conversation.get("otherUserId");
        String otherName = (String) conversation.get("otherUserName");

        long reqId = reqIdObj instanceof Number ? ((Number) reqIdObj).longValue() : 0;
        long otherId = otherIdObj instanceof Number ? ((Number) otherIdObj).longValue() : 0;

        Intent intent = new Intent(this, OrderChatActivity.class);
        intent.putExtra("REQUEST_ID", reqId);
        intent.putExtra("OTHER_USER_ID", otherId);
        intent.putExtra("OTHER_USER_NAME", otherName);
        startActivity(intent);
    }
}
