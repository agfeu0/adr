package com.advancedrace.plugin.command;

import com.advancedrace.plugin.listener.BeaconListener;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TeleportCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "플레이어만 사용 가능한 명령어입니다.");
            return false;
        }

        if (args.length != 1) {
            sender.sendMessage(ChatColor.RED + "사용법: /발전과제_tp <스트리머이름>");
            return false;
        }

        Player player = (Player) sender;
        String streamerName = args[0];

        // 신호기 위치 가져오기
        Location beaconLocation = BeaconListener.getBeaconLocation(streamerName);

        if (beaconLocation == null) {
            player.sendMessage(ChatColor.RED + streamerName + " 스트리머가 설치한 신호기가 없습니다.");
            return false;
        }

        // 신호기 위에 텔레포트 (Y + 1)
        Location teleportLocation = beaconLocation.clone();
        teleportLocation.setY(teleportLocation.getY() + 1);

        player.teleport(teleportLocation);
        player.sendMessage(ChatColor.GREEN + "✓ " + streamerName + " 스트리머의 신호기 위치로 텔레포트했습니다!");

        return true;
    }
}
