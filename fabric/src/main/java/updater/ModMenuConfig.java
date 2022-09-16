package updater;

import com.jonafanho.apitools.ModLoader;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.loader.impl.FabricLoaderImpl;
import updater.gui.ConfigScreen;

public class ModMenuConfig implements ModMenuApi {

	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		final FabricLoaderImpl fabricLoader = FabricLoaderImpl.INSTANCE;
		return parent -> new ConfigScreen(UpdaterFabric.getLaunch(), fabricLoader.tryGetGameProvider().getRawGameVersion(), ModLoader.FABRIC, fabricLoader.getGameDir());
	}
}
