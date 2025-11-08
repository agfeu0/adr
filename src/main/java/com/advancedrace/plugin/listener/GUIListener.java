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
            // 기존 네임태그 색상 초기화
            player.customName(null);
            player.setCustomNameVisible(false);

            // SpawnTier를 1로 설정 (대기 중 상태)
            teamManager.setSpawnTier(player, 1);

            // 스펙테이터 상태 제거 (팀 변경 후 복귀한 경우)
            teamManager.removeFromSpectator(player);

            // 팀 변경 기회 사용 (스펙테이터 상태에서 팀을 변경한 경우만)
            // 처음 팀을 선택하는 경우는 기회를 사용하지 않음
            if (teamManager.isSpectatorWithChance(player)) {
                teamManager.useDeathChance(player);
            }

            // 스트리머의 표시도 함께 업데이트 (시청자가 참여했으므로 스트리머 정보 갱신)
            Player streamer = Bukkit.getPlayer(streamerName);
            if (streamer != null && streamer.isOnline()) {
                // 2틱 지연으로 스트리머 업데이트 (플레이어가 팀에 완전히 추가되도록 대기)
                Bukkit.getScheduler().scheduleSyncDelayedTask(
                        Bukkit.getPluginManager().getPlugin("AdvancedRace"),
                        () -> PlayerNameListener.updatePlayerDisplay(streamer, teamManager),
                        2
                );
            }

            // 플레이어 디스플레이 업데이트 (탭리스트, 네임태그) - 1틱 지연
            Bukkit.getScheduler().scheduleSyncDelayedTask(
                    Bukkit.getPluginManager().getPlugin("AdvancedRace"),
                    () -> PlayerNameListener.updatePlayerDisplay(player, teamManager),
                    1
            );

            // 스트리머의 스코어보드만 갱신 (시청자는 소환될 때만 스코어보드 표시)
            if (streamer != null && streamer.isOnline()) {
                Bukkit.getScheduler().scheduleSyncDelayedTask(
                        Bukkit.getPluginManager().getPlugin("AdvancedRace"),
                        () -> ScoreboardManager.setupScoreboard(streamer, teamManager),
                        3
                );
            }

            // 스트리머와 팀 플레이어의 시청자 수 업데이트
            TeamManager.Team team = teamManager.getTeamByStreamer(streamerName);
            if (team != null) {
                int viewerCount = team.getPlayerCount();
                // 스트리머에게 시청자 수 전송 (위에서 이미 정의된 streamer 사용)
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
            player.sendMessage(ChatColor.YELLOW + "스트리머가 발전과제를 달성하면 게임에 소환됩니다!");

            player.closeInventory();
        } else {
            player.sendMessage(ChatColor.RED + "팀 합류에 실패했습니다.");
        }
    }
}
