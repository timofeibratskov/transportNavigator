package com.example.telegram_bot.bot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Component
public class TelegramBot implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {

    private final String botToken;
    private final String botName;
    private final TelegramClient telegramClient;
    private final RestTemplate restTemplate;

    @Value("${api.url}")
    private String externalApiUrl;

    public TelegramBot(@Value("${bot.token}") String botToken,
                       @Value("${bot.name}") String botName) {
        this.botToken = botToken;
        this.botName = botName;
        this.telegramClient = new OkHttpTelegramClient(botToken);
        this.restTemplate = new RestTemplate();
    }

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

            if (userMessage.equals("/start")) {
                sendText(chatId, "Привет! Я бот " + botName + ". Отправь мне что-нибудь, и я спрошу у сервера.");
                return;
            }

            try {
                String responseFromApi = restTemplate.getForObject(
                        externalApiUrl + "?query=" + userMessage,
                        String.class
                );

                sendText(chatId, "Мой API ответил: " + responseFromApi);
            } catch (Exception e) {
                sendText(chatId, "Ошибка связи с сервером: " + e.getMessage());
            }
        }
    }

    private void sendText(long chatId, String text) {
        SendMessage sm = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .build();
        try {
            telegramClient.execute(sm);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}