package com.example.telegram_bot.service;

import com.example.telegram_bot.model.UserSession;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SessionService {
    private final Map<Long, UserSession> sessions = new ConcurrentHashMap<>();

    public UserSession getSession(long chatId) {
        return sessions.computeIfAbsent(chatId, k -> new UserSession());
    }

    public void deleteSession(long chatId) {
        sessions.remove(chatId);
    }
}