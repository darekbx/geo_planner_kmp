package com.darekbx.geoplanner.kmp.cloud

import com.darekbx.geoplanner.kmp.cloud.dto.AuthResponse

class FirebaseSession {

    private var authResponse: AuthResponse? = null

    fun setAuthResponse(response: AuthResponse) {
        this.authResponse = response
    }

    fun getToken(): String? {
        return this.authResponse?.idToken
    }
}