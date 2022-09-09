package updater;

import com.google.gson.*;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

public class Updater {

	public static final String MOD_ID = "updater";
	private static final String DEFAULT_CONFIG;

	static {
		final JsonObject mtrObject = new JsonObject();
		mtrObject.addProperty("id", "mtr");
		mtrObject.addProperty("source", "latest");

		final JsonObject luObject = new JsonObject();
		luObject.addProperty("id", "london-underground-addon");
		luObject.addProperty("source", "latest");

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
			final JsonObject configObject = new JsonParser().parse(FileUtils.readFileToString(configFile, StandardCharsets.UTF_8)).getAsJsonObject();

			if (configObject.has("synced")) {
				configObject.getAsJsonArray("synced").forEach(serverElement -> {
					final String url = serverElement.getAsString();
					System.out.println("Reading mods from " + url);
					readConnectionSafeJson(url, jsonElement -> {
						try {
							readConfig(jsonElement.getAsJsonArray(), downloader);
						} catch (Exception e) {
							e.printStackTrace();
						}
					});
				});
			}

			if (configObject.has("local")) {
				readConfig(configObject.getAsJsonArray("local"), downloader);
			}
		} catch (Exception ignored) {
			try {
				System.out.println("Writing default config");
				FileUtils.writeStringToFile(configFile, DEFAULT_CONFIG, StandardCharsets.UTF_8);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		if (downloader.cleanAndCheckUpdate()) {
			Launcher.launch(classPath, launchArguments);
		} else {
			System.out.println("No mod updates");
		}
	}

	public static void readConnectionSafe(String url, Consumer<InputStream> callback, String... requestProperties) {
		try {
			final URLConnection urlConnection = new URL(url).openConnection();

			for (int i = 0; i < requestProperties.length / 2; i++) {
				urlConnection.setRequestProperty(requestProperties[2 * i], requestProperties[2 * i + 1]);
			}

			try (final InputStream inputStream = urlConnection.getInputStream()) {
				callback.accept(inputStream);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void readConnectionSafeJson(String url, Consumer<JsonElement> callback, String... requestProperties) {
		readConnectionSafe(url, inputStream -> {
			try (final InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
				callback.accept(new JsonParser().parse(inputStreamReader));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}, requestProperties);
	}

	private static void readConfig(JsonArray modArray, Downloader downloader) {
		modArray.forEach(modElement -> {
			try {
				final JsonObject modObject = modElement.getAsJsonObject();
				final String modId = modObject.get("id").getAsString();

				if (modObject.has("source")) {
					final String source = modObject.get("source").getAsString().toLowerCase();
					if (source.equals("curseforge")) {
						downloader.getCurseForgeMod(modId);
					} else if (source.equals("modrinth")) {
						downloader.getModrinthMod(modId);
					} else {
						System.out.println("Skipping mod " + modId + ", unknown source \"" + source + "\"");
					}
				} else if (modObject.has("url") && modObject.has("sha1")) {
					final long millis = System.currentTimeMillis();
					if ((!modObject.has("before") || millis < modObject.get("before").getAsLong()) && (!modObject.has("after") || millis >= modObject.get("after").getAsLong())) {
						downloader.getMod(modId, modObject.get("url").getAsString(), modObject.get("sha1").getAsString());
					}
				} else {
					System.out.println("Skipping mod " + modId + ", either \"source\" or \"url\" and \"sha1\" must be defined");
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}
}
