package updater;

public enum Loader {

	FABRIC("fabric", "net.fabricmc.fabric-loader"),
	FORGE("forge", "net.minecraftforge"),
	QUILT("quilt", "org.quiltmc.quilt-loader");

	public final String loader;
	public final String className;

	Loader(String loader, String className) {
		this.loader = loader;
		this.className = className;
	}
}
