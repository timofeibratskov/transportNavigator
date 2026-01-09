package com.example.telegram_bot.util;

import com.example.telegram_bot.model.Stop;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.util.ArrayList;
import java.util.List;

@Component
public class KeyboardFactory {
    public InlineKeyboardMarkup buildStopButtons(List<Stop> stops, String type) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        for (int i = 0; i < stops.size(); i++) {
            Stop stop = stops.get(i);
            rows.add(new InlineKeyboardRow(
                    InlineKeyboardButton.builder()
                            .text("Выбрать №" + (i + 1))
                            .callbackData(type + ":" + stop.id())
                            .build()
            ));
        }
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }
}
