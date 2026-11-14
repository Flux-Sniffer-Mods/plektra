package rocks.pkb.plektra.data.emoji

import android.content.Context
import android.os.Build
import androidx.annotation.VisibleForTesting
import rocks.pkb.plektra.data.search.SearchScorer
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Shared emoji search index used by both the settings picker dialog and the inline IME picker.
 *
 * Search terms are loaded from local CLDR-derived assets in assets/common/emoji_search/<locale>.tsv.
 */
object EmojiSearchRepository {
    private const val SEARCH_ASSET_DIR = "common/emoji_search"
    private const val EN_LOCALE = "en"

    /**
     * [matched] is the exact sequence the query named, typed or pasted as an emoji (👋🏽, 👩🏽‍💻):
     * what the result shows and types instead of [entry] in the default skin tone.
     */
    data class EmojiSearchResult(
        val entry: EmojiRepository.EmojiEntry,
        val categoryId: String,
        val score: Int,
        val matched: String? = null
    )

    class EmojiSearchIndex internal constructor(
        internal val items: List<IndexedEmoji>
    )

    internal data class IndexedEmoji(
        val entry: EmojiRepository.EmojiEntry,
        val categoryId: String,
        val categoryOrder: Int,
        val terms: List<SearchScorer.Term>
    )

    internal data class MetadataRecord(
        val name: String?,
        val keywords: List<String>
    )

    private data class RankedResult(
        val result: EmojiSearchResult,
        val categoryOrder: Int
    )

    private val cacheMutex = Mutex()
    private val indexCache = LinkedHashMap<String, EmojiSearchIndex>()
    private const val MAX_CACHED_INDEXES = 3

    /** The last index built, by locale chain: read without waiting (the typing path). */
    @Volatile
    private var lastBuilt: Pair<String, EmojiSearchIndex>? = null

    /** The index for the device's languages if it is built already, else null. */
    fun cachedSearchIndex(context: Context): EmojiSearchIndex? {
        val built = lastBuilt ?: return null
        return built.second.takeIf { built.first == getPreferredLocaleChain(context).joinToString("|") }
    }

    /** Words that name too many emoji, or none in particular, to suggest one for. */
    private val GENERIC_WORDS = setOf(
        "button", "symbol", "sign", "mark", "face", "hand", "up", "down", "left", "right", "arrow",
        "time", "done", "double", "fast", "person", "man", "woman", "people", "light", "flag",
        "small", "large", "big", "black", "white", "open", "closed", "square", "circle", "other",
        "the", "and", "with", "for", "you", "not", "new", "top", "end", "back", "on", "off", "free", "ok"
    )
    private const val MAX_KEYWORD_EMOJI = 30

    /**
     * One emoji for a typed word, for the suggestion bar: one whose name is the word ("pizza",
     * "rocket"), else the first in picker order of those with it as a keyword ("happy", "love"),
     * unless the word is generic or names too many emoji. The index holds only emoji the
     * device can draw.
     */
    fun suggestionFor(index: EmojiSearchIndex, word: String): String? {
        if (word.length < 3) return null
        val query = normalizeSearchText(word)
        if (query.isEmpty() || query in GENERIC_WORDS) return null
        index.items.firstOrNull { item ->
            item.terms.any { it.kind == SearchScorer.Kind.NAME && it.text == query }
        }?.let { return it.entry.base }
        val byKeyword = index.items.filter { item ->
            item.terms.any { it.kind == SearchScorer.Kind.KEYWORD && it.text == query }
        }
        if (byKeyword.isEmpty() || byKeyword.size > MAX_KEYWORD_EMOJI) return null
        return byKeyword.minByOrNull { it.categoryOrder }?.entry?.base
    }

    suspend fun getSearchIndex(context: Context): EmojiSearchIndex {
        val localeChain = getPreferredLocaleChain(context)
        val cacheKey = localeChain.joinToString("|")

        cacheMutex.withLock {
            indexCache[cacheKey]?.let { existing ->
                return existing
            }
        }

        val built = withContext(Dispatchers.IO) {
            buildIndex(context, localeChain)
        }

        cacheMutex.withLock {
            indexCache[cacheKey]?.let { existing ->
                return existing
            }
            indexCache[cacheKey] = built
            lastBuilt = cacheKey to built
            while (indexCache.size > MAX_CACHED_INDEXES) {
                val firstKey = indexCache.entries.firstOrNull()?.key ?: break
                indexCache.remove(firstKey)
            }
            return built
        }
    }

    fun search(
        index: EmojiSearchIndex,
        query: String,
        limit: Int = 200
    ): List<EmojiSearchResult> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()

        // Emoji typed or pasted as the query (👋🏽, 🫱🏻‍🫲🏼) normalize to nothing: they match literally.
        val normalizedQuery = normalizeSearchText(trimmed)
        val literalQuery = withoutVariationSelectors(trimmed)

