package pe.moneyflow.app.widget

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import pe.moneyflow.core.domain.usecase.GetDashboardUseCase

/**
 * A Glance widget runs in a broadcast receiver, which Hilt cannot field-inject. This entry point
 * lets the widget pull the dashboard use case from the application's Hilt graph on demand.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun getDashboardUseCase(): GetDashboardUseCase
}
