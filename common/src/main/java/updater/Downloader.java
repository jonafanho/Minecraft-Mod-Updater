package updater;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

public class Downloader {

	private boolean hasUpdate = false;
	private final String minecraftVersion;
	private final ModLoader modLoader;
	private final Path modsPath;
	private final Path modsPathTemp;
	private final Set<String> visitedMods = new HashSet<>();
	private final Set<File> modsToDelete = new HashSet<>();

	private static final int DOWNLOAD_ATTEMPTS = 5;
	private static final byte[] EMPTY_ZIP = {80, 75, 5, 6, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

	public Downloader(String minecraftVersion, ModLoader modLoader, Path gameDirectory) {
		this.minecraftVersion = minecraftVersion;
		this.modLoader = modLoader;
		modsPath = gameDirectory.resolve("mods");
		modsPathTemp = gameDirectory.resolve("mods-temp");

		FileUtils.listFiles(modsPath.toFile(), new String[]{"jar"}, false).forEach(file -> {
			if (!file.getName().startsWith("Mod-Updater-")) {
				modsToDelete.add(file);
			}
		});

		printCurseForgeKey();
	}

	public void getCurseForgeMod(String modId) {
		Updater.readConnectionSafeJson(String.format("https://api.curseforge.com/v1/mods/%s/files?gameVersion=%s&modLoaderType=%s", modId, minecraftVersion, modLoader.name), jsonElement -> downloadMod(
				modId,
				jsonElement.getAsJsonObject().getAsJsonArray("data"),
				fileObject -> fileObject.get("fileName").getAsString(),
				fileObject -> fileObject.getAsJsonArray("hashes").get(0).getAsJsonObject().get("value").getAsString(),
				fileObject -> {
					final int fileId = fileObject.get("id").getAsInt();
					String fileNameEncoded = fileObject.get("fileName").getAsString();
					try {
						fileNameEncoded = URLEncoder.encode(fileNameEncoded, StandardCharsets.UTF_8.name());
					} catch (Exception e) {
						e.printStackTrace();
					}
					return String.format("https://mediafiles.forgecdn.net/files/%s/%s/%s", fileId / 1000, fileId % 1000, fileNameEncoded);
				},
				fileObject -> fileObject.getAsJsonArray("dependencies").forEach(dependency -> {
					final JsonObject dependencyObject = dependency.getAsJsonObject();
					if (dependencyObject.get("relationType").getAsInt() == 3) {
						getCurseForgeMod(dependencyObject.get("modId").getAsString());
					}
				})
		), "x-api-key", Keys.CURSE_FORGE_KEY);
	}

	public void getModrinthMod(String modId) {
		Updater.readConnectionSafeJson(String.format("https://api.modrinth.com/v2/project/%s/version?game_versions=%%5B%%22%s%%22%%5D&loaders=%%5B%%22%s%%22%%5D", modId, minecraftVersion, modLoader.name), jsonElement -> downloadMod(
				modId,
				jsonElement.getAsJsonArray(),
				fileObject -> fileObject.getAsJsonArray("files").get(0).getAsJsonObject().get("filename").getAsString(),
				fileObject -> fileObject.getAsJsonArray("files").get(0).getAsJsonObject().getAsJsonObject("hashes").get("sha1").getAsString(),
				fileObject -> fileObject.getAsJsonArray("files").get(0).getAsJsonObject().get("url").getAsString(),
				fileObject -> fileObject.getAsJsonArray("dependencies").forEach(dependency -> {
					final JsonObject dependencyObject = dependency.getAsJsonObject();
					if (dependencyObject.get("dependency_type").getAsString().equals("required")) {
						final JsonElement dependencyElement = dependencyObject.get("project_id");
						if (!dependencyElement.isJsonNull()) {
							getModrinthMod(dependencyElement.getAsString());
						}
					}
				})
		));
	}

	public void getMod(String modId, String url, String hash) {
		final JsonArray tempArray = new JsonArray();
		tempArray.add(new JsonObject());
		final String newModId = modId.replace(".jar", "").replaceAll("[^\\w-_.]", "");
		downloadMod(newModId, tempArray, jsonObject -> newModId + ".jar", jsonObject -> hash, jsonObject -> url, jsonObject -> {
		});
	}

	public boolean cleanAndCheckUpdate() {
		if (!modsToDelete.isEmpty()) {
			hasUpdate = true;
		}

		modsToDelete.forEach(file -> {
			try {
				System.out.println("Deleting " + file.getName());
				specialCopy(null, file);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});

		return hasUpdate;
	}

	private void downloadMod(String modId, JsonArray filesArray, Function<JsonObject, String> getName, Function<JsonObject, String> getHash, Function<JsonObject, String> getUrl, Consumer<JsonObject> callback) {
		if (!visitedMods.contains(modId) && filesArray.size() > 0) {
			visitedMods.add(modId);

			try {
				final JsonObject fileObject = filesArray.get(0).getAsJsonObject();
				final String fileName = getName.apply(fileObject);
				final Path modPathTemp = modsPathTemp.resolve(fileName);

				if (!hashMatch(getHash.apply(fileObject), modPathTemp)) {
					for (int i = 1; i <= DOWNLOAD_ATTEMPTS; i++) {
						Updater.readConnectionSafe(getUrl.apply(fileObject), inputStream -> {
							try {
								FileUtils.copyInputStreamToFile(inputStream, modPathTemp.toFile());
							} catch (Exception e) {
								e.printStackTrace();
							}
						}, "User-Agent", "Mozilla/5.0");

						if (hashMatch(getHash.apply(fileObject), modPathTemp)) {
							System.out.println("Downloaded " + fileName);
							break;
						} else {
							System.out.println("Failed to download " + fileName + " (" + i + "/" + DOWNLOAD_ATTEMPTS + ")");
						}
					}
				}

				final Path modPath = modsPath.resolve(fileName);

				if (!hashMatch(getHash.apply(fileObject), modPath)) {
					hasUpdate = true;
					specialCopy(modPathTemp, modPath.toFile());
				}

				modsToDelete.remove(modPath.toFile());
				callback.accept(fileObject);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private static void specialCopy(Path source, File destination) {
		try {
			try (final InputStream inputStream = source == null ? new ByteArrayInputStream(EMPTY_ZIP) : Files.newInputStream(source)) {
				FileUtils.copyInputStreamToFile(inputStream, destination);
				if (source == null) {
					FileUtils.forceDeleteOnExit(destination);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static boolean hashMatch(String expectedHash, Path path) {
		if (Files.exists(path)) {
			try (final InputStream inputStream = Files.newInputStream(path)) {
				return DigestUtils.sha1Hex(inputStream).equals(expectedHash);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	@SuppressWarnings("all")
	private static void printCurseForgeKey() {
		if (Keys.CURSE_FORGE_KEY.length() > 8) {
			System.out.println("Using CurseForge API key: " + Keys.CURSE_FORGE_KEY.substring(0, 4) + "..." + Keys.CURSE_FORGE_KEY.substring(Keys.CURSE_FORGE_KEY.length() - 4));
		}
	}

	public enum ModLoader {
		FABRIC("fabric"), FORGE("forge");

		private final String name;

		ModLoader(String name) {
			this.name = name;
		}
	}
}
