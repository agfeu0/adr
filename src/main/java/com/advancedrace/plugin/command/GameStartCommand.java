package com.advancedrace.plugin.command;

import com.advancedrace.plugin.manager.DataPersistence;
import com.advancedrace.plugin.manager.GameStateManager;
import com.advancedrace.plugin.manager.TeamManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GameStartCommand implements CommandExecutor {

    private GameStateManager gameStateManager;
    private TeamManager teamManager;

    public GameStartCommand(GameStateManager gameStateManager, TeamManager teamManager) {
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

        // 이미 게임이 진행 중이면
        if (gameStateManager.isRunning()) {
            player.sendMessage(ChatColor.RED + "이미 게임이 진행 중입니다.");
            return false;
        }

        // 팀이 없으면
        if (teamManager.getStreamerNames().isEmpty()) {
            player.sendMessage(ChatColor.RED + "팀이 없습니다. 먼저 /스트리머 명령어로 팀을 만들어주세요.");
            return false;
        }

        // 게임 시작
        gameStateManager.startGame();

        // 게임 데이터 저장
        DataPersistence.saveGameData(teamManager);

        player.sendMessage(ChatColor.GREEN + "✓ 게임이 시작되었습니다!");
        player.sendMessage(ChatColor.YELLOW + "현재 팀: " + teamManager.getStreamerNames().size() + "개");

        return true;
    }
}
