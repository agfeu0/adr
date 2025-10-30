package com.advancedrace.plugin.listener;

import com.advancedrace.plugin.manager.TeamManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;

public class InventorySizeListener implements Listener {

    private TeamManager teamManager;

    public InventorySizeListener(TeamManager teamManager) {
        this.teamManager = teamManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();

        // OP나 스트리머는 제한 없음
        if (player.isOp() || isStreamer(player)) {
            return;
        }

        // 시청자인지 확인
        if (teamManager.getPlayerTeam(player) == null) {
            return;
        }

        // 5번 슬롯(인덱스 4)을 클릭했으면 취소
        if (event.getSlot() == 4) {
            event.setCancelled(true);
            return;
        }

        // 5칸 이상을 클릭했으면 취소
        if (event.getSlot() >= 5) {
            event.setCancelled(true);
        }
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
