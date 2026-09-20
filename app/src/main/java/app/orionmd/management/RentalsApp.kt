package app.orionmd.management

import android.app.Application

class RentalsApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Nothing to eagerly initialize - the database only opens once the user unlocks the app
        // (see app.orionmd.management.data.AppSession), so there is never a moment where decrypted
        // data exists in memory before a correct PIN/pattern/password has been entered.
    }
}
