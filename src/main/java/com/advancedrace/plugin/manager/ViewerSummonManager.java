package com.advancedrace.plugin.manager;

import com.advancedrace.plugin.util.ViewerInitializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

public class ViewerSummonManager {

    private TeamManager teamManager;

    public ViewerSummonManager(TeamManager teamManager) {
        this.teamManager = teamManager;
    }

    /**
     * 팀에 시청자를 소환 (대기중인 플레이어 중에서)
     * @param streamerName 스트리머 이름
     * @param count 소환할 인원수
     * @return 실제 소환된 인원수
     */
    public int summonViewers(String streamerName, int count) {
        // 팀 확인
        TeamManager.Team team = teamManager.getTeamByStreamer(streamerName);
        if (team == null) {
            return 0;
        }

        // 팀에 속한 플레이어 중 아직 초기화되지 않은 플레이어 찾기
        List<Player> waitingPlayers = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            // 스트리머 자신은 제외
            if (player.getName().equals(streamerName)) {
                continue;
            }

            // 팀 변경 기회가 있으면 절대 소환 안함 (첫 사망 플레이어 제외)
            if (teamManager.hasDeathChance(player)) {
                continue;
            }

            // 이 팀에 속한 플레이어 확인
            if (streamerName.equals(teamManager.getTeam(player) != null ? teamManager.getTeam(player).getStreamer() : null)) {
                // 아직 스폰되지 않은 플레이어만 (스폰 순위가 설정되지 않은 플레이어)
                // 그리고 이미 소환되지 않은 플레이어만
                if (teamManager.getSpawnTier(player) == 1 &&
                    !teamManager.getSummonedViewersSet(streamerName).contains(player.getName())) {
                    waitingPlayers.add(player);
                }
            }
        }

        // 소환할 수 있는 인원만큼만 소환
        int actualCount = Math.min(count, waitingPlayers.size());

        for (int i = 0; i < actualCount; i++) {
            Player viewer = waitingPlayers.get(i);

            // 소환된 플레이어 표시
            teamManager.markViewerAsSummoned(streamerName, viewer.getName());

            // 소환된 플레이어는 후순위로 설정
            teamManager.setSpawnTier(viewer, 2);

            // 서바이벌 모드로 설정
            viewer.setGameMode(GameMode.SURVIVAL);

            // 초기화 (TeamManager 전달해서 5초 지연 적용)
            com.advancedrace.plugin.util.ViewerInitializer.initializeViewer(viewer, teamManager);

            // 기본 템 제공 (돌 곡괭이)
            giveStartingItems(viewer);

            // 팀장을 가리키는 나침반 지급
            ViewerInitializer.updateCompass(viewer, streamerName);

            // 스코어보드 설정 및 시청자 수 업데이트
            com.advancedrace.plugin.util.ScoreboardManager.setupScoreboard(viewer, teamManager);
            com.advancedrace.plugin.util.ScoreboardManager.updateViewerCount(viewer, team.getPlayerCount());

            viewer.sendMessage("§a✓ 게임에 소환되었습니다!");

            // 팀장에게 텔레포트 및 폭죽 효과 (5초 후)
            Player streamer = Bukkit.getPlayer(streamerName);
            if (streamer != null && streamer.isOnline()) {
                teleportStreamerAndFirework(streamer, viewer);
            }
        }

        // 팀의 모든 플레이어에게 시청자 수 업데이트 (새로운 시청자 추가됨)
        for (Player p : Bukkit.getOnlinePlayers()) {
            // 시청자인지 확인
            if (streamerName.equals(teamManager.getTeam(p) != null ? teamManager.getTeam(p).getStreamer() : null)) {
                com.advancedrace.plugin.util.ScoreboardManager.updateViewerCount(p, team.getPlayerCount());
            }
            // 스트리머인지 확인
            else if (streamerName.equals(p.getName())) {
                com.advancedrace.plugin.util.ScoreboardManager.updateViewerCount(p, team.getPlayerCount());
            }
        }

        return actualCount;
    }

    /**
     * 시청자를 팀장 위치로 텔레포트하고 폭죽 효과 생성
     */
    private void teleportStreamerAndFirework(Player streamer, Player viewer) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("AdvancedRace");
        if (plugin == null) {
            return;
        }

        // 바로 텔레포트
        if (!streamer.isOnline() || !viewer.isOnline()) {
            return;
        }

        // 팀장의 위치 저장
        org.bukkit.Location streamerLocation = streamer.getLocation().clone();

        // 시청자를 팀장 위치로 텔레포트
        viewer.teleport(streamer.getLocation());

        // 폭죽 효과 재생 (1초 후)
        if (plugin != null) {
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                spawnFirework(streamerLocation);
            }, 20); // 20틱 = 1초
        }
    }

    /**
     * 폭죽 효과 생성
     */
    private void spawnFirework(org.bukkit.Location location) {
        Firework firework = location.getWorld().spawn(location, Firework.class);
        FireworkMeta meta = firework.getFireworkMeta();

        // 폭죽 효과 설정 (무지개 색상)
        FireworkEffect effect = FireworkEffect.builder()
                .withColor(Color.RED, Color.ORANGE, Color.YELLOW, Color.GREEN, Color.AQUA, Color.BLUE, Color.PURPLE)
                .with(FireworkEffect.Type.BALL_LARGE)
                .withFlicker()
                .withTrail()
                .build();

        meta.addEffect(effect);
        meta.setPower(2); // 폭죽 높이

        firework.setFireworkMeta(meta);
    }

    /**
     * 시청자에게 기본 템 제공 (돌 도구)
     */
    private void giveStartingItems(Player player) {
        // 인벤토리를 0-4 슬롯만 사용 가능하므로 슬롯 0-3에만 아이템 배치

        // 슬롯 0: 돌 곡괭이
        ItemStack pickaxe = new ItemStack(Material.STONE_PICKAXE);
        player.getInventory().setItem(0, pickaxe);

        // 슬롯 1: 돌 도끼
        ItemStack axe = new ItemStack(Material.STONE_AXE);
        player.getInventory().setItem(1, axe);

        // 슬롯 2: 돌 삽
        ItemStack shovel = new ItemStack(Material.STONE_SHOVEL);
        player.getInventory().setItem(2, shovel);

        // 슬롯 3: 횃불 (32개)
        ItemStack torch = new ItemStack(Material.TORCH, 32);
        player.getInventory().setItem(3, torch);
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
