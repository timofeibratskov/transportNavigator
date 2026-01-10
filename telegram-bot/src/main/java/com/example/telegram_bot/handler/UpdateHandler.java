package com.example.telegram_bot.handler;

import com.example.telegram_bot.client.RaspisanieClient;
import com.example.telegram_bot.model.enums.BotState;
import com.example.telegram_bot.model.Stop;
import com.example.telegram_bot.model.UserSession;
import com.example.telegram_bot.service.RoutingService;
import com.example.telegram_bot.util.KeyboardFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UpdateHandler {

    private final RaspisanieClient client;
    private final RoutingService routingService;
    private final KeyboardFactory keyboardFactory;

    public SendMessage handleUpdate(Update update, UserSession session) {
        long chatId = extractChatId(update);

        if (update.hasCallbackQuery()) {
            return handleCallback(update, session, chatId);
        }

        String text = update.getMessage().getText();

        if (text.startsWith("/")) {
            return handleCommand(text, session, chatId);
        }

        return handleStateInput(text, session, chatId);
    }

    private SendMessage handleCommand(String text, UserSession session, long chatId) {
        switch (text) {
            case "/start" -> {
                session.reset();
                return createMessage(chatId, """
                        👋 <b>Добро пожаловать!</b>
                                        
                        Я помогу вам быстро найти нужный маршрут и расписание транспорта.
                                        
                        🚀 Чтобы начать, просто введите команду /route
                        📖 Если нужна помощь, загляните в /info
                        """);
            }
            case "/info" -> {
                session.reset();
                return createMessage(chatId, """
                        ℹ️ <b>Как пользоваться ботом:</b>
                                        
                        1️⃣ Нажмите /route
                        2️⃣ Введите название <b>начальной</b> остановки.
                        3️⃣ Выберите подходящий вариант из предложенного списка.
                        4️⃣ Повторите то же самое для <b>конечной</b> остановки.                """);
            }
            case "/route" -> {
                session.setState(BotState.WAITING_ORIGIN);
                return createMessage(chatId, """
                        🗺️ <b>Построение маршрута</b>
                                        
                        <b>Шаг 1:</b> Введите название <u>начальной</u> остановки (откуда едем):
                        """);
            }
            default -> {
                return createMessage(chatId, "⚠️ <b>Неизвестная команда.</b>\nПопробуйте использовать /route для поиска.");
            }
        }
    }

    private SendMessage handleStateInput(String text, UserSession session, long chatId) {
        if (session.getState() == BotState.WAITING_ORIGIN || session.getState() == BotState.WAITING_DEST) {
            List<Stop> stops = client.searchStops(text);
            if (stops.isEmpty()) return createMessage(chatId, "❌ Остановки не найдены. Попробуйте другое название:");

            String type = session.getState() == BotState.WAITING_ORIGIN ? "origin" : "dest";
            if ("origin".equals(type)) session.setFoundOriginStops(stops);
            else session.setFoundDestStops(stops);

            StringBuilder messageText = new StringBuilder("🔍 <b>Найденные остановки:</b>\n\n");
            for (int i = 0; i < stops.size(); i++) {
                Stop stop = stops.get(i);
                messageText.append(i + 1).append(". <b>").append(stop.name()).append("</b>\n")
                        .append("└-> <i>").append(formatDescription(stop.description())).append("</i>\n\n");
            }
            messageText.append("Нажмите на кнопку с соответствующим номером:");

            return SendMessage.builder()
                    .chatId(chatId)
                    .text(messageText.toString())
                    .parseMode("HTML")
                    .replyMarkup(keyboardFactory.buildStopButtons(stops, type))
                    .build();
        }
        return createMessage(chatId, "Используйте /route для начала поиска.");
    }

    private SendMessage handleCallback(Update update, UserSession session, long chatId) {
        String data = update.getCallbackQuery().getData();
        String[] parts = data.split(":");
        String type = parts[0];
        UUID stopId = UUID.fromString(parts[1]);

        if ("origin".equals(type)) {
            Stop selected = session.getFoundOriginStops().stream().filter(s -> s.id().equals(stopId)).findFirst().orElse(null);
            if (selected != null) {
                session.setOriginId(selected.id());
                session.setOriginName(selected.name());
                session.setState(BotState.WAITING_DEST);
                return createMessage(chatId, "✅ Начало: " + selected.name() + "\n\nШаг 2: Введите конечную остановку:");
            }
        } else if ("dest".equals(type)) {
            Stop selected = session.getFoundDestStops().stream().filter(s -> s.id().equals(stopId)).findFirst().orElse(null);
            if (selected != null) {
                session.setDestId(selected.id());
                session.setDestName(selected.name());
                String routeResult = routingService.buildAndFormatRoute(session);
                session.reset();
                return createMessage(chatId, routeResult);
            }
        }
        return createMessage(chatId, "Ошибка выбора. Попробуйте снова /route");
    }

    private long extractChatId(Update update) {
        return update.hasCallbackQuery() ?
                update.getCallbackQuery().getMessage().getChatId() :
                update.getMessage().getChatId();
    }

    private SendMessage createMessage(long chatId, String text) {
        return SendMessage.builder().chatId(chatId).text(text).parseMode("HTML").build();
    }

    private String formatDescription(String description) {
        if (description == null) return "Нет описания";
        if (description.contains("_")) {
            return description.split(" _")[1];
        }
        return description;
    }
}