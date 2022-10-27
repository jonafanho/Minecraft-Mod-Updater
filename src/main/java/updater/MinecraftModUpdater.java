package updater;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.commons.io.FileUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

public class MinecraftModUpdater implements Constants {

	private static final int MAX_RETRIES = 5;

	public static void main(String[] args) {
		if (args.length < 1) {
			return;
		}

		final String rootDirectory = args[0].replace("\\", "/");
		final String feriumConfigFile = rootDirectory + "/config.json";
		try {
			Files.deleteIfExists(Paths.get(feriumConfigFile));
		} catch (Exception e) {
			e.printStackTrace();
		}

		final PackConfig packConfig = new PackConfig(rootDirectory);
		if (packConfig.minecraftVersion.isEmpty() || packConfig.loader == null) {
			System.out.println("Minecraft version check failed");
			return;
		}

		final Set<String> modIdsToAdd = new HashSet<>();
		final Set<String> modFilesToCopy = new HashSet<>();
		Utilities.readFileJson(Paths.get(rootDirectory, "minecraft-mod-updater.json"), jsonElement -> {
			jsonElement.getAsJsonObject().getAsJsonArray("synced").forEach(syncedElement -> Utilities.openConnectionSafeJson(syncedElement.getAsString(), packElement -> downloadMods(packElement.getAsJsonArray(), rootDirectory, modIdsToAdd, modFilesToCopy)));
			downloadMods(jsonElement.getAsJsonObject().getAsJsonArray("local"), rootDirectory, modIdsToAdd, modFilesToCopy);
		});

		if (!modIdsToAdd.isEmpty()) {
			final String feriumFile = getFerium(rootDirectory);
			if (feriumFile == null) {
				System.out.println("Failed to run Ferium");
			} else {
				Utilities.runCommand(
						feriumFile, "--config-file", feriumConfigFile,
						"profile", "create",
						"--game-version", packConfig.minecraftVersion,
						"--mod-loader", packConfig.loader.loader,
						"--name", "test",
						"--output-dir", rootDirectory + "/.minecraft/mods"
				);
				modIdsToAdd.forEach(modId -> Utilities.runCommand(feriumFile, "--config-file", feriumConfigFile, "add", modId));
				Utilities.runCommand(feriumFile, "--config-file", feriumConfigFile, "upgrade");
			}
		}

		modFilesToCopy.forEach(modFile -> {
			try {
				Files.copy(Paths.get(rootDirectory, "temp", modFile), Paths.get(rootDirectory, ".minecraft", "mods", modFile));
				System.out.println("Copied " + modFile);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	private static String getFerium(String rootDirectory) {
		for (final String file : FILES) {
			try {
				String testFile = String.format("%s/%s/ferium", rootDirectory, file);
				if (Utilities.runCommand(testFile, "--version")) {
					return testFile;
				}
			} catch (Exception ignored) {
			}
		}

		return null;
	}

	private static void downloadMods(JsonArray jsonArray, String rootDirectory, Set<String> modIdsToAdd, Set<String> modFilesToCopy) {
		final Path tempDirectory = Paths.get(rootDirectory, "temp");

		jsonArray.forEach(modElement -> {
			try {
				final JsonObject modObject = modElement.getAsJsonObject();
				final String id = modObject.get("id").getAsString();

				if (modObject.has("url") && modObject.has("sha1")) {
					final String fileName = id.replace(".jar", "") + ".jar";
					final Path modPathTemp = tempDirectory.resolve(fileName);
					final String sha1 = modObject.get("sha1").getAsString();

					for (int i = 0; i <= MAX_RETRIES; i++) {
						if (sha1.equals(Utilities.getHash(modPathTemp))) {
							modFilesToCopy.add(fileName);
							break;
						} else {
							System.out.println("Downloading " + fileName + (i > 0 ? String.format(" (%s/%s)", i + 1, MAX_RETRIES) : ""));
							Utilities.openConnectionSafe(modObject.get("url").getAsString(), inputStream -> {
								try {
									FileUtils.copyInputStreamToFile(inputStream, modPathTemp.toFile());
								} catch (Exception e) {
									e.printStackTrace();
								}
							}, "User-Agent", "Mozilla/5.0");
						}
					}
				} else {
					modIdsToAdd.add(id);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}
}
