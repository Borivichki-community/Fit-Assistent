package dev.andre.nutritionist.data.local

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

object DatabaseMigrations {

    val MIGRATION_2_3: Migration = object : Migration(2, 3) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `local_recipes` (
                    `id` TEXT NOT NULL,
                    `author_type` TEXT NOT NULL,
                    `author_id` TEXT,
                    `origin_recipe_id` TEXT,
                    `title` TEXT NOT NULL,
                    `description` TEXT,
                    `instructions` TEXT,
                    `visibility` TEXT NOT NULL,
                    `servings_count` INTEGER NOT NULL,
                    `total_weight_g` REAL NOT NULL,
                    `calories_per_100g` REAL NOT NULL,
                    `proteins_per_100g` REAL NOT NULL,
                    `fats_per_100g` REAL NOT NULL,
                    `carbs_per_100g` REAL NOT NULL,
                    `sync_status` TEXT NOT NULL,
                    `idempotency_key` TEXT NOT NULL,
                    `is_deleted` INTEGER NOT NULL,
                    `updated_at` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )

            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `local_recipe_ingredients` (
                    `id` TEXT NOT NULL,
                    `recipe_id` TEXT NOT NULL,
                    `product_id` TEXT,
                    `product_name` TEXT NOT NULL,
                    `weight_grams` REAL NOT NULL,
                    `calories` REAL NOT NULL,
                    `proteins` REAL NOT NULL,
                    `fats` REAL NOT NULL,
                    `carbs` REAL NOT NULL,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`recipe_id`) REFERENCES `local_recipes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )

            connection.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_local_recipe_ingredients_recipe_id` ON `local_recipe_ingredients` (`recipe_id`)",
            )

            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `local_meal_plans` (
                    `id` TEXT NOT NULL,
                    `author_id` TEXT NOT NULL,
                    `author_type` TEXT NOT NULL,
                    `title` TEXT NOT NULL,
                    `description` TEXT,
                    `duration_days` INTEGER NOT NULL,
                    `visibility` TEXT NOT NULL,
                    `access_mode` TEXT NOT NULL,
                    `price_amount_minor` INTEGER NOT NULL,
                    `currency` TEXT NOT NULL,
                    `avg_daily_calories` REAL NOT NULL,
                    `avg_daily_proteins` REAL NOT NULL,
                    `avg_daily_fats` REAL NOT NULL,
                    `avg_daily_carbs` REAL NOT NULL,
                    `sync_status` TEXT NOT NULL,
                    `updated_at` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )

            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `local_meal_plan_items` (
                    `id` TEXT NOT NULL,
                    `plan_id` TEXT NOT NULL,
                    `day_number` INTEGER NOT NULL,
                    `meal_slot` TEXT NOT NULL,
                    `item_type` TEXT NOT NULL,
                    `item_id` TEXT NOT NULL,
                    `name_snapshot` TEXT NOT NULL,
                    `weight_grams` REAL NOT NULL,
                    `calories` REAL NOT NULL,
                    `proteins` REAL NOT NULL,
                    `fats` REAL NOT NULL,
                    `carbs` REAL NOT NULL,
                    `estimated_cost_minor` INTEGER,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`plan_id`) REFERENCES `local_meal_plans`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )

            connection.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_local_meal_plan_items_plan_id` ON `local_meal_plan_items` (`plan_id`)",
            )
        }
    }

    val MIGRATION_3_4: Migration = object : Migration(3, 4) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `favorite_products` (
                    `id` TEXT NOT NULL,
                    `product_id` TEXT NOT NULL,
                    `name` TEXT NOT NULL,
                    `category` TEXT NOT NULL,
                    `calories` REAL NOT NULL,
                    `proteins` REAL NOT NULL,
                    `fats` REAL NOT NULL,
                    `carbs` REAL NOT NULL,
                    `price_amount_minor` INTEGER,
                    `saved_at` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )

            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `saved_recipes` (
                    `id` TEXT NOT NULL,
                    `plan_id` TEXT,
                    `title` TEXT NOT NULL,
                    `meal_type` TEXT NOT NULL,
                    `calories` REAL NOT NULL,
                    `proteins` REAL NOT NULL,
                    `fats` REAL NOT NULL,
                    `carbs` REAL NOT NULL,
                    `cost_minor` INTEGER NOT NULL,
                    `ingredients_json` TEXT NOT NULL,
                    `instructions` TEXT NOT NULL,
                    `saved_at` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )
        }
    }
}
