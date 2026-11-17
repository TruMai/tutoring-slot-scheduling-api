package edu.sjsu.cmpe172.tutoring.ai;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * Loads policy/FAQ file into the vector store. Loading happens on first use,
 * so app and the tests start without calling the API
 */
@Component
public class KnowledgeBase {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBase.class);

    private final VectorStore vectorStore;
    private final Resource file;
    private boolean loaded = false;

    public KnowledgeBase(VectorStore vectorStore,
                         @Value("classpath:knowledge/policies.txt") Resource file) {
        this.vectorStore = vectorStore;
        this.file = file;
    }

    public synchronized void ensureLoaded() {
        if (loaded) return;
        try {
            String text = new String(file.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            // one document per paragraph (paragraphs are separated by a blank line)
            List<Document> docs = Arrays.stream(text.split("\\R\\s*\\R"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Document::new)
                .toList();
            vectorStore.add(docs);   // embeds each document and stores the vector
            loaded = true;
            log.info("Knowledge base loaded: {} documents", docs.size());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}