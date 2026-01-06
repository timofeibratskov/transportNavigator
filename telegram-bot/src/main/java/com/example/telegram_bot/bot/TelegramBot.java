package com.example.telegram_bot.bot;

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

    @Value("${bot.token}")
    private String botToken;

    @Value("${bot.name}")
    private String botName;

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return this;
    }

    @Override
    public void consume(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String userMessage = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            // Используем switch - так удобнее расширять список команд
            switch (userMessage) {
                case "/start" -> handleStartCommand(chatId);
                case "/info" -> handleInfoCommand(chatId);
                default -> sendText(chatId, "Вы написали: " + userMessage + "\nИспользуйте /info для справки.");
            }
        }
    }

    private void handleStartCommand(long chatId) {
        String welcomeText = "👋 Привет! Я бот " + botName + ".\n" +
                "Я помогу тебе узнать расписание транспорта.";
        sendText(chatId, welcomeText);
    }

    private void handleInfoCommand(long chatId) {
        String infoText = """
                ℹ️ <b>Информация о проекте</b>
                Этот бот может не только выводить расписание по остановке.
                он может построить маршрут""";

        SendMessage sm = SendMessage.builder()
                .chatId(chatId)
                .parseMode("HTML")
                .text(infoText)
                .build();
        executeMessage(sm);
    }

    private void sendText(long chatId, String text) {
        SendMessage sm = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .parseMode("HTML")
                .build();
        executeMessage(sm);
    }

    private void executeMessage(SendMessage sm) {
        try {
            telegramClient.execute(sm);
        } catch (Exception e) {
            log.error("Ошибка при отправке сообщения в Telegram: {}", e.getMessage());
        }
    }
}