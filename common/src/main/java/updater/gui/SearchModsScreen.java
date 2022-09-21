package updater.gui;

import com.jonafanho.apitools.Mod;
import com.jonafanho.apitools.ModLoader;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.Util;
import updater.Config;
import updater.Keys;
import updater.mappings.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SearchModsScreen extends AddFromLinkScreen {

	private final String minecraftVersion;
	private final ModLoader modLoader;
	private final DashboardList modsList;
	private final List<Mod> modsData = new ArrayList<>();

	private static final String FEATURED = "jonafanho";

	public SearchModsScreen(String minecraftVersion, ModLoader modLoader, ConfigScreen configScreen) {
		super(configScreen, false, Text.translatable("gui.updater.browse_mods"), Text.translatable("gui.updater.search"));
		this.minecraftVersion = minecraftVersion;
		this.modLoader = modLoader;

		modsList = new DashboardList((data, index) -> {
			Config.addModObject(modsData.get(index));
			setData();
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
	}

	@Override
	protected void init() {
		super.init();
		updatePositions();
		modsList.y = getYOffset();
		modsList.width = width - SQUARE_SIZE * 2;
		modsList.height = height - SQUARE_SIZE - getYOffset();
		modsList.init(this::addDrawableChild);
	}

	@Override
	public void tick() {
		super.tick();
		modsList.tick();
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

	@Override
	protected void renderAdditional(PoseStack matrices) {
		modsList.render(matrices, font);
	}

	@Override
	protected void onClickBeforeThread() {
		modsData.clear();
		updatePositions();
	}

	@Override
	protected void onClick(String text) {
		final List<Mod> tempMods = Mod.searchMods(text, minecraftVersion, modLoader, Keys.CURSE_FORGE_KEY);
		tempMods.sort((a, b) -> a.authors.contains(FEATURED) ? -1 : b.authors.contains(FEATURED) ? 1 : a.compareTo(b));
		if (minecraft != null) {
			minecraft.execute(() -> {
				modsData.addAll(tempMods);
				setData();
				setMessage(modsData.isEmpty() ? Text.translatable("gui.updater.no_results", text) : null);
			});
		}
	}

	private void updatePositions() {
		modsList.x = modsData.isEmpty() ? width : SQUARE_SIZE;
	}

	private void setData() {
		modsList.setData(modsData.stream().map(mod -> new DashboardList.Data((Config.containsMod(mod) ? String.format("(%s) ", Text.translatable("gui.updater.added").getString()) : "") + mod.name.trim(), mod.description, Text.translatable("gui.updater.mod_details", mod.downloads, mod.dateModified).getString())).collect(Collectors.toList()));
		updatePositions();
	}
}
