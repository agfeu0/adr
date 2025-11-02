package com.advancedrace.plugin.listener;

import com.advancedrace.plugin.AdvancedRace;
import com.advancedrace.plugin.manager.AdvancementManager;
import com.advancedrace.plugin.manager.DataPersistence;
import com.advancedrace.plugin.manager.TeamManager;
import com.advancedrace.plugin.manager.ViewerSummonManager;
import com.advancedrace.plugin.util.ScoreboardManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.advancement.Advancement;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;

import java.util.HashMap;
import java.util.Map;

public class AdvancementListener implements Listener {

    private TeamManager teamManager;
    private AdvancementManager advancementManager;
    private ViewerSummonManager viewerSummonManager;

    // 플레이어가 최근에 죽은 시간 추적 (사망 후 5초간 소환 메시지 억제)
    private Map<String, Long> recentDeathTimes = new HashMap<>();

    // 팀별 발전과제 점수 추적
    private Map<String, Integer> teamScores = new HashMap<>();

    public AdvancementListener(TeamManager teamManager, AdvancementManager advancementManager, ViewerSummonManager viewerSummonManager) {
        this.teamManager = teamManager;
        this.advancementManager = advancementManager;
        this.viewerSummonManager = viewerSummonManager;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player deadPlayer = event.getEntity();
        // 플레이어 사망 시간 기록 (사망 후 5초간 소환 메시지 억제)
        recentDeathTimes.put(deadPlayer.getName(), System.currentTimeMillis());
    }

    @EventHandler
    public void onAdvancementDone(PlayerAdvancementDoneEvent event) {
        Player player = event.getPlayer();
        Advancement advancement = event.getAdvancement();

        // 스트리머인지 확인
        if (!isStreamer(player)) {
            return;
        }

        // 루트 발전과제는 무시 (보통 시작 발전과제)
        if (advancement.getDisplay() == null || advancement.getDisplay().isHidden()) {
            return;
        }

        String advancementName = advancement.getKey().toString();

        // 플레이어 킬 관련 발전과제는 무시 (의도하지 않은 소환 방지)
        if (advancementName.contains("kill_player") || advancementName.contains("combat")) {
            return;
        }

        // 이미 다른 스트리머가 달성한 발전과제인지 확인
        if (advancementManager.isCompleted(advancementName)) {
            return;
        }

        // 발전과제 완료 표시
        advancementManager.markCompleted(advancementName);

        // 발전과제 유형에 따라 소환 인원 결정
        int summonCount = advancementManager.getAdvancementType(advancementName);

        // 시청자 소환
        int summoned = viewerSummonManager.summonViewers(player.getName(), summonCount);

        // 팀의 발전과제 점수 업데이트
        String streamerName = player.getName();
        int scorePoints = (summonCount == 1) ? 1 : 3; // 일반은 1점, 특수는 3점
        int currentScore = teamScores.getOrDefault(streamerName, 0);
        int newScore = currentScore + scorePoints;
        teamScores.put(streamerName, newScore);

        // 팀의 모든 플레이어 스코어보드 업데이트
        for (Player p : Bukkit.getOnlinePlayers()) {
            // 시청자인지 확인
            TeamManager.Team team = teamManager.getTeam(p);
            if (team != null && team.getStreamer().equals(streamerName)) {
                ScoreboardManager.updateScore(p, newScore);
            }
            // 스트리머인지 확인
            else if (streamerName.equals(p.getName())) {
                ScoreboardManager.updateScore(p, newScore);
            }
        }

        // 게임 데이터 저장 (점수 및 남은 시간 포함)
        long remainingSeconds = 0;
        if (AdvancedRace.getInstance().getGameTimerTask() != null) {
            remainingSeconds = AdvancedRace.getInstance().getGameTimerTask().getRemainingSeconds();
        }
        DataPersistence.saveGameData(teamManager, teamScores, remainingSeconds);

        // 소환 결과 메시지는 5틱 후에 전송
        Bukkit.getScheduler().scheduleSyncDelayedTask(
                Bukkit.getPluginManager().getPlugin("AdvancedRace"),
                () -> {
                    if (summoned > 0) {
                        player.sendMessage(ChatColor.GREEN + "→ 시청자 " + summoned + "명이 소환되었습니다!");
                    } else {
                        player.sendMessage(ChatColor.RED + "→ 소환할 대기 중인 시청자가 없습니다.");
                    }
                },
                5 // 5틱 지연
        );
    }

    /**
     * 게임 시작 시 모든 팀의 점수 초기화
     */
    public void initializeTeamScores() {
        teamScores.clear();
        for (String streamerName : teamManager.getStreamerNames()) {
            teamScores.put(streamerName, 0);
        }
    }

    /**
     * 팀별 점수 조회
     */
    public Map<String, Integer> getTeamScores() {
        return new HashMap<>(teamScores);
    }

    /**
     * 팀의 점수 설정 (로드 시 사용)
     */
    public void setTeamScore(String streamerName, int score) {
        teamScores.put(streamerName, score);
    }

    private boolean isStreamer(Player player) {
        for (String streamerName : teamManager.getStreamerNames()) {
            if (streamerName.equals(player.getName())) {
                return true;
            }
        }
        return false;
    }
}
