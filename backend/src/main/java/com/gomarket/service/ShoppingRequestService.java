package com.gomarket.service;

import com.gomarket.model.ChatMessage;
import com.gomarket.model.ShoppingRequest;
import com.gomarket.model.ShoppingRequestItem;
import com.gomarket.model.User;
import com.gomarket.repository.ChatMessageRepository;
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
    private final ChatMessageRepository chatMessageRepository;

    public ShoppingRequestService(ShoppingRequestRepository requestRepository,
                                   ShoppingRequestItemRepository itemRepository,
                                   UserRepository userRepository,
                                   WalletService walletService,
                                   ChatMessageRepository chatMessageRepository) {
        this.requestRepository = requestRepository;
        this.itemRepository = itemRepository;
        this.userRepository = userRepository;
        this.walletService = walletService;
        this.chatMessageRepository = chatMessageRepository;
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

        // Shopper fee (tiền công)
        Double shopperFee = toDouble(body.get("shopperFee"));
        if (shopperFee == null) shopperFee = 0.0;
        if (shopperFee > 0 && shopperFee < 20000) {
            throw new RuntimeException("Phí đi chợ tối thiểu là 20,000đ");
        }
        request.setShopperFee(shopperFee);

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

        // Đóng băng tiền nếu thanh toán qua ví
        if ("WALLET".equals(request.getPaymentMethod())) {
            Double budget = request.getBudget() != null ? request.getBudget() : 0.0;
            double totalFreeze = budget + shopperFee;
            if (totalFreeze > 0) {
                // Save first to get ID
                ShoppingRequest saved = requestRepository.save(request);
                walletService.freeze(request.getUserId(), saved.getId(), (long) totalFreeze);
                saved.setFrozenAmount(totalFreeze);
                saved = requestRepository.save(saved);
                enrichWithUserInfo(saved);
                return saved;
            }
        }

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

        // Auto-chat: Shopper nhận đơn
        sendAutoChat(saved.getId(), shopperId, saved.getUserId(),
                buildAcceptMessage(saved, shopper));

        return saved;
    }

    @Transactional
    public ShoppingRequest updateStatus(Long requestId, String status) {
        ShoppingRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn #" + requestId));

        // Nếu status giống cũ → bỏ qua (tránh double-click)
        String oldStatus = request.getStatus();
        if (status.equals(oldStatus)) {
            enrichWithUserInfo(request);
            return request;
        }

        request.setStatus(status);

        if ("COMPLETED".equals(status)) {
            // Tính tổng giá thực tế từ items đã mua
            double total = 0;
            boolean hasActualPrices = false;
            if (request.getItems() != null) {
                for (ShoppingRequestItem item : request.getItems()) {
                    if (Boolean.TRUE.equals(item.getIsPurchased()) && item.getActualPrice() != null) {
                        total += item.getActualPrice();
                        hasActualPrices = true;
                    }
                }
            }

            // Nếu shopper không nhập giá thực tế → dùng ngân sách làm chi phí thực tế
            double budget = request.getBudget() != null ? request.getBudget() : 0;
            if (!hasActualPrices) {
                total = budget;
            }
            request.setTotalActualCost(total);

            // Xử lý thanh toán ví
            if ("WALLET".equals(request.getPaymentMethod())) {
                double shopperFee = request.getShopperFee() != null ? request.getShopperFee() : 0;
                double frozenAmount = request.getFrozenAmount() != null ? request.getFrozenAmount() : 0;

                // Bước 1: Hoàn lại TOÀN BỘ tiền đóng băng
                if (frozenAmount > 0) {
                    walletService.unfreeze(request.getUserId(), request.getId(), (long) frozenAmount);
                }

                // Bước 2: Trừ chi phí thực tế (tiền hàng thực tế + phí đi chợ)
                long actualCharge = (long) (total + shopperFee);
                if (actualCharge > 0) {
                    walletService.pay(request.getUserId(), request.getId(), actualCharge);
                }

                // Bước 3: Chuyển phí đi chợ cho shopper
                if (shopperFee > 0 && request.getShopperId() != null) {
                    walletService.creditShopper(request.getShopperId(), request.getId(), (long) shopperFee);
                }

                request.setFrozenAmount(0.0);
                request.setPaymentStatus("PAID");
            }

            // Cập nhật số đơn hoàn thành cho shopper
            if (request.getShopperId() != null) {
                User shopper = userRepository.findById(request.getShopperId()).orElse(null);
                if (shopper != null) {
                    int current = shopper.getTotalOrders() != null ? shopper.getTotalOrders() : 0;
                    shopper.setTotalOrders(current + 1);
                    userRepository.save(shopper);
                }
            }
        }

        ShoppingRequest saved = requestRepository.save(request);
        enrichWithUserInfo(saved);

        // Auto-chat theo trạng thái
        if (saved.getShopperId() != null) {
            String autoMsg = null;
            switch (status) {
                case "SHOPPING":
                    autoMsg = "🛒 Em bắt đầu đi chợ cho bạn rồi nha! Có gì thay đổi cứ nhắn em.";
                    break;
                case "DELIVERING":
                    autoMsg = buildDeliveringMessage(saved);
                    break;
                case "COMPLETED":
                    autoMsg = "✅ Đơn hàng đã giao thành công! Cảm ơn bạn đã sử dụng GoMarket. Hẹn gặp lại! 🎉";
                    break;
            }
            if (autoMsg != null) {
                sendAutoChat(saved.getId(), saved.getShopperId(), saved.getUserId(), autoMsg);
            }
        }

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

        // Hoàn tiền đóng băng nếu thanh toán ví
        if ("WALLET".equals(request.getPaymentMethod()) && request.getFrozenAmount() != null
                && request.getFrozenAmount() > 0) {
            walletService.unfreeze(request.getUserId(), request.getId(),
                    request.getFrozenAmount().longValue());
            request.setFrozenAmount(0.0);
        }

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

    // ═══ AUTO CHAT HELPERS ═══

    private void sendAutoChat(Long requestId, Long senderId, Long receiverId, String message) {
        try {
            ChatMessage msg = new ChatMessage();
            msg.setRequestId(requestId);
            msg.setSenderId(senderId);
            msg.setReceiverId(receiverId);
            msg.setMessage(message);
            chatMessageRepository.save(msg);
        } catch (Exception e) {
            // Không để lỗi chat ảnh hưởng flow chính
        }
    }

    private String buildAcceptMessage(ShoppingRequest request, User shopper) {
        String shopperName = shopper != null ? shopper.getFullName() : "Shopper";
        StringBuilder sb = new StringBuilder();
        sb.append("👋 Chào bạn! Em là ").append(shopperName)
          .append(", em đã nhận đơn #").append(String.format("%03d", request.getId()))
          .append(" của bạn rồi nha!\n\n");

        // Liệt kê items
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            sb.append("📋 Danh sách cần mua:\n");
            for (ShoppingRequestItem item : request.getItems()) {
                sb.append("• ").append(item.getItemText());
                if (item.getQuantityNote() != null && !item.getQuantityNote().isEmpty()) {
                    sb.append(" (").append(item.getQuantityNote()).append(")");
                }
                sb.append("\n");
            }
        }

        sb.append("\nEm sẽ bắt đầu đi chợ sớm nhất có thể. Nếu có gì thay đổi bạn cứ nhắn em nhé! 😊");
        return sb.toString();
    }

    private String buildDeliveringMessage(ShoppingRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("🚗 Em đã mua xong và đang giao hàng tới bạn!\n\n");

        // Liệt kê items đã mua + giá thực tế
        if (request.getItems() != null) {
            sb.append("📋 Chi tiết đã mua:\n");
            double total = 0;
            for (ShoppingRequestItem item : request.getItems()) {
                if (Boolean.TRUE.equals(item.getIsPurchased())) {
                    sb.append("✅ ").append(item.getItemText());
                    if (item.getActualPrice() != null && item.getActualPrice() > 0) {
                        sb.append(" — ").append(String.format("%,.0fđ", item.getActualPrice()));
                        total += item.getActualPrice();
                    }
                    sb.append("\n");
                } else {
                    sb.append("❌ ").append(item.getItemText()).append(" (hết hàng)\n");
                }
            }
            if (total > 0) {
                sb.append("\n💰 Tổng tiền hàng: ").append(String.format("%,.0fđ", total));
            }
        }

        if (request.getDeliveryAddress() != null) {
            sb.append("\n📍 Giao tới: ").append(request.getDeliveryAddress());
        }

        sb.append("\n\nBạn chuẩn bị nhận hàng nhé! 🏃");
        return sb.toString();
    }
}
