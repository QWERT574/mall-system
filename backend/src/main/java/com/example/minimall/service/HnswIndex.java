package com.example.minimall.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * HNSW (Hierarchical Navigable Small World) 近似最近邻索引
 *
 * 算法原理：
 * - 多层图结构，上层稀疏下层密集
 * - 搜索从最顶层开始，逐层向下细化
 * - 插入时随机选择层级（指数分布）
 * - 每层维护邻居列表，M为最大邻居数
 *
 * 性能：O(log n) 搜索复杂度，100万条数据单次检索 < 10ms
 *
 * 参考: Malkov & Yashunin (2016) "Efficient and robust approximate nearest neighbor search using HNSW"
 */
public class HnswIndex {
    private static final Logger logger = LoggerFactory.getLogger(HnswIndex.class);

    // 索引参数
    private final int M;              // 每层最大邻居数
    private final int maxM0;          // 第0层最大邻居数（通常为2*M）
    private final int efConstruction; // 构建时搜索宽度
    private final double ml;          // 层级分布参数 = 1/ln(M)

    // 索引数据
    private final Map<Long, float[]> vectors;        // id -> 向量
    private final Map<Long, Node> nodes;             // id -> 节点
    private final List<Map<Long, Set<Long>>> graph;  // 各层的邻接表
    private volatile Long entryPoint;                // 入口点
    private volatile int maxLevel;                   // 当前最大层级

    // 读写锁保证线程安全
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    // 维度
    private final int dim;

    private static class Node {
        final long id;
        final int level;

        Node(long id, int level) {
            this.id = id;
            this.level = level;
        }
    }

    public HnswIndex(int dim, int M, int efConstruction) {
        if (dim <= 0) {
            throw new IllegalArgumentException("dim must be positive: " + dim);
        }
        if (M < 2) {
            throw new IllegalArgumentException("M must be >= 2: " + M);
        }
        if (efConstruction < 1) {
            throw new IllegalArgumentException("efConstruction must be >= 1: " + efConstruction);
        }
        this.dim = dim;
        this.M = M;
        this.maxM0 = M * 2;
        this.efConstruction = efConstruction;
        this.ml = 1.0 / Math.log(M);
        this.vectors = new ConcurrentHashMap<>();
        this.nodes = new ConcurrentHashMap<>();
        this.graph = new ArrayList<>();
        this.entryPoint = null;
        this.maxLevel = -1;
    }

