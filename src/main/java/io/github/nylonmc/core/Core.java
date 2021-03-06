package io.github.nylonmc.core;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.stream.Stream;

import org.beeware.rubicon.Python;
import org.beeware.rubicon.Python.Remapper;

import io.github.nylonmc.libreflectionremapper.LibReflectionRemapper;

public class Core {
	public static final String MODID = "nylonmc-core";

	public static final String NYLONMC_FOLDER = FabricLoader.getInstance().getGameDir().toString() + File.separator
			+ "nylonmc";
	public static final String NYLONMC_PATH_FOLDER = NYLONMC_FOLDER + File.separator + "python_path";
	public static final String NYLONMC_NATIVE_FOLDER = NYLONMC_FOLDER + File.separator + "native";

	private static final Remapper REMAPPER = new Remapper () {
		public String field(Class cls, String name) {
			try {
				return LibReflectionRemapper.getFieldName(cls, name);
			} catch (Exception e) {
				return name;
			}
		}
        public String method(Class cls, String name) {
			try {
				return LibReflectionRemapper.getFieldName(cls, name);
			} catch (Exception e) {
				return name;
			}
		}
	};

	static {
		rebuildNylonmcDirectory();
		NativeHack.addNativePath(NYLONMC_NATIVE_FOLDER);
		Python.setRemapper(REMAPPER);
		PythonThread.init();
		System.out.println("Python Loaded");
	}

	/**
	 * Runs a python module's main() function. Using this method ensures python will be statically
	 * loaded.
	 * 
	 * @param module Module to run __main__ of
	 */
	public static void runPythonModule(String module) {
		PythonThread.runPython("import importlib\n" +
							   "the_module = importlib.import_module(\"" + module + "\")\n" +
							   "the_module.main()");
	}

	private static void rebuildNylonmcDirectory() {
		try {
			// Add Natives If Absent
			// todo Update Mechinism
			Path a = Paths.get(NYLONMC_NATIVE_FOLDER + File.separator + "librubicon.so");
			if (!Files.isRegularFile(a)) {
				Path b = FabricLoader.getInstance().getModContainer(MODID).get().getPath("librubicon.so");
				Files.createDirectories(Paths.get(NYLONMC_NATIVE_FOLDER));
				Files.copy(b, a);
			}
			// Delete old python files
			// todo Versioning System For Production So We Don't Rebuild The Entire
			yeetDirectoryIfExists(Paths.get(NYLONMC_PATH_FOLDER));
			// Rebuild Python Path
			Files.createDirectories(Paths.get(NYLONMC_PATH_FOLDER));
			for (ModContainer modContainer : FabricLoader.getInstance().getAllMods()) {
				Path c = modContainer.getPath("python");
				if (Files.isDirectory(c)) {
					//Contains Python Code
					Files.walkFileTree(c, new CopyDirVisitor(c, Paths.get(NYLONMC_PATH_FOLDER)));
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static void yeetDirectoryIfExists(Path directory) throws IOException {
		if (!Files.isDirectory(directory)) return;
		// create a stream
		Stream<Path> files = Files.walk(directory);
	
		// delete directory including files and sub-folders
		files.sorted(Comparator.reverseOrder())
				.map(Path::toFile)
				.forEach(File::deleteOnExit);
	
		// close the stream
		files.close();
	}
}
