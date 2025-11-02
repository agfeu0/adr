package com.advancedrace.plugin.command;

import com.advancedrace.plugin.AdvancedRace;
import com.advancedrace.plugin.listener.PlayerNameListener;
import com.advancedrace.plugin.manager.DataPersistence;
import com.advancedrace.plugin.manager.GameStateManager;
import com.advancedrace.plugin.manager.TeamManager;
import com.advancedrace.plugin.task.GameTimerTask;
import com.advancedrace.plugin.util.ScoreboardManager;
import com.advancedrace.plugin.util.ViewerInitializer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class GameEndCommand implements CommandExecutor {

    private GameStateManager gameStateManager;
    private TeamManager teamManager;

    public GameEndCommand(GameStateManager gameStateManager, TeamManager teamManager) {
        this.gameStateManager = gameStateManager;
        this.teamManager = teamManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "플레이어만 사용 가능한 명령어입니다.");
            return false;
        }

        Player player = (Player) sender;

        // OP만 사용 가능
        if (!player.isOp()) {
            player.sendMessage(ChatColor.RED + "이 명령어는 OP만 사용 가능합니다.");
            return false;
        }

        // 게임 상태 확인 (진행 중이면 종료)
        if (gameStateManager.isRunning()) {
            gameStateManager.endGame();
        }

        // 게임 타이머 정지
        AdvancedRace advancedRace = AdvancedRace.getInstance();
        GameTimerTask timerTask = advancedRace.getGameTimerTask();
        if (timerTask != null) {
            timerTask.stop();
        }

        // Bukkit Scoreboard에서 모든 팀 제거
        Scoreboard mainScoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        for (Team scoreboardTeam : mainScoreboard.getTeams()) {
            if (scoreboardTeam.getName().startsWith("team_")) {
                scoreboardTeam.unregister();
            }
        }

        // 모든 온라인 플레이어 초기화
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            // 스코어보드 제거
            ScoreboardManager.removeScoreboard(onlinePlayer);

            // 모든 포션 효과 제거
            for (PotionEffect effect : onlinePlayer.getActivePotionEffects()) {
                onlinePlayer.removePotionEffect(effect.getType());
            }

            // 크기 원래대로 복구
            if (onlinePlayer.getAttribute(org.bukkit.attribute.Attribute.GENERIC_SCALE) != null) {
                onlinePlayer.getAttribute(org.bukkit.attribute.Attribute.GENERIC_SCALE).setBaseValue(1.0);
            }

            // 최대 체력 원래대로 복구
            if (onlinePlayer.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH) != null) {
                onlinePlayer.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).setBaseValue(20.0);
            }
            onlinePlayer.setHealth(20.0);

            // 게임모드를 SURVIVAL로 복구
            if (onlinePlayer.getGameMode() == GameMode.SPECTATOR) {
                onlinePlayer.setGameMode(GameMode.SURVIVAL);
            }

            // 인벤토리 클리어
            onlinePlayer.getInventory().clear();

            // 플레이어 디스플레이 네임 초기화 (탭리스트, 네임태그)
            onlinePlayer.playerListName(null);
            onlinePlayer.customName(null);
            onlinePlayer.setCustomNameVisible(false);
        }

        // 게임 데이터 삭제
        DataPersistence.deleteGameData();

        // 모든 팀 제거
        teamManager.removeAllTeams();

        // 모든 플레이어의 팀 변경 기회 초기화
        teamManager.clearAllDeathChances();

        // 모든 플레이어의 스폰 순위 초기화
        teamManager.clearAllSpawnTiers();

        // 모든 플레이어의 사망 횟수 초기화
        teamManager.clearAllDeathCounts();

        // 발전과제 팀 점수 초기화
        if (advancedRace.getAdvancementListener() != null) {
            advancedRace.getAdvancementListener().initializeTeamScores();
        }

        // 발전과제 완료 기록 초기화
        if (advancedRace.getAdvancementManager() != null) {
            advancedRace.getAdvancementManager().resetAdvancements();
        }

        player.sendMessage(ChatColor.GREEN + "✓ 게임이 종료되었습니다!");
        player.sendMessage(ChatColor.YELLOW + "모든 플레이어의 설정이 초기화되었습니다.");

        return true;
    }
}
