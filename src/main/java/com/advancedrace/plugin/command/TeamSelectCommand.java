package com.advancedrace.plugin.command;

import com.advancedrace.plugin.AdvancedRace;
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

        // 스트리머인 경우 사용 불가
        if (isStreamer(player)) {
            player.sendMessage(ChatColor.RED + "스트리머는 이 명령어를 사용할 수 없습니다.");
            return false;
        }

        // 이미 팀에 속해있으면 팀 변경 불가
        if (teamManager.getTeam(player) != null) {
            player.sendMessage(ChatColor.RED + "이미 팀에 속해있습니다. 팀을 변경할 수 없습니다.");
            return false;
        }

        // 팀 선택 GUI 열기
        TeamSelectGUI gui = new TeamSelectGUI(teamManager);
        gui.openGUI(player);

        return true;
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
