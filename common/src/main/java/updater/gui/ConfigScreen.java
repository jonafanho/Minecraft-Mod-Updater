package updater.gui;

import com.jonafanho.apitools.Mod;
import com.jonafanho.apitools.ModLoader;
import com.jonafanho.apitools.NetworkUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.components.Button;
import org.apache.commons.io.FileUtils;
import updater.Config;
import updater.Downloader;
import updater.Keys;
import updater.Updater;
import updater.mappings.ScreenMapper;
import updater.mappings.Text;
import updater.mappings.UtilitiesClient;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;

public class ConfigScreen extends ScreenMapper implements IGui {

	private boolean hasChanges = false;

	private final Path gameDirectory;
	private final Path modsLocal;
	private final Button buttonOpenFolder;
	private final Button buttonBrowseMods;
	private final Button buttonDiscardChanges;
	private final Button buttonAddFromLink;
	private final Button buttonRelaunchMinecraft;
	private final DashboardList serverList;
	private final DashboardList localList;

	public ConfigScreen(Runnable launch, String minecraftVersion, ModLoader modLoader, Path gameDirectory) {
		super(Text.literal(""));
		this.gameDirectory = gameDirectory;
		modsLocal = gameDirectory.resolve(Updater.MODS_LOCAL_DIRECTORY);
		Config.loadConfig(gameDirectory);

		buttonOpenFolder = new Button(0, 0, 0, SQUARE_SIZE, Text.translatable("modmenu.modsFolder"), button -> Util.getPlatform().openFile(modsLocal.toFile()));
		buttonBrowseMods = new Button(0, 0, 0, SQUARE_SIZE, Text.translatable("gui.updater.browse_mods"), button -> UtilitiesClient.setScreen(Minecraft.getInstance(), new SearchModsScreen(minecraftVersion, modLoader, this)));
		buttonDiscardChanges = new Button(0, 0, 0, SQUARE_SIZE, Text.translatable("gui.updater.discard_changes"), button -> {
			Config.loadConfig(gameDirectory);
			updateListData(false);
		});
		buttonAddFromLink = new Button(0, 0, 0, SQUARE_SIZE, Text.translatable("gui.updater.add_from_link"), button -> UtilitiesClient.setScreen(Minecraft.getInstance(), new AddFromLinkScreen(this, true,
				Text.translatable("gui.updater.add_from_link"),
				Text.translatable("gui.updater.add"),
				Text.translatable("gui.updater.examples").getString(),
				"https://www.curseforge.com/minecraft/mc-mods/minecraft-transit-railway",
				"https://modrinth.com/mod/minecraft-transit-railway",
				"https://github.com/zbx1425/SlideShow/releases/download/0.5.4/slideshow-1.18.2-0.5.4.jar"
		) {
			@Override
			protected void onClick(String text) {
				final Mod mod = Mod.getModFromUrl(text, Keys.CURSE_FORGE_KEY);

				if (mod == null) {
					final boolean[] failed = {true};
					final String[] urlSplit = text.split("/");

					NetworkUtils.openConnectionSafe(text, inputStream -> {
						try {
							final String fileName = Downloader.cleanModName(urlSplit[urlSplit.length - 1]);
							final Path tempPath = gameDirectory.resolve(Updater.MODS_TEMP_DIRECTORY).resolve(fileName + ".jar");
							FileUtils.copyInputStreamToFile(inputStream, tempPath.toFile());
							try (final JarFile jarFile = new JarFile(tempPath.toFile())) {
								Updater.LOGGER.info("Valid jar file " + jarFile.getName());
							}
							final String sha1 = Downloader.getHash(tempPath);
							Minecraft.getInstance().execute(() -> {
								Config.addModObject(fileName, text, sha1);
								setMessage(Text.translatable("gui.updater.added_jar_file", text));
								updateListData(true);
							});
							failed[0] = false;
						} catch (Exception e) {
							e.printStackTrace();
						}
					}, "User-Agent", "Mozilla/5.0");

					if (failed[0]) {
						final int[] modCount = {-1};
						NetworkUtils.openConnectionSafeJson(text, jsonElement -> {
							try {
								modCount[0] = Config.getModObjects(jsonElement.getAsJsonArray()).size();
							} catch (Exception e) {
								e.printStackTrace();
							}
						});
						Minecraft.getInstance().execute(() -> {
							if (modCount[0] > 0) {
								Config.addServerUrl(text);
							}
							updateListData(true);
							setMessage(modCount[0] == 0 ? Text.translatable("gui.updater.no_mods_in_modpack") : modCount[0] > 0 ? Text.translatable("gui.updater.added_modpack", modCount[0], text) : Text.translatable("gui.updater.invalid_url"));
						});
					}
				} else {
					Minecraft.getInstance().execute(() -> {
						Config.addModObject(mod);
						setMessage(Text.translatable("gui.updater.added_mod_url", text));
						updateListData(true);
					});
				}
			}
		}));
		buttonRelaunchMinecraft = new Button(0, 0, 0, SQUARE_SIZE, Text.translatable("gui.updater.relaunch_minecraft"), button -> {
			Config.saveConfig(gameDirectory);
			launch.run();
		});

		serverList = new DashboardList((data, index) -> {
			Config.removeServerUrl(index);
			updateListData(true);
		}, null, "-", 1);
		localList = new DashboardList((data, index) -> {
			if (Config.removeModObject(index)) {
				updateListData(true);
			} else {
				data.firstText(fileName -> {
					try {
						Files.move(modsLocal.resolve(fileName), gameDirectory.resolve(Updater.MODS_BACKUP_DIRECTORY).resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
					} catch (Exception e) {
						e.printStackTrace();
					}
				});
				updateListData(hasChanges);
			}
		}, null, "-", 3);
	}

	@Override
	protected void init() {
		super.init();

		final int smallColumnWidth = (width - SQUARE_SIZE * 2) / 3;

		IGui.setPositionAndWidth(buttonOpenFolder, SQUARE_SIZE, height - SQUARE_SIZE * 3, smallColumnWidth);
		IGui.setPositionAndWidth(buttonBrowseMods, SQUARE_SIZE + smallColumnWidth, height - SQUARE_SIZE * 3, smallColumnWidth);
		IGui.setPositionAndWidth(buttonDiscardChanges, SQUARE_SIZE + smallColumnWidth * 2, height - SQUARE_SIZE * 3, smallColumnWidth);
		IGui.setPositionAndWidth(buttonAddFromLink, SQUARE_SIZE, height - SQUARE_SIZE * 2, smallColumnWidth * 2);
		IGui.setPositionAndWidth(buttonRelaunchMinecraft, SQUARE_SIZE + smallColumnWidth * 2, height - SQUARE_SIZE * 2, smallColumnWidth);

		final int columnWidth = (width - SQUARE_SIZE * 3) / 2;
		final int column2Start = SQUARE_SIZE * 2 + columnWidth;

		serverList.x = SQUARE_SIZE;
		serverList.y = SQUARE_SIZE + TEXT_HEIGHT + TEXT_PADDING;
		serverList.width = columnWidth;
		serverList.height = height - SQUARE_SIZE * 5 - TEXT_HEIGHT - TEXT_PADDING;
		localList.x = column2Start;
		localList.y = SQUARE_SIZE + TEXT_HEIGHT + TEXT_PADDING;
		localList.width = columnWidth;
		localList.height = height - SQUARE_SIZE * 5 - TEXT_HEIGHT - TEXT_PADDING;

		updateListData(hasChanges);
		addDrawableChild(buttonOpenFolder);
		addDrawableChild(buttonBrowseMods);
		addDrawableChild(buttonDiscardChanges);
		addDrawableChild(buttonAddFromLink);
		addDrawableChild(buttonRelaunchMinecraft);
		serverList.init(this::addDrawableChild);
		localList.init(this::addDrawableChild);
	}

	@Override
	public void render(PoseStack matrices, int mouseX, int mouseY, float delta) {
		try {
			renderBackground(matrices);
			serverList.render(matrices, font);
			localList.render(matrices, font);
			super.render(matrices, mouseX, mouseY, delta);
			Gui.drawCenteredString(matrices, font, Text.translatable("gui.updater.synced_packs"), (width - SQUARE_SIZE) / 4 + SQUARE_SIZE / 2, SQUARE_SIZE, ARGB_WHITE);
			Gui.drawCenteredString(matrices, font, Text.translatable("gui.updater.local_mods"), (width - SQUARE_SIZE) / 4 + width / 2, SQUARE_SIZE, ARGB_WHITE);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void tick() {
		serverList.tick();
		localList.tick();
	}

	@Override
	public void onClose() {
		super.onClose();
		Config.saveConfig(gameDirectory);
	}

	@Override
	public void mouseMoved(double mouseX, double mouseY) {
		serverList.mouseMoved(mouseX, mouseY);
		localList.mouseMoved(mouseX, mouseY);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
		serverList.mouseScrolled(mouseX, mouseY, amount);
		localList.mouseScrolled(mouseX, mouseY, amount);
		return super.mouseScrolled(mouseX, mouseY, amount);
	}

	private void updateListData(boolean hasChanges) {
		final List<DashboardList.Data> serverUrlList = new ArrayList<>();
		Config.forEachServerUrl(serverUrl -> serverUrlList.add(new DashboardList.Data(serverUrl)));
		serverList.setData(serverUrlList);

		final List<DashboardList.Data> modObjectsList = new ArrayList<>();
		Config.forEachModObject(modObject -> modObjectsList.add(new DashboardList.Data(modObject.toStringArray())));
		Downloader.iterateFiles(modsLocal.toFile(), false, file -> modObjectsList.add(new DashboardList.Data(file.getName(), Text.translatable("gui.updater.local_folder").getString())));
		localList.setData(modObjectsList);

		this.hasChanges = hasChanges;
		buttonDiscardChanges.active = hasChanges;
	}
}
