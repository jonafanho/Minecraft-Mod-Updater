package updater;

import com.jonafanho.apitools.ModLoader;
import com.jonafanho.apitools.NetworkUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;

public class Updater {

	public static final String MOD_ID = "updater";
	public static final String MODS_DIRECTORY = "mods";
	public static final String MODS_LOCAL_DIRECTORY = "mods-local";
	public static final String MODS_TEMP_DIRECTORY = "mods-temp";
	public static final String MODS_BACKUP_DIRECTORY = "mods-backup";
	public static final Logger LOGGER = LogManager.getLogger("Minecraft Mod Updater");

	public static void init() {

	}

	public static void init(Runnable launch, String minecraftVersion, ModLoader modLoader, Path gameDirectory) {
		final Downloader downloader = new Downloader(minecraftVersion, modLoader, gameDirectory);
		Config.loadConfig(gameDirectory);
		Config.forEachServerUrl(serverUrl -> {
			LOGGER.info("Reading mods from " + serverUrl);
			NetworkUtils.openConnectionSafeJson(serverUrl, jsonElement -> {
				try {
					Config.getModObjects(jsonElement.getAsJsonArray()).forEach(modObject -> modObject.download(downloader));
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
		});
		Config.forEachModObject(modObject -> modObject.download(downloader));

		final Path tempFile = gameDirectory.resolve(MODS_TEMP_DIRECTORY).resolve("temp.txt");
		final boolean skipLaunch = Files.exists(tempFile);
		try {
			Files.deleteIfExists(tempFile);
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (downloader.cleanAndCheckUpdate()) {
			if (skipLaunch) {
				LOGGER.info("Skipping Minecraft relaunch");
			} else {
				try {
					Files.createFile(tempFile);
				} catch (Exception e) {
					e.printStackTrace();
				}
				launch.run();
			}
		} else {
			LOGGER.info("No mod updates");
		}
	}
}
