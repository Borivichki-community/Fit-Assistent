package dev.andre.nutritionist.presentation.catalog

import dev.andre.nutritionist.core.kotea.CommandsFlowHandler
import dev.andre.nutritionist.domain.coroutines.CoroutineDispatchers
import dev.andre.nutritionist.domain.repository.CatalogRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class CatalogSearchCommandsFlowHandler(
    private val catalogRepository: CatalogRepository,
    private val dispatchers: CoroutineDispatchers,
    private val debounceMillis: Long = DEFAULT_DEBOUNCE_MILLIS,
) : CommandsFlowHandler<CatalogSearchCommand, CatalogSearchEvent> {

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    override fun handle(commands: Flow<CatalogSearchCommand>): Flow<CatalogSearchEvent> = channelFlow {
        val searchChannel = Channel<CatalogSearchCommand.Search>(Channel.UNLIMITED)
        val otherChannel = Channel<CatalogSearchCommand>(Channel.UNLIMITED)

        launch {
            commands.collect { command ->
                when (command) {
                    is CatalogSearchCommand.Search -> searchChannel.send(command)
                    is CatalogSearchCommand.ToggleFavorite,
                    CatalogSearchCommand.ObserveFavorites -> otherChannel.send(command)
                }
            }
            searchChannel.close()
            otherChannel.close()
        }

        launch {
            searchChannel.receiveAsFlow()
                .debounce { cmd ->
                    if (cmd.forceRefresh || cmd.pageToken.isNotEmpty()) 0L else debounceMillis
                }
                .distinctUntilChanged { old, new ->
                    !new.forceRefresh &&
                        old.query == new.query &&
                        old.category == new.category &&
                        old.pageToken == new.pageToken
                }
                .flatMapLatest { cmd -> executeSearch(cmd) }
                .collect { send(it) }
        }

        launch {
            otherChannel.receiveAsFlow()
                .flatMapMerge { command ->
                    when (command) {
                        is CatalogSearchCommand.ToggleFavorite -> executeToggleFavorite(command)
                        CatalogSearchCommand.ObserveFavorites -> observeFavorites()
                        is CatalogSearchCommand.Search -> flow { }
                    }
                }
                .collect { send(it) }
        }
    }

    private fun executeSearch(command: CatalogSearchCommand.Search): Flow<CatalogSearchEvent> = flow {
        val result = catalogRepository.searchProducts(
            query = command.query,
            category = command.category.orEmpty(),
            pageToken = command.pageToken,
        )
        result.onSuccess { (products, nextToken) ->
            if (command.pageToken.isEmpty()) {
                emit(CatalogSearchEvent.Internal.Loaded(products = products, nextPageToken = nextToken))
            } else {
                emit(CatalogSearchEvent.Internal.NextPageLoaded(products = products, nextPageToken = nextToken))
            }
        }.onFailure { error ->
            if (error is CancellationException) throw error
            emit(CatalogSearchEvent.Internal.Failed(error))
        }
    }.flowOn(dispatchers.io)

    private fun executeToggleFavorite(command: CatalogSearchCommand.ToggleFavorite): Flow<CatalogSearchEvent> = flow {
        val result = catalogRepository.toggleFavoriteProduct(command.product)
        result.onSuccess { isFavorite ->
            emit(
                CatalogSearchEvent.Internal.FavoriteToggled(
                    productId = command.product.id,
                    isFavorite = isFavorite,
                ),
            )
        }.onFailure { error ->
            if (error is CancellationException) throw error
            emit(CatalogSearchEvent.Internal.Failed(error))
        }
    }.flowOn(dispatchers.io)

    private fun observeFavorites(): Flow<CatalogSearchEvent> =
        catalogRepository.observeFavoriteProducts()
            .map<_, CatalogSearchEvent> { favorites ->
                CatalogSearchEvent.Internal.FavoritesUpdated(
                    favoriteIds = favorites.mapTo(mutableSetOf()) { it.id },
                )
            }
            .catch { error ->
                if (error is CancellationException) throw error
                emit(CatalogSearchEvent.Internal.Failed(error))
            }
            .flowOn(dispatchers.io)

    companion object {
        const val DEFAULT_DEBOUNCE_MILLIS = 300L
    }
}
