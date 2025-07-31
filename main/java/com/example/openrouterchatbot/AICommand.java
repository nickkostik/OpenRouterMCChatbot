package com.example.openrouterchatbot;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

public class AICommand implements CommandExecutor, TabCompleter {

    private final OpenRouterChatbot plugin;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private static final String API_URL = "https://openrouter.ai/api/v1/chat/completions";

    public AICommand(OpenRouterChatbot plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(formatMessage("invalid-usage", sender, "", ""));
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "set":
                handleSetModel(sender, args);
                break;
            case "reloadmodels":
                handleReloadModels(sender);
                break;
            default:
                handleAiPrompt(sender, args);
                break;
        }

        return true;
    }

    private void handleSetModel(CommandSender sender, String[] args) {
        if (plugin.getConfig().getBoolean("command-permissions.require-op-for-set") && !sender.hasPermission("openrouterchatbot.set")) {
            sender.sendMessage(formatMessage("no-permission", sender, "", ""));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(formatMessage("invalid-usage", sender, "", ""));
            return;
        }
        String model = args[1];
        plugin.getConfig().set("model", model);
        plugin.saveConfig();
        sender.sendMessage(ChatColor.GREEN + "AI model set to: " + model);
    }

    private void handleReloadModels(CommandSender sender) {
        if (plugin.getConfig().getBoolean("command-permissions.require-op-for-reload") && !sender.hasPermission("openrouterchatbot.reload")) {
            sender.sendMessage(formatMessage("no-permission", sender, "", ""));
            return;
        }
        plugin.fetchModels();
        sender.sendMessage(ChatColor.GREEN + "Reloading OpenRouter models...");
    }

    private void handleAiPrompt(CommandSender sender, String[] args) {
        if (plugin.getConfig().getBoolean("command-permissions.require-op-for-ai") && !sender.hasPermission("openrouterchatbot.use")) {
            sender.sendMessage(formatMessage("no-permission", sender, "", ""));
            return;
        }

        String apiKey = plugin.getConfig().getString("openrouter-api-key");
        if (apiKey == null || apiKey.equals("YOUR_OPENROUTER_API_KEY") || apiKey.isEmpty()) {
            sender.sendMessage(formatMessage("api-key-not-set", sender, "", ""));
            return;
        }

        StringJoiner promptBuilder = new StringJoiner(" ");
        for (String arg : args) {
            promptBuilder.add(arg);
        }
        String prompt = promptBuilder.toString();

        final boolean broadcast = plugin.getConfig().getBoolean("message-visibility.broadcast-to-server", false);

        final String model = plugin.getConfig().getString("model", "openai/gpt-4o");

        if (plugin.getConfig().getBoolean("features.display-user-prompt", true)) {
            broadcastMessage(formatMessage("prompt-display-format", sender, prompt, model), sender, broadcast);
        }

        if (plugin.getConfig().getBoolean("features.send-thinking-message", true)) {
            broadcastMessage(formatMessage("thinking-message", sender, prompt, model), sender, broadcast);
        }

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                boolean useExtended = plugin.getConfig().getBoolean("features.enable-extended-context", true);
                boolean useWebSearch = plugin.getConfig().getBoolean("features.enable-web-search", true);

                StringBuilder finalModelBuilder = new StringBuilder(model);
                if (useExtended) {
                    synchronized (plugin.getModelCache()) {
                        if (plugin.getModelCache().contains(model + ":extended")) {
                            finalModelBuilder.append(":extended");
                        }
                    }
                }
                if (useWebSearch) {
                    finalModelBuilder.append(":online");
                }
                String finalModel = finalModelBuilder.toString();


                JSONObject payload = new JSONObject();
                payload.put("model", finalModel);

                // Add API parameters from config
                payload.put("temperature", plugin.getConfig().getDouble("api-settings.temperature", 1.0));
                payload.put("max_tokens", plugin.getConfig().getInt("api-settings.max_tokens", 2048));
                payload.put("top_p", plugin.getConfig().getDouble("api-settings.top_p", 1.0));
                payload.put("frequency_penalty", plugin.getConfig().getDouble("api-settings.frequency_penalty", 0.0));
                payload.put("presence_penalty", plugin.getConfig().getDouble("api-settings.presence_penalty", 0.0));

                List<String> stop = plugin.getConfig().getStringList("api-settings.stop");
                if (stop != null && !stop.isEmpty()) {
                    payload.put("stop", new JSONArray(stop));
                }

                if (plugin.getConfig().isSet("api-settings.seed") && !plugin.getConfig().getString("api-settings.seed").isEmpty()) {
                    payload.put("seed", plugin.getConfig().getInt("api-settings.seed"));
                }

                List<String> transforms = plugin.getConfig().getStringList("api-settings.transforms");
                if (transforms != null && !transforms.isEmpty()) {
                    payload.put("transforms", new JSONArray(transforms));
                }

                String route = plugin.getConfig().getString("api-settings.route");
                if (route != null && !route.isEmpty()) {
                    payload.put("route", route);
                }


                JSONArray messages = new JSONArray();
                String instructions = plugin.getConfig().getString("api-settings.instructions");
                if (instructions != null && !instructions.isEmpty()) {
                    messages.put(new JSONObject().put("role", "system").put("content", instructions));
                }
                String finalPrompt = prompt;
                if (plugin.getConfig().getBoolean("features.include-player-name-in-prompt", true) && sender instanceof Player) {
                    finalPrompt = sender.getName() + ": " + prompt;
                }
                messages.put(new JSONObject().put("role", "user").put("content", finalPrompt));
                payload.put("messages", messages);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(API_URL))
                        .header("Authorization", "Bearer " + apiKey)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (response.statusCode() == 200) {
                        JSONObject jsonResponse = new JSONObject(response.body());
                        String content = jsonResponse.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
                        broadcastMessage(formatMessage("ai-prefix", sender, prompt, model) + content, sender, broadcast);
                    } else {
                        broadcastMessage(formatMessage("error-prefix", sender, prompt, model) + "Error from OpenRouter API: " + response.statusCode() + " - " + response.body(), sender, broadcast);
                    }
                });

            } catch (IOException | InterruptedException e) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    broadcastMessage(formatMessage("error-prefix", sender, prompt, model) + "An error occurred while contacting the AI: " + e.getMessage(), sender, broadcast);
                });
            }
        });
    }

    private void broadcastMessage(String message, CommandSender sender, boolean broadcast) {
        if (broadcast) {
            plugin.getServer().broadcastMessage(message);
        } else {
            sender.sendMessage(message);
        }
    }

    private String formatMessage(String key, CommandSender sender, String prompt, String model) {
        String message = plugin.getConfig().getString("message-formatting." + key, "&cMessage not found: " + key);
        message = message.replace("%player%", sender.getName());
        message = message.replace("%prompt%", prompt);
        message = message.replace("%model%", model);
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
            String input = args[1].toLowerCase();
            synchronized (plugin.getModelCache()) {
                return plugin.getModelCache().stream()
                        .filter(model -> model.toLowerCase().startsWith(input))
                        .collect(Collectors.toList());
            }
        }
        if (args.length == 1) {
            List<String> subcommands = new ArrayList<>();
            subcommands.add("set");
            subcommands.add("reloadmodels");
            return subcommands.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}