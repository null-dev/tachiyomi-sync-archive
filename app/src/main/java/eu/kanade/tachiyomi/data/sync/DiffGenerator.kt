package eu.kanade.tachiyomi.data.sync

import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaCategory
import uy.kohesive.injekt.injectLazy
import java.util.*

/**
 * Compares the snapshotted library and the current library.
 */

class DiffGenerator {
    val db: DatabaseHelper by injectLazy()

    fun generate(snapshot: LibrarySnapshot): LibraryDiff {
        val categoryIdGetter = CategoryIdGetter()
        val sortedSnapshotCategories = snapshot.categories.sorted()
        val sortedModifiedCategories = db.getCategories().executeAsBlocking()
                .sortedWith(FunctionComparator(categoryIdGetter))
        val resultCategories = fastIntIdCompare(sortedSnapshotCategories,
                sortedModifiedCategories,
                categoryIdGetter,
                CategoryTimestampGetter(),
                snapshot.timestamp)

        val chapterIdGetter = ChapterIdGetter()
        val sortedSnapshotChapters = snapshot.chapters.sorted()
        val sortedModifiedChapters = db.getAllChapters().executeAsBlocking()
                .sortedWith(FunctionComparator(chapterIdGetter))
        val resultChapters = fastLongIdCompare(sortedSnapshotChapters,
                sortedModifiedChapters,
                chapterIdGetter,
                ChapterTimestampGetter(),
                snapshot.timestamp)

        val mangaIdGetter = MangaIdGetter()
        val sortedSnapshotManga = snapshot.manga.sorted()
        val sortedModifiedManga = db.getMangas().executeAsBlocking()
                .sortedWith(FunctionComparator(mangaIdGetter))
        val resultManga = fastLongIdCompare(sortedSnapshotManga,
                sortedModifiedManga,
                mangaIdGetter,
                MangaTimestampGetter(),
                snapshot.timestamp)

        val mangaCategoryIdGetter = MangaCategoryIdGetter()
        val mangaCategoryIdComparator = FunctionComparator(mangaCategoryIdGetter)
        val sortedSnapshotMangaCategories = snapshot.category_mappings
                .sortedWith(mangaCategoryIdComparator)
        val sortedModifiedMangaCategories = db.getAllMangaCategories().executeAsBlocking()
                .sortedWith(mangaCategoryIdComparator)
        val resultMangaCategories = fastObjectIdCompare(sortedSnapshotMangaCategories,
                sortedModifiedMangaCategories,
                mangaCategoryIdGetter,
                null, //These cannot be modified so we don't bother to store/check timestamp
                snapshot.timestamp)

        fun bsFindManga(targetId: Long): Manga {
            return sortedModifiedManga[sortedModifiedManga.binarySearchBy(key = targetId, selector = { it.id })]
        }

        fun mapMangaCategoryToMappings(mangaCategories: List<MangaCategory>): List<Pair<Int, LibraryDiff.MangaReference>>
                = mangaCategories.map { Pair(it.category_id, LibraryDiff.MangaReference.fromManga(bsFindManga(it.manga_id))) }

        val result = LibraryDiff()
        result.modifiedCategories += resultCategories.added + resultCategories.modified
        result.removedCategories += resultCategories.removed
        result.modifiedChapters += resultChapters.added + resultChapters.modified
        result.modifiedManga += resultManga.added + resultManga.modified
        result.addedMangaCategoryMappings += mapMangaCategoryToMappings(resultMangaCategories.added)
        result.removedMangaCategoryMappings += mapMangaCategoryToMappings(resultMangaCategories.removed)
        return result
    }

    class FunctionComparator<T, FR : Comparable<FR>>(val func: Function<T, FR>) : Comparator<T> {
        override fun compare(o1: T, o2: T)
                = func.apply(o1).compareTo(func.apply(o2))
    }

    class CategoryIdGetter : Function<Category, Int> {
        override fun apply(t: Category) = t.id!!
    }

    class CategoryTimestampGetter : Function<Category, Long> {
        override fun apply(t: Category) = t.last_modified
    }

    class ChapterIdGetter : Function<Chapter, Long> {
        override fun apply(t: Chapter) = t.id!!
    }

    class ChapterTimestampGetter : Function<Chapter, Long> {
        override fun apply(t: Chapter) = t.last_modified
    }

    class MangaIdGetter : Function<Manga, Long> {
        override fun apply(t: Manga) = t.id!!
    }

    class MangaTimestampGetter : Function<Manga, Long> {
        override fun apply(t: Manga) = t.last_modified
    }

    class MangaCategoryIdGetter : Function<MangaCategory, Long> {
        override fun apply(t: MangaCategory) = t.id!!
    }

