package com.gomarket.service;

import com.gomarket.dto.RecipeResponse.WeatherInfo;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class WeatherService {

    private static final Logger log = LoggerFactory.getLogger(WeatherService.class);

    @Value("${api.openweather.key}")
    private String apiKey;

    @Value("${api.openweather.url}")
    private String apiUrl;

    private final OkHttpClient client = new OkHttpClient();

    public WeatherInfo getWeather(double lat, double lng) {
        try {
            String url = String.format(
                    "%s?lat=%f&lon=%f&appid=%s&units=metric&lang=vi",
                    apiUrl, lat, lng, apiKey
            );

            Request request = new Request.Builder().url(url).build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String json = response.body().string();
                    return parseWeatherResponse(json);
                }
            }
        } catch (Exception e) {
            log.error("Failed to get weather: {}", e.getMessage());
        }

        // Fallback data
        return getDefaultWeather();
    }

    private WeatherInfo parseWeatherResponse(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();

        WeatherInfo weather = new WeatherInfo();

        // Main
        JsonObject main = root.getAsJsonObject("main");
        weather.setTemp(main.get("temp").getAsDouble());
        weather.setHumidity(main.get("humidity").getAsInt());

        // Weather description
        JsonObject weatherObj = root.getAsJsonArray("weather").get(0).getAsJsonObject();
        weather.setDescription(weatherObj.get("description").getAsString());
        weather.setIcon(weatherObj.get("icon").getAsString());

        // City
        weather.setCity(root.get("name").getAsString());

        return weather;
    }

    private WeatherInfo getDefaultWeather() {
        WeatherInfo weather = new WeatherInfo();
        weather.setTemp(28);
        weather.setDescription("trời nắng");
        weather.setHumidity(65);
        weather.setIcon("01d");
        weather.setCity("TP. Hồ Chí Minh");
        return weather;
    }

    public String getWeatherSummary(WeatherInfo weather) {
        String tempDesc;
        if (weather.getTemp() < 20) tempDesc = "lạnh";
        else if (weather.getTemp() < 25) tempDesc = "mát mẻ";
        else if (weather.getTemp() < 32) tempDesc = "ấm áp";
        else tempDesc = "nóng";

        return String.format("%s, %s, %.0f°C", weather.getDescription(), tempDesc, weather.getTemp());
    }
}
