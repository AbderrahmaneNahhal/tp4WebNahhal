package ma.emsi.nahhal.tp4webnahhal.llm;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.ClassPathDocumentLoader;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallenv15.BgeSmallEnV15EmbeddingModel;
import dev.langchain4j.model.google.genai.GoogleGenAiChatModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.router.LanguageModelQueryRouter;
import dev.langchain4j.rag.query.transformer.CompressingQueryTransformer;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import jakarta.enterprise.context.Dependent;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
                .logRequestsAndResponses(true)
                .build();

        EmbeddingModel embeddingModel = new BgeSmallEnV15EmbeddingModel();

        ContentRetriever retrieverRag =
                creerRetriever("rag.pdf", embeddingModel);

        ContentRetriever retrieverMachineLearning =
                creerRetriever("MachineLearning.pdf", embeddingModel);

        Map<ContentRetriever, String> sources = new LinkedHashMap<>();

        sources.put(retrieverRag, """
                Support spécialisé sur le RAG et le fine-tuning :
                recherche documentaire, embeddings, découpage en segments,
                transformation des questions, routage et comparaison
                entre RAG et fine-tuning.
                À privilégier pour les questions sur ces techniques.
                """);

        sources.put(retrieverMachineLearning, """
                Support d'introduction au machine learning :
                apprentissage supervisé et non supervisé, classification,
                régression, clustering, entraînement et évaluation.
                À privilégier pour les questions sur les bases
                de l'apprentissage automatique.
                """);

        LanguageModelQueryRouter router =
                new LanguageModelQueryRouter(model, sources);

        // Reformule la question en utilisant l'historique.
        CompressingQueryTransformer transformer =
                new CompressingQueryTransformer(model);

        // Transformation, puis routage, puis récupération des passages.
        DefaultRetrievalAugmentor augmentor =
                DefaultRetrievalAugmentor.builder()
                        .queryTransformer(transformer)
                        .queryRouter(router)
                        .build();

        chatMemory = MessageWindowChatMemory.withMaxMessages(20);
        ajouterRoleSysteme();

        assistant = AiServices.builder(Assistant.class)
                .chatModel(model)
                .chatMemory(chatMemory)
                .retrievalAugmentor(augmentor)
                .build();
    }

    private ContentRetriever creerRetriever(
            String nomFichier, EmbeddingModel embeddingModel) {

        System.out.println("Indexation du document : " + nomFichier);

        Document document = ClassPathDocumentLoader.loadDocument(
                nomFichier, new ApacheTikaDocumentParser());

        List<TextSegment> segments = DocumentSplitters
                .recursive(1200, 150)
                .split(document);

        List<Embedding> embeddings =
                embeddingModel.embedAll(segments).content();

        EmbeddingStore<TextSegment> store =
                new InMemoryEmbeddingStore<>();

        store.addAll(embeddings, segments);

        System.out.println(
                "Indexation terminée : " + nomFichier
                        + " (" + segments.size() + " segments)");

        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(store)
                .embeddingModel(embeddingModel)
                .maxResults(2)
                .minScore(0.5)
                .build();
    }

    private void ajouterRoleSysteme() {
        if (systemRole != null && !systemRole.isBlank()) {
            chatMemory.add(SystemMessage.from(systemRole));
        }
    }
}