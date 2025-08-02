package com.victory.poolassistant.core;

import android.util.Log;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Professional Logging System
 * Provides structured logging with file output and filtering
 */
public class Logger {
    
    private static final String TAG_PREFIX = "PoolAssistant";
    private static boolean debugMode = false;
    private static boolean fileLoggingEnabled = false;
    private static File logFile;
    private static final SimpleDateFormat dateFormat = 
        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
    
    // Log levels
    public static final int VERBOSE = Log.VERBOSE;
    public static final int DEBUG = Log.DEBUG;
    public static final int INFO = Log.INFO;
    public static final int WARN = Log.WARN;
    public static final int ERROR = Log.ERROR;
    
    /**
     * Initialize logger
     */
    public static void initialize(boolean debug) {
        debugMode = debug;
        
        if (debug) {
            enableFileLogging();
        }
        
        i("Logger", "Logger initialized - Debug: " + debug);
    }
    
    /**
     * Enable file logging
     */
    private static void enableFileLogging() {
        try {
            File logDir = new File("/data/data/com.victory.poolassistant/logs");
            if (!logDir.exists()) {
                logDir.mkdirs();
            }
            
            String fileName = "pool_assistant_" + 
                new SimpleDateFormat("yyyy_MM_dd", Locale.getDefault()).format(new Date()) + 
                ".log";
            
            logFile = new File(logDir, fileName);
            fileLoggingEnabled = true;
            
        } catch (Exception e) {
            Log.w(TAG_PREFIX, "Failed to enable file logging: " + e.getMessage());
        }
    }
    
    /**
     * Verbose log
     */
    public static void v(String tag, String message) {
        if (debugMode) {
            Log.v(formatTag(tag), message);
            writeToFile(VERBOSE, tag, message);
        }
    }
    
    /**
     * Debug log
     */
    public static void d(String tag, String message) {
        if (debugMode) {
            Log.d(formatTag(tag), message);
            writeToFile(DEBUG, tag, message);
        }
    }
    
    /**
     * Info log
     */
    public static void i(String tag, String message) {
        Log.i(formatTag(tag), message);
        writeToFile(INFO, tag, message);
    }
    
    /**
     * Warning log
     */
    public static void w(String tag, String message) {
        Log.w(formatTag(tag), message);
        writeToFile(WARN, tag, message);
    }
    
    /**
     * Warning log with throwable
     */
    public static void w(String tag, String message, Throwable throwable) {
        Log.w(formatTag(tag), message, throwable);
        writeToFile(WARN, tag, message + "\n" + Log.getStackTraceString(throwable));
    }
    
    /**
     * Error log
     */
    public static void e(String tag, String message) {
        Log.e(formatTag(tag), message);
        writeToFile(ERROR, tag, message);
    }
    
    /**
     * Error log with throwable
     */
    public static void e(String tag, String message, Throwable throwable) {
        Log.e(formatTag(tag), message, throwable);
        writeToFile(ERROR, tag, message + "\n" + Log.getStackTraceString(throwable));
    }
    
    /**
     * Performance log (for timing operations)
     */
    public static void perf(String tag, String operation, long startTime) {
        long duration = System.currentTimeMillis() - startTime;
        d(tag, "PERF: " + operation + " took " + duration + "ms");
    }
    
    /**
     * Memory log (for memory usage)
     */
    public static void memory(String tag, String context) {
        if (debugMode) {
            Runtime runtime = Runtime.getRuntime();
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            long maxMemory = runtime.maxMemory();
            
            d(tag, "MEMORY: " + context + 
                " - Used: " + formatBytes(usedMemory) + 
                " / Max: " + formatBytes(maxMemory) +
                " (" + (usedMemory * 100 / maxMemory) + "%)");
        }
    }
    
    /**
     * Network log (for network operations)
     */
    public static void network(String tag, String method, String url, int responseCode, long duration) {
        d(tag, "NETWORK: " + method + " " + url + 
            " - " + responseCode + " (" + duration + "ms)");
    }
    
    /**
     * User action log (for analytics)
     */
    public static void action(String tag, String action, String details) {
        i(tag, "ACTION: " + action + 
            (details != null ? " - " + details : ""));
    }
    
    /**
     * Security log (for security events)
     */
    public static void security(String tag, String event, String details) {
        w(tag, "SECURITY: " + event + 
            (details != null ? " - " + details : ""));
    }
    
    /**
     * Format tag with prefix
     */
    private static String formatTag(String tag) {
        return TAG_PREFIX + "-" + tag;
    }
    
    /**
     * Write log to file
     */
    private static void writeToFile(int level, String tag, String message) {
        if (!fileLoggingEnabled || logFile == null) return;
        
        try {
            String timestamp = dateFormat.format(new Date());
            String levelStr = getLevelString(level);
            String logLine = timestamp + " " + levelStr + " " + formatTag(tag) + ": " + message + "\n";
            
            FileWriter writer = new FileWriter(logFile, true);
            writer.write(logLine);
            writer.close();
            
        } catch (IOException e) {
            // Silent fail to avoid infinite recursion
        }
    }
    
    /**
     * Get level string
     */
    private static String getLevelString(int level) {
        switch (level) {
            case VERBOSE: return "V";
            case DEBUG: return "D";
            case INFO: return "I";
            case WARN: return "W";
            case ERROR: return "E";
            default: return "?";
        }
    }
    
    /**
     * Format bytes to human readable
     */
    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
        if (bytes < 1024 * 1024 * 1024) return (bytes / (1024 * 1024)) + " MB";
        return (bytes / (1024 * 1024 * 1024)) + " GB";
    }
    
    /**
     * Clear log files
     */
    public static void clearLogs() {
        try {
            File logDir = new File("/data/data/com.victory.poolassistant/logs");
            if (logDir.exists()) {
                File[] files = logDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        file.delete();
                    }
                }
            }
            i("Logger", "Log files cleared");
        } catch (Exception e) {
            e("Logger", "Failed to clear logs", e);
        }
    }
    
    /**
     * Get log files info
     */
    public static String getLogInfo() {
        try {
            File logDir = new File("/data/data/com.victory.poolassistant/logs");
            if (!logDir.exists()) return "No log directory";
            
            File[] files = logDir.listFiles();
            if (files == null || files.length == 0) return "No log files";
            
            StringBuilder info = new StringBuilder();
            info.append("Log files (").append(files.length).append("):\n");
            
            for (File file : files) {
                info.append("- ").append(file.getName())
                    .append(" (").append(formatBytes(file.length())).append(")\n");
            }
            
            return info.toString();
            
        } catch (Exception e) {
            return "Error getting log info: " + e.getMessage();
        }
    }
}