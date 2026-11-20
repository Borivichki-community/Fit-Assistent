package dev.andre.nutritionist.android.servicecheck

import androidx.annotation.StringRes
import dev.andre.nutritionist.android.R
import dev.andre.nutritionist.android.servicecheck.ServiceCheckAction.ADD_ENTRY
import dev.andre.nutritionist.android.servicecheck.ServiceCheckAction.DELETE_ENTRY
import dev.andre.nutritionist.android.servicecheck.ServiceCheckAction.GENERATE_WEEK_MENU
import dev.andre.nutritionist.android.servicecheck.ServiceCheckAction.GET_DAILY_ENTRIES
import dev.andre.nutritionist.android.servicecheck.ServiceCheckAction.GET_PROFILE
import dev.andre.nutritionist.android.servicecheck.ServiceCheckAction.GET_RANGE
import dev.andre.nutritionist.android.servicecheck.ServiceCheckAction.HEALTH
import dev.andre.nutritionist.android.servicecheck.ServiceCheckAction.KEYCLOAK_DISCOVERY
import dev.andre.nutritionist.android.servicecheck.ServiceCheckAction.KEYCLOAK_SESSION
import dev.andre.nutritionist.android.servicecheck.ServiceCheckAction.PREPARE_ENTRY
import dev.andre.nutritionist.android.servicecheck.ServiceCheckAction.SEARCH_PRODUCTS
import dev.andre.nutritionist.android.servicecheck.ServiceCheckAction.UPDATE_PROFILE

internal enum class ServiceCheckTarget(
    @StringRes val label: Int,
    @StringRes val description: Int,
    val defaultGrpcPort: Int?,
    val actions: List<ServiceCheckAction>,
) {
    PROFILE(
        R.string.service_check_profile,
        R.string.service_check_profile_description,
        9091,
        listOf(HEALTH, GET_PROFILE, UPDATE_PROFILE),
    ),
    DIARY(
        R.string.service_check_diary,
        R.string.service_check_diary_description,
        9093,
        listOf(HEALTH, PREPARE_ENTRY, ADD_ENTRY, GET_DAILY_ENTRIES, GET_RANGE, DELETE_ENTRY),
    ),
    CATALOG(
        R.string.service_check_catalog,
        R.string.service_check_catalog_description,
        9192,
        listOf(HEALTH, SEARCH_PRODUCTS),
    ),
    RECIPE(
        R.string.service_check_recipe,
        R.string.service_check_recipe_description,
        9094,
        listOf(HEALTH, GENERATE_WEEK_MENU),
    ),
    KEYCLOAK(
        R.string.service_check_keycloak,
        R.string.service_check_keycloak_description,
        null,
        listOf(KEYCLOAK_DISCOVERY, KEYCLOAK_SESSION),
    ),
}
