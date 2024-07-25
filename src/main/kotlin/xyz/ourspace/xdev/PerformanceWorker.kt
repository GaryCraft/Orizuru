package xyz.ourspace.xdev

import me.lucko.spark.api.Spark
import me.lucko.spark.api.SparkProvider
import me.lucko.spark.api.statistic.StatisticWindow
import me.lucko.spark.api.statistic.StatisticWindow.TicksPerSecond
import me.lucko.spark.api.statistic.types.DoubleStatistic
import org.bukkit.Bukkit
import xyz.ourspace.xdev.types.PerformanceData
import xyz.ourspace.xdev.types.ServerPlayerStats
import xyz.ourspace.xdev.utils.Logger


class PerformanceWorker {
	companion object {
		fun start() {
			val intervalMinutes = Orizuru.instance.config.getLong("metrics.interval")
			// Declare Spark
			var spark: Spark? = null
			var tries = 0
			// Declare handles for statistics
			var tpsI: DoubleStatistic<TicksPerSecond>? = null
			var cpuUsageI: DoubleStatistic<StatisticWindow.CpuUsage>? = null
			// Start scheduled task
			var handle: Int? = null
			handle = Bukkit.getScheduler().scheduleSyncRepeatingTask(Orizuru.instance, Runnable {
				// Try to assign spark
				if (spark == null) {
					runCatching {
						spark = SparkProvider.get()
					}.onFailure {
						// Increment tries
						tries++
						// if too many tries, cancel this task
						if (tries >= 5) {
							Logger.consoleLogError("Spark is not installed, performance metrics will not be posted")
							handle?.let { Bukkit.getScheduler().cancelTask(it) }
							return@Runnable
						}
						Logger.consoleLogWarning("Spark is not initialized, retrying later")
						return@Runnable
					}
				}
				// Spark should never be null here
				if (spark == null) {
					return@Runnable
				}
				tpsI = spark?.tps()
				cpuUsageI = spark?.cpuProcess()
				if (tpsI == null || cpuUsageI == null) {
					return@Runnable
				}
				val tps = tpsI!!.poll()
				val memory = Runtime.getRuntime().freeMemory() / 1024 / 1024
				val maxMemory = Runtime.getRuntime().maxMemory() / 1024 / 1024
				val usedMemory = maxMemory - memory
				val memPercent = usedMemory * 100 / maxMemory
				val cpuUsage: Double = cpuUsageI!!.poll(StatisticWindow.CpuUsage.MINUTES_1) * 100
				val playerStats = ServerPlayerStats(
						Bukkit.getOnlinePlayers().size,
						Bukkit.getMaxPlayers()
				)
				val data = PerformanceData(
						playerStats,
						tps,
						memory,
						maxMemory,
						usedMemory,
						memPercent,
						cpuUsage,
				)
				Orizuru.instance.connection.postAsync(
						"Performance Metrics for the last $intervalMinutes minutes",
						OrizContentType.PERFORMANCE,
						data
				)
				// Logger.consoleLog("Posted performance metrics")
				// Wait 20 seconds before first run, then run every intervalMinutes minutes
			}, 20 * 20, intervalMinutes * 20 * 60)

		}
	}
}