package me.gorgeousone.netherview.utils;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public final class TeleportUtils {
	
	private TeleportUtils() {}
	
	public static void setTemporarilyInvulnerable(Player player, JavaPlugin plugin, long duration) {

		player.setInvulnerable(true);

		new BukkitRunnable() {
			@Override
			public void run() {
				player.setInvulnerable(false);
			}
		}.runTaskLater(plugin, duration);
	}
}
