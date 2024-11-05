package it.danielezotta.patelettromagnetismo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import it.danielezotta.patelettromagnetismo.composables.PermitList
import it.danielezotta.patelettromagnetismo.ui.theme.PATPermessiElettromagnetismoTheme
import it.danielezotta.patelettromagnetismo.viewmodels.MainViewModel

class MainActivity : ComponentActivity() {

    lateinit var mainViewModel: MainViewModel

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mainViewModel = ViewModelProvider(this)[MainViewModel::class.java]

        mainViewModel.getPermits()

        enableEdgeToEdge()
        setContent {
            PATPermessiElettromagnetismoTheme {
                Surface (
                    modifier = Modifier.fillMaxSize()
                ) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = {
                            TopAppBar(
                                title = {
                                    Row (
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Image(painter = painterResource(R.drawable.trentino_coa), contentDescription = null, Modifier.height(48.dp).padding(vertical = 4.dp))
                                        Spacer(modifier = Modifier.padding(6.dp))
                                        Text(stringResource(R.string.elettromagnetismo))
                                    }
                                }
                            )
                        }
                    ) { innerPadding ->
                        Box (modifier = Modifier.padding(innerPadding)) {
                            PermitScreen(mainViewModel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PermitScreen(mainViewModel: MainViewModel) {

    PermitList(mainViewModel)

}
