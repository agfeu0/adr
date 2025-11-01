package com.advancedrace.plugin.listener;

import com.advancedrace.plugin.manager.TeamManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

public class PlayerDeathListener implements Listener {

    private TeamManager teamManager;
    private Random random = new Random();

    public PlayerDeathListener(TeamManager teamManager) {
        this.teamManager = teamManager;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player deadPlayer = event.getEntity();
        Player killer = deadPlayer.getKiller();

        // 시청자인지 확인
        TeamManager.Team deadTeam = teamManager.getTeam(deadPlayer);
        if (deadTeam != null) {
            // 시청자이면 베리어와 나침반은 드랍 안 함
            event.getDrops().removeIf(item ->
                item.getType() == Material.BARRIER || item.getType() == Material.COMPASS
            );
        }

        // 킬러가 없으면 무시
        if (killer == null) {
            return;
        }

        // 킬러의 팀과 죽은 플레이어의 팀 확인
        TeamManager.Team killerTeam = teamManager.getTeam(killer);

        // 팀이 없으면 무시
        if (killerTeam == null || deadTeam == null) {
            return;
        }

        // 킬러와 죽은 플레이어가 같은 팀인지 확인
        if (!killerTeam.getName().equals(deadTeam.getName())) {
            return;
        }

        // 킬러가 자신의 팀원을 처치했으므로 50% 확률로 네더의 별 획득
        if (random.nextDouble() < 0.5) {
            ItemStack netherStar = new ItemStack(Material.NETHER_STAR, 1);
            killer.getWorld().dropItem(killer.getLocation(), netherStar);
            killer.sendMessage("§a✓ 네더의 별을 획득했습니다!");
        }
    }
}
