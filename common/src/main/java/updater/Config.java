package updater;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jonafanho.apitools.Mod;
import com.jonafanho.apitools.ModId;
import com.jonafanho.apitools.ModProvider;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class Config {

	private static final List<String> SERVER_URLS = new ArrayList<>();
	private static final List<ModObject> MOD_OBJECTS = new ArrayList<>();

	public static void loadConfig(Path gameDirectory) {
		SERVER_URLS.clear();
		MOD_OBJECTS.clear();

		try {
			final JsonObject configObject = new JsonParser().parse(FileUtils.readFileToString(getConfigFile(gameDirectory), StandardCharsets.UTF_8)).getAsJsonObject();

			if (configObject.has("synced")) {
				configObject.getAsJsonArray("synced").forEach(serverElement -> SERVER_URLS.add(serverElement.getAsString()));
			}

			if (configObject.has("local")) {
				MOD_OBJECTS.addAll(getModObjects(configObject.getAsJsonArray("local")));
			}

			Updater.LOGGER.info("Successfully read config");
		} catch (Exception ignored) {
			try {
				Updater.LOGGER.info("Resetting config");
				saveConfig(gameDirectory);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		Collections.sort(SERVER_URLS);
		Collections.sort(MOD_OBJECTS);
	}

	public static void saveConfig(Path gameDirectory) {
		final JsonObject jsonObject = new JsonObject();

		final JsonArray syncedArray = new JsonArray();
		SERVER_URLS.forEach(syncedArray::add);
		jsonObject.add("synced", syncedArray);

		final JsonArray localArray = new JsonArray();
		MOD_OBJECTS.forEach(modObject -> localArray.add(modObject.toJsonObject()));
		jsonObject.add("local", localArray);

		try {
			FileUtils.writeStringToFile(getConfigFile(gameDirectory), new GsonBuilder().setPrettyPrinting().create().toJson(jsonObject), StandardCharsets.UTF_8);
			Updater.LOGGER.info("Wrote config to file");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static List<ModObject> getModObjects(JsonArray modArray) {
		final List<ModObject> modObjects = new ArrayList<>();

		modArray.forEach(modElement -> {
			try {
				final JsonObject modObject = modElement.getAsJsonObject();
				final String modId = modObject.get("id").getAsString();
				final String[] comments = modObject.has("comment") ? modObject.get("comment").getAsString().split("\\|") : new String[0];

				if (modObject.has("source")) {
					final String source = modObject.get("source").getAsString().toLowerCase();
					if (source.equals("curseforge")) {
						modObjects.add(new ModObject(modId, ModProvider.CURSE_FORGE, comments));
					} else if (source.equals("modrinth")) {
						modObjects.add(new ModObject(modId, ModProvider.MODRINTH, comments));
					}
				} else if (modObject.has("url") && modObject.has("sha1")) {
					final Long before = modObject.has("before") ? modObject.get("before").getAsLong() : null;
					final Long after = modObject.has("after") ? modObject.get("after").getAsLong() : null;
					modObjects.add(new ModObject(modId, modObject.get("url").getAsString(), modObject.get("sha1").getAsString(), before, after, comments));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		});

		return modObjects;
	}

	public static void forEachServerUrl(Consumer<String> consumer) {
		SERVER_URLS.forEach(consumer);
	}

	public static void forEachModObject(Consumer<ModObject> consumer) {
		MOD_OBJECTS.forEach(consumer);
	}

	public static void addServerUrl(String url) {
		if (!url.isEmpty() && !SERVER_URLS.contains(url)) {
			SERVER_URLS.add(url);
		}
		Collections.sort(SERVER_URLS);
	}

	public static void addModObject(Mod mod) {
		if (!containsMod(mod)) {
			final Set<ModId> modIds = mod.modIds;
			modIds.stream().findFirst().ifPresent(modId -> MOD_OBJECTS.add(new ModObject(modId.modId, modId.modProvider, mod.name, mod.description)));
			Collections.sort(MOD_OBJECTS);
		}
	}

	public static boolean containsMod(Mod mod) {
		final Set<ModId> modIds = mod.modIds;

		for (final ModObject modObject : MOD_OBJECTS) {
			for (final ModId modId : modIds) {
				if (modId.modId.equals(modObject.modId) && modId.modProvider.equals(modObject.modProvider)) {
					return true;
				}
			}
		}

		return false;
	}

	public static void addModObject(String fileName, String url, String sha1) {
		for (final ModObject modObject : MOD_OBJECTS) {
			if (url.equals(modObject.url)) {
				return;
			}
		}

		MOD_OBJECTS.add(new ModObject(fileName, url, sha1, null, null));
		Collections.sort(MOD_OBJECTS);
	}

	public static void removeServerUrl(int index) {
		SERVER_URLS.remove(index);
	}

	public static boolean removeModObject(int index) {
		if (index >= MOD_OBJECTS.size()) {
			return false;
		} else {
			MOD_OBJECTS.remove(index);
			return true;
		}
	}

	private static File getConfigFile(Path gameDirectory) {
		return gameDirectory.resolve("config").resolve("minecraft-mod-updater.json").toFile();
	}

	public static class ModObject implements Comparable<ModObject> {

		private final String modId;
		private final ModProvider modProvider;
		private final String url;
		private final String sha1;
		private final long before;
		private final long after;
		private final String[] comments;

		private ModObject(String modId, ModProvider modProvider, String... comments) {
			this.modId = modId;
			this.modProvider = modProvider;
			url = null;
			sha1 = null;
			before = Long.MAX_VALUE;
			after = 0;
			this.comments = comments;
		}

		private ModObject(String modId, String url, String sha1, Long before, Long after, String... comments) {
			this.modId = modId;
			modProvider = null;
			this.url = url;
			this.sha1 = sha1;
			this.before = before == null ? Long.MAX_VALUE : before;
			this.after = after == null ? 0 : after;
			this.comments = comments;
		}

		public void download(Downloader downloader) {
			if (modProvider != null) {
				downloader.downloadMod(new ModId(modId, modProvider));
			} else if (url != null && sha1 != null) {
				final long millis = System.currentTimeMillis();
				if (millis < before && millis >= after) {
					downloader.downloadMod(modId, sha1, url);
				}
			}
		}

		public String[] toStringArray() {
			final List<String> result = new ArrayList<>();
			Collections.addAll(result, comments);
			if (comments.length == 0) {
				result.add(modId);
			}
			result.add(modProvider == null ? url : modProvider.name);
			return result.toArray(new String[0]);
		}

		private JsonObject toJsonObject() {
			final JsonObject jsonObject = new JsonObject();
			jsonObject.addProperty("id", modId);

			if (modProvider != null) {
				jsonObject.addProperty("source", modProvider.id);
			} else if (url != null && sha1 != null) {
				jsonObject.addProperty("url", url);
				jsonObject.addProperty("sha1", sha1);
				if (before != Long.MAX_VALUE) {
					jsonObject.addProperty("before", before);
				}
				if (after != 0) {
					jsonObject.addProperty("after", after);
				}
			}

			if (comments.length > 0) {
				jsonObject.addProperty("comment", String.join("|", comments));
			}

			return jsonObject;
		}

		@Override
		public int compareTo(ModObject modObject) {
			return String.join(" ", toStringArray()).compareTo(String.join(" ", modObject.toStringArray()));
		}
	}
}
