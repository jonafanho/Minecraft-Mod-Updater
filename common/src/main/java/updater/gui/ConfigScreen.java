package updater.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.Util;
import net.minecraft.client.gui.components.Button;
import updater.Config;
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
	private final DashboardList serverList;
	private final DashboardList localList;

	public ConfigScreen(Path gameDirectory) {
		super(Text.literal(""));
		this.gameDirectory = gameDirectory;
		Config.loadConfig(gameDirectory);

		buttonOpenFolder = new Button(0, 0, 0, SQUARE_SIZE, Text.translatable("modmenu.modsFolder"), button -> Util.getPlatform().openFile(gameDirectory.resolve(Updater.MODS_LOCAL_DIRECTORY).toFile()));
		buttonBrowseMods = new Button(0, 0, 0, SQUARE_SIZE, Text.translatable("gui.updater.browse_mods"), button -> {
			if (minecraft != null) {
				UtilitiesClient.setScreen(minecraft, new SearchModsScreen(this));
			}
		});
		buttonDiscardChanges = new Button(0, 0, 0, SQUARE_SIZE, Text.translatable("gui.updater.discard_changes"), button -> {
			Config.loadConfig(gameDirectory);
			updateListData(false);
		});

		serverList = new DashboardList((data, index) -> {
			Config.removeServerUrl(index);
			updateListData(true);
		}, "-", 1);
		localList = new DashboardList((data, index) -> {
			Config.removeModObject(index);
			updateListData(true);
		}, "-", 2);
	}

	@Override
	protected void init() {
		super.init();

		final int smallColumnWidth = (width - SQUARE_SIZE * 4) / 3;

		IGui.setPositionAndWidth(buttonOpenFolder, SQUARE_SIZE, height - SQUARE_SIZE * 2, smallColumnWidth);
		IGui.setPositionAndWidth(buttonBrowseMods, SQUARE_SIZE * 2 + smallColumnWidth, height - SQUARE_SIZE * 2, smallColumnWidth);
		IGui.setPositionAndWidth(buttonDiscardChanges, SQUARE_SIZE * 3 + smallColumnWidth * 2, height - SQUARE_SIZE * 2, smallColumnWidth);

		final int columnWidth = (width - SQUARE_SIZE * 3) / 2;
		final int column2Start = SQUARE_SIZE * 2 + columnWidth;

		serverList.x = SQUARE_SIZE;
		serverList.y = SQUARE_SIZE;
		serverList.width = columnWidth;
		serverList.height = height - SQUARE_SIZE * 4;
		localList.x = column2Start;
		localList.y = SQUARE_SIZE;
		localList.width = columnWidth;
		localList.height = height - SQUARE_SIZE * 4;

		updateListData(hasChanges);
		addDrawableChild(buttonOpenFolder);
		addDrawableChild(buttonBrowseMods);
		addDrawableChild(buttonDiscardChanges);
		serverList.init(this::addDrawableChild);
		localList.init(this::addDrawableChild);
	}

	@Override
	public void render(PoseStack matrices, int mouseX, int mouseY, float delta) {
		try {
			renderBackground(matrices);
			super.render(matrices, mouseX, mouseY, delta);
			serverList.render(matrices, font);
			localList.render(matrices, font);
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
		localList.setData(modObjectsList);

		this.hasChanges = hasChanges;
		buttonDiscardChanges.active = hasChanges;
	}
}
