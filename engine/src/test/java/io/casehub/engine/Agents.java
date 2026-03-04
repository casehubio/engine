package io.casehub.engine;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import io.quarkiverse.langchain4j.RegisterAiService;

import java.util.List;

public class Agents {

    public record SentimentRequest(String documentId, String content) {}

    public record SentimentResult(String sentiment, double confidence, List<String> keywords) {}

    @RegisterAiService
    @SystemMessage("""
            You are an expert sentiment analysis specialist.

            Given:
            - a document ID
            - the document content text,

            you MUST respond with a short JSON document that can be mapped to:
              SentimentResult {
                String sentiment;        // POSITIVE, NEGATIVE or NEUTRAL
                double confidence;       // 0.0 to 1.0
                List<String> keywords;   // up to 5 key terms driving the sentiment
              }

            Be precise and objective. Base your analysis strictly on the provided text.
            """)
    public interface SentimentAnalysisAgent {

        @UserMessage("""
                Document ID: {data.documentId}

                Here is the document content to analyze:

                {data.content}

                Produce a SentimentResult JSON as specified above.
                """)
        SentimentResult analyze(@MemoryId String memoryId, @V("data") SentimentRequest input);
    }

    public record SummaryRequest(String documentId, String content) {}

    public record SummaryResult(String summary, List<String> keyPoints) {}

    @RegisterAiService
    @SystemMessage("""
            You are a concise and accurate document summarizer.

            Given:
            - a document ID
            - the document content text,

            you MUST respond with a short JSON document that can be mapped to:
              SummaryResult {
                String summary;            // 1-3 sentence summary
                List<String> keyPoints;    // up to 5 key takeaways
              }

            Be factual. Do not add opinions or information not present in the source text.
            """)
    public interface ContentSummarizerAgent {

        @UserMessage("""
                Document ID: {data.documentId}

                Here is the document content to summarize:

                {data.content}

                Produce a SummaryResult JSON as specified above.
                """)
        SummaryResult summarize(@MemoryId String memoryId, @V("data") SummaryRequest input);
    }
}
