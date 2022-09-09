package updater;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;

import java.io.InputStream;
import java.net.URLEncoder;
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
	private final Set<String> visitedMods = new HashSet<>();

	public Downloader(String minecraftVersion, ModLoader modLoader, Path gameDirectory) {
		this.minecraftVersion = minecraftVersion;
		this.modLoader = modLoader;
		modsPath = gameDirectory.resolve("mods");
		printCurseForgeKey();
		visitedMods.add("minecraft-transit-railway");
		visitedMods.add("XKPAmI6u");
		visitedMods.add("266707");
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
						fileNameEncoded = URLEncoder.encode(fileNameEncoded, "UTF-8");
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
						getModrinthMod(dependencyObject.get("project_id").getAsString());
					}
				})
		));
	}

	public void getMod(String modId, String url, String hash) {
		final JsonArray tempArray = new JsonArray();
		tempArray.add(new JsonObject());
		final String newModId = modId.replace(".jar", "").replaceAll("[^\\w-_]", "");
		downloadMod(newModId, tempArray, jsonObject -> newModId + ".jar", jsonObject -> hash, jsonObject -> url, jsonObject -> {
		});
	}

	public boolean hasUpdate() {
		return hasUpdate;
	}

	private void downloadMod(String modId, JsonArray filesArray, Function<JsonObject, String> getName, Function<JsonObject, String> getHash, Function<JsonObject, String> getUrl, Consumer<JsonObject> firstCallback) {
		if (!visitedMods.contains(modId)) {
			visitedMods.add(modId);

			for (int i = 0; i < filesArray.size(); i++) {
				try {
					final JsonObject fileObject = filesArray.get(i).getAsJsonObject();
					final String fileName = getName.apply(fileObject);
					final Path modPath = modsPath.resolve(fileName);

					if (i == 0) {
						final boolean download;

						if (Files.exists(modPath)) {
							download = !hashMatch(getHash.apply(fileObject), modPath);
						} else {
							download = true;
						}

						if (download) {
							for (int j = 0; j < 2; j++) {
								Updater.readConnectionSafe(getUrl.apply(fileObject), inputStream -> {
									try {
										FileUtils.copyInputStreamToFile(inputStream, modPath.toFile());
									} catch (Exception e) {
										e.printStackTrace();
									}
								}, "User-Agent", "Mozilla/5.0");

								if (hashMatch(getHash.apply(fileObject), modPath)) {
									hasUpdate = true;
									System.out.println("Downloaded " + modPath.getFileName() + (j > 0 ? " after " + (j + 1) + " tries" : ""));
									break;
								}
							}
						}

						firstCallback.accept(fileObject);
					} else {
						if (Files.exists(modPath)) {
							FileUtils.forceDeleteOnExit(modPath.toFile());
							hasUpdate = true;
							System.out.println("Deleting " + modPath.getFileName());
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	private static boolean hashMatch(String expectedHash, Path path) {
		try (final InputStream inputStream = Files.newInputStream(path)) {
			return DigestUtils.sha1Hex(inputStream).equals(expectedHash);
		} catch (Exception e) {
			e.printStackTrace();
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
