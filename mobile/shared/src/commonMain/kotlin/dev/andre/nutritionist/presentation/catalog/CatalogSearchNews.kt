package dev.andre.nutritionist.presentation.catalog

sealed interface CatalogSearchNews {
    data class ShowSnackbar(val message: String) : CatalogSearchNews
}
