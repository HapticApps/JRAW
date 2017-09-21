package net.dean.jraw.references

import com.squareup.moshi.Types
import net.dean.jraw.*
import net.dean.jraw.JrawUtils.urlEncode
import net.dean.jraw.databind.Enveloped
import net.dean.jraw.models.Account
import net.dean.jraw.models.Multireddit
import net.dean.jraw.models.PublicContribution
import net.dean.jraw.models.Trophy
import net.dean.jraw.models.internal.TrophyList
import net.dean.jraw.pagination.DefaultPaginator

abstract class UserReference<T : UserFlairReference>(reddit: RedditClient, val username: String) : AbstractReference<String>(reddit, username) {
    abstract val isSelf: Boolean

    @EndpointImplementation(Endpoint.GET_ME, Endpoint.GET_USER_USERNAME_ABOUT)
    fun about(): Account {
        val body = reddit.request {
            it.path(if (isSelf) "/api/v1/me" else "/user/$username/about")
        }.body

        // /api/v1/me returns an Account that isn't wrapped with the data/kind nodes
        if (isSelf)
            return JrawUtils.adapter<Account>().fromJson(body)!!
        return JrawUtils.adapter<Account>(Enveloped::class.java).fromJson(body)!!
    }

    @EndpointImplementation(Endpoint.GET_ME_TROPHIES, Endpoint.GET_USER_USERNAME_TROPHIES)
    fun trophies(): List<Trophy> {
        return reddit.request {
            if (isSelf)
                it.endpoint(Endpoint.GET_ME_TROPHIES)
            else
                it.endpoint(Endpoint.GET_USER_USERNAME_TROPHIES, username)
        }.deserializeEnveloped<TrophyList>().trophies
    }

    /**
     * Creates a new [Paginator.Builder] which can iterate over a user's public history.
     *
     * Possible `where` values:
     *
     * - `overview` — submissions and comments
     * - `submitted` — only submissions
     * - `comments` — only comments
     * - `gilded` — submissions and comments which have received reddit Gold
     *
     * If this user reference is for the currently logged-in user, these `where` values can be used:
     *
     * - `upvoted`
     * - `downvoted`
     * - `hidden`
     * - `saved`
     *
     * Only `overview`, `submitted`, and `comments` are sortable.
     */
    @EndpointImplementation(Endpoint.GET_USER_USERNAME_WHERE, type = MethodType.NON_BLOCKING_CALL)
    fun history(where: String): DefaultPaginator.Builder<PublicContribution<*>> {
        // Encode URLs to prevent accidental malformed URLs
        return DefaultPaginator.Builder.create(reddit, "/user/${urlEncode(username)}/${urlEncode(where)}",
            sortingAlsoInPath = false)
    }

    /**
     * Creates a [MultiredditReference] for a multireddit that belongs to this user.
     */
    fun multi(name: String) = MultiredditReference(reddit, subject, name)

    /**
     * Lists the multireddits this client is able to view.
     *
     * If this UserReference is for the logged-in user, all multireddits will be returned. Otherwise, only public
     * multireddits will be returned.
     */
    @EndpointImplementation(Endpoint.GET_MULTI_MINE, Endpoint.GET_MULTI_USER_USERNAME)
    fun listMultis(): List<Multireddit> {
        val res = reddit.request {
            if (isSelf) {
                it.endpoint(Endpoint.GET_MULTI_MINE)
            } else {
                it.endpoint(Endpoint.GET_MULTI_USER_USERNAME, subject)
            }
        }

        val type = Types.newParameterizedType(List::class.java, Multireddit::class.java)
        val adapter = JrawUtils.moshi.adapter<List<Multireddit>>(type, Enveloped::class.java)

        return res.deserializeWith(adapter)
    }

    /**
     * Returns a [UserFlairReference] for this user for the given subreddit. If this user is not the authenticated user,
     * the authenticated must be a moderator of the given subreddit to access anything specific to this user.
     */
    abstract fun flairOn(subreddit: String): T
}
