package com.example.gomarket;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView recyclerViewMessages;
    private EditText edtMessage;
    private ImageView btnSend, btnBack;
    private ArrayList<ChatMessage> messageList;
    private ChatMessageAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Ánh xạ view
        recyclerViewMessages = findViewById(R.id.recyclerViewMessages);

        // Tạo dữ liệu mẫu
        messageList = new ArrayList<>();
        messageList.add(new ChatMessage("Chào bạn! Tôi đang đi chợ cho bạn đây 😊", false, "14:30"));
        messageList.add(new ChatMessage("Cảm ơn bạn nhé! Nhớ chọn rau tươi giúp mình", true, "14:31"));
        messageList.add(new ChatMessage("Dạ được ạ, mình sẽ chọn kỹ cho bạn", false, "14:32"));
        messageList.add(new ChatMessage("Bạn ơi, thịt ba chỉ hết rồi, đổi thịt nạc vai được không?", false, "14:35"));
        messageList.add(new ChatMessage("Được luôn bạn, thịt nạc vai cũng ngon 👍", true, "14:36"));
        messageList.add(new ChatMessage("Ok bạn, mình mua xong rồi, đang trên đường giao nhé!", false, "14:40"));

        // Setup RecyclerView
        adapter = new ChatMessageAdapter(messageList);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerViewMessages.setLayoutManager(layoutManager);
        recyclerViewMessages.setAdapter(adapter);
    }
}
