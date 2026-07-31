package com.example.minimall.service;

import com.example.minimall.config.RagConfig;
import com.example.minimall.model.KnowledgeChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 文档分块处理器。
 * <p>
 * 将长文档按滑动窗口策略切分为语义连贯的文本块（chunk），用于后续向量化与检索。
 * 分块策略：
 * <ol>
 *   <li>按段落/换行符预切分</li>
 *   <li>在段落内按句子边界（句号、问号、感叹号）做二次切分</li>
 *   <li>将句子组装到 chunkSize 大小的块中，块之间有 chunkOverlap 重叠</li>
 * </ol>
 * </p>
 */
@Service
public class DocumentProcessor {
    private static final Logger logger = LoggerFactory.getLogger(DocumentProcessor.class);

    private final RagConfig ragConfig;

    public DocumentProcessor(RagConfig ragConfig) {
        this.ragConfig = ragConfig;
    }

    /**
     * 将文档内容切分为文本块
     *
     * @param content 文档原始内容
     * @return 文本块列表
     */
    public List<String> chunk(String content) {
        if (content == null || content.trim().isEmpty()) {
            return new ArrayList<>();
        }

        int chunkSize = ragConfig.getChunkSize();
        int overlap = ragConfig.getChunkOverlap();
        String normalized = content.trim().replaceAll("\r\n", "\n").replaceAll("\n{3,}", "\n\n");

        // 第一步：按段落预切分
        List<String> paragraphs = new ArrayList<>();
        for (String para : normalized.split("\n\n+")) {
            String trimmed = para.trim();
            if (!trimmed.isEmpty()) {
                paragraphs.add(trimmed);
            }
        }

        // 第二步：段落内按句子切分
        List<String> sentences = new ArrayList<>();
        for (String para : paragraphs) {
            splitIntoSentences(para, sentences);
        }

        // 第三步：滑动窗口组装块
        List<String> chunks = new ArrayList<>();
        StringBuilder currentChunk = new StringBuilder();
        int currentLen = 0;

        for (String sentence : sentences) {
            int sentLen = sentence.length();

            // 单句超过 chunkSize，直接按 chunkSize 硬切
            if (sentLen >= chunkSize) {
                if (currentChunk.length() > 0) {
                    chunks.add(currentChunk.toString().trim());
                    // 保留 overlap
                    String overlapText = currentChunk.substring(Math.max(0, currentChunk.length() - overlap));
                    currentChunk = new StringBuilder(overlapText);
                    currentLen = overlapText.length();
                }
                // 硬切长句
                for (int i = 0; i < sentence.length(); i += chunkSize - overlap) {
                    int end = Math.min(i + chunkSize, sentence.length());
                    chunks.add(sentence.substring(i, end).trim());
                }
                currentChunk = new StringBuilder();
                currentLen = 0;
                continue;
            }

            if (currentLen + sentLen > chunkSize && currentChunk.length() > 0) {
                chunks.add(currentChunk.toString().trim());
                // 保留尾部 overlap 作为下一个块的开头
                String overlapText = getOverlapText(currentChunk.toString(), overlap);
                currentChunk = new StringBuilder(overlapText);
                currentLen = overlapText.length();
            }

            if (currentChunk.length() > 0) {
                currentChunk.append(" ");
            }
            currentChunk.append(sentence);
            currentLen += sentLen + 1;
        }

        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }

        logger.debug("文档分块完成: {} 字符 → {} 个块, 平均块长 {} 字符",
                content.length(), chunks.size(),
                chunks.stream().mapToInt(String::length).average().orElse(0));
        return chunks;
    }

    /**
     * 将段落切分为句子
     */
    private void splitIntoSentences(String paragraph, List<String> sentences) {
        // 中文/英文句末标点 + 换行
        String[] parts = paragraph.split("(?<=[。！？!?；;\n])");
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                sentences.add(trimmed);
            }
        }
    }

    /**
     * 获取文本末尾的 overlap 部分（用于块间重叠）
     */
    private String getOverlapText(String text, int overlapLen) {
        if (text.length() <= overlapLen) return text;
        // 尽量从句子边界开始
        int cutStart = text.length() - overlapLen;
        // 向前找句子边界
        for (int i = cutStart; i > Math.max(0, cutStart - 50); i--) {
            char c = text.charAt(i);
            if (c == '。' || c == '！' || c == '？' || c == '!' || c == '?' || c == '\n' || c == ';') {
                cutStart = i + 1;
                break;
            }
        }
        return text.substring(cutStart);
    }

    /**
     * 估算文本的 token 数量（近似值）
     * 中文约 1.5 字/token，英文约 4 字符/token
     */
    public int estimateTokens(String text) {
        if (text == null || text.isEmpty()) return 0;
        int chineseCount = 0;
        int otherCount = 0;
        for (char c : text.toCharArray()) {
            if (c >= '\u4e00' && c <= '\u9fff') {
                chineseCount++;
            } else {
                otherCount++;
            }
        }
        return (int) Math.ceil(chineseCount / 1.5 + otherCount / 4.0);
    }

    /**
     * 构建分块元数据 JSON
     */
    public String buildChunkMeta(int chunkIndex, int totalChunks, int startPos, int endPos) {
        return String.format("{\"index\":%d,\"total\":%d,\"start\":%d,\"end\":%d}",
                chunkIndex, totalChunks, startPos, endPos);
    }
}
