package com.advancedrace.plugin.command;

import com.advancedrace.plugin.manager.TeamManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class StreamerCommand implements CommandExecutor, TabCompleter {

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
                player.sendMessage(ChatColor.RED + "지원하는 색깔: red(빨강), orange(주황), yellow(노랑), dark_green(초록), green(연두), dark_blue(파랑), light_blue(하늘), dark_purple(보라), magenta(분홍), gray(회색), black(검정), white(하양)");
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
            Bukkit.broadcastMessage(ChatColor.GREEN + "✓ " + streamerName + " 스트리머 팀이 생성되었습니다(색깔: " + colorCode + colorName + ChatColor.GREEN + ")");
            Bukkit.broadcastMessage(ChatColor.YELLOW + "시청자들이 /팀선택 명령어로 팀에 합류할 수 있습니다.");
            return true;
        } else {
            player.sendMessage(ChatColor.RED + "팀 생성에 실패했습니다.");
            return false;
        }
    }

    private String getColorCode(String colorName) {
        return switch (colorName.toLowerCase()) {
            case "red" -> "§c";
            case "orange" -> "§6";
            case "yellow" -> "§e";
            case "dark_green" -> "§2";
            case "green" -> "§a";
            case "dark_blue" -> "§1";
            case "light_blue" -> "§b";
            case "dark_purple" -> "§5";
            case "magenta" -> "§d";
            case "gray" -> "§7";
            case "black" -> "§0";
            case "white" -> "§f";
            default -> null;
        };
    }

    private String getColorName(String colorCode) {
        return switch (colorCode) {
            case "§c" -> "빨강";
            case "§6" -> "주황";
            case "§e" -> "노랑";
            case "§2" -> "초록";
            case "§a" -> "연두";
            case "§1" -> "파랑";
            case "§b" -> "하늘";
            case "§5" -> "보라";
            case "§d" -> "분홍";
            case "§7" -> "회색";
            case "§0" -> "검정";
            case "§f" -> "하양";
            default -> "알 수 없음";
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();

        // 1번째 인자일 때 온라인 플레이어 목록 제공
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            for (org.bukkit.entity.Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(input)) {
                    completions.add(player.getName());
                }
            }
        }
        // 2번째 인자일 때 색깔 목록 제공
        else if (args.length == 2) {
            String[] colors = {
                "red", "orange", "yellow", "dark_green", "green",
                "dark_blue", "light_blue", "dark_purple", "magenta",
                "gray", "black", "white"
            };

            String input = args[1].toLowerCase();
            for (String color : colors) {
                if (color.startsWith(input)) {
                    completions.add(color);
                }
            }
        }

        return completions;
    }
}
