package com.example.gomarket;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class ShopperMapActivity extends AppCompatActivity {

    private CardView btnCall, btnChat;
    private Button btnChonShopper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shopper_map);

        // Ánh xạ view
        btnCall = findViewById(R.id.btnCall);
        btnChat = findViewById(R.id.btnChat);
        btnChonShopper = findViewById(R.id.btnChonShopper);

        // Nút gọi điện cho Shopper
        btnCall.setOnClickListener(v ->
                Toast.makeText(this, "Đang gọi cho Shopper Nguyễn Văn A...", Toast.LENGTH_SHORT).show()
        );

        // Nút chat -> Mở ChatActivity
        btnChat.setOnClickListener(v -> {
            Intent intent = new Intent(ShopperMapActivity.this, ChatActivity.class);
            startActivity(intent);
        });

        // Nút chọn Shopper
        btnChonShopper.setOnClickListener(v ->
                Toast.makeText(this, "Đã chọn Shopper Nguyễn Văn A!", Toast.LENGTH_SHORT).show()
        );
    }
}
