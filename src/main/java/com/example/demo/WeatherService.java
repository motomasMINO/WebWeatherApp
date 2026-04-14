package com.example.demo;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

// 天気情報を処理するサービスクラス
@Service
public class WeatherService {
    private static final String API_KEY = "7e30e1e9d070182750eb5bcca3fccfde"; // OpenWeatherMapのAPIキー
    private final ObjectMapper objectMapper = new ObjectMapper(); // JSON処理用

    public JsonNode getWeatherData(String location) {
        // 位置情報を取得し、現在の天気を取得
        try {
            String url = "https://api.openweathermap.org/data/2.5/weather?q=" + location +
                    "&units=metric&lang=ja&appid=" + API_KEY; // 現在の天気を取得するAPIエンドポイント

            HttpClient client = HttpClient.newHttpClient(); // HTTPクライアントを作成
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build(); // HTTPリクエストを作成
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString()); // HTTPリクエストを送信し、レスポンスを受け取る

            // レスポンスのステータスコードをチェックし、成功した場合はJSONを解析して返す
            if (response.statusCode() == 200) {
                return objectMapper.readTree(response.body());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<HourlyForecast> getFiveDayForecast(String location) {
        // 位置情報を取得し、5日分の3時間ごとの天気を取得
        Coordinates coords = fetchCoordinates(location); // 位置情報から座標を取得
        if (coords == null) return List.of(); // 座標が取得できない場合は空のリストを返す

        String url = String.format(
            "https://api.openweathermap.org/data/2.5/forecast?lat=%f&lon=%f&units=metric&lang=ja&appid=%s",
            coords.lat(), coords.lon(), API_KEY // URLを座標とAPIキーを使って構築
        );

        // HTTPリクエストを送信
        try {
             HttpClient client = HttpClient.newHttpClient();
             HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
             HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                // エラーが起きた場合は詳細を表示
                System.err.println("Failed to get 5-day forecast. Status code: " + response.statusCode());
                return List.of(); // エラーが起きた場合は空のリストを返す
            }

            JsonNode root = objectMapper.readTree(response.body()); // レスポンスのJSONを解析
            JsonNode list = root.get("list"); // 3時間ごとの天気予報のリストを取得
            if (list == null || !list.isArray()) return List.of(); // 予報のリストが存在しない場合は空のリストを返す

            // 予報のリストをHourlyForecastオブジェクトのリストに変換
            List<HourlyForecast> result = new ArrayList<>();
            for (JsonNode item : list) {
                HourlyForecast forecast = new HourlyForecast(
                    Instant.ofEpochSecond(item.get("dt").asLong()).atZone(ZoneId.systemDefault()).toLocalDateTime(), // 予報の日時
                    item.get("weather").get(0).get("description").asText(), // 天気の説明
                    item.get("weather").get(0).get("icon").asText(), // アイコン
                    item.get("main").get("temp").asDouble() // 温度
                );
                result.add(forecast); // 予報をリストに追加
            }

            return result;

        } catch (Exception e) {
            e.printStackTrace();
            return List.of(); // エラーが起きた場合は空のリストを返す
        }
    }

    private Coordinates fetchCoordinates(String location) {
        // 位置情報を取得するためのジオコーディングAPIを呼び出す
        try {
            String url = "http://api.openweathermap.org/geo/1.0/direct?q=" + location +
                    "&limit=1&appid=" + API_KEY; // ジオコーディングAPIのURLを構築

            HttpClient client = HttpClient.newHttpClient(); // HTTPクライアントを作成
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build(); // HTTPリクエストを作成
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString()); // HTTPリクエストを送信し、レスポンスを受け取る

            //System.out.println("Geocoding response: " + response.body());

            // レスポンスのステータスコードをチェックし、成功した場合は座標を解析して返す
            if (response.statusCode() == 200) {
                JsonNode jsonNode = objectMapper.readTree(response.body()); // レスポンスのJSONを解析
                if (jsonNode.isArray() && jsonNode.size() > 0) { // 結果が配列で、少なくとも1件の結果がある場合
                    JsonNode loc = jsonNode.get(0); // 最初の結果を使用
                    double lat = loc.get("lat").asDouble(); // 緯度
                    double lon = loc.get("lon").asDouble(); // 経度
                    return new Coordinates(lat, lon); // 座標を返す
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
