package com.advancedrace.plugin.task;

import com.advancedrace.plugin.AdvancedRace;
import com.advancedrace.plugin.manager.GameStateManager;
import com.advancedrace.plugin.manager.TeamManager;
import com.advancedrace.plugin.util.ScoreboardManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        AdvancedRace advancedRace = AdvancedRace.getInstance();
        if (advancedRace == null) {
            return;
        }

        TeamManager teamManager = advancedRace.getTeamManager();
        Map<String, Integer> teamScores = advancedRace.getAdvancementListener().getTeamScores();

        // 스트리머 정보와 점수를 저장할 리스트 생성
        List<Map.Entry<String, Integer>> scoredStreamers = new java.util.ArrayList<>(teamScores.entrySet());

        // 점수순으로 정렬하고, 동점일 경우 시청자 수로 정렬
        scoredStreamers.sort((a, b) -> {
            int scoreCompare = Integer.compare(b.getValue(), a.getValue()); // 내림차순
            if (scoreCompare != 0) {
                return scoreCompare;
            }
            // 동점인 경우 시청자 수로 비교
            TeamManager.Team teamA = teamManager.getTeamByStreamer(a.getKey());
            TeamManager.Team teamB = teamManager.getTeamByStreamer(b.getKey());
            int viewersA = teamA != null ? teamA.getPlayerCount() : 0;
            int viewersB = teamB != null ? teamB.getPlayerCount() : 0;
            return Integer.compare(viewersB, viewersA); // 내림차순
        });

        // 우승팀 결정 (첫 번째가 우승팀)
        String winnerStreamer = scoredStreamers.isEmpty() ? null : scoredStreamers.get(0).getKey();

        // 스코어보드 로그 출력
        StringBuilder scoreLog = new StringBuilder("\n[AdvancedRace] 게임 종료 - 최종 순위:\n");
        for (int i = 0; i < scoredStreamers.size(); i++) {
            String streamerName = scoredStreamers.get(i).getKey();
            int score = scoredStreamers.get(i).getValue();
            TeamManager.Team team = teamManager.getTeamByStreamer(streamerName);
            int viewers = team != null ? team.getPlayerCount() : 0;
            scoreLog.append((i + 1)).append("위: ").append(streamerName).append(" (점수: ").append(score)
                    .append(", 시청자: ").append(viewers).append(")\n");
        }
        Bukkit.getLogger().info(scoreLog.toString());

        // 각 스트리머에게 타이틀 표시 (승리/패배)
        for (String streamerName : teamManager.getStreamerNames()) {
            Player streamer = Bukkit.getPlayer(streamerName);
            if (streamer != null && streamer.isOnline()) {
                Component title;
                Component subtitle;

                if (streamerName.equals(winnerStreamer)) {
                    // 우승팀
                    title = Component.text("승리", NamedTextColor.GOLD);
                    subtitle = Component.empty();
                } else {
                    // 패배팀
                    title = Component.text("패배", NamedTextColor.GRAY);
                    subtitle = Component.empty();
                }

                streamer.showTitle(
                    Title.title(
                        title,
                        subtitle,
                        Title.Times.times(
                            Duration.ofMillis(500),  // fade in
                            Duration.ofMillis(3000), // stay
                            Duration.ofMillis(500)   // fade out
                        )
                    )
                );
            }
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
