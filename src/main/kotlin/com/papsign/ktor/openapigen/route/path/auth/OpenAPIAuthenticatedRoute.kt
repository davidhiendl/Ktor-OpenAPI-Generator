package com.papsign.ktor.openapigen.route.path.auth

import com.papsign.ktor.openapigen.modules.CachingModuleProvider
import com.papsign.ktor.openapigen.modules.providers.AuthProvider
import com.papsign.ktor.openapigen.modules.registerModule
import com.papsign.ktor.openapigen.route.OpenAPIRoute
import com.papsign.ktor.openapigen.route.response.AuthResponseContextImpl
import com.papsign.ktor.openapigen.route.response.OpenAPIPipelineAuthContext
import io.ktor.routing.Route
import kotlin.reflect.KClass

class OpenAPIAuthenticatedRoute<A>(
    route: Route,
    provider: CachingModuleProvider = CachingModuleProvider(),
    val authProvider: AuthProvider<A>
) : OpenAPIRoute<OpenAPIAuthenticatedRoute<A>>(route, provider) {

    override fun child(route: Route): OpenAPIAuthenticatedRoute<A> {
        return OpenAPIAuthenticatedRoute(route, provider.child(), authProvider)
    }

    inline fun <P : Any, R : Any, B : Any> handle(
        pClass: KClass<P>,
        rClass: KClass<R>,
        bClass: KClass<B>,
        crossinline body: suspend OpenAPIPipelineAuthContext<A, R>.(P, B) -> Unit
    ) {
        child().apply {// child in case path is branch to prevent propagation of the mutable nature of the provider
            provider.registerModule(authProvider)
            handle<P, R, B>(pClass, rClass, bClass) { pipeline, responder, p, b ->
                AuthResponseContextImpl<A, R>(pipeline, authProvider, this, responder).body(p, b)
            }
        }
    }

    inline fun <P : Any, R : Any> handle(
        pClass: KClass<P>,
        rClass: KClass<R>,
        crossinline body: suspend OpenAPIPipelineAuthContext<A, R>.(P) -> Unit
    ) {
        child().apply {// child in case path is branch to prevent propagation of the mutable nature of the provider
            provider.registerModule(authProvider)
            handle<P, R, Unit>(pClass, rClass, Unit::class) { pipeline, responder, p: P, _ ->
                AuthResponseContextImpl<A, R>(pipeline, authProvider, this, responder).body(p)
            }
        }
    }
}
