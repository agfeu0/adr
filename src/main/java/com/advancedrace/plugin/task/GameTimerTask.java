package com.advancedrace.plugin.task;

import com.advancedrace.plugin.manager.GameStateManager;
import com.advancedrace.plugin.util.ScoreboardManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class GameTimerTask {

    private long gameStartTime;
    private long gameDuration; // 밀리초 단위
    private GameStateManager gameStateManager;
    private Plugin plugin;
    private int taskId = -1;

    public GameTimerTask(GameStateManager gameStateManager, Plugin plugin, long durationSeconds) {
        this.gameStateManager = gameStateManager;
        this.plugin = plugin;
        this.gameDuration = durationSeconds * 1000; // 초를 밀리초로 변환
        this.gameStartTime = System.currentTimeMillis();
    }

    /**
     * 게임 타이머 시작
     */
    public void start() {
        // 매 틱마다 실행 (1 틱 = 50ms)
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::updateTimer, 0, 1);
    }

    /**
     * 타이머 업데이트
     */
    private void updateTimer() {
        // 게임이 실행 중이 아니면 정지
        if (!gameStateManager.isRunning()) {
            stop();
            return;
        }

        long elapsedTime = System.currentTimeMillis() - gameStartTime;
        long remainingTime = gameDuration - elapsedTime;

        // 게임 시간이 끝났으면
        if (remainingTime <= 0) {
            endGame();
            return;
        }

        // 남은 시간을 HH:MM:SS 형식으로 변환
        long seconds = remainingTime / 1000;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secondsRemainder = seconds % 60;
        String timeString = String.format("%02d:%02d:%02d", hours, minutes, secondsRemainder);

        // 모든 온라인 플레이어의 스코어보드 업데이트
        for (Player player : Bukkit.getOnlinePlayers()) {
            ScoreboardManager.updateTime(player, timeString);
        }
    }

    /**
     * 게임 종료 처리
     */
    private void endGame() {
        stop();
        gameStateManager.endGame();

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage("§c게임 시간이 끝났습니다!");
        }
    }

    /**
     * 타이머 정지
     */
    public void stop() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }

    /**
     * 현재 남은 시간 반환 (초 단위)
     */
    public long getRemainingSeconds() {
        long elapsedTime = System.currentTimeMillis() - gameStartTime;
        long remainingTime = gameDuration - elapsedTime;
        return Math.max(0, remainingTime / 1000);
    }

    /**
     * 게임이 활성화되어 있는지 확인
     */
    public boolean isActive() {
        return taskId != -1;
    }
}
