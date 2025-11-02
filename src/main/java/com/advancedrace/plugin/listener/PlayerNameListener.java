package com.advancedrace.plugin.listener;

import com.advancedrace.plugin.manager.DataPersistence;
import com.advancedrace.plugin.manager.TeamManager;
import com.advancedrace.plugin.util.ScoreboardManager;
import com.advancedrace.plugin.util.ViewerInitializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class PlayerNameListener implements Listener {

    private TeamManager teamManager;

    public PlayerNameListener(TeamManager teamManager) {
        this.teamManager = teamManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // 저장된 팀 정보 확인 및 자동 추가
        String streamerName = DataPersistence.getStreamerForPlayer(player.getName());
        if (streamerName != null) {
            TeamManager.Team team = teamManager.getTeamByStreamer(streamerName);
            if (team != null) {
                // 팀에 추가
                teamManager.addPlayerToTeam(player, streamerName);
                // SpawnTier를 1로 설정 (대기 중 상태)
                teamManager.setSpawnTier(player, 1);
                // 나침반 업데이트
                ViewerInitializer.updateCompass(player, streamerName);
            }
        }

        updatePlayerDisplay(player, teamManager);
        // 스코어보드 설정
        ScoreboardManager.setupScoreboard(player, teamManager);
    }

    public static void updatePlayerDisplay(Player player, TeamManager teamManager) {
        TeamManager.Team team = teamManager.getTeam(player);
        boolean isStreamer = false;

        // 플레이어가 속한 팀이 없으면, 스트리머인지 확인
        if (team == null) {
            team = getStreamerTeam(player, teamManager);
            if (team != null) {
                isStreamer = true;
            }
        }

        if (team != null) {
            // Scoreboard 팀 설정으로 네임태그 색상 적용
            Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
            String teamName = "team_" + team.getStreamer();
            Team scoreboardTeam = scoreboard.getTeam(teamName);

            // 팀이 없으면 생성
            if (scoreboardTeam == null) {
                scoreboardTeam = scoreboard.registerNewTeam(teamName);
                scoreboardTeam.setColor(getChatColor(team.getColor()));
            }

            // 플레이어를 팀에 추가
            if (!scoreboardTeam.hasEntry(player.getName())) {
                scoreboardTeam.addEntry(player.getName());
            }

            if (isStreamer) {
                // 스트리머: 닉네임에 색깔 적용 (시청자와 동일한 방식)
                Component listName = Component.text(player.getName(), TextColor.color(getColorValue(team.getColor())));
                player.playerListName(listName);

                Component displayName = Component.text(player.getName(), TextColor.color(getColorValue(team.getColor())));
                player.customName(displayName);
                player.setCustomNameVisible(true);
            } else {
                // 시청자: [팀이름] 접두사 + 닉네임 (모두 팀 색깔)
                Component listName = Component.text("[" + team.getStreamer() + "팀] ", TextColor.color(getColorValue(team.getColor())))
                        .append(Component.text(player.getName(), TextColor.color(getColorValue(team.getColor()))));
                player.playerListName(listName);

                Component displayName = Component.text("[" + team.getStreamer() + "팀] ", TextColor.color(getColorValue(team.getColor())))
                        .append(Component.text(player.getName(), TextColor.color(getColorValue(team.getColor()))));
                player.customName(displayName);
                player.setCustomNameVisible(true);
            }
        } else {
            // 팀이 없으면 기본값으로
            player.playerListName(Component.text(player.getName()));
            player.customName(null);
            player.setCustomNameVisible(false);

            // Scoreboard 팀에서 제거
            Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
            for (Team scoreboardTeam : scoreboard.getTeams()) {
                if (scoreboardTeam.hasEntry(player.getName())) {
                    scoreboardTeam.removeEntry(player.getName());
                }
            }
        }
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

    private static int getColorValue(String colorCode) {
        return switch (colorCode) {
            case "§c" -> 0xFF5555; // 빨강
            case "§6" -> 0xFFAA00; // 주황
            case "§e" -> 0xFFFF55; // 노랑
            case "§2" -> 0x00AA00; // 초록
            case "§a" -> 0x55FF55; // 연두
            case "§1" -> 0x0000AA; // 파랑
            case "§b" -> 0x55FFFF; // 하늘
            case "§5" -> 0xAA00AA; // 보라
            case "§d" -> 0xFF55FF; // 분홍
            case "§7" -> 0xAAAAAA; // 회색
            case "§0" -> 0x000000; // 검정
            case "§f" -> 0xFFFFFF; // 하양
            default -> 0xFFFFFF;
        };
    }

    private static ChatColor getChatColor(String colorCode) {
        return switch (colorCode) {
            case "§c" -> ChatColor.RED;
            case "§6" -> ChatColor.GOLD;
            case "§e" -> ChatColor.YELLOW;
            case "§2" -> ChatColor.DARK_GREEN;
            case "§a" -> ChatColor.GREEN;
            case "§1" -> ChatColor.DARK_BLUE;
            case "§b" -> ChatColor.AQUA;
            case "§5" -> ChatColor.DARK_PURPLE;
            case "§d" -> ChatColor.LIGHT_PURPLE;
            case "§7" -> ChatColor.GRAY;
            case "§0" -> ChatColor.BLACK;
            case "§f" -> ChatColor.WHITE;
            default -> ChatColor.WHITE;
        };
    }
}
