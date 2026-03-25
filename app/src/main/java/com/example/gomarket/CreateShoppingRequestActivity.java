package com.example.gomarket;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gomarket.model.Product;
import com.example.gomarket.model.ShoppingRequest;
import com.example.gomarket.network.ApiClient;
import com.example.gomarket.network.ApiService;
import com.example.gomarket.util.SessionManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.card.MaterialCardView;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateShoppingRequestActivity extends AppCompatActivity {

    private EditText etAddress, etBudget, etNotes, etShopperFee;
    private TextView tvSummaryBudget, tvSummaryFee, tvSummaryTotal;
    private LinearLayout containerItems;
    private RadioGroup rgPayment;
    private ApiService apiService;
    private SessionManager session;
    private MapView mapView;
    private Marker mapMarker;

    private double latitude = 0, longitude = 0;
    private final List<View> itemViews = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_shopping_request);

        apiService = ApiClient.getApiService(this);
        session = new SessionManager(this);

        initViews();
        addItemRow(); // Start with 1 item row

        // Check if opened from AI Chef (pre-filled items with prices)
        boolean fromAIChef = getIntent().getBooleanExtra("fromAIChef", false);
        ArrayList<String> prefilledNames = getIntent().getStringArrayListExtra("itemNames");
        ArrayList<String> prefilledQuantities = getIntent().getStringArrayListExtra("itemQuantities");
        double[] prefilledPrices = getIntent().getDoubleArrayExtra("itemPrices");

        // Backward compatibility: old "items" key
        if (prefilledNames == null) {
            prefilledNames = getIntent().getStringArrayListExtra("items");
        }

        if (prefilledNames != null && !prefilledNames.isEmpty()) {
            containerItems.removeAllViews();
            itemViews.clear();
            for (int i = 0; i < prefilledNames.size(); i++) {
                String name = prefilledNames.get(i);
                String qty = (prefilledQuantities != null && i < prefilledQuantities.size())
                        ? prefilledQuantities.get(i) : "";
                double price = (prefilledPrices != null && i < prefilledPrices.length)
                        ? prefilledPrices[i] : 0;
                addItemRow(name, qty, price);
            }
        }

        double preBudget = getIntent().getDoubleExtra("budget", 0);
        if (preBudget > 0) {
            etBudget.setText(String.format("%.0f", preBudget));
        }
    }

    private void initViews() {
        etAddress = findViewById(R.id.etAddress);
        etBudget = findViewById(R.id.etBudget);
        etNotes = findViewById(R.id.etNotes);
        etShopperFee = findViewById(R.id.etShopperFee);
        tvSummaryBudget = findViewById(R.id.tvSummaryBudget);
        tvSummaryFee = findViewById(R.id.tvSummaryFee);
        tvSummaryTotal = findViewById(R.id.tvSummaryTotal);
        containerItems = findViewById(R.id.containerItems);
        rgPayment = findViewById(R.id.rgPayment);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnAddItem).setOnClickListener(v -> addItemRow());
        findViewById(R.id.btnSubmit).setOnClickListener(v -> showConfirmDialog());
        findViewById(R.id.btnGetLocation).setOnClickListener(v -> getCurrentLocation());
        findViewById(R.id.btnDictionary).setOnClickListener(v -> showProductDictionary());
        findViewById(R.id.btnEstimatePrice).setOnClickListener(v -> recalculateBudgetFromPrices());

        // Live summary update
        TextWatcher summaryWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { updateSummary(); }
        };
        etBudget.addTextChangedListener(summaryWatcher);
        etShopperFee.addTextChangedListener(summaryWatcher);

        // Map setup
        Configuration.getInstance().setUserAgentValue(getPackageName());
        mapView = findViewById(R.id.mapView);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(15.0);
        // Default to HCMC
        GeoPoint defaultPoint = new GeoPoint(10.7769, 106.7009);
        mapView.getController().setCenter(defaultPoint);

        mapMarker = new Marker(mapView);
        mapMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        mapMarker.setTitle("Vị trí giao hàng");
        mapView.getOverlays().add(mapMarker);

        // Tap map to pick location
        org.osmdroid.events.MapEventsReceiver receiver = new org.osmdroid.events.MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                latitude = p.getLatitude();
                longitude = p.getLongitude();
                updateMapMarker(p);
                reverseGeocode(latitude, longitude);
                return true;
            }
            @Override
            public boolean longPressHelper(GeoPoint p) { return false; }
        };
        mapView.getOverlays().add(0, new org.osmdroid.views.overlay.MapEventsOverlay(receiver));
    }

    private void addItemRow() {
        addItemRow("", "", 0);
    }

    private void addItemRow(String name, String quantity) {
        addItemRow(name, quantity, 0);
    }

    private void addItemRow(String name, String quantity, double estimatedPrice) {
        View itemView = LayoutInflater.from(this)
                .inflate(R.layout.item_add_shopping_item, containerItems, false);

        EditText etName = itemView.findViewById(R.id.etItemName);
        EditText etQty = itemView.findViewById(R.id.etQuantity);
        EditText etPrice = itemView.findViewById(R.id.etEstimatedPrice);

        if (!name.isEmpty()) etName.setText(name);
        if (!quantity.isEmpty()) etQty.setText(quantity);
        if (estimatedPrice > 0) etPrice.setText(String.format("%.0f", estimatedPrice));

        itemView.findViewById(R.id.btnRemove).setOnClickListener(v -> {
            if (itemViews.size() > 1) {
                containerItems.removeView(itemView);
                itemViews.remove(itemView);
            } else {
                Toast.makeText(this, "Cần ít nhất 1 món", Toast.LENGTH_SHORT).show();
            }
        });

        containerItems.addView(itemView);
        itemViews.add(itemView);
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
            return;
        }

        Toast.makeText(this, "Đang lấy vị trí...", Toast.LENGTH_SHORT).show();

        FusedLocationProviderClient fusedClient = LocationServices.getFusedLocationProviderClient(this);
        fusedClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                latitude = location.getLatitude();
                longitude = location.getLongitude();
                updateMapMarker(new GeoPoint(latitude, longitude));
                reverseGeocode(latitude, longitude);
            } else {
                Toast.makeText(this, "Không lấy được vị trí. Hãy bật GPS và thử lại.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void reverseGeocode(double lat, double lng) {
        try {
            android.location.Geocoder geocoder = new android.location.Geocoder(this, java.util.Locale.getDefault());
            List<android.location.Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            if (addresses != null && !addresses.isEmpty()) {
                android.location.Address addr = addresses.get(0);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i <= addr.getMaxAddressLineIndex(); i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(addr.getAddressLine(i));
                }
                String fullAddress = sb.toString();
                etAddress.setText(fullAddress);
                Toast.makeText(this, "Đã lấy địa chỉ", Toast.LENGTH_SHORT).show();
            } else {
                etAddress.setText(String.format("%.6f, %.6f", lat, lng));
                Toast.makeText(this, "Đã lấy tọa độ GPS", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            etAddress.setText(String.format("%.6f, %.6f", lat, lng));
            Toast.makeText(this, "Đã lấy tọa độ GPS", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateMapMarker(GeoPoint point) {
        mapMarker.setPosition(point);
        mapView.getController().animateTo(point);
        mapView.getController().setZoom(17.0);
        mapView.invalidate();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
    }

    private void updateSummary() {
        double budget = 0, fee = 0;
        try { budget = Double.parseDouble(etBudget.getText().toString().trim()); } catch (Exception ignored) {}
        try { fee = Double.parseDouble(etShopperFee.getText().toString().trim()); } catch (Exception ignored) {}
        tvSummaryBudget.setText(String.format("%,.0fđ", budget));
        tvSummaryFee.setText(String.format("%,.0fđ", fee));
        tvSummaryTotal.setText(String.format("%,.0fđ", budget + fee));
    }

    private void showConfirmDialog() {
        String address = etAddress.getText().toString().trim();
        String budgetStr = etBudget.getText().toString().trim();
        String feeStr = etShopperFee.getText().toString().trim();

        if (address.isEmpty()) {
            etAddress.setError("Vui lòng nhập địa chỉ");
            return;
        }

        // Validate shopper fee
        double shopperFee = 0;
        if (!feeStr.isEmpty()) {
            shopperFee = Double.parseDouble(feeStr);
            if (shopperFee > 0 && shopperFee < 20000) {
                etShopperFee.setError("Tối thiểu 20,000đ");
                return;
            }
        }

        // Count items
        int itemCount = 0;
        for (View itemView : itemViews) {
            EditText etName = itemView.findViewById(R.id.etItemName);
            if (!etName.getText().toString().trim().isEmpty()) itemCount++;
        }
        if (itemCount == 0) {
            Toast.makeText(this, "Vui lòng thêm ít nhất 1 món cần mua", Toast.LENGTH_SHORT).show();
            return;
        }

        double budget = 0;
        if (!budgetStr.isEmpty()) budget = Double.parseDouble(budgetStr);

        boolean isWallet = rgPayment.getCheckedRadioButtonId() == R.id.rbWallet;
        double total = budget + shopperFee;

        String msg = "📍 " + address + "\n"
                + "🛒 " + itemCount + " món cần mua\n"
                + "💰 Ngân sách: " + String.format("%,.0fđ", budget) + "\n"
                + "🤝 Phí đi chợ: " + String.format("%,.0fđ", shopperFee) + "\n"
                + "💳 Thanh toán: " + (isWallet ? "Ví GoMarket" : "Tiền mặt (COD)") + "\n"
                + "━━━━━━━━━━━━━━\n"
                + "📋 Tổng: " + String.format("%,.0fđ", total);

        if (isWallet) {
            msg += "\n\n⚠️ Số tiền " + String.format("%,.0fđ", total)
                    + " sẽ được đóng băng trong ví cho đơn này.";
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Xác nhận đặt đơn?")
                .setMessage(msg)
                .setPositiveButton("Đặt đơn", (d, w) -> submitRequest())
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void submitRequest() {
        String address = etAddress.getText().toString().trim();
        String budgetStr = etBudget.getText().toString().trim();
        String notes = etNotes.getText().toString().trim();

        if (address.isEmpty()) {
            etAddress.setError("Vui lòng nhập địa chỉ");
            return;
        }

        // Collect items
        List<Map<String, String>> items = new ArrayList<>();
        for (View itemView : itemViews) {
            EditText etName = itemView.findViewById(R.id.etItemName);
            EditText etQty = itemView.findViewById(R.id.etQuantity);
            String name = etName.getText().toString().trim();
            if (!name.isEmpty()) {
                Map<String, String> item = new HashMap<>();
                item.put("itemText", name);
                item.put("quantityNote", etQty.getText().toString().trim());
                items.add(item);
            }
        }

        if (items.isEmpty()) {
            Toast.makeText(this, "Vui lòng thêm ít nhất 1 món cần mua", Toast.LENGTH_SHORT).show();
            return;
        }

        // Build request body
        Map<String, Object> body = new HashMap<>();
        body.put("userId", (long) session.getUserId());
        body.put("deliveryAddress", address);
        body.put("items", items);
        body.put("notes", notes);

        if (!budgetStr.isEmpty()) {
            body.put("budget", Double.parseDouble(budgetStr));
        }

        String feeStr = etShopperFee.getText().toString().trim();
        if (!feeStr.isEmpty()) {
            body.put("shopperFee", Double.parseDouble(feeStr));
        }

        if (latitude != 0) {
            body.put("latitude", latitude);
            body.put("longitude", longitude);
        }

        boolean isCOD = rgPayment.getCheckedRadioButtonId() == R.id.rbCOD;
        body.put("paymentMethod", isCOD ? "COD" : "WALLET");

        // Disable submit button
        findViewById(R.id.btnSubmit).setEnabled(false);

        apiService.createShoppingRequest(body).enqueue(new Callback<ShoppingRequest>() {
            @Override
            public void onResponse(Call<ShoppingRequest> call, Response<ShoppingRequest> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(CreateShoppingRequestActivity.this,
                            "Đã tạo đơn! Đang tìm người đi chợ hộ...", Toast.LENGTH_LONG).show();

                    // Open OrderWaitingActivity to track order
                    android.content.Intent intent = new android.content.Intent(
                            CreateShoppingRequestActivity.this, OrderWaitingActivity.class);
                    intent.putExtra("REQUEST_ID", response.body().getId());
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(CreateShoppingRequestActivity.this,
                            "Lỗi tạo đơn", Toast.LENGTH_SHORT).show();
                    findViewById(R.id.btnSubmit).setEnabled(true);
                }
            }

            @Override
            public void onFailure(Call<ShoppingRequest> call, Throwable t) {
                Toast.makeText(CreateShoppingRequestActivity.this,
                        "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                findViewById(R.id.btnSubmit).setEnabled(true);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation();
        }
    }

    // ═══ TÍNH TỔNG GIÁ ═══

    private void recalculateBudgetFromPrices() {
        double total = 0;
        int count = 0;
        for (View itemView : itemViews) {
            EditText etPrice = itemView.findViewById(R.id.etEstimatedPrice);
            String priceStr = etPrice.getText().toString().trim();
            if (!priceStr.isEmpty()) {
                try {
                    total += Double.parseDouble(priceStr);
                    count++;
                } catch (NumberFormatException ignored) {}
            }
        }

        if (count == 0) {
            Toast.makeText(this, "Chưa có giá nào để tính", Toast.LENGTH_SHORT).show();
            return;
        }

        etBudget.setText(String.format("%.0f", total));
        Toast.makeText(this, String.format("Đã cập nhật ngân sách: %,.0fđ (%d món)", total, count),
                Toast.LENGTH_SHORT).show();
    }

    // ═══ TỪ ĐIỂN NGUYÊN LIỆU ═══

    private void showProductDictionary() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_product_search, null);
        dialog.setContentView(sheetView);

        // Make it full-screen height
        FrameLayout bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) {
            BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            behavior.setSkipCollapsed(true);
            bottomSheet.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
        }

        EditText etSearch = sheetView.findViewById(R.id.etSearchProduct);
        RecyclerView rvResults = sheetView.findViewById(R.id.rvSearchResults);
        LinearLayout emptyState = sheetView.findViewById(R.id.emptySearchState);
        TextView tvEmptyMessage = sheetView.findViewById(R.id.tvEmptyMessage);
        LinearLayout bottomAction = sheetView.findViewById(R.id.bottomAction);
        MaterialCardView btnFill = sheetView.findViewById(R.id.btnFillItems);
        TextView tvFillButton = sheetView.findViewById(R.id.tvFillButton);
        MaterialCardView badgeSelected = sheetView.findViewById(R.id.badgeSelected);
        TextView tvSelectedCount = sheetView.findViewById(R.id.tvSelectedCount);

        // Setup RecyclerView
        List<Product> searchResults = new ArrayList<>();
        Set<Integer> selectedIds = new HashSet<>();
        List<Product> selectedProducts = new ArrayList<>();

        ProductSearchAdapter adapter = new ProductSearchAdapter(searchResults, selectedIds, product -> {
            if (selectedIds.contains(product.getId())) {
                selectedIds.remove(product.getId());
                selectedProducts.removeIf(p -> p.getId() == product.getId());
            } else {
                selectedIds.add(product.getId());
                selectedProducts.add(product);
            }

            // Update UI
            int count = selectedProducts.size();
            if (count > 0) {
                bottomAction.setVisibility(View.VISIBLE);
                badgeSelected.setVisibility(View.VISIBLE);
                tvSelectedCount.setText("Đã chọn: " + count);
                tvFillButton.setText("Điền " + count + " món vào danh sách");
            } else {
                bottomAction.setVisibility(View.GONE);
                badgeSelected.setVisibility(View.GONE);
            }
        });

        rvResults.setLayoutManager(new LinearLayoutManager(this));
        rvResults.setAdapter(adapter);

        // Show empty state initially
        emptyState.setVisibility(View.VISIBLE);
        rvResults.setVisibility(View.GONE);

        // Search with debounce
        Handler searchHandler = new Handler(Looper.getMainLooper());
        Runnable[] searchRunnable = new Runnable[1];

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (searchRunnable[0] != null) {
                    searchHandler.removeCallbacks(searchRunnable[0]);
                }
                String query = s.toString().trim();
                if (query.length() < 2) {
                    searchResults.clear();
                    adapter.notifyDataSetChanged();
                    emptyState.setVisibility(View.VISIBLE);
                    rvResults.setVisibility(View.GONE);
                    tvEmptyMessage.setText("Gõ tên nguyên liệu để tìm kiếm");
                    return;
                }

                searchRunnable[0] = () -> {
                    tvEmptyMessage.setText("Đang tìm kiếm...");
                    emptyState.setVisibility(View.VISIBLE);
                    rvResults.setVisibility(View.GONE);

                    apiService.hybridSearch(query).enqueue(new Callback<List<Product>>() {
                        @Override
                        public void onResponse(Call<List<Product>> call, Response<List<Product>> response) {
                            if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                                searchResults.clear();
                                searchResults.addAll(response.body());
                                adapter.notifyDataSetChanged();
                                rvResults.setVisibility(View.VISIBLE);
                                emptyState.setVisibility(View.GONE);
                            } else {
                                searchResults.clear();
                                adapter.notifyDataSetChanged();
                                emptyState.setVisibility(View.VISIBLE);
                                rvResults.setVisibility(View.GONE);
                                tvEmptyMessage.setText("Không tìm thấy \"" + query + "\"");
                            }
                        }

                        @Override
                        public void onFailure(Call<List<Product>> call, Throwable t) {
                            emptyState.setVisibility(View.VISIBLE);
                            rvResults.setVisibility(View.GONE);
                            tvEmptyMessage.setText("Lỗi kết nối");
                        }
                    });
                };
                searchHandler.postDelayed(searchRunnable[0], 400);
            }
        });

        // Fill button → add selected products to shopping list (with price!)
        btnFill.setOnClickListener(v -> {
            for (Product product : selectedProducts) {
                String unit = product.getUnit() != null ? product.getUnit() : "";
                addItemRow(product.getName(), unit, product.getPrice());
            }
            Toast.makeText(this, "Đã thêm " + selectedProducts.size() + " món vào danh sách",
                    Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
        etSearch.requestFocus();
    }

    // ═══ INLINE ADAPTER FOR PRODUCT SEARCH ═══

    interface OnProductToggleListener {
        void onToggle(Product product);
    }

    static class ProductSearchAdapter extends RecyclerView.Adapter<ProductSearchAdapter.VH> {
        private final List<Product> products;
        private final Set<Integer> selectedIds;
        private final OnProductToggleListener listener;

        ProductSearchAdapter(List<Product> products, Set<Integer> selectedIds,
                             OnProductToggleListener listener) {
            this.products = products;
            this.selectedIds = selectedIds;
            this.listener = listener;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_product_search_result, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            Product p = products.get(position);
            h.tvName.setText(p.getName());
            h.tvCategory.setText(p.getCategory() != null ? p.getCategory() : "");
            h.tvUnit.setText(p.getUnit() != null ? "· " + p.getUnit() : "");
            h.tvPrice.setText(p.getFormattedPrice());

            boolean selected = selectedIds.contains(p.getId());
            h.checkCircle.setBackgroundResource(selected
                    ? R.drawable.bg_badge_ai : R.drawable.bg_input_rounded);
            h.tvCheckMark.setVisibility(selected ? View.VISIBLE : View.GONE);

            h.itemView.setOnClickListener(v -> {
                listener.onToggle(p);
                notifyItemChanged(position);
            });
        }

        @Override
        public int getItemCount() {
            return products.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvCategory, tvUnit, tvPrice, tvCheckMark;
            View checkCircle;

            VH(View v) {
                super(v);
                tvName = v.findViewById(R.id.tvProductName);
                tvCategory = v.findViewById(R.id.tvCategory);
                tvUnit = v.findViewById(R.id.tvUnit);
                tvPrice = v.findViewById(R.id.tvPrice);
                tvCheckMark = v.findViewById(R.id.tvCheckMark);
                checkCircle = v.findViewById(R.id.checkCircle);
            }
        }
    }
}
