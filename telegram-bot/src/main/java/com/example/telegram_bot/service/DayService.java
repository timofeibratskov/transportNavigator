package com.example.telegram_bot.service;

import com.example.telegram_bot.model.enums.Day;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class DayService {

    private static final String API_URL = "https://isdayoff.ru/today?cc=by";
    private LocalDate currentDate;
    private Day currentDayType;

    private final RestTemplate restTemplate;

    public Day getDayType() {
        LocalDate today = LocalDate.now();

        if (this.currentDate != null && this.currentDate.equals(today)) {
            return this.currentDayType;
        }

        try {
            String response = restTemplate.getForObject(API_URL, String.class);
            Day dayType = parseResponse(response);

            this.currentDate = today;
            this.currentDayType = dayType;

            log.info("Получено из API: {}, день: {}", response, dayType);
            return dayType;

        } catch (Exception e) {
            log.error("Ошибка при обращении к API: {}", e.getMessage());
            return getLocalDayType();
        }
    }


    private Day parseResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            return getLocalDayType();
        }

        String code = response.trim();

        if ("0".equals(code) || "2".equals(code) || "4".equals(code)) {
            return Day.WEEKDAY;
        }

        if ("1".equals(code) || "8".equals(code)) {
            return Day.WEEKEND;
        }

        log.warn("Неожиданный код ответа: {}", code);
        return getLocalDayType();
    }

    private Day getLocalDayType() {
        LocalDate today = LocalDate.now();
        int dayOfWeek = today.getDayOfWeek().getValue();
        return (dayOfWeek == 6 || dayOfWeek == 7) ? Day.WEEKEND : Day.WEEKDAY;
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void clearCurrentDayType() {
        log.info("Сброс кеша в полночь");
        currentDate = null;
        currentDayType = null;
    }

}
