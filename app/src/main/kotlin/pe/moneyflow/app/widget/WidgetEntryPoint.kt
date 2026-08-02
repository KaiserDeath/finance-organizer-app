package pe.moneyflow.app.widget

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import pe.moneyflow.core.domain.repository.SettingsRepository
import pe.moneyflow.core.domain.usecase.GetDashboardUseCase

/**
 * A Glance widget runs in a broadcast receiver, which Hilt cannot field-inject. This entry point
 * lets the widget pull the dashboard use case from the application's Hilt graph on demand.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun getDashboardUseCase(): GetDashboardUseCase

    /** Discreet mode has to reach the widget too — a home screen is the most public surface here. */
    fun settingsRepository(): SettingsRepository
}
