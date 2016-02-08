package co.mchost.gitbroadcast.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

public class GitUtil {

    public static String getGitFile(String url) throws IOException {
        return IOUtils.toString(new InputStreamReader(new URL(url).openStream()));
    }
}
