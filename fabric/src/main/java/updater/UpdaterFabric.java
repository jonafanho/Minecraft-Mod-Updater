package updater;

import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

public class UpdaterFabric implements PreLaunchEntrypoint {

	@Override
	public void onPreLaunch() {
		Updater.init();
	}
}
