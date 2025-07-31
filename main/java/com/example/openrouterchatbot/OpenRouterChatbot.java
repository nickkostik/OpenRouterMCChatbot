package com.example.openrouterchatbot;

import org.bukkit.plugin.java.JavaPlugin;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashSet;
import java.util.Set;

public class OpenRouterChatbot extends JavaPlugin {

    private static final String MODELS_API_URL = "https://openrouter.ai/api/v1/models";
    private final Set<String> modelCache = new HashSet<>();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Override
    public void onEnable() {
        // Generate and load config.yml
        saveDefaultConfig();

        // Register command and tab completer
        AICommand aiCommand = new AICommand(this);
        this.getCommand("ai").setExecutor(aiCommand);
        this.getCommand("ai").setTabCompleter(aiCommand);

        // Asynchronously fetch and cache models
        fetchModels();
    }

    @Override
    public void onDisable() {
        getLogger().info("OpenRouterChatbot has been disabled.");
    }

    public void fetchModels() {
        getServer().getScheduler().runTaskAsynchronously(this, () -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(MODELS_API_URL))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    synchronized (modelCache) {
                        modelCache.clear();
                        JSONObject jsonResponse = new JSONObject(response.body());
                        JSONArray data = jsonResponse.getJSONArray("data");
                        for (int i = 0; i < data.length(); i++) {
                            modelCache.add(data.getJSONObject(i).getString("id"));
                        }
                    }
                    getLogger().info("Successfully cached " + modelCache.size() + " models from OpenRouter.");
                } else {
                    getLogger().warning("Failed to fetch models from OpenRouter. Status code: " + response.statusCode());
                }
            } catch (IOException | InterruptedException e) {
                getLogger().severe("Error fetching models from OpenRouter: " + e.getMessage());
            }
        });
    }

    public Set<String> getModelCache() {
        return modelCache;
    }
}