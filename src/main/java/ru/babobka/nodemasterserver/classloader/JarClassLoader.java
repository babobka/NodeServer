package ru.babobka.nodemasterserver.classloader;

import java.io.IOException;

/**
 * Created by dolgopolov.a on 12.12.15.
 */
public class JarClassLoader extends MultiClassLoader {
	private JarResource jarResources;

	public JarClassLoader(String jarName) throws IOException {
		// Create the JarResource and suck in the jar file.
		jarResources = new JarResource(jarName);
	}

	protected byte[] loadClassBytes(String className) {
		// Support the MultiClassLoader's class name munging facility.
		String localClassName = formatClassName(className);
		// Attempt to get the class data from the JarResource.
		return (jarResources.getResource(localClassName));
	}
}
