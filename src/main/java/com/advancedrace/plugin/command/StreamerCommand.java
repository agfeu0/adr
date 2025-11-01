package com.advancedrace.plugin.command;

import com.advancedrace.plugin.manager.TeamManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class StreamerCommand implements CommandExecutor {

    private TeamManager teamManager;

    public StreamerCommand(TeamManager teamManager) {
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

        if (args.length < 1 || args.length > 2) {
            sender.sendMessage(ChatColor.RED + "사용법: /스트리머 <닉네임> [색깔]");
            return false;
        }

        String streamerName = args[0];
        String colorCode = null;

        // 색깔이 지정되면 처리
        if (args.length == 2) {
            colorCode = getColorCode(args[1]);
            if (colorCode == null) {
                player.sendMessage(ChatColor.RED + "지원하는 색깔: red(빨강), gold(금색), yellow(노랑), green(초록), aqua(하늘), blue(파랑), light_purple(자주)");
                return false;
            }
        }

        // 이미 스트리머가 존재하는지 확인
        if (teamManager.getTeamByStreamer(streamerName) != null) {
            player.sendMessage(ChatColor.RED + "이미 존재하는 스트리머입니다: " + streamerName);
            return false;
        }

        // 팀 생성
        if (teamManager.createTeam(streamerName, colorCode)) {
            String colorName = colorCode != null ? getColorName(colorCode) : "랜덤";
            player.sendMessage(ChatColor.GREEN + "✓ " + streamerName + " 스트리머 팀이 생성되었습니다! (색깔: " + colorCode + colorName + ChatColor.GREEN + ")");
            player.sendMessage(ChatColor.YELLOW + "시청자들이 /팀선택 명령어로 팀에 합류할 수 있습니다.");
            return true;
        } else {
            player.sendMessage(ChatColor.RED + "팀 생성에 실패했습니다.");
            return false;
        }
    }

    private String getColorCode(String colorName) {
        return switch (colorName.toLowerCase()) {
            case "red" -> "§c";
            case "gold" -> "§6";
            case "yellow" -> "§e";
            case "green" -> "§a";
            case "aqua" -> "§b";
            case "blue" -> "§9";
            case "light_purple" -> "§d";
            default -> null;
        };
    }

    private String getColorName(String colorCode) {
        return switch (colorCode) {
            case "§c" -> "빨강";
            case "§6" -> "금색";
            case "§e" -> "노랑";
            case "§a" -> "초록";
            case "§b" -> "하늘색";
            case "§9" -> "파랑";
            case "§d" -> "자주색";
            default -> "알 수 없음";
        };
    }
}
