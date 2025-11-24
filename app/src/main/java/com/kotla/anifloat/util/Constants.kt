package com.kotla.anifloat.util

object Constants {
    // Replace with your actual Client ID from Anilist Developer Settings
    const val CLIENT_ID = "32506" 
    const val REDIRECT_URI = "anifloat://auth/callback"
    const val ANILIST_AUTH_URL = "https://anilist.co/api/v2/oauth/authorize?client_id=$CLIENT_ID&response_type=token"
}

