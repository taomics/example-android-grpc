@file:OptIn(ExperimentalMaterial3Api::class)

package ai.taomics.example_android_grpc

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ai.taomics.example_android_grpc.ui.theme.ExampleandroidgrpcTheme
import ai.taomics.helloworld.GreeterGrpcKt
import ai.taomics.helloworld.helloRequest
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.launch
import java.io.Closeable

class MainActivity : ComponentActivity() {
     private val uri by lazy { Uri.parse("https://example-go-grpc.gentlepond-fcbb8d44.japaneast.azurecontainerapps.io")}
     private val greeterService by lazy { GreeterRCP(uri) }
 
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ExampleandroidgrpcTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Greeting(greeterService)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Greeting(greeterRCP: GreeterRCP?) {
    val scope = rememberCoroutineScope()
    var nameState = remember { mutableStateOf("World")}

    Column(
        modifier = Modifier.padding(vertical = 24.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        Text(
            "GRPC Example",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
            textAlign = TextAlign.Center,
            )

        Text(
            "server: ${greeterRCP?.serverUri ?: "Uknkown"}",
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            )
        
        TextField(
            value = nameState.value,
            onValueChange = { nameState.value = it },
            label = { Text("Name")},
            modifier = Modifier.padding(vertical = 12.dp),
            )

        Button({ scope.launch {
            greeterRCP?.sayHello(nameState.value)
        }}) {
            Text("Send")
        }

        Text(
            "Result: ${greeterRCP?.responseState?.value ?: "disabled"}",
            modifier = Modifier.padding(vertical = 12.dp),
            )
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ExampleandroidgrpcTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ){
            Greeting(null)
        }
    }
}

class GreeterRCP(uri: Uri): Closeable {
    val serverUri = uri
    val responseState = mutableStateOf("")

    private val channel = let {
        println("Connecting to ${uri.host}:${uri.port}")

        ManagedChannelBuilder.forAddress(uri.host, uri.port)
        .useTransportSecurity()
        .executor(Dispatchers.IO.asExecutor()).build()
    }

    private val greeter = GreeterGrpcKt.GreeterCoroutineStub(channel)

    suspend fun sayHello(name: String) {
        val request = helloRequest { this.name = name }

        try {
            val response = greeter.sayHello(request)
            responseState.value = response.message
        } catch (e: Exception) {
            responseState.value = e.message ?: "Unknown Error"
            e.printStackTrace()
        }
    }

    override fun close() {
        channel.shutdownNow()
    }
}
