package com.advancedrace.plugin.listener;

import com.advancedrace.plugin.manager.TeamManager;
import com.advancedrace.plugin.util.ViewerInitializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class CompassInteractListener implements Listener {

    private TeamManager teamManager;

    public CompassInteractListener(TeamManager teamManager) {
        this.teamManager = teamManager;
    }

    @EventHandler
    public void onCompassClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        // 나침반이 아니면 무시
        if (item == null || item.getType() != Material.COMPASS) {
            return;
        }

        // 슬롯 5 (인덱스 4)에 있는 나침반인지 확인
        if (player.getInventory().getHeldItemSlot() != 4) {
            return;
        }

        // 우클릭이 아니면 무시
        if (!event.getAction().isRightClick()) {
            return;
        }

        // 시청자인지 확인 (스트리머는 제외)
        TeamManager.Team team = teamManager.getTeam(player);
        if (team == null) {
            return;
        }

        // 나침반 새로고침
        String streamerName = team.getStreamer();
        ViewerInitializer.updateCompass(player, streamerName);

        event.setCancelled(true);
    }
}
