package com.advancedrace.plugin.listener;

import com.advancedrace.plugin.manager.TeamManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

public class BlockPlaceListener implements Listener {

    private TeamManager teamManager;

    public BlockPlaceListener(TeamManager teamManager) {
        this.teamManager = teamManager;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();

        // 베리어 블록 설치 시도 시 취소
        if (event.getBlockPlaced().getType() == Material.BARRIER) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "베리어 블록은 설치할 수 없습니다.");
        }
    }
}
