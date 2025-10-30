package com.advancedrace.plugin.manager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class ViewerSummonManager {

    private TeamManager teamManager;

    public ViewerSummonManager(TeamManager teamManager) {
        this.teamManager = teamManager;
    }

    /**
     * 팀에 시청자를 소환 (대기중인 플레이어 중에서)
     * @param streamerName 스트리머 이름
     * @param count 소환할 인원수
     * @return 실제 소환된 인원수
     */
    public int summonViewers(String streamerName, int count) {
        // 팀 확인
        TeamManager.Team team = teamManager.getTeamByStreamer(streamerName);
        if (team == null) {
            return 0;
        }

        // 온라인 플레이어 중에서 팀에 속하지 않은 플레이어 찾기
        List<Player> availablePlayers = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            // 이미 팀에 속한 플레이어는 제외
            if (teamManager.getPlayerTeam(player) == null) {
                // OP나 스트리머는 제외
                if (!player.isOp() && !isStreamer(player)) {
                    availablePlayers.add(player);
                }
            }
        }

        // 소환할 수 있는 인원만큼만 소환
        int actualCount = Math.min(count, availablePlayers.size());

        for (int i = 0; i < actualCount; i++) {
            Player player = availablePlayers.get(i);
            // 팀에 추가
            if (teamManager.addPlayerToTeam(player, streamerName)) {
                // 초기화
                com.advancedrace.plugin.util.ViewerInitializer.initializeViewer(player);
                com.advancedrace.plugin.util.ViewerInitializer.updateCompass(player, streamerName);

                player.sendMessage("§a✓ " + streamerName + " 팀에 소환되었습니다!");
            }
        }

        return actualCount;
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
