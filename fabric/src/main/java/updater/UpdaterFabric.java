package updater;

import com.jonafanho.apitools.ModLoader;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import net.fabricmc.loader.impl.FabricLoaderImpl;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;

public class UpdaterFabric implements PreLaunchEntrypoint {

	@Override
	public void onPreLaunch() {
		final FabricLoaderImpl fabricLoader = FabricLoaderImpl.INSTANCE;
		Updater.init(getLaunch(), fabricLoader.tryGetGameProvider().getRawGameVersion(), ModLoader.FABRIC, fabricLoader.getGameDir());
	}

	public static Runnable getLaunch() {
		final FabricLoaderImpl fabricLoader = FabricLoaderImpl.INSTANCE;
		return () -> Launcher.launch(FabricLauncherBase.getLauncher().getClassPath(), fabricLoader.getLaunchArguments(false), fabricLoader.getEnvironmentType() == EnvType.SERVER);
	}
}
