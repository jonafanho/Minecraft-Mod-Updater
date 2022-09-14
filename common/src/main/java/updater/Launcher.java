package updater;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Launcher {

	public static void launch(List<Path> classPath, String[] launchArguments, Path gameDirectory) {
		final File lastPidFile = gameDirectory.resolve(Updater.MODS_TEMP_DIRECTORY).resolve("pid.txt").toFile();
		try {
			final long lastPid = Long.parseLong(FileUtils.readFileToString(lastPidFile, StandardCharsets.UTF_8));
			FileUtils.deleteQuietly(lastPidFile);
			final String jvmName = ManagementFactory.getRuntimeMXBean().getName();
			final long thisPid = Long.parseLong(jvmName.substring(0, jvmName.indexOf('@')));
			if (lastPid == thisPid) {
				Updater.LOGGER.info("Skipping Minecraft relaunch");
				return;
			} else {
				Updater.LOGGER.info("Running Minecraft with process ID " + thisPid);
			}
		} catch (Exception ignored) {
		}

		String className = null;
		for (final StackTraceElement stackTraceElement : Thread.currentThread().getStackTrace()) {
			if (stackTraceElement.getMethodName().equals("main")) {
				className = stackTraceElement.getClassName();
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
					checkForSpace(System.getProperty("java.home") + "/bin/javaw"),
					runtimeMXBean.getInputArguments().stream().map(Launcher::checkForSpaceAfterEquals).collect(Collectors.joining(" ")),
					classPath.stream().map(path -> checkForSpace(path.toString())).collect(Collectors.joining(";")),
					className,
					Arrays.stream(launchArguments).map(Launcher::checkForSpace).collect(Collectors.joining(" "))
			)).replace(formatPath(oldLibraryPath.toString()), formatPath(newLibraryPath.toString()));

			try {
				final long pid = Runtime.getRuntime().exec(command).pid();
				FileUtils.writeStringToFile(lastPidFile, String.valueOf(pid), StandardCharsets.UTF_8);
				Updater.LOGGER.info("Restarting Minecraft with process ID " + pid);
			} catch (Exception e) {
				e.printStackTrace();
			}
			System.exit(0);
		}
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
