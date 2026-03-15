package com.example.gomarket;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.gomarket.adapter.MessageAdapter;
import com.example.gomarket.model.MessageModel;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView rvMessages;
    private EditText edtMessage;
    private ImageView btnSend;
    private ImageView btnBack;
    private ImageView btnImagePicker;
    private TextView tvShopperNameTitle;
    private ShapeableImageView ivShopperAvatar;
    private View quickRepliesContainer;

    private List<MessageModel> messageList;
    private MessageAdapter adapter;
    private String currentUserId = "user123";
    private String shopperId;
    private String shopperName;
    private String shopperAvatarUrl;

    private static final int REQUEST_IMAGE_PERMISSION = 100;

    // Activity result launcher for image picker
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        initViews();
        initImagePicker();
        setupListeners();
        loadChatData();
    }

    private void initViews() {
        rvMessages = findViewById(R.id.rvMessages);
        edtMessage = findViewById(R.id.edtMessage);
        btnSend = findViewById(R.id.btnSend);
        btnBack = findViewById(R.id.btnBack);
        btnImagePicker = findViewById(R.id.btnImagePicker);
        tvShopperNameTitle = findViewById(R.id.tvShopperNameTitle);
        ivShopperAvatar = findViewById(R.id.ivShopperAvatar);
        quickRepliesContainer = findViewById(R.id.quickRepliesContainer);
    }

    private void initImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            sendImageMessage(imageUri.toString());
                        }
                    }
                }
        );
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnSend.setOnClickListener(v -> {
            String text = edtMessage.getText().toString().trim();
            if (!text.isEmpty()) {
                sendMessage(text);
                edtMessage.setText("");
            }
        });

        btnImagePicker.setOnClickListener(v -> openImagePicker());

        // Quick replies listeners
        findViewById(R.id.quickReply1).setOnClickListener(v -> sendQuickReply("Được, lấy đi chị"));
        findViewById(R.id.quickReply2).setOnClickListener(v -> sendQuickReply("Không cần món đó"));
        findViewById(R.id.quickReply3).setOnClickListener(v -> sendQuickReply("Thay thế"));
        findViewById(R.id.quickReply4).setOnClickListener(v -> sendQuickReply("Cảm ơn bạn"));
    }

    private void loadChatData() {
        shopperId = getIntent().getStringExtra("SHOPPER_ID");
        shopperName = getIntent().getStringExtra("SHOPPER_NAME");
        shopperAvatarUrl = getIntent().getStringExtra("SHOPPER_AVATAR");

        if (shopperName != null) {
            tvShopperNameTitle.setText(shopperName);
        } else {
            tvShopperNameTitle.setText("Chat với Shopper");
            shopperId = "s1";
        }

        // Load shopper avatar
        if (shopperAvatarUrl != null && !shopperAvatarUrl.isEmpty()) {
            Glide.with(this)
                    .load(shopperAvatarUrl)
                    .circleCrop()
                    .placeholder(R.drawable.avatar_placeholder)
                    .error(R.drawable.avatar_placeholder)
                    .into(ivShopperAvatar);
        } else {
            ivShopperAvatar.setImageResource(R.drawable.avatar_placeholder);
        }

        // Đơn hàng: hiển thị nguyên liệu cần mua từ intent
        ArrayList<String> missing = getIntent().getStringArrayListExtra("MISSING_INGREDIENTS");
        View orderSection = findViewById(R.id.orderSection);
        if (missing != null && !missing.isEmpty()) {
            if (orderSection != null) orderSection.setVisibility(View.VISIBLE);
            TextView chip1 = findViewById(R.id.orderChip1);
            TextView chip2 = findViewById(R.id.orderChip2);
            TextView chipMore = findViewById(R.id.orderChipMore);
            if (chip1 != null && missing.size() > 0) {
                chip1.setVisibility(View.VISIBLE);
                chip1.setText(missing.get(0) + " 1kg");
            }
            if (chip2 != null && missing.size() > 1) {
                chip2.setVisibility(View.VISIBLE);
                chip2.setText(missing.get(1) + " 1 bó");
            }
            if (chipMore != null && missing.size() > 2) {
                chipMore.setVisibility(View.VISIBLE);
                chipMore.setText("+" + (missing.size() - 2) + " món khác");
            }
        } else {
            if (orderSection != null) orderSection.setVisibility(View.GONE);
        }

        messageList = new ArrayList<>();
        adapter = new MessageAdapter(this, messageList, currentUserId);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);
        rvMessages.setAdapter(adapter);

        loadInitialMessages();
    }

    private void loadInitialMessages() {
        long now = System.currentTimeMillis();
        messageList.add(new MessageModel("m1", "chat1", shopperId, "Chào bạn! Mình có thể giúp gì cho bạn?", now - 60000));
        adapter.notifyDataSetChanged();
        rvMessages.smoothScrollToPosition(messageList.size() - 1);
    }

    private void sendMessage(String text) {
        long now = System.currentTimeMillis();
        messageList.add(new MessageModel("m" + now, "chat1", currentUserId, text, now));
        adapter.notifyItemInserted(messageList.size() - 1);
        rvMessages.smoothScrollToPosition(messageList.size() - 1);
        simulateShopperReply(text);
    }

    private void sendQuickReply(String text) {
        edtMessage.setText(text);
        edtMessage.setSelection(text.length());
        sendMessage(text);
    }

    private void sendImageMessage(String imageUri) {
        long now = System.currentTimeMillis();
        MessageModel imageMessage = new MessageModel("m" + now, "chat1", currentUserId, "[Hình ảnh]", now);
        imageMessage.setImageUrl(imageUri);
        messageList.add(imageMessage);
        adapter.notifyItemInserted(messageList.size() - 1);
        rvMessages.smoothScrollToPosition(messageList.size() - 1);
        Toast.makeText(this, "Đã gửi hình ảnh", Toast.LENGTH_SHORT).show();
        simulateShopperReply("hình ảnh");
    }

    private void openImagePicker() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_IMAGE_PERMISSION);
        } else {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_IMAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImagePicker();
            } else {
                Toast.makeText(this, "Cần cấp quyền để gửi hình ảnh", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void simulateShopperReply(String userMessage) {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            long now = System.currentTimeMillis();
            String reply = "Dạ vâng, mình đã ghi nhận và đang đi lấy đồ ạ.";
            if (userMessage.toLowerCase().contains("cảm ơn")) {
                reply = "Không có chi bạn nhé!";
            } else if (userMessage.toLowerCase().contains("thiếu") || userMessage.toLowerCase().contains("còn hàng")) {
                reply = "Bạn chờ mình báo lại xem ở siêu thị còn hàng không nha.";
            } else if (userMessage.toLowerCase().contains("giá")) {
                reply = "Mình sẽ kiểm tra giá và báo lại cho bạn ngay.";
            }

            messageList.add(new MessageModel("m" + now, "chat1", shopperId, reply, now));
            adapter.notifyItemInserted(messageList.size() - 1);
            rvMessages.smoothScrollToPosition(messageList.size() - 1);
        }, 2000);
    }
}
