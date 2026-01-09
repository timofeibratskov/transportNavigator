package com.example.telegram_bot.service;

import com.example.telegram_bot.client.RaspisanieClient;
import com.example.telegram_bot.dto.RoutingRequestDto;
import com.example.telegram_bot.dto.RoutingResponseDto;
import com.example.telegram_bot.model.Day;
import com.example.telegram_bot.dto.PathDto;
import com.example.telegram_bot.model.UserSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoutingService {

    private final RaspisanieClient client;
//todo    private final CalendarService calendarService;

    public String buildAndFormatRoute(UserSession session) {
        try {

            RoutingRequestDto request = new RoutingRequestDto(
                    session.getOriginId(),
                    session.getDestId(),
                    LocalTime.now(),
                    getDayType()
            );

            RoutingResponseDto responseDto = client.planRoute(request);
            return formatRoute(responseDto, session.getOriginName(), session.getDestName());
        } catch (Exception e) {
            log.error("Ошибка при построении маршрута: {}", e.getMessage());
            return "⚠️ Не удалось построить маршрут. Попробуйте позже.";
        }
    }

    private String formatRoute(RoutingResponseDto routingResponseDto, String originName, String destName) {
        StringBuilder sb = new StringBuilder();
        sb.append("🗺️ <b>Ваш маршрут</b>\n━━━━━━━━━━━━━━━\n\n");
        sb.append("📍 <b>Откуда:</b> ").append(originName).append("\n");
        sb.append("📍 <b>Куда:</b> ").append(destName).append("\n\n");
        sb.append("⏱ <b>В пути:</b> ").append(routingResponseDto.routeTime()).append("\n");
        sb.append("📍 <b>Кол-во остановок:</b> ").append(routingResponseDto.stopsAmount()).append("\n");
        sb.append("🔄 <b>Пересадок:</b> ").append(routingResponseDto.transfers()).append("\n\n");
        sb.append("<b>Детали:</b>\n");

        int step = 1;
        String currentRoute = null;

        for (PathDto path : routingResponseDto.pathDtoList()) {
            String routeKey = path.transport() + "_" + path.number();
            if (!routeKey.equals(currentRoute)) {
                if (currentRoute != null) sb.append("\n🔄 <i>Пересадка</i>\n");
                String emoji = path.transport().name().equals("BUS") ? "🚌" : "🚎";
                sb.append("<b>").append(step++).append(". ").append(emoji)
                        .append(" №").append(path.number()).append("</b>\n");
                currentRoute = routeKey;
            }
            sb.append("   • ").append(path.stop().name()).append(" <code>").append(path.time()).append("</code>\n");
        }
        return sb.append("\n✅ Приятной поездки!").toString();
    }

    private Day getDayType() {
        DayOfWeek dayOfWeek = LocalDateTime.now().getDayOfWeek();
        if (dayOfWeek.equals(DayOfWeek.SUNDAY) || dayOfWeek.equals(DayOfWeek.SATURDAY)) {
            return Day.WEEKEND;
        } else return Day.WEEKDAY;
    }
}