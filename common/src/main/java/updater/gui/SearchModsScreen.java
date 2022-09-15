package updater.gui;

import com.jonafanho.apitools.Mod;
import com.jonafanho.apitools.ModLoader;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import updater.Config;
import updater.Keys;
import updater.mappings.ScreenMapper;
import updater.mappings.Text;
import updater.mappings.UtilitiesClient;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SearchModsScreen extends ScreenMapper implements IGui {

	private Component message = Text.translatable("gui.updater.browse_mods");

	private final ConfigScreen configScreen;
	private final EditBox textFieldSearch;
	private final Button buttonSearch;
	private final DashboardList modsList;
	private final List<Mod> modsData = new ArrayList<>();

	public SearchModsScreen(String minecraftVersion, ModLoader modLoader, ConfigScreen configScreen) {
		super(Text.literal(""));
		this.configScreen = configScreen;

		textFieldSearch = new EditBox(Minecraft.getInstance().font, 0, 0, 0, SQUARE_SIZE, Text.literal(""));
		modsList = new DashboardList((data, index) -> {
			Config.addModObject(modsData.get(index));
			configScreen.updateListData(true);
			onClose();
		}, (data, index) -> modsData.get(index).modIds.forEach(modId -> {
			switch (modId.modProvider) {
				case CURSE_FORGE:
					Util.getPlatform().openUri(String.format("https://minecraft.curseforge.com/projects/%s", modId.modId));
					break;
				case MODRINTH:
					Util.getPlatform().openUri(String.format("https://modrinth.com/mod/%s", modId.modId));
					break;
			}
		}), "+", 3);
		buttonSearch = new Button(0, 0, 0, SQUARE_SIZE, Text.translatable("gui.updater.search"), button -> {
			final String query = textFieldSearch.getValue();
			modsData.clear();
			modsData.addAll(Mod.searchMods(query, minecraftVersion, modLoader, Keys.CURSE_FORGE_KEY));
			modsList.setData(modsData.stream().map(mod -> new DashboardList.Data(mod.name, mod.description, Text.translatable("gui.updater.mod_details", mod.downloads, mod.dateModified).getString())).collect(Collectors.toList()));
			updatePositions();
			message = Text.translatable("gui.updater.no_results", query);
		});
	}

	@Override
	protected void init() {
		super.init();

		IGui.setPositionAndWidth(textFieldSearch, SQUARE_SIZE + TEXT_FIELD_PADDING / 2, SQUARE_SIZE + TEXT_FIELD_PADDING / 2, width - SQUARE_SIZE * 5 - TEXT_FIELD_PADDING);
		IGui.setPositionAndWidth(buttonSearch, width - SQUARE_SIZE * 4, SQUARE_SIZE + TEXT_FIELD_PADDING / 2, SQUARE_SIZE * 3);

		textFieldSearch.setResponder(text -> setSearchButtonActive());
		setSearchButtonActive();
		updatePositions();

		modsList.y = SQUARE_SIZE * 2 + TEXT_FIELD_PADDING;
		modsList.width = width - SQUARE_SIZE * 2;
		modsList.height = height - SQUARE_SIZE * 3 - TEXT_FIELD_PADDING;

		addDrawableChild(textFieldSearch);
		addDrawableChild(buttonSearch);
		modsList.init(this::addDrawableChild);
	}

	@Override
	public void render(PoseStack matrices, int mouseX, int mouseY, float delta) {
		try {
			renderBackground(matrices);
			modsList.render(matrices, font);
			super.render(matrices, mouseX, mouseY, delta);
			if (modsData.isEmpty()) {
				font.drawShadow(matrices, message, SQUARE_SIZE, SQUARE_SIZE * 2 + TEXT_FIELD_PADDING + TEXT_PADDING, ARGB_WHITE);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void tick() {
		textFieldSearch.tick();
		modsList.tick();
	}

	@Override
	public void onClose() {
		super.onClose();
		if (minecraft != null) {
			UtilitiesClient.setScreen(minecraft, configScreen);
		}
	}

	@Override
	public void mouseMoved(double mouseX, double mouseY) {
		modsList.mouseMoved(mouseX, mouseY);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
		modsList.mouseScrolled(mouseX, mouseY, amount);
		return super.mouseScrolled(mouseX, mouseY, amount);
	}

	private void setSearchButtonActive() {
		buttonSearch.active = !textFieldSearch.getValue().isEmpty();
	}

	private void updatePositions() {
		modsList.x = modsData.isEmpty() ? width : SQUARE_SIZE;
	}
}
