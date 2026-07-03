package com.example.myapplication

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.*

enum class CalendarType {
    JALALI,      // Solar Hijri / Persian
    GREGORIAN,   // Gregorian
    HIJRI        // Islamic / Lunar
}

enum class WeekendMode {
    THU_FRI, // Thu + Fri weekend
    SAT_SUN  // Sat + Sun weekend
}

// ------------------------------------------------------------
// Converters (java.time + enums for Room)
// ------------------------------------------------------------

object PlannerConverters {
    @TypeConverter fun instantToLong(v: Instant?): Long? = v?.toEpochMilli()
    @TypeConverter fun longToInstant(v: Long?): Instant? = v?.let(Instant::ofEpochMilli)

    @TypeConverter fun localDateToLong(v: LocalDate?): Long? = v?.toEpochDay()
    @TypeConverter fun longToLocalDate(v: Long?): LocalDate? = v?.let(LocalDate::ofEpochDay)

    @TypeConverter fun localTimeToString(v: LocalTime?): String? = v?.toString()
    @TypeConverter fun stringToLocalTime(v: String?): LocalTime? = v?.let(LocalTime::parse)

    @TypeConverter fun dayOfWeekToInt(v: DayOfWeek?): Int? = v?.value
    @TypeConverter fun intToDayOfWeek(v: Int?): DayOfWeek? = v?.let(DayOfWeek::of)

    // Enums
    @TypeConverter fun calendarTypeToString(v: CalendarType?): String? = v?.name
    @TypeConverter fun stringToCalendarType(v: String?): CalendarType? =
        v?.let { runCatching { CalendarType.valueOf(it) }.getOrNull() }

    @TypeConverter fun weekendModeToString(v: WeekendMode?): String? = v?.name
    @TypeConverter fun stringToWeekendMode(v: String?): WeekendMode? =
        v?.let { runCatching { WeekendMode.valueOf(it) }.getOrNull() }
}

// ------------------------------------------------------------
// Entities
// ------------------------------------------------------------

@Entity(
    tableName = "events",
    indices = [Index("startsAt"), Index("endsAt")]
)

data class EventEntity(
    @PrimaryKey val id: String,
    val title: String,
    val notes: String?,
    val startsAt: Instant, // UTC
    val endsAt: Instant,   // UTC
    val allDay: Boolean,
    val done: Boolean
)

@Entity(
    tableName = "weekly_slots",
    indices = [Index(value = ["dayOfWeek", "startMinutes"])]
)

data class WeeklySlotEntity(
    @PrimaryKey val id: String,
    val dayOfWeek: DayOfWeek,
    val startMinutes: Int,
    val title: String
)

@Entity(tableName = "reminders", indices = [Index("timeOfDay")])

data class ReminderEntity(
    @PrimaryKey val id: String,
    val title: String,
    val timeOfDay: LocalTime,
    val type: Int,
    val daysMask: Int,
    val dayOfMonth: Int,
    val month: Int,
    val lastDoneOn: LocalDate?
)

@Entity(tableName = "settings")

data class SettingsEntity(
    @PrimaryKey val singleton: Int = 0,
    val firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    val defaultReminderMinutes: Int = 10,
    val vibrationOnReminder: Boolean = true,
    val soundOnReminder: Boolean = true,

    // Language & Time
    val languageTag: String? = null,
    val timeZoneId: String? = null,

    // Calendar preferences (NEW)
    val defaultCalendarType: CalendarType = CalendarType.JALALI,
    val weekendMode: WeekendMode = WeekendMode.THU_FRI
)

@Entity(
    tableName = "goals",
    indices = [Index("startMonth"), Index("everyDay")]
)

data class GoalEntity(
    @PrimaryKey val id: String,
    val title: String,
    val startMonth: Int,
    val monthsCount: Int,
    val everyDay: Boolean,
    val daysMask: Int,
    val timeOfDay: LocalTime?
)

// ------------------------------------------------------------
// DAOs
// ------------------------------------------------------------

@Dao
interface EventDao {
    @Query(
        """
        SELECT * FROM events
        WHERE startsAt >= :start AND startsAt < :end
        ORDER BY startsAt
        """
    )
    fun observeRange(start: Instant, end: Instant): Flow<List<EventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: EventEntity)

    @Query("DELETE FROM events WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM events WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): EventEntity?
}

