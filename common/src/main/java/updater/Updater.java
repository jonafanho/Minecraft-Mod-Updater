package updater;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;

import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Function;

public class Updater {

	public static final String MOD_ID = "updater";
	private static final Path MODS_PATH = Paths.get(System.getProperty("user.home"), "Desktop", "mods");

	public static void init() {

	}

	private static void getCurseForgeMod(String modId, String minecraftVersion, String modLoader) {
		try {
			final URLConnection connection = new URL(String.format("https://api.curseforge.com/v1/mods/%s/files?gameVersion=%s&modLoaderType=%s", modId, minecraftVersion, modLoader)).openConnection();
			connection.setRequestProperty("x-api-key", "key");
			final JsonArray filesArray = new JsonParser().parse(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonArray("data");

			downloadMod(
					filesArray,
					fileObject -> fileObject.get("fileName").getAsString(),
					fileObject -> fileObject.getAsJsonArray("hashes").get(0).getAsJsonObject().get("value").getAsString(),
					fileObject -> {
						final int fileId = fileObject.get("id").getAsInt();
						return String.format("https://mediafiles.forgecdn.net/files/%s/%s/%s", fileId / 1000, fileId % 1000, fileObject.get("fileName").getAsString());
					}
			);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void getModrinthMod(String modId, String minecraftVersion, String modLoader) {
		try {
			final String url = String.format("https://api.modrinth.com/v2/project/%s/version?game_versions=%%5B%%22%s%%22%%5D&loaders=%%5B%%22%s%%22%%5D", modId, minecraftVersion, modLoader);
			final URLConnection connection = new URL(url).openConnection();
			final JsonArray filesArray = new JsonParser().parse(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)).getAsJsonArray();

			downloadMod(
					filesArray,
					fileObject -> fileObject.getAsJsonArray("files").get(0).getAsJsonObject().get("filename").getAsString(),
					fileObject -> fileObject.getAsJsonArray("files").get(0).getAsJsonObject().getAsJsonObject("hashes").get("sha1").getAsString(),
					fileObject -> fileObject.getAsJsonArray("files").get(0).getAsJsonObject().get("url").getAsString()
			);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void downloadMod(JsonArray filesArray, Function<JsonObject, String> getName, Function<JsonObject, String> getHash, Function<JsonObject, String> getUrl) {
		for (int i = 0; i < filesArray.size(); i++) {
			try {
				final JsonObject fileObject = filesArray.get(i).getAsJsonObject();
				final String fileName = getName.apply(fileObject);
				final Path modPath = MODS_PATH.resolve(fileName);

				if (i == 0) {
					final boolean download;

					if (Files.exists(modPath)) {
						final String expectedHash = getHash.apply(fileObject);
						final String actualHash = DigestUtils.sha1Hex(Files.newInputStream(modPath));
						download = !expectedHash.equals(actualHash);
					} else {
						download = true;
					}

					if (download) {
						FileUtils.copyURLToFile(new URL(getUrl.apply(fileObject)), modPath.toFile());
						System.out.println("Downloaded " + modPath);
					}
				} else {
					if (Files.deleteIfExists(modPath)) {
						System.out.println("Deleted " + modPath);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
