package com.example.gomarket;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gomarket.adapter.ShopperChecklistAdapter;
import com.example.gomarket.model.ShoppingRequest;
import com.example.gomarket.model.ShoppingRequestItem;
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

public class ShopperShoppingActivity extends AppCompatActivity
        implements ShopperChecklistAdapter.OnItemUpdateListener {

    private RecyclerView rvChecklist;
    private ShopperChecklistAdapter adapter;
    private List<ShoppingRequestItem> items = new ArrayList<>();

    private TextView tvBuyerName, tvBudget, tvDeliveryAddress, tvNotes;
    private TextView tvTotalCost, tvStatusButton, tvTitle;

    private ApiService apiService;
    private long requestId;
    private ShoppingRequest currentRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shopper_shopping);

        apiService = ApiClient.getApiService(this);
        requestId = getIntent().getLongExtra("REQUEST_ID", -1);

        initViews();
        loadRequest();
    }

    private void initViews() {
        rvChecklist = findViewById(R.id.rvChecklist);
        tvBuyerName = findViewById(R.id.tvBuyerName);
        tvBudget = findViewById(R.id.tvBudget);
        tvDeliveryAddress = findViewById(R.id.tvDeliveryAddress);
        tvNotes = findViewById(R.id.tvNotes);
        tvTotalCost = findViewById(R.id.tvTotalCost);
        tvStatusButton = findViewById(R.id.tvStatusButton);
        tvTitle = findViewById(R.id.tvTitle);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnUpdateStatus).setOnClickListener(v -> updateStatus());
        findViewById(R.id.btnCallBuyer).setOnClickListener(v -> callBuyer());
        findViewById(R.id.btnChatBuyer).setOnClickListener(v -> openChat());

        adapter = new ShopperChecklistAdapter(items, this);
        rvChecklist.setLayoutManager(new LinearLayoutManager(this));
        rvChecklist.setAdapter(adapter);
    }

    private void loadRequest() {
        if (requestId == -1) {
            Toast.makeText(this, "Không tìm thấy đơn", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        apiService.getShoppingRequest(requestId).enqueue(new Callback<ShoppingRequest>() {
            @Override
            public void onResponse(Call<ShoppingRequest> call, Response<ShoppingRequest> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentRequest = response.body();
                    displayRequest();
                }
            }
            @Override
            public void onFailure(Call<ShoppingRequest> call, Throwable t) {
                Toast.makeText(ShopperShoppingActivity.this,
                        "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayRequest() {
        tvBuyerName.setText(currentRequest.getUserName() != null ?
                currentRequest.getUserName() : "Người mua");
        tvBudget.setText(currentRequest.getBudget() != null ?
                String.format("💰 %,.0fđ", currentRequest.getBudget()) : "");
        tvDeliveryAddress.setText("📍 " +
                (currentRequest.getDeliveryAddress() != null ? currentRequest.getDeliveryAddress() : ""));

        if (currentRequest.getNotes() != null && !currentRequest.getNotes().isEmpty()) {
            tvNotes.setVisibility(View.VISIBLE);
            tvNotes.setText("📝 " + currentRequest.getNotes());
        }

        // Items
        items.clear();
        if (currentRequest.getItems() != null) {
            items.addAll(currentRequest.getItems());
        }
        adapter.notifyDataSetChanged();

        // Status button
        updateStatusButton();
        updateTotalCost();
    }

    private void updateStatusButton() {
        if (currentRequest == null) return;
        String status = currentRequest.getStatus();
        if (status == null) return;

        switch (status) {
            case "ACCEPTED":
                tvStatusButton.setText("Bắt đầu mua sắm");
                tvTitle.setText("Đơn #" + String.format("%03d", currentRequest.getId()));
                break;
            case "SHOPPING":
                tvStatusButton.setText("Đã mua xong - Bắt đầu giao");
                tvTitle.setText("Đang đi chợ");
                break;
            case "DELIVERING":
                tvStatusButton.setText("Đã giao hàng xong");
                tvTitle.setText("Đang giao hàng");
                break;
            case "COMPLETED":
                tvStatusButton.setText("Đơn đã hoàn thành ✓");
                tvTitle.setText("Hoàn thành");
                findViewById(R.id.btnUpdateStatus).setEnabled(false);
                break;
        }
    }

    private boolean isUpdatingStatus = false;

    private void updateStatus() {
        if (currentRequest == null || isUpdatingStatus) return;
        String currentStatus = currentRequest.getStatus();
        String nextStatus;

        switch (currentStatus) {
            case "ACCEPTED": nextStatus = "SHOPPING"; break;
            case "SHOPPING": nextStatus = "DELIVERING"; break;
            case "DELIVERING": nextStatus = "COMPLETED"; break;
            default: return;
        }

        // Confirm before completing
        if ("COMPLETED".equals(nextStatus)) {
            double totalCost = 0;
            int purchasedCount = 0;
            for (ShoppingRequestItem item : items) {
                if (item.getActualPrice() != null && item.getActualPrice() > 0) {
                    totalCost += item.getActualPrice();
                    purchasedCount++;
                }
            }

            String msg = "Bạn đã giao hàng thành công cho người mua?";
            if (purchasedCount > 0) {
                double shopperFee = currentRequest.getShopperFee() != null ? currentRequest.getShopperFee() : 0;
                msg += "\n\n📋 Chi tiết thanh toán:"
                     + "\n• Tiền hàng thực tế: " + String.format("%,.0fđ", totalCost)
                     + "\n• Phí đi chợ: " + String.format("%,.0fđ", shopperFee)
                     + "\n• Tổng trừ ví người mua: " + String.format("%,.0fđ", totalCost + shopperFee);
            }

            new AlertDialog.Builder(this)
                    .setTitle("Xác nhận giao hàng")
                    .setMessage(msg)
                    .setPositiveButton("Xác nhận", (d, w) -> syncAllItemsAndComplete(nextStatus))
                    .setNegativeButton("Hủy", null)
                    .show();
        } else {
            doUpdateStatus(nextStatus);
        }
    }

    private void doUpdateStatus(String nextStatus) {
        isUpdatingStatus = true;
        findViewById(R.id.btnUpdateStatus).setEnabled(false);

        Map<String, String> body = new HashMap<>();
        body.put("status", nextStatus);

        apiService.updateRequestStatus(requestId, body).enqueue(new Callback<ShoppingRequest>() {
            @Override
            public void onResponse(Call<ShoppingRequest> call, Response<ShoppingRequest> response) {
                isUpdatingStatus = false;
                if (response.isSuccessful() && response.body() != null) {
                    currentRequest = response.body();
                    updateStatusButton();
                    Toast.makeText(ShopperShoppingActivity.this,
                            "Cập nhật trạng thái thành công", Toast.LENGTH_SHORT).show();

                    if ("COMPLETED".equals(currentRequest.getStatus())) {
                        new AlertDialog.Builder(ShopperShoppingActivity.this)
                                .setTitle("Hoàn thành!")
                                .setMessage("Đơn hàng đã giao thành công! 🎉")
                                .setPositiveButton("OK", (d, w) -> finish())
                                .setCancelable(false)
                                .show();
                    } else {
                        findViewById(R.id.btnUpdateStatus).setEnabled(true);
                    }
                } else {
                    findViewById(R.id.btnUpdateStatus).setEnabled(true);
                    String errorMsg = "Không thể cập nhật trạng thái";
                    try {
                        if (response.errorBody() != null) {
                            String err = response.errorBody().string();
                            if (err.contains("chính mình") || err.contains("own")) {
                                errorMsg = "Bạn không thể cập nhật đơn của chính mình";
                            } else if (!err.isEmpty()) {
                                errorMsg = "Lỗi: " + err;
                            }
                        }
                    } catch (Exception ignored) {}
                    Toast.makeText(ShopperShoppingActivity.this,
                            errorMsg, Toast.LENGTH_LONG).show();
                }
            }
            @Override
            public void onFailure(Call<ShoppingRequest> call, Throwable t) {
                isUpdatingStatus = false;
                findViewById(R.id.btnUpdateStatus).setEnabled(true);
                Toast.makeText(ShopperShoppingActivity.this,
                        "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void updateTotalCost() {
        double total = 0;
        for (ShoppingRequestItem item : items) {
            if (item.getActualPrice() != null) total += item.getActualPrice();
        }
        tvTotalCost.setText(String.format("%,.0fđ", total));
    }

    private void callBuyer() {
        if (currentRequest != null && currentRequest.getUserPhone() != null) {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + currentRequest.getUserPhone()));
            startActivity(intent);
        }
    }

    private void openChat() {
        if (currentRequest == null) return;
        Intent intent = new Intent(this, OrderChatActivity.class);
        intent.putExtra("REQUEST_ID", requestId);
        intent.putExtra("OTHER_USER_ID", currentRequest.getUserId());
        intent.putExtra("OTHER_USER_NAME", currentRequest.getUserName());
        startActivity(intent);
    }

    @Override
    public void onItemChecked(ShoppingRequestItem item, boolean checked) {
        Map<String, Object> body = new HashMap<>();
        body.put("isPurchased", checked);
        if (item.getActualPrice() != null) body.put("actualPrice", item.getActualPrice());

        apiService.updateRequestItem(requestId, item.getId(), body).enqueue(new Callback<Map<String, Object>>() {
            @Override public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {}
            @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
        });
    }

    @Override
    public void onPriceChanged(ShoppingRequestItem item, Double price) {
        updateTotalCost();
        // Sync giá ngay lên server
        Map<String, Object> body = new HashMap<>();
        body.put("isPurchased", item.isPurchased());
        if (price != null) body.put("actualPrice", price);
        apiService.updateRequestItem(requestId, item.getId(), body).enqueue(new Callback<Map<String, Object>>() {
            @Override public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {}
            @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
        });
    }

    /** Sync tất cả item prices trước khi chuyển trạng thái COMPLETED */
    private void syncAllItemsAndComplete(String nextStatus) {
        // Đếm items cần sync
        int totalToSync = 0;
        for (ShoppingRequestItem item : items) {
            if (item.getActualPrice() != null && item.getActualPrice() > 0) {
                totalToSync++;
            }
        }

        if (totalToSync == 0) {
            // Không có giá → hỏi xác nhận
            new AlertDialog.Builder(this)
                    .setTitle("Chưa nhập giá thực tế")
                    .setMessage("Bạn chưa nhập giá thực tế cho món nào. Hệ thống sẽ dùng ngân sách ("
                            + String.format("%,.0fđ", currentRequest.getBudget())
                            + ") làm chi phí.\n\nBạn có muốn tiếp tục?")
                    .setPositiveButton("Tiếp tục", (d, w) -> doUpdateStatus(nextStatus))
                    .setNegativeButton("Nhập giá", null)
                    .show();
            return;
        }

        // Sync tất cả items trước, rồi complete
        final int[] synced = {0};
        final int syncCount = totalToSync;

        for (ShoppingRequestItem item : items) {
            if (item.getActualPrice() != null && item.getActualPrice() > 0) {
                Map<String, Object> body = new HashMap<>();
                body.put("isPurchased", true);
                body.put("actualPrice", item.getActualPrice());

                apiService.updateRequestItem(requestId, item.getId(), body).enqueue(new Callback<Map<String, Object>>() {
                    @Override
                    public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                        synced[0]++;
                        if (synced[0] >= syncCount) {
                            doUpdateStatus(nextStatus);
                        }
                    }
                    @Override
                    public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                        synced[0]++;
                        if (synced[0] >= syncCount) {
                            doUpdateStatus(nextStatus);
                        }
                    }
                });
            }
        }
    }
}
