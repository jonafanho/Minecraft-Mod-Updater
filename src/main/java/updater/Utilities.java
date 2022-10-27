package updater;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

public interface Utilities {

	static void openConnectionSafe(String url, Consumer<InputStream> callback, String... requestProperties) {
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

	static void openConnectionSafeJson(String url, Consumer<JsonElement> callback, String... requestProperties) {
		openConnectionSafe(url, inputStream -> {
			try (final InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
				callback.accept(JsonParser.parseReader(inputStreamReader));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}, requestProperties);
	}

	static void readFileJson(Path path, Consumer<JsonElement> callback) {
		try {
			try (final BufferedReader bufferedReader = Files.newBufferedReader(path)) {
				callback.accept(JsonParser.parseReader(bufferedReader));
			} catch (Exception e) {
				e.printStackTrace();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	static JsonObject findInJsonArray(JsonArray jsonArray, String key, String value) {
		for (int i = 0; i < jsonArray.size(); i++) {
			final JsonObject jsonObject = jsonArray.get(i).getAsJsonObject();
			if (jsonObject.get(key).getAsString().equals(value)) {
				return jsonObject;
			}
		}
		return null;
	}

	static boolean runCommand(String... command) {
		try {
			final ProcessBuilder builder = new ProcessBuilder(command);
			builder.redirectErrorStream(true);
			final Process process = builder.start();
			try (final InputStream inputStream = process.getInputStream()) {
				try (final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
					String line;
					while ((line = bufferedReader.readLine()) != null) {
						System.out.println(line);
					}
				}
			}
			return true;
		} catch (Exception ignored) {
		}
		return false;
	}

	static String getHash(Path path) {
		if (Files.exists(path)) {
			try (final InputStream inputStream = Files.newInputStream(path)) {
				return DigestUtils.sha1Hex(inputStream);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}
}