    /**
     * 添加向量到索引
     */
    public void add(long id, float[] vector) {
        if (vector == null || vector.length != dim) {
            throw new IllegalArgumentException("invalid vector, expected dim=" + dim);
        }
        // 拷贝以防止外部修改
        final float[] vec = new float[dim];
        System.arraycopy(vector, 0, vec, 0, dim);

        lock.writeLock().lock();
        try {
            // 1. 随机选择层级
            int level = randomLevel();

            // 2. 创建节点并存储向量
            nodes.put(id, new Node(id, level));
            vectors.put(id, vec);

            // 确保图层级足够
            ensureLayers(level);

            // 初始化新节点在各层的邻居集合（0..level）
            for (int l = 0; l <= level; l++) {
                graph.get(l).putIfAbsent(id, ConcurrentHashMap.newKeySet());
            }

            // 3. 若是第一个节点，直接作为入口
            Long ep = entryPoint;
            if (ep == null) {
                entryPoint = id;
                maxLevel = level;
                return;
            }

            // 4. 从顶层到 level+1 层，贪心搜索找到每层最近节点
            int curMax = maxLevel;
            for (int l = curMax; l > level; l--) {
                ep = greedySearchLayer(vec, ep, l);
            }

            // 5. 从 min(level, maxLevel) 到 0 层，搜索邻居并建立双向连接
            for (int l = Math.min(level, curMax); l >= 0; l--) {
                PriorityQueue<SearchResult> results = searchLayer(vec, new long[]{ep}, efConstruction, l);

                // 选择 M 个邻居（第 0 层用 maxM0）
                int maxConn = (l == 0) ? maxM0 : M;
                List<SearchResult> candidates = new ArrayList<>(results);
                List<SearchResult> selected = selectNeighbors(candidates, maxConn);

                // 建立双向连接
                Set<Long> newNeighbors = graph.get(l).get(id);
                for (SearchResult sr : selected) {
                    // 新节点 -> 邻居
                    newNeighbors.add(sr.id);

                    // 邻居 -> 新节点（反向边）
                    Set<Long> neighborSet = graph.get(l).get(sr.id);
                    if (neighborSet != null) {
                        neighborSet.add(id);
                        // 超过限制则剪枝
                        int limit = (l == 0) ? maxM0 : M;
                        if (neighborSet.size() > limit) {
                            pruneNeighbors(sr.id, l, limit);
                        }
                    }
                }

                // 下一层的入口点：本层结果中最近的节点
                SearchResult best = pickClosest(candidates);
                if (best != null) {
                    ep = best.id;
                }
            }

            // 6. 若新节点层级更高，更新入口点
            if (level > curMax) {
                entryPoint = id;
                maxLevel = level;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 批量添加向量
     */
    public void addAll(Map<Long, float[]> vectorMap) {
        if (vectorMap == null) {
            return;
        }
        for (Map.Entry<Long, float[]> entry : vectorMap.entrySet()) {
            add(entry.getKey(), entry.getValue());
        }
    }

    /**
     * 搜索最近邻
     *
     * @param queryVector 查询向量
     * @param k           返回数量
     * @param ef          搜索宽度（越大越精确，越小越快）
     * @return 按相似度降序排列的 (id, score) 列表
     */
    public List<SearchResult> search(float[] queryVector, int k, int ef) {
        if (queryVector == null || queryVector.length != dim) {
            throw new IllegalArgumentException("invalid query vector, expected dim=" + dim);
        }
        if (k <= 0) {
            return Collections.emptyList();
        }
        lock.readLock().lock();
        try {
            Long ep = entryPoint;
            if (ep == null || vectors.isEmpty()) {
                return Collections.emptyList();
            }
            int effectiveEf = Math.max(ef, k);

            // 1. 从顶层到第 1 层，贪心搜索找到每层最近节点
            for (int l = maxLevel; l >= 1; l--) {
                ep = greedySearchLayer(queryVector, ep, l);
            }

            // 2. 第 0 层以 ef 宽度搜索
            PriorityQueue<SearchResult> results = searchLayer(queryVector, new long[]{ep}, effectiveEf, 0);

            // 3. 返回 top-k（按相似度降序）
            List<SearchResult> all = new ArrayList<>(results);
            Collections.sort(all, new Comparator<SearchResult>() {
                @Override
                public int compare(SearchResult a, SearchResult b) {
                    return Double.compare(b.score, a.score); // 降序
                }
            });
            int end = Math.min(k, all.size());
            return new ArrayList<>(all.subList(0, end));
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 删除向量
     */
    public void remove(long id) {
        lock.writeLock().lock();
        try {
            if (!nodes.containsKey(id)) {
                return;
            }
            Node node = nodes.get(id);
            // 从所有层的邻接表中移除该节点的所有引用
            for (int l = 0; l <= node.level && l < graph.size(); l++) {
                Map<Long, Set<Long>> layer = graph.get(l);
                Set<Long> neighbors = layer.remove(id);
                if (neighbors != null) {
                    for (Long n : neighbors) {
                        Set<Long> nSet = layer.get(n);
                        if (nSet != null) {
                            nSet.remove(id);
                        }
                    }
                }
            }

            vectors.remove(id);
            nodes.remove(id);

            // 若删除的是入口点，重新选择
            if (entryPoint != null && entryPoint == id) {
                reassignEntryPoint();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 获取索引大小
     */
    public int size() {
        return vectors.size();
    }

    /**
     * 清空索引
     */
    public void clear() {
        lock.writeLock().lock();
        try {
            vectors.clear();
            nodes.clear();
            graph.clear();
            entryPoint = null;
            maxLevel = -1;
        } finally {
            lock.writeLock().unlock();
        }
    }

    // ==================== 内部方法 ====================

    private int randomLevel() {
        double r = ThreadLocalRandom.current().nextDouble();
        // 防止 nextDouble() 返回 0.0 导致 log(0)
        if (r <= 0.0) {
            r = Double.MIN_VALUE;
        }
        return (int) Math.floor(-Math.log(r) * ml);
    }

    private double cosineSimilarity(float[] a, float[] b) {
        double dot = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < dim; i++) {
            dot += (double) a[i] * (double) b[i];
            normA += (double) a[i] * a[i];
            normB += (double) b[i] * b[i];
        }
        double denom = Math.sqrt(normA) * Math.sqrt(normB);
        if (denom == 0.0) {
            return 0.0; // 零向量处理
        }
        return dot / denom;
    }

    /**
     * 贪心搜索：在某一层从 entryPoint 开始，找到最近的节点
     */
    private long greedySearchLayer(float[] queryVector, long entryPoint, int layer) {
        long current = entryPoint;
        double currentSim = similarityTo(queryVector, current);
        boolean improved = true;
        while (improved) {
            improved = false;
            long bestNeighbor = current;
            double bestSim = currentSim;
            Set<Long> neighbors = getNeighbors(layer, current);
            for (Long e : neighbors) {
                double sim = similarityTo(queryVector, e);
                if (sim > bestSim) {
                    bestSim = sim;
                    bestNeighbor = e;
                }
            }
            if (bestNeighbor != current) {
                current = bestNeighbor;
                currentSim = bestSim;
                improved = true;
            }
        }
        return current;
    }

    /**
     * 在某一层搜索 ef 个最近邻居
     * 使用访问集合 + 候选队列(按相似度最大堆，最近在顶) + 结果队列(按相似度最小堆，最远在顶)
     */
    private PriorityQueue<SearchResult> searchLayer(float[] queryVector, long[] entryPoints, int ef, int layer) {
        // 候选队列：最大堆（相似度最高在顶，即最近者优先弹出展开）
        PriorityQueue<SearchResult> candidates = new PriorityQueue<>(new Comparator<SearchResult>() {
            @Override
            public int compare(SearchResult a, SearchResult b) {
                return Double.compare(b.score, a.score);
            }
        });
        // 结果队列：最小堆（相似度最低在顶，即最远者在顶，便于淘汰）
        PriorityQueue<SearchResult> results = new PriorityQueue<>(new Comparator<SearchResult>() {
            @Override
            public int compare(SearchResult a, SearchResult b) {
                return Double.compare(a.score, b.score);
            }
        });
        Set<Long> visited = new HashSet<>();

        for (long ep : entryPoints) {
            if (!visited.contains(ep) && vectors.containsKey(ep)) {
                visited.add(ep);
                double sim = similarityTo(queryVector, ep);
                SearchResult sr = new SearchResult(ep, sim);
                candidates.add(sr);
                results.add(sr);
            }
        }

        while (!candidates.isEmpty()) {
            SearchResult c = candidates.poll(); // 当前最近候选
            SearchResult f = results.peek();    // 结果中最远者
            if (f != null && c.score < f.score) {
                // 最近候选仍比结果中最远者还远，搜索结束
                break;
            }
            Set<Long> neighbors = getNeighbors(layer, c.id);
            for (Long e : neighbors) {
                if (!visited.contains(e)) {
                    visited.add(e);
                    double sim = similarityTo(queryVector, e);
                    SearchResult f2 = results.peek();
                    if (results.size() < ef || (f2 != null && sim > f2.score)) {
                        SearchResult sr = new SearchResult(e, sim);
                        candidates.add(sr);
                        results.add(sr);
                        if (results.size() > ef) {
                            results.poll(); // 淘汰最远者
                        }
                    }
                }
            }
        }
        return results;
    }

    /**
     * 选择 M 个邻居（简单实现：按相似度降序取前 M 个）
     */
    private List<SearchResult> selectNeighbors(List<SearchResult> candidates, int M) {
        if (candidates.size() <= M) {
            // 仍按相似度降序排序
            Collections.sort(candidates, new Comparator<SearchResult>() {
                @Override
                public int compare(SearchResult a, SearchResult b) {
                    return Double.compare(b.score, a.score);
                }
            });
            return new ArrayList<>(candidates);
        }
        Collections.sort(candidates, new Comparator<SearchResult>() {
            @Override
            public int compare(SearchResult a, SearchResult b) {
                return Double.compare(b.score, a.score); // 降序
            }
        });
        List<SearchResult> selected = new ArrayList<>(M);
        for (int i = 0; i < M && i < candidates.size(); i++) {
            selected.add(candidates.get(i));
        }
        return selected;
    }

    // ============== 辅助方法 ==============

    private void ensureLayers(int level) {
        while (graph.size() <= level) {
            graph.add(new ConcurrentHashMap<Long, Set<Long>>());
        }
    }

    private Set<Long> getNeighbors(int layer, long id) {
        if (layer < 0 || layer >= graph.size()) {
            return Collections.emptySet();
        }
        Map<Long, Set<Long>> layerMap = graph.get(layer);
        Set<Long> set = layerMap.get(id);
        return set == null ? Collections.<Long>emptySet() : set;
    }

    private double similarityTo(float[] queryVector, long id) {
        float[] v = vectors.get(id);
        if (v == null) {
            return Double.NEGATIVE_INFINITY;
        }
        return cosineSimilarity(queryVector, v);
    }

    private SearchResult pickClosest(Collection<SearchResult> col) {
        SearchResult best = null;
        for (SearchResult sr : col) {
            if (best == null || sr.score > best.score) {
                best = sr;
            }
        }
        return best;
    }

    /**
     * 对某节点在某层的邻居集合进行剪枝：保留最近的 limit 个
     */
    private void pruneNeighbors(long id, int layer, int limit) {
        Set<Long> neighbors = graph.get(layer).get(id);
        if (neighbors == null || neighbors.size() <= limit) {
            return;
        }
        float[] base = vectors.get(id);
        if (base == null) {
            return;
        }
        List<SearchResult> cand = new ArrayList<>(neighbors.size());
        for (Long n : neighbors) {
            float[] nv = vectors.get(n);
            if (nv == null) {
                continue;
            }
            cand.add(new SearchResult(n, cosineSimilarity(base, nv)));
        }
        List<SearchResult> selected = selectNeighbors(cand, limit);
        Set<Long> newSet = ConcurrentHashMap.newKeySet();
        for (SearchResult sr : selected) {
            newSet.add(sr.id);
        }
        graph.get(layer).put(id, newSet);
    }

    /**
     * 删除入口点后，从最高层剩余节点中重新选择一个入口点
     */
    private void reassignEntryPoint() {
        Long newEp = null;
        int newMax = -1;
        // 从顶层向下寻找第一个有节点的层
        for (int l = graph.size() - 1; l >= 0; l--) {
            Map<Long, Set<Long>> layer = graph.get(l);
            if (layer != null && !layer.isEmpty()) {
                newEp = layer.keySet().iterator().next();
                newMax = l;
                break;
            }
        }
        entryPoint = newEp;
        maxLevel = newMax;
    }

    public static class SearchResult {
        public final long id;
        public final double score;

        public SearchResult(long id, double score) {
            this.id = id;
            this.score = score;
        }

        @Override
        public String toString() {
            return "SearchResult{id=" + id + ", score=" + score + "}";
        }
    }
}
