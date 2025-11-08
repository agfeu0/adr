package com.advancedrace.plugin.listener;

import com.advancedrace.plugin.manager.TeamManager;
import com.advancedrace.plugin.util.ViewerInitializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
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

        // 베리어 아이템 클릭 방지
        if (event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.BARRIER) {
            event.setCancelled(true);
            return;
        }

        // 나침반 조작 방지 (슬롯 4에서의 모든 동작 차단)
        if (event.getSlot() == 4) {
            event.setCancelled(true);
            return;
        }

        // 다른 곳에서 나침반을 슬롯 4로 옮기려는 시도 방지
        if (event.getCursor() != null && event.getCursor().getType() == Material.COMPASS) {
            event.setCancelled(true);
            return;
        }

        // 5칸 이상을 클릭했으면 취소
        if (event.getSlot() >= 5) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        Player player = (Player) event.getWhoClicked();

        // OP나 스트리머는 제한 없음
        if (player.isOp() || isStreamer(player)) {
            return;
        }

        // 시청자인지 확인
        if (teamManager.getPlayerTeam(player) == null) {
            return;
        }

        // 드래그로 베리어 슬롯에 접근하려는 것 방지
        for (int slot : event.getRawSlots()) {
            if (slot >= 5 && slot < 40) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();

        // OP나 스트리머는 제한 없음
        if (player.isOp() || isStreamer(player)) {
            return;
        }

        // 시청자인지 확인
        if (teamManager.getPlayerTeam(player) == null) {
            return;
        }

        // 나침반이 버려지려고 하면 방지
        if (event.getItemDrop().getItemStack().getType() == Material.COMPASS) {
            event.setCancelled(true);
        }

        // 베리어가 버려지려고 하면 방지
        if (event.getItemDrop().getItemStack().getType() == Material.BARRIER) {
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