        return index.items.mapNotNull { item ->
            val score = scoreItem(item, literalQuery, normalizedQuery)
            if (score <= 0) return@mapNotNull null
            RankedResult(
                result = EmojiSearchResult(
                    entry = item.entry,
                    categoryId = item.categoryId,
                    score = score,
                    matched = (listOf(item.entry.base) + item.entry.variants)
                        .firstOrNull { withoutVariationSelectors(it) == literalQuery }
                ),
                categoryOrder = item.categoryOrder
            )
        }
            .sortedWith(
                compareByDescending<RankedResult> { it.result.score }
                    .thenBy { it.categoryOrder }
                    .thenBy { it.result.entry.base }
            )
            .map { it.result }
            .take(limit)
    }

    fun clearCache() {
        lastBuilt = null
        indexCache.clear()
    }

    /** [text] without emoji/text presentation selectors, so ❤ finds ❤️ and the other way round. */
    private fun withoutVariationSelectors(text: String): String =
        text.filterNot { it == '\uFE0F' || it == '\uFE0E' }

    private fun scoreItem(
        item: IndexedEmoji,
        literalQuery: String,
        normalizedQuery: String
    ): Int {
        if (withoutVariationSelectors(item.entry.base) == literalQuery) return 2000
        if (item.entry.variants.any { withoutVariationSelectors(it) == literalQuery }) return 1900

        val best = SearchScorer.score(item.terms, normalizedQuery)
        if (best == 0) return 0
        // Stable preference for canonical category order.
        return best - item.categoryOrder
    }

    private suspend fun buildIndex(context: Context, localeChain: List<String>): EmojiSearchIndex {
        val categories = EmojiRepository.getEmojiCategories(context)
        val metadataByEmoji = loadMetadataMap(context, localeChain)

        val items = ArrayList<IndexedEmoji>()
        categories.forEachIndexed { categoryOrder, category ->
            category.emojis.forEach { entry ->
                val terms = LinkedHashMap<String, SearchScorer.Term>()

                addTermsForEmoji(
                    terms = terms,
                    metadataByEmoji = metadataByEmoji,
                    emoji = entry.base
                )
                entry.variants.forEach { variant ->
                    addTermsForEmoji(
                        terms = terms,
                        metadataByEmoji = metadataByEmoji,
                        emoji = variant
                    )
                }

                // If no metadata is available, still index the literal emoji string as a fallback.
                if (terms.isEmpty()) {
                    val literal = normalizeSearchText(entry.base)
                    if (literal.isNotEmpty()) {
                        terms[literal] = SearchScorer.Term(literal, SearchScorer.Kind.KEYWORD, false)
                    }
                }

                items.add(
                    IndexedEmoji(
                        entry = entry,
                        categoryId = category.id,
                        categoryOrder = categoryOrder,
                        terms = terms.values.toList()
                    )
                )
            }
        }

        return EmojiSearchIndex(items = items)
    }

    private fun addTermsForEmoji(
        terms: MutableMap<String, SearchScorer.Term>,
        metadataByEmoji: Map<String, List<Pair<MetadataRecord, Boolean>>>,
        emoji: String
    ) {
        val records = metadataByEmoji[emoji].orEmpty()
        records.forEach { (record, preferredLocale) ->
            record.name?.let { name ->
                val normalized = normalizeSearchText(name)
                if (normalized.isNotEmpty() && normalized !in terms) {
                    terms[normalized] = SearchScorer.Term(normalized, SearchScorer.Kind.NAME, preferredLocale)
                }
            }
            record.keywords.forEach { keyword ->
                val normalized = normalizeSearchText(keyword)
                if (normalized.isNotEmpty() && normalized !in terms) {
                    terms[normalized] = SearchScorer.Term(normalized, SearchScorer.Kind.KEYWORD, preferredLocale)
                }
            }
        }
    }

    private fun loadMetadataMap(
        context: Context,
        localeChain: List<String>
    ): Map<String, List<Pair<MetadataRecord, Boolean>>> {
        val out = LinkedHashMap<String, MutableList<Pair<MetadataRecord, Boolean>>>()
        localeChain.forEachIndexed { index, localeTag ->
            val isPreferred = index == 0
            val records = loadMetadataAsset(context, localeTag) ?: return@forEachIndexed
            records.forEach { (emoji, record) ->
                out.getOrPut(emoji) { mutableListOf() }.add(record to isPreferred)
            }
        }
        return out
    }

    private fun loadMetadataAsset(
        context: Context,
        localeTag: String
    ): Map<String, MetadataRecord>? {
        val candidates = localeAssetCandidates(localeTag)
        for (candidate in candidates) {
            val assetPath = "$SEARCH_ASSET_DIR/$candidate.tsv"
            val parsed = runCatching {
                context.assets.open(assetPath).use { input ->
                    BufferedReader(InputStreamReader(input)).lineSequence()
                        .mapNotNull { line ->
                            if (line.isBlank()) return@mapNotNull null
                            val parts = line.split('\t')
                            if (parts.isEmpty()) return@mapNotNull null
                            val emoji = parts.getOrNull(0)?.trim().orEmpty()
                            if (emoji.isEmpty()) return@mapNotNull null
                            val name = parts.getOrNull(1)?.takeIf { it.isNotBlank() }
                            val keywords = parts.getOrNull(2)
                                ?.split('|')
                                ?.map { it.trim() }
                                ?.filter { it.isNotEmpty() }
                                .orEmpty()
                            emoji to MetadataRecord(name = name, keywords = keywords)
                        }
                        .toMap()
                }
            }.getOrNull()

            if (!parsed.isNullOrEmpty()) {
                return parsed
            }
        }
        return null
    }

    private fun localeAssetCandidates(localeTag: String): List<String> {
        val normalized = localeTag.replace('-', '_').lowercase(Locale.ROOT)
        val languageOnly = normalized.substringBefore('_')
        return buildList {
            if (normalized.isNotBlank()) add(normalized)
            if (languageOnly.isNotBlank() && languageOnly != normalized) add(languageOnly)
        }.distinct()
    }

    internal fun getPreferredLocaleChain(context: Context): List<String> {
        val locales = context.resources.configuration.locales
        val out = ArrayList<String>()
        for (i in 0 until locales.size()) {
            val locale = locales[i] ?: continue
            val tag = locale.toLanguageTag()
            if (tag.isNotBlank()) out.add(tag)
            val language = locale.language
            if (!language.isNullOrBlank()) out.add(language)
        }
        out.add(EN_LOCALE)
        return out.distinct()
    }

    @VisibleForTesting
    internal fun normalizeSearchText(text: String): String = SearchScorer.normalize(text)
}
