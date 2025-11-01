package com.advancedrace.plugin.listener;

import com.advancedrace.plugin.manager.TeamManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerNameListener implements Listener {

    private TeamManager teamManager;

    public PlayerNameListener(TeamManager teamManager) {
        this.teamManager = teamManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        updatePlayerDisplay(player, teamManager);
    }

    public static void updatePlayerDisplay(Player player, TeamManager teamManager) {
        TeamManager.Team team = teamManager.getTeam(player);

        if (team != null) {
            // 탭리스트 이름 설정
            Component listName = Component.text("[" + team.getStreamer() + "팀] " + player.getName(),
                    TextColor.color(getColorValue(team.getColor())));
            player.playerListName(listName);

            // 네임태그 설정 (위의 이름)
            Component displayName = Component.text("[" + team.getStreamer() + "팀] " + player.getName(),
                    TextColor.color(getColorValue(team.getColor())));
            player.customName(displayName);
            player.setCustomNameVisible(true);
        } else {
            // 팀이 없으면 기본값으로
            player.playerListName(Component.text(player.getName()));
            player.customName(null);
            player.setCustomNameVisible(false);
        }
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
}
