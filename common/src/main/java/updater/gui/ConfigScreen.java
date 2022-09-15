package updater.gui;

import com.jonafanho.apitools.Mod;
import com.jonafanho.apitools.ModLoader;
import com.jonafanho.apitools.NetworkUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.Util;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.components.Button;
import updater.Config;
import updater.Keys;
import updater.Updater;
import updater.mappings.ScreenMapper;
import updater.mappings.Text;
import updater.mappings.UtilitiesClient;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ConfigScreen extends ScreenMapper implements IGui {

	private boolean hasChanges = false;

	private final Path gameDirectory;
	private final Button buttonOpenFolder;
	private final Button buttonBrowseMods;
	private final Button buttonDiscardChanges;
	private final Button buttonAddServerPackFromLink;
	private final Button buttonAddModFromLink;
	private final Button buttonRelaunchMinecraft;
	private final DashboardList serverList;
	private final DashboardList localList;

	public ConfigScreen(Runnable launch, String minecraftVersion, ModLoader modLoader, Path gameDirectory) {
		super(Text.literal(""));
		this.gameDirectory = gameDirectory;
		Config.loadConfig(gameDirectory);

		buttonOpenFolder = new Button(0, 0, 0, SQUARE_SIZE, Text.translatable("modmenu.modsFolder"), button -> Util.getPlatform().openFile(gameDirectory.resolve(Updater.MODS_LOCAL_DIRECTORY).toFile()));
		buttonBrowseMods = new Button(0, 0, 0, SQUARE_SIZE, Text.translatable("gui.updater.browse_mods"), button -> {
			if (minecraft != null) {
				UtilitiesClient.setScreen(minecraft, new SearchModsScreen(minecraftVersion, modLoader, this));
			}
		});
		buttonDiscardChanges = new Button(0, 0, 0, SQUARE_SIZE, Text.translatable("gui.updater.discard_changes"), button -> {
			Config.loadConfig(gameDirectory);
			updateListData(false);
		});
		buttonAddServerPackFromLink = new Button(0, 0, 0, SQUARE_SIZE, Text.translatable("gui.updater.add_server_pack_from_link"), button -> {
			if (minecraft != null) {
				UtilitiesClient.setScreen(minecraft, new AddFromLinkScreen(this, url -> {
					final boolean[] success = {false};
					NetworkUtils.openConnectionSafeJson(url, jsonElement -> {
						try {
							Config.getModObjects(jsonElement.getAsJsonArray());
							Config.addServerUrl(url);
							updateListData(true);
							success[0] = true;
						} catch (Exception e) {
							e.printStackTrace();
						}
					});
					return success[0] ? null : Text.translatable("gui.updater.invalid_modpack_url").getString();
				}, Text.translatable("gui.updater.add_server_pack_from_link")));
			}
		});
		buttonAddModFromLink = new Button(0, 0, 0, SQUARE_SIZE, Text.translatable("gui.updater.add_mod_from_link"), button -> {
			if (minecraft != null) {
				UtilitiesClient.setScreen(minecraft, new AddFromLinkScreen(this, url -> {
					final Mod mod = Mod.getModFromUrl(url, Keys.CURSE_FORGE_KEY);
					if (mod == null) {
						Config.addModObject(url);
					} else {
						Config.addModObject(mod);
					}
					updateListData(true);
					return null;
				},
						Text.translatable("gui.updater.add_mod_from_link"),
						Text.translatable("gui.updater.examples").getString(),
						"https://www.curseforge.com/minecraft/mc-mods/minecraft-transit-railway",
						"https://modrinth.com/mod/minecraft-transit-railway",
						"https://github.com/zbx1425/SlideShow/releases/download/0.5.4/slideshow-1.18.2-0.5.4.jar"
				));
			}
		});
		buttonRelaunchMinecraft = new Button(0, 0, 0, SQUARE_SIZE, Text.translatable("gui.updater.relaunch_minecraft"), button -> {
			Config.saveConfig(gameDirectory);
			launch.run();
		});

		serverList = new DashboardList((data, index) -> {
			Config.removeServerUrl(index);
			updateListData(true);
		}, null, "-", 1);
		localList = new DashboardList((data, index) -> {
			Config.removeModObject(index);
			updateListData(true);
		}, null, "-", 3);
	}

	@Override
	protected void init() {
		super.init();

		final int smallColumnWidth = (width - SQUARE_SIZE * 2) / 3;

		IGui.setPositionAndWidth(buttonOpenFolder, SQUARE_SIZE, height - SQUARE_SIZE * 3, smallColumnWidth);
		IGui.setPositionAndWidth(buttonBrowseMods, SQUARE_SIZE + smallColumnWidth, height - SQUARE_SIZE * 3, smallColumnWidth);
		IGui.setPositionAndWidth(buttonDiscardChanges, SQUARE_SIZE + smallColumnWidth * 2, height - SQUARE_SIZE * 3, smallColumnWidth);
		IGui.setPositionAndWidth(buttonAddServerPackFromLink, SQUARE_SIZE, height - SQUARE_SIZE * 2, smallColumnWidth);
		IGui.setPositionAndWidth(buttonAddModFromLink, SQUARE_SIZE + smallColumnWidth, height - SQUARE_SIZE * 2, smallColumnWidth);
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
		addDrawableChild(buttonAddServerPackFromLink);
		addDrawableChild(buttonAddModFromLink);
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

	public void updateListData(boolean hasChanges) {
		final List<DashboardList.Data> serverUrlList = new ArrayList<>();
		Config.forEachServerUrl(serverUrl -> serverUrlList.add(new DashboardList.Data(serverUrl)));
		serverList.setData(serverUrlList);

		final List<DashboardList.Data> modObjectsList = new ArrayList<>();
		Config.forEachModObject(modObject -> modObjectsList.add(new DashboardList.Data(modObject.toStringArray())));
		localList.setData(modObjectsList);

		this.hasChanges = hasChanges;
		buttonDiscardChanges.active = hasChanges;
	}
}
