package com.advancedrace.plugin.util;

import com.advancedrace.plugin.listener.PlayerNameListener;
import com.advancedrace.plugin.manager.TeamManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

public class TablistManager {

    /**
     * 탭 리스트를 팀별로 정렬
     * 스트리머와 해당 팀원이 함께 표시되고, 팀에 속하지 않은 플레이어는 맨 뒤
     */
    public static void organizeTablist(TeamManager teamManager) {
        // 플레이어를 팀별로 분류
        Map<String, List<Player>> teamPlayers = new LinkedHashMap<>();
        List<Player> unassignedPlayers = new ArrayList<>();

        // 먼저 모든 스트리머의 팀을 순서대로 추가
        for (String streamerName : teamManager.getStreamerNames()) {
            TeamManager.Team team = teamManager.getTeamByStreamer(streamerName);
            if (team != null) {
                List<Player> players = new ArrayList<>();

                // 스트리머 먼저 추가
                Player streamer = Bukkit.getPlayer(streamerName);
                if (streamer != null && streamer.isOnline()) {
                    players.add(streamer);
                }

                // 팀원 추가
                for (Player player : team.getPlayers()) {
                    if (!player.getName().equals(streamerName) && player.isOnline()) {
                        players.add(player);
                    }
                }

                if (!players.isEmpty()) {
                    teamPlayers.put(streamerName, players);
                }
            }
        }

        // 팀에 속하지 않은 플레이어 분류
        for (Player player : Bukkit.getOnlinePlayers()) {
            boolean isInTeam = false;
            for (List<Player> teamMemberList : teamPlayers.values()) {
                if (teamMemberList.contains(player)) {
                    isInTeam = true;
                    break;
                }
            }
            if (!isInTeam) {
                unassignedPlayers.add(player);
            }
        }

        // 탭 리스트 갱신 (실제 탭 리스트는 Bukkit이 자동으로 정렬하므로,
        // 플레이어 이름 업데이트를 통해 시각적 순서를 유지)
        int priority = 0;

        // 각 팀의 플레이어 정렬
        for (Map.Entry<String, List<Player>> entry : teamPlayers.entrySet()) {
            String streamerName = entry.getKey();
            List<Player> players = entry.getValue();

            // 팀 정보 조회
            TeamManager.Team team = teamManager.getTeamByStreamer(streamerName);
            if (team != null) {
                // 각 플레이어의 탭 리스트 이름 업데이트 (이미 설정된 것이지만 순서 보장)
                for (Player player : players) {
                    if (player.getName().equals(streamerName)) {
                        // 스트리머: 색깔만
                        PlayerNameListener.updatePlayerDisplay(player, teamManager);
                    } else {
                        // 시청자: [팀이름팀] 접두사 + 색깔
                        PlayerNameListener.updatePlayerDisplay(player, teamManager);
                    }
                }
            }
        }

        // 팀에 속하지 않은 플레이어 처리
        for (Player player : unassignedPlayers) {
            // 팀에 속하지 않으면 기본 이름 (아무것도 표시 안함)
            player.playerListName(null);
            player.customName(null);
            player.setCustomNameVisible(false);
        }
    }

    /**
     * 특정 플레이어를 탭 리스트에서 재정렬 (팀 추가/변경 시)
     */
    public static void updatePlayerTablist(Player player, TeamManager teamManager) {
        PlayerNameListener.updatePlayerDisplay(player, teamManager);
    }
}
