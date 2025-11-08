package com.advancedrace.plugin.listener;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class PlayerMoveListener implements Listener {

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        // 스펙테이터 모드이고, from 위치와 to 위치가 다르면 이동 취소
        if (player.getGameMode() == GameMode.SPECTATOR) {
            // 모든 스펙테이터의 이동을 원래 위치로 되돌림
            event.setCancelled(true);
        }
    }
}
