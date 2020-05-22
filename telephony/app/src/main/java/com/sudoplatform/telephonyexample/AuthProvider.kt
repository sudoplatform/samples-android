package com.sudoplatform.telephonyexample

import com.sudoplatform.sudotelephony.SudoAuthenticationProvider
import com.sudoplatform.sudouser.GraphQLAuthProvider


class AuthProvider(private val graphQLAuthProvider: GraphQLAuthProvider) :
    SudoAuthenticationProvider {
    override fun getLatestAuthToken() = graphQLAuthProvider.latestAuthToken
}