package ma.emsi.nahhal.tp4webnahhal.llm;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.google.genai.GoogleGenAiChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.enterprise.context.Dependent;

import java.io.Serializable;

@Dependent
public class LlmClient implements Serializable {

    private static final long serialVersionUID = 1L;

    private String systemRole;
    private transient Assistant assistant;
    private transient ChatMemory chatMemory;

    public void setSystemRole(String systemRole) {
        this.systemRole = systemRole;

        if (chatMemory != null) {
            chatMemory.clear();
            ajouterRoleSysteme();
        }
    }

    public String envoyerRequete(String question) {
        if (assistant == null) {
            initialiserAssistant();
        }

        return assistant.chat(question);
    }

    private void initialiserAssistant() {
        String apiKey = System.getenv("GEMINI_KEY");

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "La variable d'environnement GEMINI_KEY n'est pas définie.");
        }

        ChatModel model = GoogleGenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gemini-3.5-flash-lite")
                .temperature(0.3)
                .build();

        chatMemory = MessageWindowChatMemory.withMaxMessages(20);
        ajouterRoleSysteme();

        assistant = AiServices.builder(Assistant.class)
                .chatModel(model)
                .chatMemory(chatMemory)
                .build();
    }

    private void ajouterRoleSysteme() {
        if (systemRole != null && !systemRole.isBlank()) {
            chatMemory.add(SystemMessage.from(systemRole));
        }
    }
}