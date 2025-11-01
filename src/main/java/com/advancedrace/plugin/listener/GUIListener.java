package com.advancedrace.plugin.listener;

import com.advancedrace.plugin.gui.TeamSelectInventoryHolder;
import com.advancedrace.plugin.manager.TeamManager;
import com.advancedrace.plugin.util.ViewerInitializer;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class GUIListener implements Listener {

    private boolean isStreamer(Player player, TeamManager teamManager) {
        for (String streamerName : teamManager.getStreamerNames()) {
            if (streamerName.equals(player.getName())) {
                return true;
            }
        }
        return false;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inventory = event.getInventory();
        InventoryHolder holder = inventory.getHolder();

        if (!(holder instanceof TeamSelectInventoryHolder)) {
            return;
        }

        event.setCancelled(true);

        if (event.getCurrentItem() == null || event.getCurrentItem().getType().isAir()) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        TeamSelectInventoryHolder guiHolder = (TeamSelectInventoryHolder) holder;
        TeamManager teamManager = guiHolder.getTeamManager();

        // 아이템의 디스플레이 네임에서 스트리머 이름 추출
        if (event.getCurrentItem().getItemMeta() == null) {
            return;
        }

        String displayName = event.getCurrentItem().getItemMeta().getDisplayName();
        String streamerName = ChatColor.stripColor(displayName);

        // 팀에 추가
        if (teamManager.addPlayerToTeam(player, streamerName)) {
            // 시청자 초기화 (OP나 스트리머 아닐 때만)
            if (!player.isOp() && !isStreamer(player, teamManager)) {
                ViewerInitializer.initializeViewer(player);
                ViewerInitializer.updateCompass(player, streamerName);
            }
            // 플레이어 디스플레이 업데이트 (탭리스트, 네임태그)
            PlayerNameListener.updatePlayerDisplay(player, teamManager);
            player.sendMessage(ChatColor.GREEN + "✓ " + streamerName + " 팀에 합류했습니다!");
            player.closeInventory();
        } else {
            player.sendMessage(ChatColor.RED + "팀 합류에 실패했습니다.");
        }
    }
}