    /**
     * Faster comparison algorithm
     * Works only on sorted lists
     */
    fun <T> fastIntIdCompare(snapshot: List<Int>,
                             modified: List<T>,
                             idGetter: Function<T, Int>,
                             timestampGetter: Function<T, Long>?,
                             timestamp: Long): IntCompareResult<T> {
        val result = IntCompareResult<T>()

        var modifiedIndex = 0
        for (currentSnapshotId in snapshot) {
            var foundEntry = false
            while (modifiedIndex < modified.size) {
                val currentModified = modified[modifiedIndex]
                val currentModifiedId = idGetter.apply(currentModified)
                if (currentModifiedId > currentSnapshotId) {
                    //This one is too far ahead of the snapshot list, stop!
                    break
                } else if (currentModifiedId == currentSnapshotId) {
                    //This entry may be modified, compare the timestamps
                    if (timestampGetter != null
                            && timestampGetter.apply(currentModified) > timestamp) {
                        result.modified.add(currentModified)
                    }
                    foundEntry = true
                } else {
                    //This entry does not exist in the snapshot! (added)
                    result.added.add(currentModified)
                }
                modifiedIndex++
            }
            //Check that this entry was processed
            if (!foundEntry) {
                //This entry does not exist in the modified list! (removed)
                result.removed.add(currentSnapshotId)
            }
        }
        //Add remaining entries
        for(index in modifiedIndex .. modified.size - 1) {
            result.added.add(modified[index])
        }
        return result
    }

    /**
     * Faster comparison algorithm
     * Works only on sorted lists
     */
    fun <T> fastLongIdCompare(snapshot: List<Long>,
                              modified: List<T>,
                              idGetter: Function<T, Long>,
                              timestampGetter: Function<T, Long>?,
                              timestamp: Long): LongCompareResult<T> {
        val result = LongCompareResult<T>()

        var modifiedIndex = 0
        for (currentSnapshotId in snapshot) {
            var foundEntry = false
            while (modifiedIndex < modified.size) {
                val currentModified = modified[modifiedIndex]
                val currentModifiedId = idGetter.apply(currentModified)
                if (currentModifiedId > currentSnapshotId) {
                    //This one is too far ahead of the snapshot list, stop!
                    break
                } else if (currentModifiedId == currentSnapshotId) {
                    //This entry may be modified, compare the timestamps
                    if (timestampGetter != null
                            && timestampGetter.apply(currentModified) > timestamp) {
                        result.modified.add(currentModified)
                    }
                    foundEntry = true
                } else {
                    //This entry does not exist in the snapshot! (added)
                    result.added.add(currentModified)
                }
                modifiedIndex++
            }
            //Check that this entry was processed
            if (!foundEntry) {
                //This entry does not exist in the modified list! (removed)
                result.removed.add(currentSnapshotId)
            }
        }
        //Add remaining entries
        for(index in modifiedIndex .. modified.size - 1) {
            result.added.add(modified[index])
        }
        return result
    }

    /**
     * Faster comparison algorithm
     * Works only on sorted lists
     */
    fun <T, IDTYPE : Number> fastObjectIdCompare(snapshot: List<T>,
                              modified: List<T>,
                              idGetter: Function<T, IDTYPE>,
                              timestampGetter: Function<T, Long>?,
                              timestamp: Long): ObjectCompareResult<T> {
        val result = ObjectCompareResult<T>()

        var modifiedIndex = 0
        for (currentSnapshotObj in snapshot) {
            val currentSnapshotId = idGetter.apply(currentSnapshotObj).toLong()
            var foundEntry = false
            while (modifiedIndex < modified.size) {
                val currentModified = modified[modifiedIndex]
                val currentModifiedId = idGetter.apply(currentModified).toLong()
                if (currentModifiedId > currentSnapshotId) {
                    //This one is too far ahead of the snapshot list, stop!
                    break
                } else if (currentModifiedId == currentSnapshotId) {
                    //This entry may be modified, compare the timestamps
                    if (timestampGetter != null
                            && timestampGetter.apply(currentModified) > timestamp) {
                        result.modified.add(currentModified)
                    }
                    foundEntry = true
                } else {
                    result.added.add(currentModified)
                    //This entry does not exist in the snapshot! (added)
                }
                modifiedIndex++
            }
            //Check that this entry was processed
            if (!foundEntry) {
                //This entry does not exist in the modified list! (removed)
                result.removed.add(currentSnapshotObj)
            }
        }
        //Add remaining entries
        for(index in modifiedIndex .. modified.size - 1) {
            result.added.add(modified[index])
        }
        return result
    }

    class IntCompareResult<T> {
        val removed = ArrayList<Int>()
        val added = ArrayList<T>()
        val modified = ArrayList<T>()
    }

    class LongCompareResult<T> {
        val removed = ArrayList<Long>()
        val added = ArrayList<T>()
        val modified = ArrayList<T>()
    }

    class ObjectCompareResult<T> {
        val removed = ArrayList<T>()
        val added = ArrayList<T>()
        val modified = ArrayList<T>()
    }

    interface Function<in T, out R> {
        /**
         * Applies this function to the given argument.

         * @param t the function argument
         * *
         * @return the function result
         */
        fun apply(t: T): R
    }
}