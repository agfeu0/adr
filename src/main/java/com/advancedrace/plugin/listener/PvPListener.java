package com.advancedrace.plugin.listener;

import com.advancedrace.plugin.manager.TeamManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class PvPListener implements Listener {

    private TeamManager teamManager;

    public PvPListener(TeamManager teamManager) {
        this.teamManager = teamManager;
    }

    @EventHandler
    public void onPlayerDamagePlayer(EntityDamageByEntityEvent event) {
        // 피해자와 공격자 모두 플레이어인지 확인
        if (!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Player)) {
            return;
        }

        Player victim = (Player) event.getEntity();
        Player attacker = (Player) event.getDamager();

        // 공격자의 팀 확인
        TeamManager.Team attackerTeam = teamManager.getTeam(attacker);

        // 공격자가 팀에 속하지 않으면 공격 허용
        if (attackerTeam == null) {
            return;
        }

        // 피해자의 팀 확인
        TeamManager.Team victimTeam = teamManager.getTeam(victim);

        // 피해자가 팀에 속하지 않으면, 스트리머인지 확인
        if (victimTeam == null) {
            // 피해자가 스트리머인지 확인
            if (isStreamer(victim)) {
                // 공격자가 같은 팀의 시청자인지 확인
                String streamerName = victim.getName();
                if (streamerName.equals(attackerTeam.getStreamer())) {
                    // 같은 팀의 시청자가 스트리머를 때리려고 함
                    event.setCancelled(true);
                    attacker.sendMessage("§c같은 팀 멤버(스트리머)를 공격할 수 없습니다!");
                    return;
                }
            }
            // 피해자가 팀에 속하지 않고 스트리머도 아니면 공격 허용
            return;
        }

        // 같은 팀인지 확인
        if (attackerTeam.getName().equals(victimTeam.getName())) {
            // 같은 팀이면, 공격자가 스트리머인지 확인
            if (isStreamer(attacker)) {
                // 스트리머는 자신의 팀을 공격할 수 있음
                return;
            } else {
                // 스트리머가 아니면 공격 취소
                event.setCancelled(true);
                attacker.sendMessage("§c같은 팀 멤버를 공격할 수 없습니다!");
                return;
            }
        }

        // 다른 팀이면 공격 허용
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
