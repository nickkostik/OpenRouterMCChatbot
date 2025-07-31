# OpenRouter Chatbot - Minecraft Plugin

## Overview

OpenRouter Chatbot is a powerful and highly configurable Spigot plugin that integrates the OpenRouter AI API into your Minecraft server. It allows players and server administrators to interact with a variety of AI models directly in-game, with support for web search, dynamic model switching, and extensive customization. This plugin is designed to be a seamless and immersive addition to any server, providing both utility and entertainment.

## Core Features

- **In-Game AI Chat**: Access a powerful AI chatbot from both the server console and in-game chat.
- **Dynamic Model Switching**: Change the active AI model on the fly with a simple command. The plugin supports tab-completion for model names, making it easy to find and select the perfect model for your needs.
- **Universal Web Search**: By appending `:online` to the model name, you can enable web search for any query, allowing the AI to provide up-to-date information on any topic.
- **Intelligent Context Extension**: The plugin can automatically detect and use a model's extended context version (e.g., `:extended`), providing a larger context window for more complex conversations.
- **Highly Configurable**: Nearly every aspect of the plugin is customizable via a detailed `config.yml` file. You can control everything from message formats and colors to advanced API parameters like temperature and token limits.
- **Fine-Grained Permission Control**: You have complete control over who can use the plugin's commands. Each command can be configured to require operator (OP) status or a specific permission node.
- **Public or Private Messaging**: Configure the plugin to broadcast AI interactions to the entire server or keep them private to the user who initiated the prompt.

## Commands and Permissions

The plugin's commands are simple and intuitive:

| Command | Description | Permission Node | Default |
| --- | --- | --- | --- |
| `/ai <prompt>` | Sends a prompt to the configured AI model. | `openrouterchatbot.use` | All Players |
| `/ai set <model_slug>` | Sets the default AI model for all subsequent requests. | `openrouterchatbot.set` | Operators |
| `/ai reloadmodels` | Refreshes the cached list of available models from the OpenRouter API. | `openrouterchatbot.reload` | Operators |

You can change the default permission requirements in the `config.yml` file.

## Installation and Setup

1.  **Download**: Get the latest release of `OpenRouterChatbot.jar` from the project's releases page.
2.  **Install**: Place the `OpenRouterChatbot.jar` file into your server's `plugins` directory.
3.  **First Run**: Start your server. The plugin will generate its default configuration file at `plugins/OpenRouterChatbot/config.yml`.
4.  **Configure**: Stop the server and open the `config.yml` file with a text editor.
    -   **API Key**: The most important step is to add your OpenRouter API key to the `openrouter-api-key` field. You can obtain a key from [https://openrouter.ai/keys](https://openrouter.ai/keys).
    -   **Customize**: Review the rest of the `config.yml` file and customize it to your liking. The file is extensively commented to explain every option.
5.  **Restart**: Start your server again. The plugin is now configured and ready to use.

## How It Works

The plugin operates by making asynchronous calls to the OpenRouter API. This means that the server's main thread is never blocked, preventing any lag or performance issues. When a user issues a command, the plugin immediately sends a "Thinking..." message (if enabled) and then sends the request to the API in the background. Once the API responds, the plugin schedules a task to send the AI's response back to the user in the main server thread, ensuring thread safety.

On startup, the plugin also fetches and caches a list of all available models from the OpenRouter API. This allows for fast tab-completion of model names and enables the intelligent extended context feature without making an API call for every command.

## Building from Source

If you wish to build the plugin from its source code, you will need:

-   Java 8 or a newer version
-   Apache Maven

1.  Clone the repository to your local machine.
2.  Open a terminal or command prompt in the project's root directory.
3.  Run the following Maven command:
    ```bash
    mvn clean package
    ```
4.  The compiled JAR file, ready for use, will be located in the `target` directory.

---

## Configuration File (`config.yml`) Explained

This section provides a detailed, toggle-by-toggle explanation of every setting available in the `config.yml` file.

### `Primary Settings`

-   `openrouter-api-key`: This is where you must paste your unique API key from OpenRouter. The plugin will not work without it.
-   `model`: This setting determines the default AI model that the plugin will use. You can find a list of available models on the OpenRouter website. This can be changed in-game with `/ai set`.

### `command-permissions`

This section allows you to control who can use the plugin's commands.

-   `require-op-for-ai`: If set to `true`, only server operators (OPs) or players with the `openrouterchatbot.use` permission can use the `/ai <prompt>` command. If `false`, anyone can use it.
-   `require-op-for-set`: If set to `true`, only OPs or players with the `openrouterchatbot.set` permission can use the `/ai set` command. If `false`, anyone can change the model.
-   `require-op-for-reload`: If set to `true`, only OPs or players with the `openrouterchatbot.reload` permission can use the `/ai reloadmodels` command. If `false`, anyone can reload the model cache.

### `features`

This section contains toggles for various plugin behaviors.

-   `send-thinking-message`: If `true`, a "Thinking..." message will be sent to the user after they submit a prompt, providing feedback that the request is being processed.
-   `enable-web-search`: If `true`, the plugin will automatically add the `:online` suffix to the model name, enabling the AI to search the web for up-to-date information.
-   `enable-extended-context`: If `true`, the plugin will check if the selected model has an `:extended` version and use it if available. This provides the AI with a larger memory for longer conversations.
-   `display-user-prompt`: If `true`, the user's original prompt will be displayed in the chat before the AI's response. This is useful for context in public conversations.
-   `include-player-name-in-prompt`: If `true`, the name of the player who sent the prompt will be prepended to the message sent to the AI (e.g., "Notch: What is a creeper?"). This can help the AI tailor its response.

### `message-visibility`

-   `broadcast-to-server`: If `true`, all messages related to AI interactions (prompts, "Thinking..." messages, and responses) will be broadcast to all players on the server. If `false`, these messages will only be visible to the player who used the command.

### `message-formatting`

This section allows you to customize the appearance of all plugin messages. You can use standard Minecraft color codes (e.g., `&a` for green) and the following placeholders:
-   `%player%`: The name of the player who used the command.
-   `%prompt%`: The prompt that the player sent.
-   `%model%`: The AI model that was used.

### `api-settings`

This section gives you direct control over the parameters sent to the OpenRouter API. It is recommended for advanced users.

-   `instructions`: A system prompt that guides the AI's personality and behavior.
-   `temperature`: Controls the randomness of the AI's responses. Higher values (e.g., 1.2) will result in more creative but less predictable responses, while lower values (e.g., 0.5) will make the responses more focused and deterministic.
-   `max_tokens`: The maximum length, in tokens (roughly words), of the AI's response.
-   `top_p`: An alternative to temperature for controlling randomness. It's recommended to only use one of `temperature` or `top_p`.
-   `frequency_penalty` and `presence_penalty`: These settings can be used to discourage the AI from repeating itself.
-   `stop`: A list of words or phrases that will cause the AI to immediately stop generating text.
-   `seed`: If you provide an integer seed, the AI will produce the same output for the same prompt every time. This is useful for testing.
-   `transforms`: A list of transformations to apply to the model's output.
-   `route`: The routing strategy for the API call.