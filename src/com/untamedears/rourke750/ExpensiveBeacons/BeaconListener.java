package com.untamedears.rourke750.ExpensiveBeacons;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Beacon;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.BeaconInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.untamedears.citadel.Citadel;
import com.untamedears.citadel.entity.IReinforcement;
import com.untamedears.citadel.entity.PlayerReinforcement;
import com.untamedears.citadel.events.AcidBlockDestroyEvent;
import com.untamedears.citadel.events.PlayerDamageReinforcementEvent;
import com.untamedears.rourke750.ExpensiveBeacons.DataBase.Info;

public class BeaconListener implements Listener {
	private MultiBlockStructure multi;
	private StoredValues sv;
	private ExpensiveBeaconsPlugin plugin;

	public BeaconListener(MultiBlockStructure multi, StoredValues store,
			ExpensiveBeaconsPlugin plugin) {
		this.multi = multi;
		sv = store;
		this.plugin = plugin;
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onCitadelDamageEvent(PlayerDamageReinforcementEvent event) {
		// Since Blood crew op must cancel citadel event // ignore all this
		// <-------------------------
		Info info = sv.getBeaconInfo(event.getBlock().getLocation());
		if (info != null)
			event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onPistonRetractEvent(BlockPistonRetractEvent event) {
		if (sv.isInDatabase(event.getRetractLocation()) == null)
			return;
		event.setCancelled(true);
	}
	
	@EventHandler(priority = EventPriority.HIGH)
	public void blockBlowupEvent(EntityExplodeEvent event){
		Iterator<Block> iterator = event.blockList().iterator();
		while (iterator.hasNext()) {
            Block block = iterator.next();
            Info info = sv.isInDatabase(block.getLocation());
    		if (info != null)
    			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onPistonExtendEvent(BlockPistonExtendEvent event) {
		for (Block block : event.getBlocks()) {
			if (sv.isInDatabase(block.getLocation()) == null)
				continue;
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onBlockPlace(BlockPlaceEvent event) {
		if (event.getBlock().getType().equals(Material.BEACON)) {
			Location loc = event.getBlock().getLocation();
			String groupName = null;
			IReinforcement rein = Citadel.getReinforcementManager()
					.getReinforcement(loc);
			if (rein instanceof PlayerReinforcement) {
				groupName = ((PlayerReinforcement) rein).getOwner().getName();
			}
			List<Location> locs = sv.getAllBeaconLocations();
			for (Location l: locs){ // check if the beacon placed is close to another beacon
				System.out.println(l.toString());
				if (!findSide(loc) && loc.distance(l) <= 32){
					event.getPlayer().sendMessage(ChatColor.RED + "Beacon is too close to another Beacon!\n"
							+ "Either place next to a beacon or farther away from the other one.");
					event.setCancelled(true);
					return;
				}
			}
			if (groupName == null) {
				event.getPlayer().sendMessage("Reinforce the Beacon, if the beacon strcuture is set up."); // tells
																		// players
																		// to
																		// reinforce
																		// the
																		// beacon.
			}
			multi.checkBuild(loc);
		}
	}
	
	public boolean findSide(Location loc){
		BlockFace[] faces = {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};
		for (BlockFace face: faces)
			if (loc.getBlock().getRelative(face).getType().equals(Material.BEACON)) return true;
		return false;
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onBlockBreak(BlockBreakEvent event) {
		if (event.getBlock().getType().equals(Material.BEACON)) {
			Location loc = event.getBlock().getLocation();
			Info info = sv.getBeaconInfo(loc);
			if (info == null) {
				isBlock(event);
				return;
			}
			long time = info.time + 60000
					* plugin.getConfig().getLong("maturation_time");
			if (System.currentTimeMillis() <= time) {
				sv.removeBeaconInfo(info.beaconid);
				return;
			}
			if (info.hitPoints == 0) {
				if (!info.broken) {
					info.updateIfBroken(true);
					info.updateBrokenTime(System.currentTimeMillis());
				}
				if (info.broken) {
					long config = plugin.getConfig().getLong(
							"beacon_death_invulnerability");
					long brokentime = info.brokenTime + 60000 * config;
					event.getPlayer()
							.sendMessage(
									"Beacon is now broken, please wait "
											+ Math.abs(((System
													.currentTimeMillis() - info.brokenTime) / 60000 - config))
											+ " minutes until beacon is broken.");
					if (brokentime <= System.currentTimeMillis()) {
						sv.removeBeaconInfo(info.beaconid);
					} else
						event.setCancelled(true);
				}
			} else {
				event.setCancelled(true);
				info.updateHitPoints(info.hitPoints - 1);
			}
		}
		isBlock(event);
	}

	public void isBlock(BlockBreakEvent event) {
		if (event.getBlock().getType().equals(Material.DIAMOND_BLOCK)
				|| event.getBlock().getType().equals(Material.BEACON)) {
			Info info = sv.isInDatabase(event.getBlock().getLocation());
			if (info != null) {
				long time = info.time + 60000
						* plugin.getConfig().getLong("maturation_time");
				if (System.currentTimeMillis() <= time) {
					sv.removeBeaconInfo(info.beaconid);
					return;
				}
				if (info.hitPoints == 0) {
					if (!info.broken) {
						info.updateIfBroken(true);
						info.updateBrokenTime(System.currentTimeMillis());
					}
					if (info.broken) {
						long config = plugin.getConfig().getLong(
								"beacon_death_invulnerability");
						long brokentime = info.brokenTime + 60000 * config;
						event.getPlayer()
								.sendMessage(
										"Beacon is now broken, please wait "
												+ Math.abs(((System
														.currentTimeMillis() - info.brokenTime) / 60000 - config))
												+ " minutes until beacon is broken.");
						if (brokentime <= System.currentTimeMillis()) {
							sv.removeBeaconInfo(info.beaconid);
						} else
							event.setCancelled(true);
					}
				} else {
					event.setCancelled(true);
					if (info.type.equals("super")) // if they broke a block belonging to the super beacon
						event.getPlayer()
								.addPotionEffect(
										new PotionEffect(
												PotionEffectType.SLOW_DIGGING, // mining fatigue
												200, // lasts 10 seconds
												3)); // gives mining fatigue 4
					info.updateHitPoints(info.hitPoints - 1);
				}
				return;
			}
		}
	}
	
	@EventHandler(priority = EventPriority.HIGH)
	public void acidBlockCommand(AcidBlockDestroyEvent event){
		Block block = event.getAcidBlock().getRelative(BlockFace.UP);
		if (sv.isInDatabase(block.getLocation()) == null)
			return;
		event.setCancelled(true);
		event.setReasonForCancel(ChatColor.RED + "Cannot acid block a Beacon Structrue.");
	}

}
