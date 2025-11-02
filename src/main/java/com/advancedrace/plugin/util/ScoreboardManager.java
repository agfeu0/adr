package com.advancedrace.plugin.util;

import com.advancedrace.plugin.manager.TeamManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

public class ScoreboardManager {

    /**
     * 플레이어의 스코어보드 설정
     */
    public static void setupScoreboard(Player player, TeamManager teamManager) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective("game", "dummy", "§b발전과제 데스런");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        // 점수 설정 (아래에서 위로 표시되므로 역순)
        int score = 7;

        // 시청자 수 및 팀 정보 조회
        TeamManager.Team team = teamManager.getTeam(player);

        // 플레이어가 시청자가 아니면 스트리머인지 확인
        if (team == null) {
            team = getStreamerTeam(player, teamManager);
        }

        String streamerName = (team != null) ? team.getStreamer() : "없음";
        int viewerCount = (team != null) ? team.getPlayerCount() : 0;

        // 빈 줄 (맨 위)
        objective.getScore("  ").setScore(score--);

        // 남은 시간 (기본값)
        objective.getScore("§e남은 시간: §f00:00:00").setScore(score--);

        // 빈 줄
        objective.getScore(" ").setScore(score--);

        // 스트리머 정보 (스트리머 이름에 팀 색깔 적용)
        String streamerDisplay = (team != null) ? team.getColor() + streamerName : "없음";
        objective.getScore("§e스트리머: " + streamerDisplay).setScore(score--);

        // 발전과제 점수
        objective.getScore("§e발전과제: §f0점").setScore(score--);

        // 시청자 수
        objective.getScore("§e시청자: §f" + viewerCount).setScore(score--);

        // 빈 줄 (맨 아래)
        objective.getScore("   ").setScore(score--);

        player.setScoreboard(scoreboard);
    }

    /**
     * 스코어보드 업데이트 - 남은 시간
     */
    public static void updateTime(Player player, String timeString) {
        Scoreboard scoreboard = player.getScoreboard();
        Objective objective = scoreboard.getObjective("game");
        if (objective == null) return;

        // 기존 시간 항목 제거
        scoreboard.getEntries().stream()
                .filter(entry -> entry.startsWith("§e남은 시간:"))
                .forEach(scoreboard::resetScores);

        // 새로운 시간 추가
        objective.getScore("§e남은 시간: §f" + timeString).setScore(6);
    }

    /**
     * 스코어보드 업데이트 - 발전과제 점수
     */
    public static void updateScore(Player player, int score) {
        Scoreboard scoreboard = player.getScoreboard();
        Objective objective = scoreboard.getObjective("game");
        if (objective == null) return;

        // 기존 발전과제 항목 제거
        scoreboard.getEntries().stream()
                .filter(entry -> entry.startsWith("§e발전과제:"))
                .forEach(scoreboard::resetScores);

        // 새로운 발전과제 점수 추가
        objective.getScore("§e발전과제: §f" + score + "점").setScore(3);
    }

    /**
     * 스코어보드 업데이트 - 시청자 수
     */
    public static void updateViewerCount(Player player, int count) {
        Scoreboard scoreboard = player.getScoreboard();
        Objective objective = scoreboard.getObjective("game");
        if (objective == null) return;

        // 기존 시청자 항목 제거
        scoreboard.getEntries().stream()
                .filter(entry -> entry.startsWith("§e시청자:"))
                .forEach(scoreboard::resetScores);

        // 새로운 시청자 수 추가
        objective.getScore("§e시청자: §f" + count).setScore(2);
    }

    /**
     * 스코어보드 초기화
     */
    public static void removeScoreboard(Player player) {
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    /**
     * 플레이어가 스트리머인 경우 해당 팀 반환
     */
    private static TeamManager.Team getStreamerTeam(Player player, TeamManager teamManager) {
        for (String streamerName : teamManager.getStreamerNames()) {
            if (streamerName.equals(player.getName())) {
                return teamManager.getTeamByStreamer(streamerName);
            }
        }
        return null;
    }
}
