package com.hawolt.util;

import com.esotericsoftware.yamlbeans.YamlReader;
import com.hawolt.logger.Logger;
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Created: 30/07/2022 23:35
 * Author: Twitter @hawolt
 **/

public class LocaleInstallation {

    public static File RIOT_CLIENT_SERVICES, SYSTEM_YAML;

    static {
        try {
            RIOT_CLIENT_SERVICES = getRiotClientServices();
            SYSTEM_YAML = locateYaml(RIOT_CLIENT_SERVICES);
        } catch (IOException e) {
            Logger.error(e);
            System.err.println("Unable to locate RiotClientServices.exe or system.yaml, exiting (1).");
            System.exit(1);
        }
    }

    public static File getRiotClientServices() throws IOException {
        File file = Paths.get(System.getenv("ALLUSERSPROFILE"))
                .resolve(StaticConstants.RIOT_GAMES)
                .resolve(StaticConstants.RIOT_INSTALLS_JSON).toFile();
        if (!file.exists()) throw new FileNotFoundException();
        JSONObject object = new JSONObject(new String(Files.readAllBytes(file.toPath())));
        List<String> list = load(new ArrayList<>(), object);
        return list.stream().map(File::new)
                .filter(File::exists)
                .findAny()
                .orElseGet(LocaleInstallation::get);
    }
    

    public static File locateYaml(File riotClientServices) throws IOException {
        if (riotClientServices == null || !riotClientServices.exists()) {
            throw new FileNotFoundException("Unable to locate system.yaml");
        }
        File file = Paths.get(System.getenv("ALLUSERSPROFILE"))
                .resolve(StaticConstants.RIOT_GAMES)
                .resolve(StaticConstants.METADATA)
                .resolve(StaticConstants.LEAGUE_OF_LEGENDS_LIVE)
                .resolve(StaticConstants.LEAGUE_OF_LEGENDS_INSTALLATION)
                .toFile();
        if (!file.exists()) throw new FileNotFoundException();
        YamlReader reader = new YamlReader(new String(Files.readAllBytes(file.toPath())));
        String yaml = reader.read().toString();
        String path = yaml.substring(yaml.indexOf("product_install_full_path=")+26, yaml.indexOf("product_install_root=")-2);
        return Paths.get(path).resolve("system.yaml").toFile();
    }

    private static File get() {
        JOptionPane.showMessageDialog(null, "Please locate and select RiotClientServices.exe");
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("Executable Files", "exe"));
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        int option = fileChooser.showOpenDialog(null);
        if (option == JFileChooser.APPROVE_OPTION) {
            return fileChooser.getSelectedFile();
        }
        return null;
    }

    private static List<String> load(List<String> list, JSONObject object) {
        for (String key : object.keySet()) {
            if (object.get(key) instanceof JSONObject) {
                load(list, object.getJSONObject(key));
            } else {
                list.add(object.getString(key));
            }
        }
        return list;
    }
}
