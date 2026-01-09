package com.example.telegram_bot.bot;

import com.example.telegram_bot.handler.UpdateHandler;
import com.example.telegram_bot.model.UserSession;
import com.example.telegram_bot.service.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramBot implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {

    private final TelegramClient telegramClient;
    private final SessionService sessionService;
    private final UpdateHandler updateHandler;

    @Value("${bot.token}")
    private String botToken;

    @Override
    public String getBotToken() { return botToken; }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() { return this; }

    @Override
    public void consume(Update update) {
        try {
            long chatId = extractChatId(update);
            if (chatId == 0) return;
            UserSession session = sessionService.getSession(chatId);

            SendMessage response = updateHandler.handleUpdate(update, session);

            if (response != null) {
                telegramClient.execute(response);
            }
        } catch (Exception e) {
            log.error("Критическая ошибка бота: {}", e.getMessage());
        }
    }

    private long extractChatId(Update update) {
        if (update.hasMessage()) return update.getMessage().getChatId();
        if (update.hasCallbackQuery()) return update.getCallbackQuery().getMessage().getChatId();
        return 0;
    }
}