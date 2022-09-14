package updater;

import com.jonafanho.apitools.ModLoader;
import com.jonafanho.apitools.NetworkUtils;

import java.nio.file.Path;
import java.util.List;

public class Updater {

	public static final String MOD_ID = "updater";
	public static final String MODS_DIRECTORY = "mods";
	public static final String MODS_LOCAL_DIRECTORY = "mods-local";
	public static final String MODS_TEMP_DIRECTORY = "mods-temp";
	private static final String DEFAULT_CONFIG = "{}";

	public static void init() {

	}

	public static void init(List<Path> classPath, String[] launchArguments, String minecraftVersion, ModLoader modLoader, Path gameDirectory) {
		final Downloader downloader = new Downloader(minecraftVersion, modLoader, gameDirectory);
		Config.loadConfig(gameDirectory);
		Config.forEachServerUrl(serverUrl -> {
			System.out.println("Reading mods from " + serverUrl);
			NetworkUtils.openConnectionSafeJson(serverUrl, jsonElement -> {
				try {
					Config.getModObjects(jsonElement.getAsJsonArray()).forEach(modObject -> modObject.download(downloader));
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
		});
		Config.forEachModObject(modObject -> modObject.download(downloader));

		if (downloader.cleanAndCheckUpdate()) {
			Launcher.launch(classPath, launchArguments);
		} else {
			System.out.println("No mod updates");
		}
	}
}
