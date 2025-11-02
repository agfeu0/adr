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
        boolean isStreamer = false;

        // 플레이어가 속한 팀이 없으면, 스트리머인지 확인
        if (team == null) {
            team = getStreamerTeam(player);
            if (team != null) {
                isStreamer = true;
            }
        }

        // 팀에 속하면 팀 정보 추가
        if (team != null) {
            final TeamManager.Team finalTeam = team;
            final boolean finalIsStreamer = isStreamer;
            final String playerName = player.getName();

            if (isStreamer) {
                // 스트리머: 접두사 없음, 색깔만 적용
                event.renderer((source, sourceDisplayName, message, viewer) -> {
                    return Component.text()
                            .append(Component.text(playerName, net.kyori.adventure.text.format.TextColor.color(getColorValue(finalTeam.getColor()))))
                            .append(Component.text(": "))
                            .append(message)
                            .build();
                });
            } else {
                // 시청자: [팀이름] 접두사 + 닉네임 (모두 팀 색깔)
                Component prefix = Component.text("[" + finalTeam.getStreamer() + "팀] ", net.kyori.adventure.text.format.TextColor.color(getColorValue(finalTeam.getColor())));
                Component viewerName = Component.text(playerName, net.kyori.adventure.text.format.TextColor.color(getColorValue(finalTeam.getColor())));
                event.renderer((source, sourceDisplayName, message, viewer) -> {
                    return Component.text()
                            .append(prefix)
                            .append(viewerName)
                            .append(Component.text(": "))
                            .append(message)
                            .build();
                });
            }
        }
    }

    /**
     * 플레이어가 스트리머인 경우 해당 팀 반환
     */
    private TeamManager.Team getStreamerTeam(Player player) {
        for (String streamerName : teamManager.getStreamerNames()) {
            if (streamerName.equals(player.getName())) {
                return teamManager.getTeamByStreamer(streamerName);
            }
        }
        return null;
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
