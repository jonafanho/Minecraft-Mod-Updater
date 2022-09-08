package updater;

import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import net.fabricmc.loader.impl.FabricLoaderImpl;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;

public class UpdaterFabric implements PreLaunchEntrypoint {

	@Override
	public void onPreLaunch() {
		final FabricLoaderImpl fabricLoader = FabricLoaderImpl.INSTANCE;
		Updater.init(FabricLauncherBase.getLauncher().getClassPath(), fabricLoader.getLaunchArguments(false), fabricLoader.tryGetGameProvider().getRawGameVersion(), Downloader.ModLoader.FABRIC, fabricLoader.getGameDir());
	}
}
