package com.untamedears.rourke750.ExpensiveBeacons;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import com.untamedears.rourke750.ExpensiveBeacons.BeaconTypes.BeaconManager;
import com.untamedears.rourke750.ExpensiveBeacons.BeaconTypes.HasteBeacon;
import com.untamedears.rourke750.ExpensiveBeacons.BeaconTypes.MegaBeacon;
import com.untamedears.rourke750.ExpensiveBeacons.BeaconTypes.RegenBeacon;
import com.untamedears.rourke750.ExpensiveBeacons.BeaconTypes.SpeedBeacon;
import com.untamedears.rourke750.ExpensiveBeacons.BeaconTypes.StrengthBeacon;
import com.untamedears.rourke750.ExpensiveBeacons.DataBase.BeaconStorage;
import com.untamedears.rourke750.ExpensiveBeacons.DataBase.DataBase;
import com.untamedears.rourke750.ExpensiveBeacons.DataBase.Info;

public class ExpensiveBeaconsPlugin extends JavaPlugin {
	public static ExpensiveBeaconsPlugin instance;
	private BeaconManager bm;
	private StrengthBeacon strb;
	private SpeedBeacon sb;
	private BeaconListener ls;
	private StoredValues sv;
	public BufferedWriter writer;
	private File file;
	private Logger logger;
	private StaticBeaconStructure sbs;
	private MultiBlockStructure ms;
	private FileConfiguration config_;
	
	public StaticBeaconMeta meta;
	
	public void onLoad() {
		instance = this;
		logger = this.getLogger();
	}

	public void onEnable() {
		ConfigManager cm= new ConfigManager(this);
		cm.initconfig(getConfig());
		FileConfiguration con = getConfig();
		config_=con;
		StrengthBeacon strb = new StrengthBeacon();
		SpeedBeacon sb = new SpeedBeacon();
		RegenBeacon rb = new RegenBeacon();
		HasteBeacon hb= new HasteBeacon();
		MegaBeacon mb= new MegaBeacon();
		BeaconManager bm = new BeaconManager(sb, strb, rb, hb, mb);
		String dir = this.getDataFolder() + File.separator + "Expensive Beacon Types" + File.separator;
		new File(dir).mkdirs();
		String type=null;
		sbs= new StaticBeaconStructure(this);
		meta= new StaticBeaconMeta();
		String name[] = {												// Keeps file names within an array to remove ~21 if statemenets ~iebagi
							"speed_structure1.txt", "speed_structure2.txt",
							"speed_structure3.txt", "speed_structure4.txt",
							"speed_structure5.txt", "strength_structure1.txt",
							"strength_structure2.txt", "strength_structure3.txt",
							"strength_structure4.txt", "strength_structure5.txt",
							"regen_structure1.txt", "regen_structure2.txt",
							"regen_structure3.txt", "regen_structure4.txt",
							"regen_structure5.txt", 
							 "haste_structure1.txt",
							"haste_structure2.txt", "haste_structure3.txt",
							"haste_structure4.txt", "haste_structure5.txt",
							"super_structure.txt"
		};
		
		for (int i=0; i<meta.getMaxSize(); i++){
			
			type = name[i];
			
			try {
				File beacon = new File(dir + type);
				if (beacon.exists()) {
					continue;
				}
				else {
					Logger.getLogger(ExpensiveBeaconsPlugin.class.getName()).log(Level.INFO, "Making a new file", "");
					PrintWriter fstream = new PrintWriter(dir + type);
					writer = new BufferedWriter(fstream);
					bm.CreateBeaconFiles(beacon, type);
				}

			}
			catch (IOException e) {
				e.printStackTrace();
			}
			
		}
		
		for(int i=0; i<meta.getMaxSize(); i++){		// Sends loadFromFile to StaticBeaconMeta class. ~iebagi
			meta.overStruct(i, StaticBeaconStructure.loadFromFile(new File(dir, name[i])));	
		}
		Effects ef = new Effects(config_);
		BeaconStorage bc = new BeaconStorage(con, ef);
		sv = new StoredValues(bc, this, ef);
		sv.addStoredInfo(); // unloads all data beacon information in the database
		ms = new MultiBlockStructure(this, ls, sv, meta);
		ls = new BeaconListener(ms, sv, this);
		enableListener();
				
		PlayerHelper ph = new PlayerHelper(meta, sv, config_);
		CommandManager com = new CommandManager(ph);
		for (String command : getDescription().getCommands().keySet()) {
            getCommand(command).setExecutor(com);
        }
		sanityCheck(bc);
	}

	public void onDisable() {
		sv.saveMethod();
	}

	private void enableListener() {
		getServer().getPluginManager().registerEvents(ls, this);
	}
	
	private void sanityCheck(BeaconStorage bs){
		for (int id: bs.getAllBeaconIds()){
			Info info = bs.getBeaconInfo(id);
			ms.checkBuild(info.loc);
		}
	}
}
