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

    // Order data
    private int orderId = -1;
    private ArrayList<String> orderItemsList;

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
        orderId = getIntent().getIntExtra("ORDER_ID", -1);
        orderItemsList = getIntent().getStringArrayListExtra("ORDER_ITEMS");

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

        // Hiển thị thông tin đơn hàng trong order section
        // Ưu tiên ORDER_ITEMS (flow đặt hàng), fallback MISSING_INGREDIENTS (flow recipe cũ)
        ArrayList<String> items = orderItemsList;
        if (items == null || items.isEmpty()) {
            items = getIntent().getStringArrayListExtra("MISSING_INGREDIENTS");
        }

        View orderSection = findViewById(R.id.orderSection);
        if (items != null && !items.isEmpty()) {
            if (orderSection != null) orderSection.setVisibility(View.VISIBLE);
            TextView chip1 = findViewById(R.id.orderChip1);
            TextView chip2 = findViewById(R.id.orderChip2);
            TextView chipMore = findViewById(R.id.orderChipMore);
            if (chip1 != null && items.size() > 0) {
                chip1.setVisibility(View.VISIBLE);
                chip1.setText(items.get(0));
            }
            if (chip2 != null && items.size() > 1) {
                chip2.setVisibility(View.VISIBLE);
                chip2.setText(items.get(1));
            }
            if (chipMore != null && items.size() > 2) {
                chipMore.setVisibility(View.VISIBLE);
                chipMore.setText("+" + (items.size() - 2) + " món khác");
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

        if (orderId > 0 && orderItemsList != null && !orderItemsList.isEmpty()) {
            // Flow đặt hàng: shopper nhận đơn và xác nhận
            String itemsSummary = orderItemsList.size() <= 3
                    ? String.join(", ", orderItemsList)
                    : orderItemsList.get(0) + ", " + orderItemsList.get(1)
                        + " và " + (orderItemsList.size() - 2) + " món khác";

            messageList.add(new MessageModel("m1", "chat1", shopperId,
                    "Chào bạn! Mình đã nhận đơn #" + orderId + " của bạn rồi nha!", now - 60000));
            messageList.add(new MessageModel("m2", "chat1", shopperId,
                    "Đơn gồm: " + itemsSummary + ". Mình đang đi chợ lấy đồ cho bạn ngay!", now - 55000));
            messageList.add(new MessageModel("m3", "chat1", shopperId,
                    "Nếu có món nào hết hàng mình sẽ báo bạn để thay thế nhé!", now - 50000));
        } else {
            messageList.add(new MessageModel("m1", "chat1", shopperId,
                    "Chào bạn! Mình có thể giúp gì cho bạn?", now - 60000));
        }

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
            String reply;
            String lower = userMessage.toLowerCase();

            if (lower.contains("cảm ơn") || lower.contains("cam on")) {
                reply = "Không có chi bạn nhé! Mình giao sớm cho bạn!";
            } else if (lower.contains("thiếu") || lower.contains("còn hàng") || lower.contains("het hang")) {
                reply = "Bạn chờ mình kiểm tra ở siêu thị xem còn hàng không nha.";
            } else if (lower.contains("giá") || lower.contains("gia")) {
                reply = "Mình sẽ kiểm tra giá và báo lại cho bạn ngay.";
            } else if (lower.contains("thay") || lower.contains("đổi")) {
                reply = "OK bạn, mình sẽ tìm món thay thế tương tự nhé!";
            } else if (lower.contains("không cần") || lower.contains("khong can")) {
                reply = "Dạ, mình bỏ món đó ra khỏi danh sách nhé!";
            } else if (orderId > 0) {
                reply = "Dạ mình ghi nhận rồi. Đang mua đồ cho đơn #" + orderId + " của bạn nha!";
            } else {
                reply = "Dạ vâng, mình đã ghi nhận và đang đi lấy đồ ạ.";
            }

            messageList.add(new MessageModel("m" + now, "chat1", shopperId, reply, now));
            adapter.notifyItemInserted(messageList.size() - 1);
            rvMessages.smoothScrollToPosition(messageList.size() - 1);
        }, 2000);
    }
}
