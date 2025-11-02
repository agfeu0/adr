package com.advancedrace.plugin.listener;

import com.advancedrace.plugin.gui.TeamSelectInventoryHolder;
import com.advancedrace.plugin.manager.TeamManager;
import com.advancedrace.plugin.util.ScoreboardManager;
import org.bukkit.Bukkit;
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
            // SpawnTier를 1로 설정 (대기 중 상태)
            teamManager.setSpawnTier(player, 1);

            // 팀 변경 기회가 있으면 사용 (팀 변경 완료)
            if (teamManager.hasDeathChance(player)) {
                teamManager.useDeathChance(player);
            }
            teamManager.removeFromSpectator(player);

            // 플레이어 디스플레이 업데이트 (탭리스트, 네임태그) - 1틱 지연
            Bukkit.getScheduler().scheduleSyncDelayedTask(
                    Bukkit.getPluginManager().getPlugin("AdvancedRace"),
                    () -> PlayerNameListener.updatePlayerDisplay(player, teamManager),
                    1
            );

            // 스코어보드 설정
            ScoreboardManager.setupScoreboard(player, teamManager);

            // 스트리머와 팀 플레이어의 시청자 수 업데이트
            TeamManager.Team team = teamManager.getTeamByStreamer(streamerName);
            if (team != null) {
                int viewerCount = team.getPlayerCount();
                // 스트리머에게 시청자 수 전송
                Player streamer = Bukkit.getPlayer(streamerName);
                if (streamer != null && streamer.isOnline()) {
                    ScoreboardManager.updateViewerCount(streamer, viewerCount);
                }
                // 팀의 모든 시청자에게 시청자 수 업데이트
                for (Player teamPlayer : team.getPlayers()) {
                    if (teamPlayer.isOnline() && !teamPlayer.getName().equals(streamerName)) {
                        ScoreboardManager.updateViewerCount(teamPlayer, viewerCount);
                    }
                }
            }

            player.sendMessage(ChatColor.GREEN + "✓ " + streamerName + " 팀에 합류했습니다!");
            player.sendMessage(ChatColor.YELLOW + "팀장이 발전과제를 달성하면 게임에 소환됩니다!");

            player.closeInventory();
        } else {
            player.sendMessage(ChatColor.RED + "팀 합류에 실패했습니다.");
        }
    }
}
