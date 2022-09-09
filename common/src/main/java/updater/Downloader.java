package updater;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
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
	private final Set<String> visitedMods = new HashSet<>();

	public Downloader(String minecraftVersion, ModLoader modLoader, Path gameDirectory) {
		this.minecraftVersion = minecraftVersion;
		this.modLoader = modLoader;
		modsPath = gameDirectory.resolve("mods");
	}

	public void getCurseForgeMod(String modId) {
		try {
			final URLConnection connection = new URL(String.format("https://api.curseforge.com/v1/mods/%s/files?gameVersion=%s&modLoaderType=%s", modId, minecraftVersion, modLoader.name)).openConnection();
			connection.setRequestProperty("x-api-key", Keys.CURSE_FORGE_KEY);
			final JsonArray filesArray = new JsonParser().parse(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonArray("data");

			downloadMod(
					modId,
					filesArray,
					fileObject -> fileObject.get("fileName").getAsString(),
					fileObject -> fileObject.getAsJsonArray("hashes").get(0).getAsJsonObject().get("value").getAsString(),
					fileObject -> {
						final int fileId = fileObject.get("id").getAsInt();
						return String.format("https://mediafiles.forgecdn.net/files/%s/%s/%s", fileId / 1000, fileId % 1000, fileObject.get("fileName").getAsString());
					},
					fileObject -> fileObject.getAsJsonArray("dependencies").forEach(dependency -> {
						final JsonObject dependencyObject = dependency.getAsJsonObject();
						if (dependencyObject.get("relationType").getAsInt() == 3) {
							getCurseForgeMod(dependencyObject.get("modId").getAsString());
						}
					})
			);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void getModrinthMod(String modId) {
		try {
			final String url = String.format("https://api.modrinth.com/v2/project/%s/version?game_versions=%%5B%%22%s%%22%%5D&loaders=%%5B%%22%s%%22%%5D", modId, minecraftVersion, modLoader.name);
			final URLConnection connection = new URL(url).openConnection();
			final JsonArray filesArray = new JsonParser().parse(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)).getAsJsonArray();

			downloadMod(
					modId,
					filesArray,
					fileObject -> fileObject.getAsJsonArray("files").get(0).getAsJsonObject().get("filename").getAsString(),
					fileObject -> fileObject.getAsJsonArray("files").get(0).getAsJsonObject().getAsJsonObject("hashes").get("sha1").getAsString(),
					fileObject -> fileObject.getAsJsonArray("files").get(0).getAsJsonObject().get("url").getAsString(),
					fileObject -> fileObject.getAsJsonArray("dependencies").forEach(dependency -> {
						final JsonObject dependencyObject = dependency.getAsJsonObject();
						if (dependencyObject.get("dependency_type").getAsString().equals("required")) {
							getModrinthMod(dependencyObject.get("project_id").getAsString());
						}
					})
			);
		} catch (Exception e) {
			e.printStackTrace();
		}
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
								try {
									final HttpGet httpGet = new HttpGet(getUrl.apply(fileObject));
									httpGet.addHeader("User-Agent", "Mozilla/5.0");
									httpGet.addHeader("Referer", "https://www.google.com");
									final CloseableHttpClient httpClient = HttpClients.createDefault();
									FileUtils.copyInputStreamToFile(httpClient.execute(httpGet).getEntity().getContent(), modPath.toFile());
									httpClient.close();
									httpGet.releaseConnection();

									if (hashMatch(getHash.apply(fileObject), modPath)) {
										hasUpdate = true;
										System.out.println("Downloaded " + modPath.getFileName() + (j > 0 ? " after " + (j + 1) + " tries" : ""));
										break;
									}
								} catch (Exception e) {
									e.printStackTrace();
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

	private static boolean hashMatch(String expectedHash, Path path) throws IOException {
		return DigestUtils.sha1Hex(Files.newInputStream(path)).equals(expectedHash);
	}

	public enum ModLoader {
		FABRIC("fabric"), FORGE("forge");

		private final String name;

		ModLoader(String name) {
			this.name = name;
		}
	}
}
