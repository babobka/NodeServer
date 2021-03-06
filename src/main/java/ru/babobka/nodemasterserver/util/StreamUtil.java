package ru.babobka.nodemasterserver.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import org.json.JSONObject;

import ru.babobka.nodemasterserver.classloader.JarClassLoader;

import ru.babobka.subtask.model.SubTask;

/**
 * Created by dolgopolov.a on 08.07.15.
 */

public final class StreamUtil {

	private StreamUtil() {

	}

	public static String getLocalResourcePath(Class<?> clazz,
			String resourceName) {
		return clazz.getClassLoader().getResource(resourceName).getPath();
	}

	public static InputStream getLocalResource(Class<?> clazz,
			String resourceName) {
		return clazz.getClassLoader().getResourceAsStream(resourceName);
	}

	public static String readFile(InputStream is) {

		try (Scanner scanner = new Scanner(is);
				Scanner delimitedScanner = scanner.useDelimiter("\\A");) {
			return scanner.hasNext() ? scanner.next() : "";
		}
	}

	public static String readFile(File file) {
		String content = null;
		try (FileReader reader = new FileReader(file);) {
			char[] chars = new char[(int) file.length()];
			reader.read(chars);
			content = new String(chars);
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return content;
	}

	public static String readFile(String filePath) {
		return readFile(new File(filePath));
	}

	public static List<String> getJarFileListFromFolder(String folderPath) {
		File folder = new File(folderPath);
		File[] listOfFiles = folder.listFiles();
		LinkedList<String> files = new LinkedList<>();
		if (listOfFiles != null) {
			for (int i = 0; i < listOfFiles.length; i++) {
				if (listOfFiles[i].isFile()
						&& listOfFiles[i].getAbsolutePath().endsWith(".jar")) {
					files.add(listOfFiles[i].getName());
				}
			}
		}
		return files;
	}

	public static String getRunningFolder() throws URISyntaxException {
		String folder = new File(StreamUtil.class.getProtectionDomain()
				.getCodeSource().getLocation().toURI().getPath()).toString();
		if (folder.endsWith(".jar")) {
			folder = folder.substring(0, folder.lastIndexOf(File.separator));
		}
		return folder;
	}

	public static JSONObject getConfigJson(String jarFilePath)
			throws IOException {
		return new JSONObject(readTextFileFromJar(jarFilePath, "task.json"));
	}

	public static String readTextFileFromJar(String jarFilePath,
			String fileName) throws IOException {
		URL url = new URL("jar:file:" + jarFilePath + "!/" + fileName);
		InputStream is = url.openStream();
		return readFile(is);
	}

	public static SubTask getTaskClassFromJar(String jarFilePath,
			String className) throws IOException {
		try {
			JarClassLoader jarLoader = new JarClassLoader(jarFilePath);
			return (SubTask) (jarLoader.loadClass(className, true)
					.newInstance());
		} catch (Exception e) {
			throw new IOException("Can not get " + className, e);
		}
	}

	public static void sendObject(Object object, Socket socket)
			throws IOException {

		byte[] message = objectToByteArray(object);
		DataOutputStream dOut = new DataOutputStream(socket.getOutputStream());
		dOut.writeInt(message.length); // write length of the message
		dOut.write(message);
		socket.getOutputStream().flush();

	}

	@SuppressWarnings("unchecked")
	public static <T> T receiveObject(Socket socket) throws IOException {

		DataInputStream dIn = new DataInputStream(socket.getInputStream());
		int length = dIn.readInt();
		if (length > 0) {
			byte[] message = new byte[length];
			dIn.readFully(message, 0, message.length);
			return (T) byteArrayToObject(message);
		}
		return null;

	}

	private static Object byteArrayToObject(byte[] byteArray)
			throws IOException {
		try (ByteArrayInputStream bis = new ByteArrayInputStream(byteArray);
				ObjectInput in = new ObjectInputStream(bis);) {
			return in.readObject();
		} catch (ClassNotFoundException e) {
			throw new IOException(e);
		}
	}

	private static byte[] objectToByteArray(Object object) throws IOException {

		try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
				ObjectOutput out = new ObjectOutputStream(bos);) {
			out.writeObject(object);
			return bos.toByteArray();
		}
	}

}