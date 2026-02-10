package com.jermey.seal.demo.di

import com.jermey.seal.demo.data.CtVerificationRepository
import com.jermey.seal.demo.mvi.details.DetailsAction
import com.jermey.seal.demo.mvi.details.DetailsContainer
import com.jermey.seal.demo.mvi.details.DetailsIntent
import com.jermey.seal.demo.mvi.details.DetailsState
import com.jermey.seal.demo.mvi.home.HomeAction
import com.jermey.seal.demo.mvi.home.HomeContainer
import com.jermey.seal.demo.mvi.home.HomeIntent
import com.jermey.seal.demo.mvi.home.HomeState
import com.jermey.quo.vadis.flowmvi.NavigationContainer
import com.jermey.quo.vadis.flowmvi.NavigationContainerScope
import org.koin.core.qualifier.qualifier
import org.koin.dsl.module

val appModule = module {
    single { CtVerificationRepository() }
}

val containerModule = module {
    scope<NavigationContainerScope> {
        scoped<NavigationContainer<HomeState, HomeIntent, HomeAction>>(qualifier = qualifier<HomeContainer>()) {
            HomeContainer(getKoin().get(), get())
        }
        scoped<NavigationContainer<DetailsState, DetailsIntent, DetailsAction>>(qualifier = qualifier<DetailsContainer>()) {
            DetailsContainer(get())
        }
    }
}
