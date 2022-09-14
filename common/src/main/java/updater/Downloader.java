package updater;

import com.jonafanho.apitools.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Downloader {

	private boolean hasUpdate = false;
	private final String minecraftVersion;
	private final ModLoader modLoader;
	private final Path modsPath;
	private final Path modsPathLocal;
	private final Path modsPathTemp;
	private final Set<String> visitedMods = new HashSet<>();
	private final Set<File> modsToDelete = new HashSet<>();

	private static final int DOWNLOAD_ATTEMPTS = 5;
	private static final byte[] EMPTY_ZIP = {80, 75, 5, 6, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

	public Downloader(String minecraftVersion, ModLoader modLoader, Path gameDirectory) {
		this.minecraftVersion = minecraftVersion;
		this.modLoader = modLoader;
		modsPath = gameDirectory.resolve(Updater.MODS_DIRECTORY);
		modsPathLocal = gameDirectory.resolve(Updater.MODS_LOCAL_DIRECTORY);
		modsPathTemp = gameDirectory.resolve(Updater.MODS_TEMP_DIRECTORY);

		FileUtils.listFiles(modsPath.toFile(), new String[]{"jar"}, false).forEach(file -> {
			if (!file.getName().startsWith("Mod-Updater-")) {
				modsToDelete.add(file);
			}
		});

		try {
			Files.createDirectories(modsPathLocal);
		} catch (Exception e) {
			e.printStackTrace();
		}

		printCurseForgeKey();

		if (modLoader == ModLoader.FABRIC) {
			downloadMod(new ModId("modmenu", ModProvider.MODRINTH));
		}
	}

	public void downloadMod(String modId, String hash, String url) {
		downloadFile(modId.replace(".jar", "").replaceAll("[^\\w-_.]", "") + ".jar", hash, url);
	}

	public void downloadMod(ModId modId) {
		final List<ModFile> modFiles = modId.getModFiles(minecraftVersion, modLoader, Keys.CURSE_FORGE_KEY);
		if (!visitedMods.contains(modId.modId) && modFiles.size() > 0) {
			visitedMods.add(modId.modId);
			final ModFile modFile = modFiles.get(0);
			downloadFile(modFile.fileName, modFile.sha1, modFile.url);
			modFile.requiredDependencies.forEach(this::downloadMod);
		}
	}

	public boolean cleanAndCheckUpdate() {
		FileUtils.listFiles(modsPathLocal.toFile(), new String[]{"jar"}, false).forEach(file -> {
			final Path modPath = modsPath.resolve(file.getName());
			final File modFile = modPath.toFile();
			final Path sourcePath = file.toPath();
			final String sourceHash = getHash(sourcePath);

			if (sourceHash != null && !sourceHash.equals(getHash(modPath))) {
				specialCopy(sourcePath, modFile);
				Updater.LOGGER.info("Copied " + modPath);
				hasUpdate = true;
			}

			modsToDelete.remove(modFile);
		});

		modsToDelete.forEach(file -> {
			try {
				Updater.LOGGER.info("Deleting " + file.getName());
				specialCopy(null, file);
			} catch (Exception e) {
				e.printStackTrace();
			}
			hasUpdate = true;
		});

		return hasUpdate;
	}

	private void downloadFile(String fileName, String sha1, String url) {
		try {
			final Path modPathTemp = modsPathTemp.resolve(fileName);

			if (!sha1.equals(getHash(modPathTemp))) {
				for (int i = 1; i <= DOWNLOAD_ATTEMPTS; i++) {
					NetworkUtils.openConnectionSafe(url, inputStream -> {
						try {
							FileUtils.copyInputStreamToFile(inputStream, modPathTemp.toFile());
						} catch (Exception e) {
							e.printStackTrace();
						}
					}, "User-Agent", "Mozilla/5.0");

					if (sha1.equals(getHash(modPathTemp))) {
						Updater.LOGGER.info("Downloaded " + fileName);
						break;
					} else {
						Updater.LOGGER.warn("Failed to download " + fileName + " (" + i + "/" + DOWNLOAD_ATTEMPTS + ")");
					}
				}
			}

			final Path modPath = modsPath.resolve(fileName);

			if (!sha1.equals(getHash(modPath))) {
				hasUpdate = true;
				specialCopy(modPathTemp, modPath.toFile());
			}

			modsToDelete.remove(modPath.toFile());
		} catch (Exception e) {
			e.printStackTrace();
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

	private static String getHash(Path path) {
		if (Files.exists(path)) {
			try (final InputStream inputStream = Files.newInputStream(path)) {
				return DigestUtils.sha1Hex(inputStream);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	@SuppressWarnings("all")
	private static void printCurseForgeKey() {
		if (Keys.CURSE_FORGE_KEY.length() > 8) {
			Updater.LOGGER.info("Using CurseForge API key: " + Keys.CURSE_FORGE_KEY.substring(0, 4) + "..." + Keys.CURSE_FORGE_KEY.substring(Keys.CURSE_FORGE_KEY.length() - 4));
		}
	}
}