@Dao
interface WeeklySlotDao {
    @Query("SELECT * FROM weekly_slots WHERE dayOfWeek = :day ORDER BY startMinutes")
    fun observeDay(day: DayOfWeek): Flow<List<WeeklySlotEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: WeeklySlotEntity)

    @Query("DELETE FROM weekly_slots WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY timeOfDay")
    fun observeAll(): Flow<List<ReminderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ReminderEntity)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings WHERE singleton = 0")
    fun observe(): Flow<SettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SettingsEntity)

    @Query("DELETE FROM settings")
    suspend fun clear()
}

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals ORDER BY startMonth, title")
    fun observeAll(): Flow<List<GoalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: GoalEntity)

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM goals WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): GoalEntity?
}

// ------------------------------------------------------------
// Database
// ------------------------------------------------------------

@Database(
    entities = [
        EventEntity::class,
        WeeklySlotEntity::class,
        ReminderEntity::class,
        SettingsEntity::class,
        GoalEntity::class
    ],
    version = 3,
    exportSchema = true
)
@TypeConverters(PlannerConverters::class)
abstract class PlannerDb : RoomDatabase() {
    abstract fun eventDao(): EventDao
    abstract fun weeklySlotDao(): WeeklySlotDao
    abstract fun reminderDao(): ReminderDao
    abstract fun settingsDao(): SettingsDao
    abstract fun goalDao(): GoalDao

    companion object {
        // Previous migration you already had
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE settings ADD COLUMN vibrationOnReminder INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE settings ADD COLUMN soundOnReminder INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE settings ADD COLUMN languageTag TEXT")
                db.execSQL("ALTER TABLE settings ADD COLUMN timeZoneId TEXT")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS goals (
                        id TEXT NOT NULL,
                        title TEXT NOT NULL,
                        startMonth INTEGER NOT NULL,
                        monthsCount INTEGER NOT NULL,
                        everyDay INTEGER NOT NULL,
                        daysMask INTEGER NOT NULL,
                        timeOfDay TEXT,
                        PRIMARY KEY(id)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_goals_startMonth ON goals(startMonth)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_goals_everyDay ON goals(everyDay)")
            }
        }

        // NEW: settings calendar prefs
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Store enums as TEXT values (their name)
                db.execSQL("ALTER TABLE settings ADD COLUMN defaultCalendarType TEXT NOT NULL DEFAULT 'JALALI'")
                db.execSQL("ALTER TABLE settings ADD COLUMN weekendMode TEXT NOT NULL DEFAULT 'THU_FRI'")
            }
        }
    }
}

// ------------------------------------------------------------
// Facade types
// ------------------------------------------------------------

data class UiTodayEvent(
    val id: String,
    val title: String,
    val done: Boolean,
    val time: LocalTime? // null = all-day
)

data class UiGoal(
    val id: String,
    val title: String,
    val startMonth: Int,
    val monthsCount: Int,
    val everyDay: Boolean,
    val daysOfWeek: Set<DayOfWeek>,
    val time: LocalTime?
)

// ------------------------------------------------------------
// PlannerData (owns settings + app language apply)
// ------------------------------------------------------------

object PlannerData {

    @Volatile private var _db: PlannerDb? = null
    private val db: PlannerDb
        get() = _db ?: error("PlannerData not initialized. Call PlannerData.init(context).")

    @Volatile private var appScope: CoroutineScope? = null
    @Volatile private var localeSyncStarted: Boolean = false

    fun init(context: Context) {
        if (_db == null) {
            synchronized(this) {
                if (_db == null) {
                    _db = Room.databaseBuilder(
                        context.applicationContext,
                        PlannerDb::class.java,
                        "planner.db"
                    )
                        .addMigrations(PlannerDb.MIGRATION_1_2, PlannerDb.MIGRATION_2_3)
                        .build()
                }
            }
        }

        if (appScope == null) {
            synchronized(this) {
                if (appScope == null) {
                    appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
                }
            }
        }

        startLocaleSyncIfNeeded()
    }

    // ---------------- SETTINGS ----------------

    fun observeSettings(): Flow<SettingsEntity> =
        db.settingsDao().observe().map { it ?: SettingsEntity() }

