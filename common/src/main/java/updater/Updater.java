package updater;

import java.nio.file.Path;
import java.util.List;

public class Updater {

	public static final String MOD_ID = "updater";

	public static void init() {

	}

	public static void init(List<Path> classPath, String[] launchArguments, String minecraftVersion, Downloader.ModLoader modLoader, Path gameDirectory) {
		final Downloader downloader = new Downloader(minecraftVersion, modLoader, gameDirectory);

		if (downloader.hasUpdate()) {
			Launcher.launch(classPath, launchArguments);
		}
	}
}
