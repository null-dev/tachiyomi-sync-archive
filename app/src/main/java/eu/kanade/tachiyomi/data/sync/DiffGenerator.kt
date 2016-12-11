package eu.kanade.tachiyomi.data.sync

import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.*
import timber.log.Timber
import uy.kohesive.injekt.injectLazy
import java.util.*

/**
 * Compares the snapshoted library and the current library.
 */

class DiffGenerator {
    val db: DatabaseHelper by injectLazy()

    fun generate(snapshot: LibrarySnapshot): LibraryDiff {
        val categoryIdGetter = CategoryIdGetter()
        val sortedSnapshotCategories = snapshot.categories
                .sortedWith(FunctionComparator(categoryIdGetter))
        val sortedCurrentCategories = db.getCategories().executeAsBlocking()
                .sortedWith(FunctionComparator(categoryIdGetter))
        val resultCategories = fastObjectIdCompare(sortedSnapshotCategories,
                sortedCurrentCategories,
                categoryIdGetter,
                CategoryTimestampGetter(),
                snapshot.timestamp)

        val currentChapters = db.getAllChapters().executeAsBlocking()
        val modifiedChapters = findModified(currentChapters,
                ChapterTimestampGetter(),
                snapshot.timestamp)

        val sortedSnapshotFavManga = snapshot.favManga.sorted()
        val currentManga = db.getMangas().executeAsBlocking()
                .sortedWith(FunctionComparator(MangaIdGetter()))
        val modifiedManga = fastMangaCompare(sortedSnapshotFavManga,
                currentManga,
                snapshot.timestamp)

        val mangaCategoryIdGetter = MangaCategoryIdGetter()
        val mangaCategoryIdComparator = FunctionComparator(mangaCategoryIdGetter)
        val sortedSnapshotMangaCategories = snapshot.category_mappings
                .sortedWith(mangaCategoryIdComparator)
        val sortedCurrentMangaCategories = db.getAllMangaCategories().executeAsBlocking()
                .sortedWith(mangaCategoryIdComparator)
        val resultMangaCategories = fastObjectIdCompare(sortedSnapshotMangaCategories,
                sortedCurrentMangaCategories,
                mangaCategoryIdGetter,
                null, //These cannot be modified so we don't bother to store/check timestamp
                snapshot.timestamp)

        fun bsFindManga(targetId: Long): Manga {
            return currentManga[currentManga.binarySearchBy(key = targetId, selector = { it.id })]
        }

        fun mapMangaCategoryToMappings(mangaCategories: List<MangaCategory>, removal: Boolean): List<Pair<String, LibraryDiff.MangaReference>> {
            return mangaCategories.map {
                val map = if(removal) sortedSnapshotCategories else sortedCurrentCategories
                val found = map.binarySearchBy(it.category_id, selector = Category::id)
                if(found >= 0) {
                    Pair(map[found].name, LibraryDiff.MangaReference.fromManga(bsFindManga(it.manga_id)))
                } else {
                    Timber.w("Could not map ${it.category_id} to it's category object (removal: $removal)!")
                    null
                }
            }.filterNotNull()
        }

        val result = LibraryDiff()
        result.modifiedCategories += resultCategories.added + resultCategories.modified
        result.removedCategories += resultCategories.removed.map { it.name }
        result.modifiedChapters += modifiedChapters.map {
            it.manga_id?.let { manga_id ->
                val manga = bsFindManga(manga_id)
                if(!manga.favorite) return@map null //Only sync the chapters of favorited manga
                return@map Pair(LibraryDiff.MangaReference.fromManga(manga), it)
            }
            null
        }.filterNotNull()
        result.modifiedManga += modifiedManga
        result.addedMangaCategoryMappings += mapMangaCategoryToMappings(resultMangaCategories.added, false)
        result.removedMangaCategoryMappings += mapMangaCategoryToMappings(resultMangaCategories.removed, true)
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

    class ChapterTimestampGetter : Function<Chapter, Long> {
        override fun apply(t: Chapter) = t.last_modified
    }

    class MangaIdGetter : Function<Manga, Long> {
        override fun apply(t: Manga) = t.id!!
    }

    class MangaCategoryIdGetter : Function<MangaCategory, Long> {
        override fun apply(t: MangaCategory) = t.id!!
    }

    fun <T> findModified(objects: List<T>,
                                 timestampGetter: Function<T, Long>,
                                 timestamp: Long) = objects.filter { timestampGetter.apply(it) > timestamp }

    /**
     * Faster comparison algorithm
     * Works only on sorted lists
     */
    fun fastMangaCompare(oldFavs: List<Long>,
                             current: List<Manga>,
                             timestamp: Long): List<Manga> {
        val result = ArrayList<Manga>()

        var modifiedIndex = 0
        for (currentSnapshotId in oldFavs) {
            while (modifiedIndex < current.size) {
                val currentModified = current[modifiedIndex]
                if (currentModified.id!! > currentSnapshotId) {
                    //This one is too far ahead of the snapshot list, stop!
                    break
                } else if (currentModified.id!! == currentSnapshotId || currentModified.favorite) {
                    //This entry may be modified, compare the timestamps
                    if (currentModified.last_modified > timestamp) {
                        result.add(currentModified)
                    }
                }
                modifiedIndex++
            }
        }
        //Add remaining entries
        (modifiedIndex .. current.size - 1)
                .map { current[it] }
                .filterTo(result) { it.favorite }
        return result
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