package com.example.happyhunt.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Upsert
import com.example.happyhunt.domain.GeoPoint
import com.example.happyhunt.domain.Kind
import com.example.happyhunt.domain.Place
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

/**
 * A saved place keeps a copy of everything shown about it, so the Saved tab
 * works offline and a place stays even if it disappears from the map.
 */
@Entity(tableName = "saved_place")
data class SavedPlaceEntity(
    @PrimaryKey val id: String,
    val name: String?,
    val kind: String,
    val lat: Double,
    val lon: Double,
    /** The kept OpenStreetMap tags, as JSON. */
    val tags: String,
    val savedAt: Long,
)

@Dao
interface SavedPlaceDao {
    @Query("SELECT * FROM saved_place ORDER BY savedAt DESC")
    fun all(): Flow<List<SavedPlaceEntity>>

    @Query("SELECT id FROM saved_place")
    fun ids(): Flow<List<String>>

    @Query("SELECT * FROM saved_place WHERE id = :id")
    suspend fun find(id: String): SavedPlaceEntity?

    @Upsert
    suspend fun upsert(place: SavedPlaceEntity)

    @Query("DELETE FROM saved_place WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM saved_place")
    suspend fun deleteAll()
}

@Database(entities = [SavedPlaceEntity::class], version = 1)
abstract class SavedDatabase : RoomDatabase() {
    abstract fun places(): SavedPlaceDao

    companion object {
        /** Matches the backup rules, which keep only this database and the settings. */
        const val NAME = "saved.db"

        fun open(context: Context): SavedDatabase =
            Room.databaseBuilder(context, SavedDatabase::class.java, NAME).build()
    }
}

data class SavedPlace(val place: Place, val savedAt: Long)

class SavedPlacesRepository(private val dao: SavedPlaceDao, private val now: () -> Long = System::currentTimeMillis) {
    private val json = Json { ignoreUnknownKeys = true }

    val places: Flow<List<SavedPlace>> = dao.all().map { rows -> rows.mapNotNull { it.toSaved() } }

    val ids: Flow<Set<String>> = dao.ids().map { it.toSet() }

    suspend fun find(id: String): Place? = dao.find(id)?.toSaved()?.place

    /** Saves the place, or refreshes the copy of one already saved without moving it in the list. */
    suspend fun save(place: Place, savedAt: Long? = null) {
        val at = savedAt ?: dao.find(place.id)?.savedAt ?: now()
        dao.upsert(
            SavedPlaceEntity(
                id = place.id,
                name = place.name,
                kind = place.kind.name,
                lat = place.point.lat,
                lon = place.point.lon,
                tags = json.encodeToString(place.tags),
                savedAt = at,
            ),
        )
    }

    suspend fun remove(id: String) = dao.delete(id)

    suspend fun clear() = dao.deleteAll()

    private fun SavedPlaceEntity.toSaved(): SavedPlace? {
        val kind = Kind.entries.firstOrNull { it.name == kind } ?: return null
        val tags = runCatching { json.decodeFromString<Map<String, String>>(tags) }.getOrDefault(emptyMap())
        return SavedPlace(Place(id, name, kind, GeoPoint(lat, lon), tags), savedAt)
    }
}
