package updater;

import net.minecraftforge.fml.common.Mod;

@Mod(Updater.MOD_ID)
public class UpdaterForge {

	public UpdaterForge() {
		Updater.init();
	}
}
