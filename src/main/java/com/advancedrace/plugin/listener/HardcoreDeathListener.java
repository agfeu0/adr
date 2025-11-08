package com.advancedrace.plugin.listener;

import com.advancedrace.plugin.manager.TeamManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

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

        // 사망 횟수 증가
        teamManager.incrementDeathCount(deadPlayer);
        int deathCount = teamManager.getDeathCount(deadPlayer);

        // 50% 확률로 네더의 별 드랍
        if (Math.random() < 0.5) {
            deadPlayer.getWorld().dropItem(deadPlayer.getLocation(), new ItemStack(Material.NETHER_STAR));
        }

        // 첫 번째 사망: 팀 변경 기회 부여
        if (deathCount == 1) {
            // 소환된 시청자 정보 제거 (다른 팀에서 중복 소환 방지)
            String streamerName = team.getStreamer();
            teamManager.removeSummonedViewer(streamerName, deadPlayer.getName());

            // 팀에서 제거 (팀 변경 가능하도록)
            teamManager.removePlayer(deadPlayer);

            // 팀 변경 기회 부여
            teamManager.grantDeathChance(deadPlayer);

            // 메시지
            deadPlayer.sendMessage(ChatColor.YELLOW + "1회 팀 변경이 가능합니다.");

            // 1초 후 스펙테이터 모드로 전환 및 스펙테이터 등록
            Bukkit.getScheduler().scheduleSyncDelayedTask(
                    Bukkit.getPluginManager().getPlugin("AdvancedRace"),
                    () -> {
                        deadPlayer.setGameMode(GameMode.SPECTATOR);
                        teamManager.markAsSpectator(deadPlayer);
                    },
                    20 // 1초 = 20틱
            );
        } else {
            // 두 번째 이상 사망: 팀에 남아있지만 대기 중 상태로
            // SpawnTier를 1로 설정 (대기 중, 발전과제로 다시 소환 가능)
            teamManager.setSpawnTier(deadPlayer, 1);

            // 1초 후 스펙테이터 모드로 전환
            Bukkit.getScheduler().scheduleSyncDelayedTask(
                    Bukkit.getPluginManager().getPlugin("AdvancedRace"),
                    () -> {
                        deadPlayer.setGameMode(GameMode.SPECTATOR);
                    },
                    20 // 1초 = 20틱
            );
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
