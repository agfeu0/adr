package com.advancedrace.plugin.listener;

import com.advancedrace.plugin.manager.TeamManager;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.ArrayList;
import java.util.List;

public class BeaconListener implements Listener {

    private TeamManager teamManager;

    public BeaconListener(TeamManager teamManager) {
        this.teamManager = teamManager;
    }

    @EventHandler
    public void onBeaconInteract(PlayerInteractEvent event) {
        // 우클릭이 아니면 무시
        if (!event.getAction().isRightClick()) {
            return;
        }

        // 손이 오프핸드면 무시 (더블 트리거 방지)
        if (event.getHand() == EquipmentSlot.OFF_HAND) {
            return;
        }

        Player player = event.getPlayer();

        // 손에 든 아이템이 신호기인지 확인
        if (player.getInventory().getItemInMainHand().getType() != Material.BEACON) {
            return;
        }

        event.setCancelled(true);

        // 스트리머인지 확인
        if (!isStreamer(player)) {
            player.sendMessage("§c스트리머만 신호기를 사용할 수 있습니다.");
            return;
        }

        TeamManager.Team team = getStreamerTeam(player);

        // 팀이 없으면 무시
        if (team == null) {
            return;
        }

        String streamerName = team.getStreamer();

        // 팀원들 중 생존 중인 플레이어 찾기 (서바이벌 모드만)
        List<Player> survivingTeammates = new ArrayList<>();
        for (Player teammate : team.getPlayers()) {
            // 플레이어가 온라인이고 서바이벌 모드이며 살아있어야 함
            if (teammate.isOnline() && !teammate.isDead() && teammate.getGameMode() == GameMode.SURVIVAL) {
                survivingTeammates.add(teammate);
            }
        }

        // 자신을 제외한 팀원만 텔레포트
        survivingTeammates.remove(player);

        if (survivingTeammates.isEmpty()) {
            player.sendMessage("§e텔레포트할 생존 중인 팀원이 없습니다.");
            return;
        }

        // 손에 든 신호기 아이템 1개 소비
        player.getInventory().getItemInMainHand().setAmount(
            player.getInventory().getItemInMainHand().getAmount() - 1
        );

        // 스트리머 위치로 팀원들 텔레포트
        Location streamerLocation = player.getLocation();
        for (Player teammate : survivingTeammates) {
            teammate.teleport(streamerLocation);
            teammate.sendMessage("§a" + streamerName + " 스트리머에게 텔레포트되었습니다!");
        }

        player.sendMessage("§a✓ 생존 중인 팀원 " + survivingTeammates.size() + "명을 텔레포트했습니다!");
    }

    /**
     * 플레이어가 스트리머인지 확인
     */
    private boolean isStreamer(Player player) {
        for (String streamerName : teamManager.getStreamerNames()) {
            if (streamerName.equals(player.getName())) {
                return true;
            }
        }
        return false;
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
}
