package com.gmail.val59000mc.threads;

import com.gmail.val59000mc.UhcCore;
import com.gmail.val59000mc.exceptions.UhcPlayerDoesntExistException;
import com.gmail.val59000mc.game.GameManager;
import com.gmail.val59000mc.game.GameState;
import com.gmail.val59000mc.players.PlayersManager;
import com.gmail.val59000mc.players.UhcPlayer;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;

public class KillDisconnectedPlayerThread implements Runnable{
	
	private final UUID uuid;
	private int timeLeft;
	
	public KillDisconnectedPlayerThread(UUID playerUuid, int maxDisconnectPlayersTime){
		uuid = playerUuid;
		timeLeft = maxDisconnectPlayersTime;
	}

	@Override
	public void run() {
		GameManager gm = GameManager.getGameManager();

		if(!gm.getGameState().equals(GameState.PLAYING)) {
			return;
		}

		Player player = Bukkit.getPlayer(uuid);

		if (player != null){
			return; // Player is back online
		}

		if(timeLeft <= 0){
			UhcPlayer uhcPlayer;
			PlayersManager pm = gm.getPlayersManager();
			try {
				uhcPlayer = pm.getUhcPlayer(uuid);
			} catch (UhcPlayerDoesntExistException e){
				e.printStackTrace();
				return;
			}

			// If using offline zombies kill that zombie.
			if (uhcPlayer.getOfflineZombie() != null){
				Optional<LivingEntity> zombie = gm.getMapLoader().getUhcWorld(World.Environment.NORMAL).getLivingEntities()
						.stream()
						.filter(e -> e.getUniqueId().equals(uhcPlayer.getOfflineZombie()))
						.findFirst();

				// Remove zombie
				if (zombie.isPresent()) {
					pm.killOfflineUhcPlayer(uhcPlayer, zombie.get().getLocation(), new HashSet<>(uhcPlayer.getStoredItems()), null);
					zombie.get().remove();
					uhcPlayer.setOfflineZombie(null);
				}
				// No zombie found, kill player without removing zombie.
				else {
					pm.killOfflineUhcPlayer(uhcPlayer, null, new HashSet<>(uhcPlayer.getStoredItems()), null);
				}
			}else{
				pm.killOfflineUhcPlayer(uhcPlayer, new HashSet<>());
			}
		}else{
			timeLeft-=5;
			Bukkit.getScheduler().scheduleSyncDelayedTask(UhcCore.getPlugin(), this, 100);
		}
	}

}