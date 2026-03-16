package com.gomarket.service;

import com.gomarket.model.ShoppingRequest;
import com.gomarket.model.ShoppingRequestItem;
import com.gomarket.model.User;
import com.gomarket.repository.ShoppingRequestItemRepository;
import com.gomarket.repository.ShoppingRequestRepository;
import com.gomarket.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ShoppingRequestService {

    private final ShoppingRequestRepository requestRepository;
    private final ShoppingRequestItemRepository itemRepository;
    private final UserRepository userRepository;
    private final WalletService walletService;

    public ShoppingRequestService(ShoppingRequestRepository requestRepository,
                                   ShoppingRequestItemRepository itemRepository,
                                   UserRepository userRepository,
                                   WalletService walletService) {
        this.requestRepository = requestRepository;
        this.itemRepository = itemRepository;
        this.userRepository = userRepository;
        this.walletService = walletService;
    }

    @Transactional
    public ShoppingRequest createRequest(Map<String, Object> body) {
        ShoppingRequest request = new ShoppingRequest();
        request.setUserId(toLong(body.get("userId")));
        request.setDeliveryAddress((String) body.get("deliveryAddress"));
        request.setLatitude(toDouble(body.get("latitude")));
        request.setLongitude(toDouble(body.get("longitude")));
        request.setBudget(toDouble(body.get("budget")));
        request.setNotes((String) body.get("notes"));
        request.setPaymentMethod(body.getOrDefault("paymentMethod", "COD").toString());
        request.setStatus("OPEN");

        @SuppressWarnings("unchecked")
        List<Map<String, String>> itemsList = (List<Map<String, String>>) body.get("items");
        List<ShoppingRequestItem> items = new ArrayList<>();
        if (itemsList != null) {
            for (Map<String, String> itemData : itemsList) {
                ShoppingRequestItem item = new ShoppingRequestItem(
                        itemData.get("itemText"),
                        itemData.getOrDefault("quantityNote", "")
                );
                item.setShoppingRequest(request);
                items.add(item);
            }
        }
        request.setItems(items);

        ShoppingRequest saved = requestRepository.save(request);
        enrichWithUserInfo(saved);
        return saved;
    }

    public ShoppingRequest getRequest(Long id) {
        ShoppingRequest request = requestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn #" + id));
        enrichWithUserInfo(request);
        return request;
    }

    public List<ShoppingRequest> getUserRequests(Long userId) {
        List<ShoppingRequest> requests = requestRepository.findByUserIdOrderByCreatedAtDesc(userId);
        requests.forEach(this::enrichWithUserInfo);
        return requests;
    }

    public List<ShoppingRequest> getShopperRequests(Long shopperId) {
        List<ShoppingRequest> requests = requestRepository.findByShopperIdOrderByCreatedAtDesc(shopperId);
        requests.forEach(this::enrichWithUserInfo);
        return requests;
    }

    public List<ShoppingRequest> getNearbyOpenRequests(double lat, double lng) {
        List<ShoppingRequest> requests = requestRepository.findNearbyOpenRequests(lat, lng, 15.0);
        requests.forEach(this::enrichWithUserInfo);
        return requests;
    }

    @Transactional
    public ShoppingRequest acceptRequest(Long requestId, Long shopperId) {
        ShoppingRequest request = getRequest(requestId);
        if (!"OPEN".equals(request.getStatus())) {
            throw new RuntimeException("Đơn này đã được nhận hoặc đã hủy");
        }
        request.setShopperId(shopperId);
        request.setStatus("ACCEPTED");

        User shopper = userRepository.findById(shopperId).orElse(null);
        if (shopper != null) {
            request.setShopperName(shopper.getFullName());
            request.setShopperPhone(shopper.getPhone());
        }

        ShoppingRequest saved = requestRepository.save(request);
        enrichWithUserInfo(saved);
        return saved;
    }

    @Transactional
    public ShoppingRequest updateStatus(Long requestId, String status) {
        ShoppingRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn #" + requestId));
        request.setStatus(status);

        if ("COMPLETED".equals(status)) {
            // Tính tổng giá thực tế từ items đã mua
            double total = 0;
            if (request.getItems() != null) {
                for (ShoppingRequestItem item : request.getItems()) {
                    if (Boolean.TRUE.equals(item.getIsPurchased()) && item.getActualPrice() != null) {
                        total += item.getActualPrice();
                    }
                }
            }
            request.setTotalActualCost(total);

            // Cập nhật số đơn hoàn thành cho shopper
            if (request.getShopperId() != null) {
                User shopper = userRepository.findById(request.getShopperId()).orElse(null);
                if (shopper != null) {
                    shopper.setTotalOrders(shopper.getTotalOrders() + 1);
                    userRepository.save(shopper);
                }
            }
        }

        ShoppingRequest saved = requestRepository.save(request);
        enrichWithUserInfo(saved);
        return saved;
    }

    @Transactional
    public ShoppingRequestItem updateItem(Long itemId, Map<String, Object> body) {
        ShoppingRequestItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy item #" + itemId));

        if (body.containsKey("isPurchased")) {
            item.setIsPurchased((Boolean) body.get("isPurchased"));
        }
        if (body.containsKey("actualPrice")) {
            item.setActualPrice(toDouble(body.get("actualPrice")));
        }
        if (body.containsKey("note")) {
            item.setNote((String) body.get("note"));
        }

        return itemRepository.save(item);
    }

    @Transactional
    public void updateLocation(Long requestId, Double lat, Double lng) {
        ShoppingRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn #" + requestId));
        request.setShopperLat(lat);
        request.setShopperLng(lng);
        request.setLocationUpdatedAt(LocalDateTime.now());
        requestRepository.save(request);
    }

    @Transactional
    public ShoppingRequest cancelRequest(Long requestId) {
        ShoppingRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn #" + requestId));
        if ("COMPLETED".equals(request.getStatus())) {
            throw new RuntimeException("Không thể hủy đơn đã hoàn thành");
        }
        request.setStatus("CANCELLED");
        return requestRepository.save(request);
    }

    private void enrichWithUserInfo(ShoppingRequest request) {
        userRepository.findById(request.getUserId()).ifPresent(user -> {
            request.setUserName(user.getFullName());
            request.setUserPhone(user.getPhone());
        });
        if (request.getShopperId() != null) {
            userRepository.findById(request.getShopperId()).ifPresent(shopper -> {
                request.setShopperName(shopper.getFullName());
                request.setShopperPhone(shopper.getPhone());
            });
        }
    }

    private Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).longValue();
        return Long.parseLong(val.toString());
    }

    private Double toDouble(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).doubleValue();
        return Double.parseDouble(val.toString());
    }
}
