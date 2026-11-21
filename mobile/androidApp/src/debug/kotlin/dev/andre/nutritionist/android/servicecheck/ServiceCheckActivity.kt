package dev.andre.nutritionist.android.servicecheck

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.andre.nutritionist.android.ui.theme.FitAssistantTheme
import org.koin.android.ext.android.getKoin
import org.koin.androidx.compose.koinViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module
import java.time.Clock

internal class ServiceCheckActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        getKoin().loadModules(listOf(checkModule))

        setContent {
            FitAssistantTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "services") {
                    composable("services") {
                        ServiceCheckMenuScreen(onSelect = { target -> navController.navigate(target.name) })
                    }
                    ServiceCheckTarget.entries.forEach { target ->
                        composable(target.name) {
                            val viewModel: ServiceCheckViewModel = koinViewModel(
                                viewModelStoreOwner = this@ServiceCheckActivity,
                                key = target.name,
                                parameters = { parametersOf(target) },
                            )
                            ServiceCheckRoute(viewModel = viewModel, onBack = { navController.popBackStack() })
                        }
                    }
                }
            }
        }
    }

    private companion object {
        val checkModule = module {
            single { ServiceCheckClient(get(), get(), get(), get()) }
            factory { ServiceCheckCommandsFlowHandler(get(), get(), Clock.systemUTC()) }
            viewModel { (target: ServiceCheckTarget) -> ServiceCheckViewModel(target, get()) }
        }
    }
}
