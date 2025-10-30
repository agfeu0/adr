package com.advancedrace.plugin.listener;

import com.advancedrace.plugin.manager.TeamManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class HardcoreDeathListener implements Listener {

    private TeamManager teamManager;

    public HardcoreDeathListener(TeamManager teamManager) {
        this.teamManager = teamManager;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player deadPlayer = event.getEntity();

        // OP나 스트리머는 처리 안함
        if (deadPlayer.isOp() || isStreamer(deadPlayer)) {
            return;
        }

        // 시청자인지 확인
        TeamManager.Team team = teamManager.getTeam(deadPlayer);
        if (team == null) {
            return;
        }

        // 스펙테이터 모드로 전환 (영구 퇴장)
        deadPlayer.setGameMode(GameMode.SPECTATOR);

        // 팀에서 제거
        teamManager.removePlayer(deadPlayer);

        // 메시지
        String streamerName = team.getStreamer();
        Bukkit.broadcastMessage(ChatColor.RED + deadPlayer.getName() + "님이 " + streamerName + " 팀에서 영구 퇴장했습니다.");
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
