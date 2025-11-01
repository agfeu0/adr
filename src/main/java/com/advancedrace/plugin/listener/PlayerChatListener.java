package com.advancedrace.plugin.listener;

import com.advancedrace.plugin.manager.TeamManager;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class PlayerChatListener implements Listener {

    private TeamManager teamManager;

    public PlayerChatListener(TeamManager teamManager) {
        this.teamManager = teamManager;
    }

    @EventHandler
    public void onPlayerChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        TeamManager.Team team = teamManager.getTeam(player);

        // 팀에 속하면 팀 정보 추가
        if (team != null) {
            Component prefix = Component.text("[" + team.getStreamer() + "팀] ", net.kyori.adventure.text.format.TextColor.color(getColorValue(team.getColor())));
            event.renderer((source, sourceDisplayName, message, viewer) -> {
                return Component.text()
                        .append(prefix)
                        .append(sourceDisplayName)
                        .append(Component.text(": "))
                        .append(message)
                        .build();
            });
        }
    }

    private int getColorValue(String colorCode) {
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
