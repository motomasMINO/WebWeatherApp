package com.example.demo;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.fasterxml.jackson.databind.JsonNode;

// 天気情報を処理するコントローラークラス
@Controller
public class WeatherController {

    @Autowired
    private WeatherService weatherService;

    @GetMapping("/")
    public String index() {
        return "index";
    }

    // 位置情報を取得し、現在の天気を取得して表示するためのエンドポイント
    @PostMapping("/weather")
    public String getWeather(@RequestParam String location, Model model) {
        JsonNode data = weatherService.getWeatherData(location); // 位置情報を取得し、現在の天気を取得

        if (data != null) {
            model.addAttribute("icon", data.path("weather").get(0).path("icon").asText()); // アイコン
            model.addAttribute("location", location); // 位置情報
            model.addAttribute("temperature", data.path("main").path("temp").asDouble()); // 温度
            model.addAttribute("humidity", data.path("main").path("humidity").asInt()); // 湿度
            model.addAttribute("weather_condition", data.path("weather").get(0).path("description").asText()); // 天気の説明
            model.addAttribute("windspeed", data.path("wind").path("speed").asDouble()); // 風速
        } else {
            model.addAttribute("error", "天気情報を取得できませんでした。"); // 取得できなかったとき
        }

        return "result";
    }

    // 5日間の3時間ごとの天気予報を表示するためのエンドポイント
    @GetMapping("/hourly")
    public String showWeeklyForm() {
        return "hourly";
    }

    // 位置情報を取得し、5日間の3時間ごとの天気予報を取得して表示するためのエンドポイント
    @PostMapping("/hourly")
    public String getHourlyForecast(@RequestParam String location, Model model) {
        List<HourlyForecast> forecasts = weatherService.getFiveDayForecast(location); // 位置情報を取得し、5日間の3時間ごとの天気を取得
        model.addAttribute("location", location); // 位置情報
        model.addAttribute("forecasts", forecasts); // 5日間の天気予報
        return "HourlyResult";
    }
}