    suspend fun upsertSettings(update: (SettingsEntity) -> SettingsEntity) {
        val current = db.settingsDao().observe().first() ?: SettingsEntity()
        db.settingsDao().upsert(update(current).copy(singleton = 0))
    }

    suspend fun setLanguageTag(languageTag: String?) {
        val tag = languageTag?.takeIf { it.isNotBlank() }
        applyAppLanguage(tag)
        upsertSettings { it.copy(languageTag = tag) }
    }

    suspend fun setDefaultCalendarType(type: CalendarType) {
        upsertSettings { it.copy(defaultCalendarType = type) }
    }

    suspend fun setWeekendMode(mode: WeekendMode) {
        upsertSettings { it.copy(weekendMode = mode) }
    }

    suspend fun resetFactory() {
        db.clearAllTables()
        db.settingsDao().upsert(SettingsEntity())
    }

    // ---------------- APP-WIDE LANGUAGE APPLY ----------------

    private fun startLocaleSyncIfNeeded() {
        if (localeSyncStarted) return
        synchronized(this) {
            if (localeSyncStarted) return
            localeSyncStarted = true

            val scope = appScope ?: return
            scope.launch {
                observeSettings()
                    .map { it.languageTag }
                    .distinctUntilChanged()
                    .collect { tag ->
                        applyAppLanguage(tag)
                    }
            }
        }
    }

