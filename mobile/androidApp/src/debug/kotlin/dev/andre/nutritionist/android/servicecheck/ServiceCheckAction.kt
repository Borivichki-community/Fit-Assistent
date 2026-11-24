package dev.andre.nutritionist.android.servicecheck

import androidx.annotation.StringRes
import dev.andre.nutritionist.android.R

internal enum class ServiceCheckAction(@StringRes val label: Int) {
    HEALTH(R.string.service_check_health),
    KEYCLOAK_DISCOVERY(R.string.service_check_discovery),
    KEYCLOAK_SESSION(R.string.service_check_session),
    GET_PROFILE(R.string.service_check_get_profile),
    UPDATE_PROFILE(R.string.service_check_update_profile),
    PREPARE_ENTRY(R.string.service_check_prepare_entry),
    ADD_ENTRY(R.string.service_check_add_entry),
    GET_DAILY_ENTRIES(R.string.service_check_day),
    GET_RANGE(R.string.service_check_range),
    DELETE_ENTRY(R.string.service_check_delete_entry),
    SEARCH_PRODUCTS(R.string.service_check_search_products),
    GENERATE_WEEK_MENU(R.string.service_check_generate_menu),
}
