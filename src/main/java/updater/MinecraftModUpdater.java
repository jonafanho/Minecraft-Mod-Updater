package updater;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class MinecraftModUpdater implements Constants {

	public static void main(String[] args) {
		if (args.length < 1) {
			return;
		}

		String feriumFile = null;
		for (final String file : FILES) {
			try {
				String testFile = String.format("%s/%s/ferium", args[0], file);
				if (runCommand(testFile, "--version")) {
					feriumFile = testFile;
					break;
				}
			} catch (Exception ignored) {
			}
		}

		if (feriumFile == null) {
			System.out.println("Failed to run Ferium");
		} else {
			System.out.println(feriumFile);
		}
	}

	private static boolean runCommand(String... command) {
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
}
