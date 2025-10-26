package it.danielezotta.patelettromagnetismo

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import it.danielezotta.patelettromagnetismo.util.AppConstants

val Context.dataStore by preferencesDataStore(name = AppConstants.DATASTORE_NAME)
