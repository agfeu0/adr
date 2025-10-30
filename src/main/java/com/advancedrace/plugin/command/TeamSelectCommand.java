package com.advancedrace.plugin.command;

import com.advancedrace.plugin.manager.TeamManager;
import com.advancedrace.plugin.gui.TeamSelectGUI;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TeamSelectCommand implements CommandExecutor {

    private TeamManager teamManager;

    public TeamSelectCommand(TeamManager teamManager) {
        this.teamManager = teamManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "플레이어만 사용 가능한 명령어입니다.");
            return false;
        }

        Player player = (Player) sender;

        // 팀 선택 GUI 열기
        TeamSelectGUI gui = new TeamSelectGUI(teamManager);
        gui.openGUI(player);

        return true;
    }
}
