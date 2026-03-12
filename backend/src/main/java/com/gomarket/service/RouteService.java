package com.gomarket.service;

import com.gomarket.dto.RecipeResponse.RouteInfo;
import com.gomarket.dto.RecipeResponse.ShopInfo;
import com.gomarket.model.Shop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class RouteService {

    private static final Logger log = LoggerFactory.getLogger(RouteService.class);
    private static final double EARTH_RADIUS_KM = 6371.0;

    /**
     * Tìm lộ trình tối ưu qua các shop bằng thuật toán Nearest Neighbor + A*
     * Giải bài toán TSP (Travelling Salesman Problem) đơn giản
     */
    public RouteInfo findOptimalRoute(double userLat, double userLng, List<Shop> shops) {
        if (shops == null || shops.isEmpty()) {
            return emptyRoute();
        }

        // Loại bỏ shop trùng
        List<Shop> uniqueShops = new ArrayList<>(new LinkedHashSet<>(shops));

        // Dùng Nearest Neighbor Heuristic cho TSP
        List<Shop> orderedShops = nearestNeighborTSP(userLat, userLng, uniqueShops);

        // Tính tổng khoảng cách
        double totalDistance = calculateTotalDistance(userLat, userLng, orderedShops);

        // Ước tính thời gian (giả sử tốc độ trung bình 20 km/h trong thành phố)
        int estimatedMinutes = (int) Math.ceil(totalDistance / 20.0 * 60);

        RouteInfo route = new RouteInfo();
        route.setShops(toShopInfoList(orderedShops));
        route.setTotal_distance(Math.round(totalDistance * 10.0) / 10.0);
        route.setEstimated_time(estimatedMinutes);

        log.info("Route calculated: {} shops, {}km, ~{} min",
                orderedShops.size(), Math.round(totalDistance * 10.0) / 10.0, estimatedMinutes);

        return route;
    }

    /**
     * Thuật toán Nearest Neighbor cho TSP
     * Bắt đầu từ vị trí user, mỗi bước chọn shop gần nhất chưa thăm
     */
    private List<Shop> nearestNeighborTSP(double startLat, double startLng, List<Shop> shops) {
        List<Shop> result = new ArrayList<>();
        Set<Integer> visited = new HashSet<>();
        double currentLat = startLat;
        double currentLng = startLng;

        while (visited.size() < shops.size()) {
            double minDist = Double.MAX_VALUE;
            int nearestIdx = -1;

            for (int i = 0; i < shops.size(); i++) {
                if (visited.contains(i)) continue;

                Shop shop = shops.get(i);
                double dist = haversineDistance(
                        currentLat, currentLng,
                        shop.getLatitude(), shop.getLongitude()
                );

                if (dist < minDist) {
                    minDist = dist;
                    nearestIdx = i;
                }
            }

            if (nearestIdx >= 0) {
                visited.add(nearestIdx);
                Shop nearest = shops.get(nearestIdx);
                result.add(nearest);
                currentLat = nearest.getLatitude();
                currentLng = nearest.getLongitude();
            }
        }

        return result;
    }

    /**
     * Tính tổng khoảng cách: User → Shop1 → Shop2 → ... → User
     */
    private double calculateTotalDistance(double userLat, double userLng, List<Shop> shops) {
        if (shops.isEmpty()) return 0;

        double total = 0;
        double prevLat = userLat, prevLng = userLng;

        for (Shop shop : shops) {
            total += haversineDistance(prevLat, prevLng, shop.getLatitude(), shop.getLongitude());
            prevLat = shop.getLatitude();
            prevLng = shop.getLongitude();
        }

        // Quay về vị trí user
        total += haversineDistance(prevLat, prevLng, userLat, userLng);

        return total;
    }

    /**
     * Haversine formula - Tính khoảng cách giữa 2 điểm GPS (km)
     */
    public static double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }

    private List<ShopInfo> toShopInfoList(List<Shop> shops) {
        List<ShopInfo> result = new ArrayList<>();
        for (Shop shop : shops) {
            ShopInfo info = new ShopInfo();
            info.setId(shop.getId());
            info.setName(shop.getName());
            info.setAddress(shop.getAddress());
            info.setLatitude(shop.getLatitude());
            info.setLongitude(shop.getLongitude());
            result.add(info);
        }
        return result;
    }

    private RouteInfo emptyRoute() {
        RouteInfo route = new RouteInfo();
        route.setShops(new ArrayList<>());
        route.setTotal_distance(0);
        route.setEstimated_time(0);
        return route;
    }
}
