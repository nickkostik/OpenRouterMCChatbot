package com.example.openrouterchatbot;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AICommandPermissionsTest {
    private OpenRouterChatbot plugin;
    private FileConfiguration config;
    private CommandSender sender;
    private AICommand command;

    @BeforeEach
    void setUp() {
        plugin = mock(OpenRouterChatbot.class);
        config = mock(FileConfiguration.class);
        sender = mock(CommandSender.class);
        when(plugin.getConfig()).thenReturn(config);
        when(sender.getName()).thenReturn("player");
        when(config.getString(eq("message-formatting.no-permission"), anyString()))
                .thenReturn("&cNo permission");
        command = new AICommand(plugin);
    }

    @Test
    void setRequiresPermissionWithLegacyFlagDisabled() {
        when(config.getBoolean("command-permissions.require-op-for-set")).thenReturn(false);

        command.onCommand(sender, null, "ai", new String[]{"set", "some/model"});

        verify(sender).hasPermission("openrouterchatbot.set");
        verify(config, never()).set("model", "some/model");
        verify(plugin, never()).saveConfig();
        verify(sender).sendMessage("§cNo permission");
    }

    @Test
    void reloadRequiresPermissionWithLegacyFlagDisabled() {
        when(config.getBoolean("command-permissions.require-op-for-reload")).thenReturn(false);

        command.onCommand(sender, null, "ai", new String[]{"ReLoAdMoDeLs"});

        verify(sender).hasPermission("openrouterchatbot.reload");
        verify(plugin, never()).fetchModels();
        verify(sender).sendMessage("§cNo permission");
    }

    @Test
    void explicitSetPermissionAllowsModelChange() {
        when(sender.hasPermission("openrouterchatbot.set")).thenReturn(true);

        command.onCommand(sender, null, "ai", new String[]{"SeT", "some/model"});

        verify(config).set("model", "some/model");
        verify(plugin).saveConfig();
    }

    @Test
    void explicitReloadPermissionAllowsRefresh() {
        when(sender.hasPermission("openrouterchatbot.reload")).thenReturn(true);

        command.onCommand(sender, null, "ai", new String[]{"reloadmodels"});

        verify(plugin).fetchModels();
    }

    @Test
    void ordinaryPromptStillReachesPromptHandler() {
        when(config.getString(eq("message-formatting.api-key-not-set"), anyString()))
                .thenReturn("&cAPI key missing");

        command.onCommand(sender, null, "ai", new String[]{"hello"});

        verify(sender).sendMessage("§cAPI key missing");
        verify(sender, never()).hasPermission("openrouterchatbot.set");
        verify(sender, never()).hasPermission("openrouterchatbot.reload");
    }
}
