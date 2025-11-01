package com.advancedrace.plugin.listener;

import com.advancedrace.plugin.manager.AdvancementManager;
import com.advancedrace.plugin.manager.TeamManager;
import com.advancedrace.plugin.manager.ViewerSummonManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.advancement.Advancement;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;

public class AdvancementListener implements Listener {

    private TeamManager teamManager;
    private AdvancementManager advancementManager;
    private ViewerSummonManager viewerSummonManager;

    public AdvancementListener(TeamManager teamManager, AdvancementManager advancementManager, ViewerSummonManager viewerSummonManager) {
        this.teamManager = teamManager;
        this.advancementManager = advancementManager;
        this.viewerSummonManager = viewerSummonManager;
    }

    @EventHandler
    public void onAdvancementDone(PlayerAdvancementDoneEvent event) {
        Player player = event.getPlayer();
        Advancement advancement = event.getAdvancement();

        // 스트리머인지 확인
        if (!isStreamer(player)) {
            return;
        }

        // 루트 발전과제는 무시 (보통 시작 발전과제)
        if (advancement.getDisplay() == null || advancement.getDisplay().isHidden()) {
            return;
        }

        String advancementName = advancement.getKey().toString();

        // 이미 다른 스트리머가 달성한 발전과제인지 확인
        if (advancementManager.isCompleted(advancementName)) {
            return;
        }

        // 발전과제 완료 표시
        advancementManager.markCompleted(advancementName);

        // 발전과제 유형에 따라 소환 인원 결정
        int summonCount = advancementManager.getAdvancementType(advancementName);

        // 시청자 소환
        int summoned = viewerSummonManager.summonViewers(player.getName(), summonCount);

        // 발전과제 달성 공지 (모두에게)
        Bukkit.broadcastMessage(ChatColor.YELLOW + player.getName() + "님이 발전과제를 달성했습니다!");

        // 소환 결과는 스트리머한테만
        if (summoned > 0) {
            player.sendMessage(ChatColor.GREEN + "→ 시청자 " + summoned + "명이 소환되었습니다!");
        } else {
            player.sendMessage(ChatColor.RED + "→ 소환할 대기 중인 시청자가 없습니다.");
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
