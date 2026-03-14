package com.gomarket.service;

import com.gomarket.dto.OrderRequest;
import com.gomarket.model.Order;
import com.gomarket.model.OrderItem;
import com.gomarket.model.Product;
import com.gomarket.repository.OrderRepository;
import com.gomarket.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public OrderService(OrderRepository orderRepository, ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public Order createOrder(OrderRequest request) {
        Order order = new Order();
        order.setUserId(request.getUser_id());
        order.setDeliveryAddress(request.getDelivery_address());
        order.setLatitude(request.getLatitude());
        order.setLongitude(request.getLongitude());
        order.setStatus("PENDING");
        order.setShopperName("Nguyễn Văn A"); // Demo
        order.setPaymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod() : "COD");
        order.setNotes(request.getNotes());

        List<OrderItem> items = new ArrayList<>();
        double total = 0;

        for (OrderRequest.OrderItemRequest itemReq : request.getItems()) {
            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProductId(itemReq.getProduct_id());
            item.setQuantity(itemReq.getQuantity());

            Product product = productRepository.findById(itemReq.getProduct_id()).orElse(null);
            if (product != null) {
                item.setProductName(product.getName());
                item.setPrice(product.getPrice());
                item.setImageUrl(product.getImageUrl());
                total += product.getPrice() * itemReq.getQuantity();
            } else {
                item.setProductName("Sản phẩm #" + itemReq.getProduct_id());
                item.setPrice(0.0);
            }

            items.add(item);
        }

        order.setItems(items);
        order.setTotalPrice(total);

        return orderRepository.save(order);
    }

    public Order getOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng #" + id));
    }

    public List<Order> getUserOrders(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public Order updateStatus(Long id, String status) {
        Order order = getOrder(id);
        order.setStatus(status);
        return orderRepository.save(order);
    }

    /** Plan 2: Shipper cập nhật tọa độ GPS */
    @Transactional
    public void updateLocation(Long orderId, Double lat, Double lng) {
        Order order = getOrder(orderId);
        order.setShopperLat(lat);
        order.setShopperLng(lng);
        order.setLocationUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
    }
}
