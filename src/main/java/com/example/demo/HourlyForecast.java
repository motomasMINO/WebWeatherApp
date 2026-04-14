package com.example.demo;

import java.time.LocalDateTime;

// 3時間ごとの天気予報を表すレコードクラス
public record HourlyForecast(
    LocalDateTime dateTime,
    String description,
    String icon,
    double temp
) {}
