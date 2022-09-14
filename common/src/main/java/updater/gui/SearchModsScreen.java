package updater.gui;

import updater.mappings.ScreenMapper;
import updater.mappings.Text;
import updater.mappings.UtilitiesClient;

public class SearchModsScreen extends ScreenMapper {

	private final ConfigScreen configScreen;

	public SearchModsScreen(ConfigScreen configScreen) {
		super(Text.literal(""));
		this.configScreen = configScreen;
	}

	@Override
	public void onClose() {
		super.onClose();
		if (minecraft != null) {
			UtilitiesClient.setScreen(minecraft, configScreen);
		}
	}
}
