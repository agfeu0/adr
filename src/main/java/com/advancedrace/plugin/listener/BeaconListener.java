package com.advancedrace.plugin.listener;

import com.advancedrace.plugin.manager.TeamManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Beacon;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.*;

public class BeaconListener implements Listener {

    private TeamManager teamManager;
    private Map<UUID, String> pendingTeleport = new HashMap<>();
    private static Map<String, Location> lastBeaconLocation = new HashMap<>();

    public BeaconListener(TeamManager teamManager) {
        this.teamManager = teamManager;
    }

    @EventHandler
    public void onBeaconPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();

        // 신호기가 설치되었는지 확인
        if (block.getType() != Material.BEACON) {
            return;
        }

        Player placer = event.getPlayer();
        TeamManager.Team team = teamManager.getTeam(placer);

        // 팀이 없으면 무시
        if (team == null) {
            return;
        }

        String streamerName = team.getStreamer();
        Set<Player> teamPlayers = team.getPlayers();

        // 신호기 위치 저장
        lastBeaconLocation.put(streamerName, block.getLocation());

        // 팀원들에게 메시지 전송
        Component message = Component.text()
                .append(Component.text(streamerName, NamedTextColor.YELLOW))
                .append(Component.text("님이 신호기를 설치했습니다!", NamedTextColor.WHITE))
                .append(Component.newline())
                .append(Component.text("[텔레포트]", NamedTextColor.GREEN)
                        .clickEvent(ClickEvent.runCommand("/발전과제_tp " + streamerName)))
                .append(Component.text(" 클릭하면 신호기 위치로 텔레포트됩니다.", NamedTextColor.WHITE))
                .build();

        for (Player teammate : teamPlayers) {
            if (!teammate.equals(placer)) {
                teammate.sendMessage(message);
            }
        }
    }

    public static Location getBeaconLocation(String streamerName) {
        return lastBeaconLocation.get(streamerName);
    }
}
