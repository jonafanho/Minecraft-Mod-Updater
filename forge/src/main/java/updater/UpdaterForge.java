package updater;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Updater.MOD_ID)
public class UpdaterForge {

	public UpdaterForge() {
		final IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();
	}
}
