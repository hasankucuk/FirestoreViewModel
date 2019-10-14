package io.github.achmadhafid.sample_app.entity

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class User(

    @get:Exclude
    var id: String = "",
    var name: String = "",
    var email: String? = null,
    var phone: String? = null

)