    private fun applyAppLanguage(languageTag: String?) {
        val locales = if (languageTag.isNullOrBlank()) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(languageTag)
        }
        AppCompatDelegate.setApplicationLocales(locales)
    }

    // ---------- helpers (days mask) ----------

    private fun dowToBitIndex(d: DayOfWeek): Int = when (d) {
        DayOfWeek.MONDAY -> 0
        DayOfWeek.TUESDAY -> 1
        DayOfWeek.WEDNESDAY -> 2
        DayOfWeek.THURSDAY -> 3
        DayOfWeek.FRIDAY -> 4
        DayOfWeek.SATURDAY -> 5
        DayOfWeek.SUNDAY -> 6
    }

    fun daysToMask(days: Set<DayOfWeek>): Int {
        var mask = 0
        for (d in days) mask = mask or (1 shl dowToBitIndex(d))
        return mask
    }

    fun maskToDays(mask: Int): Set<DayOfWeek> {
        val out = LinkedHashSet<DayOfWeek>()
        DayOfWeek.entries.forEach { d ->
            val bit = 1 shl dowToBitIndex(d)
            if ((mask and bit) != 0) out += d
        }
        return out
    }

    private fun effectiveZoneId(settings: SettingsEntity): ZoneId {
        val id = settings.timeZoneId?.takeIf { it.isNotBlank() }
        return runCatching { if (id != null) ZoneId.of(id) else ZoneId.systemDefault() }
            .getOrElse { ZoneId.systemDefault() }
    }

    fun observeEffectiveZoneId(): Flow<ZoneId> =
        observeSettings()
            .map { effectiveZoneId(it) }
            .distinctUntilChanged()

    // ---------- goals helpers ----------

    private fun rotateMonths(startMonth: Int, count: Int): Set<Int> {
        if (count <= 0) return emptySet()
        val out = LinkedHashSet<Int>(count)
        var m = startMonth.coerceIn(1, 12)
        repeat(count.coerceAtMost(120)) {
            out += m
            m = if (m == 12) 1 else m + 1
        }
        return out
    }

    private fun GoalEntity.occursOn(date: LocalDate): Boolean {
        val monthOk = rotateMonths(startMonth, monthsCount).contains(date.monthValue)
        val dowOk = everyDay || maskToDays(daysMask).contains(date.dayOfWeek)
        return monthOk && dowOk
    }

    private fun GoalEntity.toUi(): UiGoal {
        val days = if (everyDay) DayOfWeek.entries.toSet() else maskToDays(daysMask)
        return UiGoal(id, title, startMonth, monthsCount, everyDay, days, timeOfDay)
    }

    // ---------------- TODAY ----------------

    fun observeTodayEvents(date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Flow<List<UiTodayEvent>> {
        val start = date.atStartOfDay(zone).toInstant()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant()
        return db.eventDao().observeRange(start, end).map { list ->
            list.map { e ->
                UiTodayEvent(
                    id = e.id,
                    title = e.title,
                    done = e.done,
                    time = if (e.allDay) null else e.startsAt.atZone(zone).toLocalTime()
                )
            }
        }
    }

    suspend fun addTodayEvent(date: LocalDate, title: String, time: LocalTime?, zone: ZoneId = ZoneId.systemDefault()) {
        val id = java.util.UUID.randomUUID().toString()
        val (start, end, allDay) =
            if (time == null) {
                val s = date.atStartOfDay(zone).toInstant()
                val e = date.plusDays(1).atStartOfDay(zone).toInstant()
                Triple(s, e, true)
            } else {
                val s = date.atTime(time).atZone(zone).toInstant()
                val e = s.plus(Duration.ofHours(1))
                Triple(s, e, false)
            }

        db.eventDao().upsert(
            EventEntity(
                id = id,
                title = title.trim(),
                notes = null,
                startsAt = start,
                endsAt = end,
                allDay = allDay,
                done = false
            )
        )
    }

    suspend fun setEventDone(id: String, done: Boolean) {
        val dao = db.eventDao()
        val entity = dao.findById(id) ?: return
        dao.upsert(entity.copy(done = done))
    }

    suspend fun renameEvent(id: String, newTitle: String) {
        val dao = db.eventDao()
        val entity = dao.findById(id) ?: return
        dao.upsert(entity.copy(title = newTitle.trim()))
    }

    suspend fun deleteEvent(id: String) {
        db.eventDao().deleteById(id)
    }

    // ---------------- WEEKLY ----------------

    fun observeWeekly(day: DayOfWeek): Flow<List<Pair<Int, String>>> =
        db.weeklySlotDao().observeDay(day)
            .map { slots -> slots.sortedBy(WeeklySlotEntity::startMinutes).map { it.startMinutes to it.title } }

    suspend fun addWeeklySlot(day: DayOfWeek, startMinutes: Int, title: String) {
        db.weeklySlotDao().insert(
            WeeklySlotEntity(
                id = java.util.UUID.randomUUID().toString(),
                dayOfWeek = day,
                startMinutes = startMinutes.coerceIn(0, 23 * 60 + 59),
                title = title.trim()
            )
        )
    }

    suspend fun deleteWeeklySlotByIndex(day: DayOfWeek, startMinutes: Int, indexInBlock: Int) {
        val dao = db.weeklySlotDao()
        val rows = dao.observeDay(day).first()
        val blockRows = rows.filter { it.startMinutes == startMinutes }
        val target = blockRows.getOrNull(indexInBlock) ?: return
        dao.deleteById(target.id)
    }

    // ---------------- GOALS ----------------

    fun observeGoals(): Flow<List<UiGoal>> = db.goalDao().observeAll().map { list -> list.map { it.toUi() } }

    fun observeGoalsForDate(date: LocalDate): Flow<List<UiGoal>> =
        db.goalDao().observeAll().map { list -> list.filter { it.occursOn(date) }.map { it.toUi() } }

    suspend fun upsertGoal(ui: UiGoal) {
        val daysMask = if (ui.everyDay) 0 else daysToMask(ui.daysOfWeek)
        db.goalDao().upsert(
            GoalEntity(
                id = ui.id,
                title = ui.title.trim(),
                startMonth = ui.startMonth.coerceIn(1, 12),
                monthsCount = ui.monthsCount.coerceAtLeast(1),
                everyDay = ui.everyDay,
                daysMask = daysMask,
                timeOfDay = ui.time
            )
        )
    }

    suspend fun deleteGoal(id: String) {
        db.goalDao().deleteById(id)
    }

    // ---------------- WEEKLY helper: timed goals for a weekday ----------------

    fun observeTimedGoalsForDayOfWeek(
        day: DayOfWeek,
        referenceDate: LocalDate = LocalDate.now()
    ): Flow<List<Pair<Int, String>>> {

        val targetDate = referenceDate.with(java.time.temporal.TemporalAdjusters.nextOrSame(day))

        return observeGoalsForDate(targetDate).map { goals ->
            goals
                .filter { it.time != null }
                .map { (it.time!!.hour * 60 + it.time!!.minute) to it.title }
                .sortedBy { it.first }
        }
    }
}
