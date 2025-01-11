package keystrokesmod.script;

import keystrokesmod.Raven;
import keystrokesmod.clickgui.components.impl.CategoryComponent;
import keystrokesmod.module.Module;
import keystrokesmod.utility.NetworkUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.FMLCommonHandler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;

public class ScriptManager {
    private Minecraft mc = Minecraft.getMinecraft();
    public LinkedHashMap<Script, Module> scripts = new LinkedHashMap<>();
    public JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    public boolean deleteTempFiles = true;
    public File directory;
    public List<String> imports = Arrays.asList(Color.class.getName(), Collections.class.getName(), List.class.getName(), ArrayList.class.getName(), Arrays.class.getName(), Map.class.getName(), HashMap.class.getName(), HashSet.class.getName(), ConcurrentHashMap.class.getName(), LinkedHashMap.class.getName(), Iterator.class.getName(), Comparator.class.getName(), AtomicInteger.class.getName(), AtomicLong.class.getName(), AtomicBoolean.class.getName(), Random.class.getName(), Matcher.class.getName());
    public String tempDir = System.getProperty("java.io.tmpdir") + "cmF2ZW5fc2NyaXB0cw";
    public String b = ((String[])ScriptManager.class.getProtectionDomain().getCodeSource().getLocation().getPath().split("\\.jar!"))[0].substring(5) + ".jar";
    private Map<String, String> loadedHashes = new HashMap<>();

    public ScriptManager() {
        directory = new File(mc.mcDataDir + File.separator + "keystrokes", "scripts");
    }

    public void onEnable(Script dv) {
        if (dv.event == null) {
            dv.event = new ScriptEvents(getModule(dv));
            FMLCommonHandler.instance().bus().register(dv.event);
        }
        dv.invokeMethod("onEnable");
    }

    public Module getModule(Script dv) {
        for (Map.Entry<Script, Module> entry : this.scripts.entrySet()) {
            if (entry.getKey().equals(dv)) {
                return entry.getValue();
            }
        }
        return null;
    }

    public void loadScripts() {
        for (Module module : this.scripts.values()) {
            module.disable();
        }

        if (deleteTempFiles) {
            deleteTempFiles = false;
            final File tempDirectory = new File(tempDir);
            if (tempDirectory.exists() && tempDirectory.isDirectory()) {
                final File[] tempFiles = tempDirectory.listFiles();
                if (tempFiles != null) {
                    for (File tempFile : tempFiles) {
                        if (!tempFile.delete()) {
                            System.err.println("Failed to delete temp file: " + tempFile.getAbsolutePath());
                        }
                    }
                }
            }
        }
        else {
            if (!this.scripts.isEmpty()) {
                Iterator<Map.Entry<Script, Module>> iterator = scripts.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<Script, Module> entry = iterator.next();
                    String fileName = entry.getKey().file.getName();
                    String hash = calculateHash(entry.getKey().file);

                    String cachedHash = loadedHashes.get(fileName);
                    if (cachedHash != null && cachedHash.equals(hash) && !entry.getKey().error) {
                        continue; // no changes detected, skip loading
                    }
                    entry.getKey().delete();
                    iterator.remove();
                    loadedHashes.remove(fileName);
                }
            }
            else {
                loadedHashes.clear();
            }
        }

        final File scriptDirectory = directory;
        if (scriptDirectory.exists() && scriptDirectory.isDirectory()) {
            final File[] scriptFiles = scriptDirectory.listFiles();
            if (scriptFiles != null) {
                for (final File scriptFile : scriptFiles) {
                    if (scriptFile.isFile() && scriptFile.getName().endsWith(".java")) {
                        String fileName = scriptFile.getName();
                        String hash = calculateHash(scriptFile);

                        String cachedHash = loadedHashes.get(fileName);
                        if (cachedHash != null && cachedHash.equals(hash)) {
                            continue; // No changes detected, skip parsing
                        }
                        parseFile(scriptFile);
                        loadedHashes.put(scriptFile.getName(), hash);
                    }
                }
            }
        }
        else {
            if (scriptDirectory.mkdirs()) {
                System.out.println("Created script directory: " + scriptDirectory.getAbsolutePath());
            }
            else {
                System.err.println("Failed to create script directory: " + scriptDirectory.getAbsolutePath());
            }
        }

        for (Module module : this.scripts.values()) {
            module.disable();
        }

        for (CategoryComponent categoryComponent : Raven.clickGui.categories) {
            if (categoryComponent.categoryName == Module.category.scripts) {
                categoryComponent.reloadModules(false);
            }
        }
    }

    private boolean parseFile(final File file) {
        if (file.getName().startsWith("_") || !file.getName().endsWith(".java")) {
            return false;
        }
        String scriptName = file.getName().replace(".java", "");
        if (scriptName.isEmpty()) {
            return false;
        }
        StringBuilder scriptContents = new StringBuilder();
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                scriptContents.append(line).append("\n");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if (scriptContents.length() == 0) {
            return false;
        }
        List<String> topLevelLines = Utils.getTopLevelLines(scriptContents.toString());
        for (String line : topLevelLines) {
            if (line.startsWith("decrypt - \"") && line.endsWith("\"")) {
                String encryptedBase64 = line.substring("decrypt - \"".length(), line.length() - 1);
                String decryptedContent = Utils.decrypt(encryptedBase64);
                if (decryptedContent.isEmpty()) {
                    break;
                }
                int decryptIndex = scriptContents.indexOf(line);
                if (decryptIndex != -1) {
                    scriptContents.replace(decryptIndex, decryptIndex + line.length(), decryptedContent);
                }
            }
            if (line.startsWith("load - \"") && line.endsWith("\"")) {
                String url = line.substring("load - \"".length(), line.length() - 1);
                String externalContents = NetworkUtils.getTextFromURL(url, true);
                if (externalContents.isEmpty()) {
                    break;
                }
                int loadIndex = scriptContents.indexOf(line);
                if (loadIndex != -1) {
                    scriptContents.replace(loadIndex, loadIndex + line.length(), externalContents);
                }
            }
        }
        Script script = new Script(scriptName);
        script.file = file;
        script.createScript(scriptContents.toString());
        script.run();
        Module module = new Module(script);
        Raven.scriptManager.scripts.put(script, module);
        Raven.scriptManager.invoke("onLoad", module);
        return !script.error;
    }


    public void onDisable(Script script) {
        if (script.event != null) {
            FMLCommonHandler.instance().bus().unregister(script.event);
            script.event = null;
        }
        script.invokeMethod("onDisable");
    }

    public void invoke(String methodName, Module module, final Object... args) {
        for (Map.Entry<Script, Module> entry : this.scripts.entrySet()) {
            if (((entry.getValue().canBeEnabled() && entry.getValue().isEnabled()) || methodName.equals("onLoad")) && entry.getValue().equals(module)) {
                entry.getKey().invokeMethod(methodName, args);
            }
        }
    }

    public int invokeBoolean(String methodName, Module module, final Object... args) {
        for (Map.Entry<Script, Module> entry : this.scripts.entrySet()) {
            if (entry.getValue().canBeEnabled() && entry.getValue().isEnabled() && entry.getValue().equals(module)) {
                final int c = entry.getKey().getBoolean(methodName, args);
                if (c != -1) {
                    return c;
                }
            }
        }
        return -1;
    }

    private String calculateHash(File file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] fileBytes = Files.readAllBytes(Paths.get(file.getPath()));
            byte[] hashBytes = digest.digest(fileBytes);

            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        }
        catch (Exception e) {
            return "";
        }
    }
}