package com.untamedears.rourke750.ExpensiveBeacons;




import java.util.logging.Logger;

import net.minecraft.server.v1_5_R3.Block;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import com.untamedears.citadel.Citadel;
import com.untamedears.citadel.entity.Faction;
import com.untamedears.citadel.entity.IReinforcement;
import com.untamedears.citadel.entity.PlayerReinforcement;

public class onListener implements Listener{
	private multiblockstructure multi;
	private StoredValues sv;
	public onListener(multiblockstructure plugin, StoredValues store){
		multi=plugin;
		sv= store;
	}
	@EventHandler(priority = EventPriority.HIGH)
	public void onBlockPlace(BlockPlaceEvent event){

		Logger logger = Logger.getLogger(ExpensiveBeaconsplugin.class.getName());
		logger.info("Listener is enabled.");
		if(event.getBlock().getType().equals(Material.BEACON)) {
			Location loc= event.getBlock().getLocation();
			String groupName = null;
			IReinforcement rein = Citadel.getReinforcementManager().getReinforcement(loc);
			if (rein instanceof PlayerReinforcement) {
			  groupName = ((PlayerReinforcement)rein).getOwner().getName();
			}
			Faction group =Citadel.getGroupManager().getGroup(groupName);
			logger.info("Setspeedeffects has run");
			if (groupName == null) {
				
				//players are told to reinforce after they place.
				event.getPlayer().sendMessage("Reinforce the Beacon.");
			}
			multi.checkBuild(loc);
		}
	}
	@EventHandler(priority= EventPriority.HIGH)
	public void onBlockBreak(BlockBreakEvent event){
		if(event.getBlock().getType().equals(Material.BEACON)) {
			Location loc= event.getBlock().getLocation();
			if(sv.getTier(loc)!=null){
				sv.removeTier(loc);
				sv.removeType(loc);
			}
		}
	}
}
