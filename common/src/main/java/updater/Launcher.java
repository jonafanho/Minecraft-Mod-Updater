package updater;

import com.jonafanho.apitools.ModLoader;
import org.apache.commons.io.FileUtils;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Launcher {

	private static final List<Runnable> CALLBACKS = new ArrayList<>();

	public static void launch(List<Path> classPath, String[] launchArguments, boolean isServer) {
		if (isServer) {
			Updater.LOGGER.info("Please restart the server to apply mod updates!");
			System.exit(0);
		}

		String className = null;
		for (final StackTraceElement stackTraceElement : Thread.currentThread().getStackTrace()) {
			className = stackTraceElement.getClassName();
			if (stackTraceElement.getMethodName().equals("main") && className.contains(ModLoader.FABRIC.name)) {
				break;
			}
		}

		if (className != null) {
			final RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
			final Path oldLibraryPath = Paths.get(runtimeMXBean.getLibraryPath());
			final Path newLibraryPath = oldLibraryPath.getParent().resolve("new-natives-from-updater");

			try {
				FileUtils.copyDirectory(oldLibraryPath.toFile(), newLibraryPath.toFile());
			} catch (Exception e) {
				e.printStackTrace();
			}

			final String command = formatPath(String.format(
					"%s %s -cp %s %s %s",
					checkForSpace(System.getProperty("java.home") + "/bin/java"),
					runtimeMXBean.getInputArguments().stream().map(Launcher::checkForSpaceAfterEquals).collect(Collectors.joining(" ")),
					classPath.stream().map(path -> checkForSpace(path.toString())).collect(Collectors.joining(";")),
					className,
					Arrays.stream(launchArguments).map(Launcher::checkForSpace).collect(Collectors.joining(" "))
			)).replace(formatPath(oldLibraryPath.toString()), formatPath(newLibraryPath.toString()));

			try {
				Runtime.getRuntime().exec(command);
				Updater.LOGGER.info("Restarting Minecraft with command:\n" + command);
			} catch (Exception e) {
				e.printStackTrace();
			}
			CALLBACKS.forEach(Runnable::run);
			System.exit(0);
		}
	}

	public static void addCallback(Runnable callback) {
		CALLBACKS.add(callback);
	}

	private static String checkForSpaceAfterEquals(String argument) {
		final int index = argument.indexOf("=");
		return index < 0 ? argument : argument.substring(0, index + 1) + checkForSpace(argument.substring(index + 1));
	}

	private static String checkForSpace(String argument) {
		return argument.contains(" ") ? "\"" + argument + "\"" : argument;
	}

	private static String formatPath(String text) {
		return text.replace("\\", "/");
	}
}
