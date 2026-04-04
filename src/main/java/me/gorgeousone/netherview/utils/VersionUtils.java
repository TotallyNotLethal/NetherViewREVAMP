package me.gorgeousone.netherview.utils;

import org.bukkit.Bukkit;

public final class VersionUtils {
	
	private VersionUtils() {}
	
	public static final String VERSION_STRING = getServerVersionToken();
	private static final int[] CURRENT_VERSION_INTS;
	
	static {
		CURRENT_VERSION_INTS = getVersionAsIntArray(Bukkit.getMinecraftVersion(), "\\.");
	}
	
	public static final boolean IS_LEGACY_SERVER = !serverIsAtOrAbove("1.13.0");
	
	public static boolean isVersionLowerThan(String currentVersion, String requestedVersion) {
		
		int[] currentVersionInts = getVersionAsIntArray(currentVersion, "\\.");
		int[] requestedVersionInts = getVersionAsIntArray(requestedVersion, "\\.");
		
		for (int i = 0; i < Math.min(currentVersionInts.length, requestedVersionInts.length); i++) {
			
			int versionDiff = currentVersionInts[i] - requestedVersionInts[i];
			
			if (versionDiff > 0) {
				return false;
			}else if (versionDiff < 0) {
				return true;
			}
		}
		
		return requestedVersionInts.length > currentVersionInts.length;
	}
	
	public static boolean serverIsAtOrAbove(String requestedVersion) {
		
		int[] requestedVersionInts = getVersionAsIntArray(requestedVersion, "\\.");
		
		for (int i = 0; i < Math.max(requestedVersionInts.length, CURRENT_VERSION_INTS.length); i++) {
			
			int requestedPart = i < requestedVersionInts.length ? requestedVersionInts[i] : 0;
			int currentPart = i < CURRENT_VERSION_INTS.length ? CURRENT_VERSION_INTS[i] : 0;
			int versionDiff = requestedPart - currentPart;
			
			if (versionDiff > 0) {
				return false;
			}else if (versionDiff < 0){
				return true;
			}
		}
		
		return true;
	}
	
	private static int[] getVersionAsIntArray(String version, String delimiter) {
		
		String[] split = version.split(delimiter);
		int[] versionInts = new int[split.length];
		
		for (int i = 0; i < versionInts.length; i++) {
			versionInts[i] = Integer.parseInt(split[i]);
		}
		
		return versionInts;
	}

	private static String getServerVersionToken() {

		String[] splitClassName = Bukkit.getServer().getClass().getName().split("\\.");
		return splitClassName.length > 3 ? splitClassName[3] : "UNKNOWN";
	}
}
