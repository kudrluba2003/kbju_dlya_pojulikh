package com.senior.kbju.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.senior.kbju.data.local.dao.ActivityDao
import com.senior.kbju.data.local.dao.FoodDao
import com.senior.kbju.data.local.dao.MealLogDao
import com.senior.kbju.data.local.dao.WaterLogDao
import com.senior.kbju.data.local.dao.WeightLogDao
import com.senior.kbju.data.local.entity.ActivityEntity
import com.senior.kbju.data.local.entity.ActivityLogEntity
import com.senior.kbju.data.local.entity.FoodEntity
import com.senior.kbju.data.local.entity.MealLogEntity
import com.senior.kbju.data.local.entity.WaterLogEntity
import com.senior.kbju.data.local.entity.WeightLogEntity

// Профиль пользователя переехал в DataStore (см. data/preferences) —
// Room отвечает только за каталог продуктов, дневник, воду и динамику веса.
@Database(
    entities = [
        FoodEntity::class,
        MealLogEntity::class,
        ActivityEntity::class,
        ActivityLogEntity::class,
        WeightLogEntity::class,
        WaterLogEntity::class
    ],
    version = 7,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun foodDao(): FoodDao
    abstract fun mealLogDao(): MealLogDao
    abstract fun activityDao(): ActivityDao
    abstract fun weightLogDao(): WeightLogDao
    abstract fun waterLogDao(): WaterLogDao

    companion object {
        // Миграция 5 -> 6: добавление таблиц динамики веса, трекера воды и типа приёма пищи.
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `weight_log` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`day` INTEGER NOT NULL, " +
                        "`weightKg` REAL NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `water_log` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`day` INTEGER NOT NULL, " +
                        "`glasses` INTEGER NOT NULL, " +
                        "`ml` INTEGER NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "ALTER TABLE `meal_log` ADD COLUMN `mealType` TEXT NOT NULL DEFAULT 'LUNCH'"
                )
            }
        }

        // Миграция 6 -> 7: добавление индексов для ускорения поиска продуктов и выборки дневника.
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_food_name` ON `food` (`name`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_food_isUserCreated_name` ON `food` (`isUserCreated`, `name`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_meal_log_day` ON `meal_log` (`day`)")
            }
        }

        // Реестр миграций между версиями базы данных.
        val ALL_MIGRATIONS: Array<Migration> = arrayOf(
            MIGRATION_5_6,
            MIGRATION_6_7
        )

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "senior_kbju.db")
                .addMigrations(*ALL_MIGRATIONS)
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()
    }
}
