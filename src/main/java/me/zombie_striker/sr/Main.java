package me.zombie_striker.sr;

import de.kastenklicker.ssb.Commands;
import de.kastenklicker.ssb.FTPUtils;
import fr.xephi.authme.output.Log4JFilter;
import org.apache.logging.log4j.LogManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Main extends JavaPlugin {

	private static List<String> exceptions = new ArrayList<>();
	protected static String prefix = "&6[&3SecureServerBackup&6]&8";
	BukkitTask br = null;
	private boolean saveTheConfig = false;
	private long lastSave = 0;
	private long timedist = 0;
	private File master = null;
	private File backups = null;
	private boolean saveServerJar = false;
	private boolean savePluginJars = false;
	private boolean currentlySaving = false;
	private boolean useFTPS = false;
	private boolean useSFTP = false;
	private String naming_format = "Backup-%date%";
	private final SimpleDateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
	private long maxSaveSize = -1;
	private int maxSaveFiles = 1000;
	private boolean deleteZipOnFail = false;
	private boolean deleteZipOnFTP = false;

	private int hourToSaveAt = -1;

	private String separator = File.separator;

	private int compression = Deflater.BEST_COMPRESSION;

	//Login information
	private String user, password1, password2;

	public void setUser(String user) {
		this.user = user;
	}

	public void setPassword1(String password1) {
		this.password1 = password1;
	}

	public void setPassword2(String password2) {
		this.password2 = password2;
	}

	//Upload via FTPS/SFTP
	private ArrayList<String[]> sftpList = new ArrayList<>();

	public void setSftpList(ArrayList<String[]> sftpList) {
		this.sftpList = sftpList;
	}

	private ArrayList<String[]> ftpsList = new ArrayList<>();

	public void setFtpsList(ArrayList<String[]> ftpsServer) {
		this.ftpsList = ftpsServer;
	}


	private static boolean isExempt(String path) {
		path = path.toLowerCase().trim();
		for (String s : exceptions)
			if (path.endsWith(s.toLowerCase().trim()))
				return false;
		return true;
	}

	public static String humanReadableByteCount(long bytes, boolean si) {
		int unit = si ? 1000 : 1024;
		if (bytes < unit)
			return bytes + " B";
		int exp = (int) (Math.log(bytes) / Math.log(unit));
		String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
		return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
	}

	public static long folderSize(File directory) {
		long length = 0;
		if(directory==null)return -1;

		for (File file : Objects.requireNonNull(directory.listFiles())) {
			if (file.isFile())
				length += file.length();
			else
				length += folderSize(file);
		}
		return length;
	}

	public static File firstFileModified(File dir) {
		File[] files = dir.listFiles(File::isFile);
		long lastMod = Long.MAX_VALUE;
		File choice = null;
		assert files != null;
		for (File file : files) {
			if (file.lastModified() < lastMod) {
				choice = file;
				lastMod = file.lastModified();
			}
		}
		return choice;
	}

	public File getMasterFolder() {
		return master;
	}

	public File getBackupFolder() {
		return backups;
	}

	public long a(String path, long def) {
		if (getConfig().contains(path))
			return getConfig().getLong(path);
		saveTheConfig = true;
		getConfig().set(path, def);
		return def;
	}
	public Object a(String path, Object def) {
		if (getConfig().contains(path))
			return getConfig().get(path);
		saveTheConfig = true;
		getConfig().set(path, def);
		return def;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onEnable() {

		//Filter Logs
		try {
			Class.forName("org.apache.logging.log4j.core.filter.AbstractFilter");
			org.apache.logging.log4j.core.Logger logger4j;
			logger4j = (org.apache.logging.log4j.core.Logger) LogManager.getRootLogger();
			logger4j.addFilter(new Log4JFilter());

		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		master = getDataFolder().getAbsoluteFile().getParentFile().getParentFile();
		String path = ((String) a("getBackupFileDirectory", ""));
		backups = new File((path.isEmpty() ? master.getPath() : path) +  File.separator+"backups"+ File.separator);
		backups.mkdirs();
		saveServerJar = (boolean) a("saveServerJar", false);
		savePluginJars = (boolean) a("savePluginJars", false);

		timedist = toTime((String) a("AutosaveDelay", "1D,0H"));
		lastSave = a("LastAutosave", 0L);

		boolean automate = (boolean) a("enableautoSaving", true);

		naming_format = (String) a("FileNameFormat", naming_format);

		String unPrefix = (String) a("prefix", "&6[&3SecureServerBackup&6]&8");
		prefix = ChatColor.translateAlternateColorCodes('&', unPrefix);
		useFTPS = (boolean) a("EnableFTPS", false);
		useSFTP = (boolean) a("EnableSFTP", false);

		compression = (int) a("CompressionLevel_Max_9", compression);

		hourToSaveAt = (int) a("AutoBackup-HourToBackup", hourToSaveAt);

		if (!getConfig().contains("exceptions")) {
			exceptions.add("logs");
			exceptions.add("crash-reports");
			exceptions.add("backups");
			exceptions.add("dynmap");
			exceptions.add(".lock");
			exceptions.add("pixelprinter");
			exceptions.add("server.mv.db");
			exceptions.add("server.trace.db");
		}
		exceptions = (List<String>) a("exceptions", exceptions);

		maxSaveSize = toByteSize((String) a("MaxSaveSize", "10G"));
		maxSaveFiles = (int) a("MaxFileSaved", 1000);

		deleteZipOnFTP = (boolean) a("DeleteZipOnFTPTransfer", false);
		deleteZipOnFail = (boolean) a("DeleteZipIfFailed", false);
		separator = (String) a("FolderSeparator", separator);
		if (saveTheConfig)
			saveConfig();
		if (automate) {
			final JavaPlugin thi = this;
			br = new BukkitRunnable() {
				@Override
				public void run() {
					Calendar calendar = GregorianCalendar.getInstance(); // creates a new calendar instance
					calendar.setTime(new Date());   // assigns calendar to given date
					int hour = calendar.get(Calendar.HOUR_OF_DAY);

					if (System.currentTimeMillis() - lastSave >= timedist && (hourToSaveAt==-1 || hourToSaveAt == hour)) {
						new BukkitRunnable() {
							@Override
							public void run() {
								getConfig().set("LastAutosave", lastSave = (System.currentTimeMillis()-5000));
								save(Bukkit.getConsoleSender());
								saveConfig();
							}
						}.runTaskLater(thi, 0);
					}
				}
			}.runTaskTimerAsynchronously(this, 20, 20*60);
		}

	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

		if (args.length == 1) {
			List<String> list = new ArrayList<>();
			String[] commands = new String[]{"addServer","disableAutoSaver", "enableAutoSaver", "login", "removeServer", "restore", "save", "setLogin", "stop", "toggleOptions"};
			for (String f : commands) {
				if (f.toLowerCase().startsWith(args[0].toLowerCase()))
					list.add(f);
			}
			return list;

		}

		if (args.length > 1) {
			if (args[0].equalsIgnoreCase("restore")) {
				List<String> list = new ArrayList<>();
				for (File f : Objects.requireNonNull(getBackupFolder().listFiles())) {
					if (f.getName().toLowerCase().startsWith(args[1].toLowerCase()))
						list.add(f.getName());
				}
				return list;
			}
		}
		return super.onTabComplete(sender, command, alias, args);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!sender.hasPermission("secureserverbackup.command")) {
			sender.sendMessage(prefix + ChatColor.RED + " You do not have permission to use this command.");
			return true;
		}
		if (args.length == 0) {
			sender.sendMessage(ChatColor.GOLD + "---===+Secure Server Backup+===---");
			sender.sendMessage("/ssb save : Saves the server");
			sender.sendMessage("/ssb stop : Stops creating a backup of the server");
			sender.sendMessage("/ssb enableAutoSaver [1H,6H,1D,7D] : Configure how long it takes to autosave");
			sender.sendMessage("/ssb disableAutoSaver : Disables the autosaver");
			sender.sendMessage("/ssb toggleOptions : TBD");
			sender.sendMessage("/ssb setLogin <user> <password1> <password2> : Set login information");
			sender.sendMessage("/ssb login <user> <password1> <password2> : Log into Database");
			sender.sendMessage("/ssb addServer <host> <port> <remote user> <remote password> <path> <sftp>: Add a new Server");
			sender.sendMessage("/ssb removeServer <host> : Remove Server from Database");
			return true;
		}

		if (args[0].equalsIgnoreCase("stop")) {
			if (!sender.hasPermission("secureserverbackup.save")) {
				sender.sendMessage(prefix + ChatColor.RED + " You do not have permission to use this command.");
				return true;
			}
			if (currentlySaving) {
				currentlySaving=false;
				return true;
			}
			sender.sendMessage(prefix + " The server is not currently being saved.");
			return true;
		}
		if (args[0].equalsIgnoreCase("save")) {
			if (!sender.hasPermission("secureserverbackup.save")) {
				sender.sendMessage(prefix + ChatColor.RED + " You do not have permission to use this command.");
				return true;
			}
			if (currentlySaving) {
				sender.sendMessage(prefix + " The server is currently being saved. Please wait.");
				return true;
			}
			save(sender);
			return true;
		}
		if (args[0].equalsIgnoreCase("disableAutoSaver")) {
			if (!sender.hasPermission("secureserverbackup.save")) {
				sender.sendMessage(prefix + ChatColor.RED + " You do not have permission to use this command.");
				return true;
			}
			if (br != null)
				br.cancel();
			br = null;
			getConfig().set("enableautoSaving", false);
			saveConfig();
			sender.sendMessage(prefix + " Canceled delay.");
		}
		if (args[0].equalsIgnoreCase("enableAutoSaver")) {
			if (!sender.hasPermission("secureserverbackup.save")) {
				sender.sendMessage(prefix + ChatColor.RED + " You do not have permission to use this command.");
				return true;
			}
			if (args.length == 1) {
				sender.sendMessage(prefix + " Please select a delay [E.G. 0.5H, 6H, 1D, 7D...]");
				return true;
			}
			String delay = args[1];
			getConfig().set("AutosaveDelay", delay);
			getConfig().set("enableautoSaving", true);
			saveConfig();
			if (br != null)
				br.cancel();
			br = null;
			br = new BukkitRunnable() {
				@Override
				public void run() {
					if (System.currentTimeMillis() - lastSave > timedist) {
						save(Bukkit.getConsoleSender());
						getConfig().set("LastAutosave", lastSave = System.currentTimeMillis());
						saveConfig();
					}
				}
			}.runTaskTimerAsynchronously(this, 20, 20 * 60 * 30);

			sender.sendMessage(prefix + " Set the delay to \"" + delay + "\".");
		}
		if (args[0].equalsIgnoreCase("toggleOptions")) {
			if (!sender.hasPermission("secureserverbackup.save")) {
				sender.sendMessage(prefix + ChatColor.RED + " You do not have permission to use this command.");
				return true;
			}
			sender.sendMessage(prefix + " Coming soon !");
			return true;
		}

		//KastenKlickers commands
		return new Commands().onCommand(sender, args, prefix, this, separator, this, user, password1, password2);
	}

	public void save(CommandSender sender) {
		currentlySaving = true;
		sender.sendMessage(prefix + " Starting to save directory. Please wait.");
		List<World> autosave = new ArrayList<>();
		for (World loaded : Bukkit.getWorlds()) {
			try {
				loaded.save();
				if (loaded.isAutoSave()) {
					autosave.add(loaded);
					loaded.setAutoSave(false);
				}

			} catch (Exception ignored) {
			}
		}
		new BukkitRunnable() {
			@Override
			public void run() {
				try {
					try {
						if(Objects.requireNonNull(backups.listFiles()).length > maxSaveFiles){
							for(int i = 0; i < Objects.requireNonNull(backups.listFiles()).length-maxSaveFiles; i++){
								File oldestBack = firstFileModified(backups);
								sender.sendMessage(prefix + ChatColor.RED + oldestBack.getName()
										+ ": File goes over max amount of files that can be saved.");
								oldestBack.delete();
							}
						}
						for (int j = 0; j < Math.min(maxSaveFiles, Objects.requireNonNull(backups.listFiles()).length - 1); j++) {
							if (folderSize(backups) >= maxSaveSize) {
								File oldestBack = firstFileModified(backups);
								sender.sendMessage(prefix + ChatColor.RED + oldestBack.getName()
										+ ": The current save goes over the max savesize, and so the oldest file has been deleted. If you wish to save older backups, copy them to another location.");
								oldestBack.delete();
							} else {
								break;
							}
						}
					} catch (Error | Exception ignored) {
					}
					final long time = lastSave = System.currentTimeMillis();
					Date d = new Date(lastSave);
					File zipFile = new File(getBackupFolder(),
							naming_format.replaceAll("%date%", dateformat.format(d)) + ".zip");
					if (!zipFile.exists()) {
						zipFile.getParentFile().mkdirs();
						zipFile = new File(getBackupFolder(),
								naming_format.replaceAll("%date%", dateformat.format(d)) + ".zip");
						zipFile.createNewFile();
					}
					zipFolder(getMasterFolder().getPath(), zipFile.getPath());

					long timeDif = (System.currentTimeMillis() - time) / 1000;
					String timeDifS = (((int) (timeDif / 60)) + "M, " + (timeDif % 60) + "S");

					if(!currentlySaving){
						for (World world : autosave)
							world.setAutoSave(true);
						sender.sendMessage(prefix + " Backup canceled.");
						cancel();
						return;
					}

					sender.sendMessage(prefix + " Done! Backup took:" + timeDifS);
					File tempBackupCheck = new File(getMasterFolder(), "backups");
					sender.sendMessage(prefix + " Compressed server with size of "
							+ (humanReadableByteCount(folderSize(getMasterFolder())
							- (tempBackupCheck.exists() ? folderSize(tempBackupCheck) : 0), false))
							+ " to " + humanReadableByteCount(zipFile.length(), false));
					currentlySaving = false;
					for (World world : autosave)
						world.setAutoSave(true);
					if (useSFTP) {
						if (sftpList.isEmpty()) sender.sendMessage(prefix + ChatColor.RED + " Couldn't get servers from Database. Are you logged in?");
						else {

							int i = 1;
							int listSize = sftpList.size();
							boolean exception = false;

							for (String[] server : sftpList) {
								sender.sendMessage(prefix + " Starting SFTP Transfer(" + i + "/" + listSize + ").");
								if (new FTPUtils(prefix, sender).uploadSFTP(server, zipFile)) exception = true;
								i++;
							}

							if (exception) {
								sender.sendMessage(prefix + ChatColor.RED + " There was an exception while uploading the backup using SFTP.");
								if (deleteZipOnFail) zipFile.delete();
							}
							else {
								sender.sendMessage(prefix + ChatColor.GREEN + " Uploaded backup successfully.");
								if (deleteZipOnFTP) zipFile.delete();
							}

						}
					}
					if (useFTPS) {
						if (ftpsList.isEmpty()) sender.sendMessage(prefix + ChatColor.RED + " Couldn't get servers from Database. Are you logged in?");
						else {

							int i = 1;
							int listSize = ftpsList.size();
							boolean exception = false;

							for (String[] server : ftpsList) {
								sender.sendMessage(prefix + " Starting FTPS Transfer(" + i + "/" + listSize + ").");
								if (new FTPUtils(prefix, sender).uploadFTPS(server, zipFile)) exception = true;
								i++;
							}

							if (exception) {
								sender.sendMessage(prefix + ChatColor.RED + " There was an exception while uploading the backup using FTPS.");
								if (deleteZipOnFail) zipFile.delete();
							}
							else {
								sender.sendMessage(prefix + ChatColor.GREEN + " Uploaded backup successfully.");
								if (deleteZipOnFTP) zipFile.delete();
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}.runTaskAsynchronously(this);
	}

	public long toTime(String time) {
		long militime = 0;
		for(String split : time.split(",")) {
			split = split.trim();
			long k = 1;
			if (split.toUpperCase().endsWith("H")) {
				k *= 60 * 60;
			} else if (split.toUpperCase().endsWith("D")) {
				k *= 60 * 60 * 24;
			} else {
				k *= 60 * 60 * 24;
			}
			double j = Double.parseDouble(split.substring(0, split.length() - 1));
			militime += (j*k);
		}
		militime *= 1000;
		return militime;
	}

	public void zipFolder(String srcFolder, String destZipFile) throws Exception {
		ZipOutputStream zip;
		FileOutputStream fileWriter;

		fileWriter = new FileOutputStream(destZipFile);
		zip = new ZipOutputStream(fileWriter);


		zip.setLevel(compression);

		addFolderToZip("", srcFolder, zip);
		zip.flush();
		zip.close();
	}

	private void addFileToZip(String path, String srcFile, ZipOutputStream zip) {
		try {
			File folder = new File(srcFile);
			if (isExempt(srcFile)) {

				if(!currentlySaving)
					return;
				// this.savedBytes += folder.length();
				if (folder.isDirectory()) {
					addFolderToZip(path, srcFile, zip);
				} else {
					if (folder.getName().endsWith("jar")) {
						if (path.contains("plugins") && (!savePluginJars) || (!path.contains("plugins") && (!saveServerJar))) {
							return;
						}
					}

					byte[] buf = new byte['?'];

					FileInputStream in = new FileInputStream(srcFile);
					zip.putNextEntry(new ZipEntry(path + separator + folder.getName()));
					int len;
					while ((len = in.read(buf)) > 0) {
						zip.write(buf, 0, len);
					}
					in.close();
				}
			}
		}catch (FileNotFoundException e4){
			Bukkit.getConsoleSender().sendMessage(prefix + " FAILED TO ZIP FILE: " + srcFile+" Reason: "+e4.getClass().getName());
			e4.printStackTrace();
		}catch (IOException e5){
			if(!srcFile.endsWith(".db")) {
				Bukkit.getConsoleSender().sendMessage(prefix + " FAILED TO ZIP FILE: " + srcFile + " Reason: " + e5.getClass().getName());
				e5.printStackTrace();
			}else{
				Bukkit.getConsoleSender().sendMessage(prefix + " Skipping file " + srcFile +" due to another process that has locked a portion of the file");
			}

		}
	}

	private void addFolderToZip(String path, String srcFolder, ZipOutputStream zip) {
		if ((!path.toLowerCase().contains("backups")) && (isExempt(path))) {
			try {
				File folder = new File(srcFolder);
				String[] arrayOfString;
				int j = (Objects.requireNonNull(arrayOfString = folder.list())).length;
				for (int i = 0; i < j; i++) {
					if(!currentlySaving)
						break;
					String fileName = arrayOfString[i];
					if (path.equals("")) {
						addFileToZip(folder.getName(), srcFolder + separator + fileName, zip);
					} else {
						addFileToZip(path + separator + folder.getName(), srcFolder +  separator + fileName, zip);
					}
				}
			} catch (Exception ignored) {
			}
		}
	}

	private long toByteSize(String s) {
		long k = Long.parseLong(s.substring(0, s.length() - 1));
		if (s.toUpperCase().endsWith("G")) {
			k *= 1000 * 1000 * 1000;
		} else if (s.toUpperCase().endsWith("M")) {
			k *= 1000 * 1000;
		} else if (s.toUpperCase().endsWith("K")) {
			k *= 1000;
		} else {
			k *= 10;
		}
		return k;
	}
}
