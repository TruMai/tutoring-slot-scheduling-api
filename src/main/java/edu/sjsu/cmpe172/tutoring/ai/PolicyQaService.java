package edu.sjsu.cmpe172.tutoring.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

/** Grounded Q&A (RAG): retrieve the most relevant policy paragraphs, then let the LLM answer from them. */
@Service
public class PolicyQaService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final KnowledgeBase knowledgeBase;

    public PolicyQaService(ChatClient.Builder builder, VectorStore vectorStore, KnowledgeBase knowledgeBase) {
        this.chatClient = builder.build();
        this.vectorStore = vectorStore;
        this.knowledgeBase = knowledgeBase;
    }

    public String ask(String question) {
        knowledgeBase.ensureLoaded();
        return chatClient.prompt()
            .system("Answer using only the provided context. If the answer is not in the context, say you do not know.")
            .advisors(QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(SearchRequest.builder().topK(3).build())   // retrieve 3 paragraphs
                .build())
            .user(question)
            .call()
            .content();
    }
}