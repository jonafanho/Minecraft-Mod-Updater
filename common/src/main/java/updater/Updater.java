package updater;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

public class Updater {

	public static final String MOD_ID = "updater";
	private static final String DEFAULT_CONFIG;

	static {
		final JsonObject mtrObject = new JsonObject();
		mtrObject.addProperty("id", "mtr");
		mtrObject.addProperty("source", "latest");

		final JsonObject luObject = new JsonObject();
		luObject.addProperty("id", "london-underground-addon");
		mtrObject.addProperty("source", "latest");

		final JsonObject modMenuObject = new JsonObject();
		modMenuObject.addProperty("id", "modmenu");
		modMenuObject.addProperty("source", "modrinth");

		final JsonArray localArray = new JsonArray();
		localArray.add(mtrObject);
		localArray.add(luObject);
		localArray.add(modMenuObject);

		final JsonObject configObject = new JsonObject();
		configObject.add("synced", new JsonArray());
		configObject.add("local", localArray);

		DEFAULT_CONFIG = new GsonBuilder().setPrettyPrinting().create().toJson(configObject);
	}

	public static void init() {

	}

	public static void init(List<Path> classPath, String[] launchArguments, String minecraftVersion, Downloader.ModLoader modLoader, Path gameDirectory) {
		final File configFile = gameDirectory.resolve("config").resolve("minecraft-mod-updater.json").toFile();
		final Downloader downloader = new Downloader(minecraftVersion, modLoader, gameDirectory);

		try {
			final JsonObject configObject = new JsonParser().parse(FileUtils.readFileToString(configFile, Charset.defaultCharset())).getAsJsonObject();

			configObject.getAsJsonArray("server").forEach(serverElement -> {
				final String url = serverElement.getAsString();
				System.out.println("Reading mods from " + url);
				try (InputStream inputStream = new URL(url).openStream()) {
					readConfig(new JsonParser().parse(new InputStreamReader(inputStream, StandardCharsets.UTF_8)).getAsJsonArray(), downloader);
				} catch (Exception e) {
					e.printStackTrace();
				}
			});

			readConfig(configObject.getAsJsonArray("local"), downloader);
		} catch (Exception ignored) {
			try {
				System.out.println("Writing default config");
				FileUtils.writeStringToFile(configFile, DEFAULT_CONFIG, Charset.defaultCharset());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		if (downloader.hasUpdate()) {
			Launcher.launch(classPath, launchArguments);
		} else {
			System.out.println("No mod updates");
		}
	}

	private static void readConfig(JsonArray modArray, Downloader downloader) {
		modArray.forEach(modElement -> {
			final JsonObject modObject = modElement.getAsJsonObject();
			final String modId = modObject.get("id").getAsString();
			final String source = modObject.get("source").getAsString().toLowerCase();
			if (source.equals("curseforge")) {
				downloader.getCurseForgeMod(modId);
			} else if (source.equals("modrinth")) {
				downloader.getModrinthMod(modId);
			}
		});
	}
}
