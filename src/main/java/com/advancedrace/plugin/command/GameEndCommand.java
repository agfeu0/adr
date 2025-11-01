package com.advancedrace.plugin.command;

import com.advancedrace.plugin.manager.DataPersistence;
import com.advancedrace.plugin.manager.GameStateManager;
import com.advancedrace.plugin.manager.TeamManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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

        // 게임이 진행 중이 아니면
        if (!gameStateManager.isRunning()) {
            player.sendMessage(ChatColor.RED + "진행 중인 게임이 없습니다.");
            return false;
        }

        // 게임 종료
        gameStateManager.endGame();

        // 게임 데이터 삭제
        DataPersistence.deleteGameData();

        player.sendMessage(ChatColor.GREEN + "✓ 게임이 종료되었습니다!");
        player.sendMessage(ChatColor.YELLOW + "모든 팀 정보가 초기화되었습니다.");

        return true;
    }
}
