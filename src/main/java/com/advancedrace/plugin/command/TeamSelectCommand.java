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

        // 스트리머인 경우 사용 불가
        if (isStreamer(player)) {
            player.sendMessage(ChatColor.RED + "스트리머는 이 명령어를 사용할 수 없습니다.");
            return true;
        }

        // 소환된 시청자 (SpawnTier가 2)는 스펙테이터 모드가 아니면 사용 불가
        if (teamManager.getSpawnTier(player) == 2 && !teamManager.isSpectatorWithChance(player)) {
            player.sendMessage(ChatColor.RED + "사망 후 스펙테이터 모드에서만 팀을 변경할 수 있습니다.");
            return true;
        }

        // 사망 횟수가 2회 이상이면서 스펙테이터 상태가 아니면 팀 선택 불가
        if (teamManager.getDeathCount(player) >= 2 && !teamManager.isSpectatorWithChance(player)) {
            player.sendMessage(ChatColor.RED + "더 이상 팀변경은 불가능 합니다.");
            return true;
        }

        // 스펙테이터 상태에서 팀 변경 시 (팀 변경 기회 소모)
        if (teamManager.isSpectatorWithChance(player)) {
            teamManager.removeFromSpectator(player);
            // 팀에서 제거하여 다시 대기 상태로 만듦
            if (teamManager.getPlayerTeam(player) != null) {
                teamManager.removePlayer(player);
            }
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
