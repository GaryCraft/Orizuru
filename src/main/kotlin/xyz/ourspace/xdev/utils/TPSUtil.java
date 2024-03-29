package xyz.ourspace.xdev.utils;

import org.bukkit.Bukkit;
import org.bukkit.Server;

import java.lang.reflect.Field;

/* Code from MagicTPS  (https://github.com/ChaosPVP/MagicTPS) */
public class TPSUtil {
    private static Object minecraftServer;
    private static Field recentTps;
    private static Boolean isPaperSpigot = null;

    public static double[] getRecentTps() {
        /*if (isPaperSpigot == null) {
            try {
                double[] recentTps = getRecentTpsPaperSpigot();
                isPaperSpigot = true;
                Logger.INSTANCE.consoleLog("Using PaperSpigot-based tps measurement");
                return recentTps;
            } catch (Throwable t) {
                isPaperSpigot = false;
                Logger.INSTANCE.consoleLog("Using reflection-based tps measurement");
            }
        }*/
       /* if (isPaperSpigot) {
            try {
                return getRecentTpsPaperSpigot();
            } catch (Throwable throwable) {
                return new double[]{20, 20, 20};
            }
        } else {*/
            try {
                return getRecentTpsRefl();
            } catch (Throwable throwable) {
                return new double[]{20, 20, 20};
            }
        //}
    }

   /* private static double[] getRecentTpsPaperSpigot() throws Throwable {
        //suppress cannot find symbol warning
        //noinspection ConstantConditions
        return Bukkit.getServer().spigot().getTPS();
    }*/

    private static double[] getRecentTpsRefl() throws Throwable {
        if (minecraftServer == null) {
            Server server = Bukkit.getServer();
            Field consoleField = server.getClass().getDeclaredField("console");
            consoleField.setAccessible(true);
            minecraftServer = consoleField.get(server);
        }
        if (recentTps == null) {
            recentTps = minecraftServer.getClass().getSuperclass().getDeclaredField("recentTps");
            recentTps.setAccessible(true);
        }
        return (double[]) recentTps.get(minecraftServer);
    }
}