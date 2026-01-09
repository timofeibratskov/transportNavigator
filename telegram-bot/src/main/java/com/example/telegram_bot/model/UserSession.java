package com.example.telegram_bot.model;

import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Builder
@AllArgsConstructor
@Getter

@ToString
@NoArgsConstructor
public class UserSession {
    @Setter
    private BotState state = BotState.IDLE;
    @Setter
    private List<Stop> foundOriginStops;
    @Setter
    private List<Stop> foundDestStops;
    @Setter
    private UUID originId;
    @Setter
    private String originName;
    @Setter
    private UUID destId;
    @Setter
    private String destName;

    private LocalDateTime lastActivity = LocalDateTime.now();

    public void reset() {
        this.state = BotState.IDLE;
        this.foundOriginStops = null;
        this.foundDestStops = null;
        this.originId = null;
        this.originName = null;
        this.destId = null;
        this.destName = null;
        this.lastActivity = LocalDateTime.now();
    }

    public void updateActivity() {
        this.lastActivity = LocalDateTime.now();
    }
}