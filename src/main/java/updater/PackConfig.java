package updater;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PackConfig {

	public final String minecraftVersion;
	public final Loader loader;

	public PackConfig(String rootDirectory) {
		final Loader[] loader = {null};
		final String[] minecraftVersion = {""};
		final String[] newString = {""};
		final Path path = Paths.get(rootDirectory, "mmc-pack.json");

		Utilities.readFileJson(path, packElement -> {
			final JsonArray componentsArray = packElement.getAsJsonObject().getAsJsonArray("components");

			for (int i = 0; i < componentsArray.size(); i++) {
				final JsonObject componentObject = componentsArray.get(i).getAsJsonObject();
				final String id = componentObject.get("uid").getAsString();
				final String version = componentObject.get("version").getAsString();

				if (id.equals("net.minecraft")) {
					minecraftVersion[0] = version;
				} else {
					final String[] newVersion = {""};

					Utilities.openConnectionSafeJson(String.format("https://meta.multimc.org/v1/%s/", id), versionsElement -> {
						final JsonArray versionsArray = versionsElement.getAsJsonObject().getAsJsonArray("versions");
						for (int j = 0; j < versionsArray.size(); j++) {
							final JsonObject versionObject = versionsArray.get(j).getAsJsonObject();
							if (versionObject.has("requires")) {
								final JsonArray versionRequiresArray = versionObject.getAsJsonArray("requires");
								boolean match = true;

								for (int k = 0; k < versionRequiresArray.size(); k++) {
									final JsonObject versionRequiresObject = versionRequiresArray.get(k).getAsJsonObject();
									final JsonObject checkObject = Utilities.findInJsonArray(componentsArray, "uid", versionRequiresObject.get("uid").getAsString());
									if (checkObject == null || versionRequiresObject.has("equals") && !checkObject.get("version").getAsString().equals(versionRequiresObject.get("equals").getAsString())) {
										match = false;
									}
								}

								if (match) {
									newVersion[0] = versionObject.get("version").getAsString();
									break;
								}
							} else {
								newVersion[0] = versionObject.get("version").getAsString();
								break;
							}
						}
					});

					if (!newVersion[0].isEmpty() && !newVersion[0].equals(version)) {
						System.out.println("Updated " + id + " to " + newVersion[0]);
						componentsArray.get(i).getAsJsonObject().addProperty("version", newVersion[0]);
					}
				}

				for (final Loader checkLoader : Loader.values()) {
					if (id.equals(checkLoader.className)) {
						loader[0] = checkLoader;
						break;
					}
				}
			}

			newString[0] = packElement.toString();
		});

		if (!newString[0].isEmpty()) {
			try {
				Files.write(path, newString[0].getBytes(StandardCharsets.UTF_8));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		this.minecraftVersion = minecraftVersion[0];
		this.loader = loader[0];
	}
}
