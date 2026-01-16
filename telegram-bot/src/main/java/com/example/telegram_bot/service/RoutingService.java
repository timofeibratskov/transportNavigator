package com.example.telegram_bot.service;

import com.example.telegram_bot.client.RaspisanieClient;
import com.example.telegram_bot.dto.RoutingRequestDto;
import com.example.telegram_bot.dto.RoutingResponseDto;
import com.example.telegram_bot.dto.SegmentDto;
import com.example.telegram_bot.model.UserSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoutingService {

    private final RaspisanieClient client;
    private final DayService dayService;

    public String buildAndFormatRoute(UserSession session) {
        try {
            RoutingRequestDto request = new RoutingRequestDto(
                    session.getOriginId(),
                    session.getDestId(),
                    LocalTime.now(),
                    dayService.getDayType()
            );

            List<RoutingResponseDto> responses = client.getAllPlans(request);

            if (responses.isEmpty()) {
                return "⚠️ Маршруты не найдены. Попробуйте другие остановки.";
            }

            return formatAllRoutes(responses, session.getOriginName(), session.getDestName());

        } catch (Exception e) {
            log.error("Ошибка при построении маршрута: {}", e.getMessage());
            return "⚠️ Не удалось построить маршрут. Попробуйте позже.";
        }
    }

    private String formatAllRoutes(List<RoutingResponseDto> responses, String originName, String destName) {
        if (responses.size() == 1) {
            return formatSingleRoute(responses.getFirst(), originName, destName);
        } else {
            return formatMultipleRoutes(responses);
        }
    }

    private String formatSingleRoute(RoutingResponseDto route, String originName, String destName) {

        return "🗺️ <b>Ваш маршрут</b>\n" +
                "━━━━━━━━━━━━━━━\n\n" +
                "📍 <b>Откуда:</b> " + originName + "\n" +
                "📍 <b>Куда:</b> " + destName + "\n\n" +
                "⏱ <b>В пути:</b> " + route.routeTime() + "\n" +
                "🚏 <b>Остановок:</b> " + route.totalStops() + "\n" +
                "🔄 <b>Пересадок:</b> " + route.transfers() + "\n\n" +
                formatSegments(route.segments()) +
                "\n✅ <i>Приятной поездки!</i>";
    }

    private String formatMultipleRoutes(List<RoutingResponseDto> responses) {
        StringBuilder sb = new StringBuilder();

        sb.append("🗺️ <b>Найдено ").append(responses.size()).append(" вариант-а(ов) маршрута</b>\n");
        sb.append("━━━━━━━━━━━━━━━\n\n");

        for (int i = 0; i < responses.size(); i++) {
            RoutingResponseDto route = responses.get(i);

            sb.append("━━━━━━━━━━━━━━━\n");
            sb.append("<b>Вариант ").append(i + 1).append("</b> ");
            sb.append("⏱ Время: ").append(route.routeTime());
            sb.append(" | 🚏 Остановок: ").append(route.totalStops());
            sb.append(" | 🔄 Пересадок: ").append(route.transfers()).append("\n\n");

            sb.append(formatSegments(route.segments()));
            sb.append("\n");
        }

        sb.append("✅ <i>Выберите подходящий вариант!</i>");

        return sb.toString();
    }

    private String formatSegments(List<SegmentDto> segments) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < segments.size(); i++) {
            SegmentDto segment = segments.get(i);

            String emoji = segment.transport().name().equals("BUS") ? "🚌" : "🚎";

            sb.append(emoji).append(" <b>№").append(segment.routeNumber()).append("</b>");
            sb.append(" <i>(").append(segment.direction()).append(")</i>\n");

            sb.append("   ├ <b>Сесть:</b> ").append(segment.boardingStop().name())
                    .append(" <code>").append(segment.boardingTime()).append("</code>\n");

            if (segment.stopsCount() > 2) {
                sb.append("   │ <i>Проехать ").append(segment.stopsCount() - 2).append(" ост.</i>\n");
            }

            sb.append("   └ <b>Выйти:</b> ").append(segment.exitStop().name())
                    .append(" <code>").append(segment.exitTime()).append("</code>\n");

            if (i < segments.size() - 1) {
                sb.append("\n   🔄 <i>Пересадка</i>\n\n");
            }
        }

        return sb.toString();
    }
}