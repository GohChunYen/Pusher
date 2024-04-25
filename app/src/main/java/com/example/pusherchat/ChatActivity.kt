package com.example.pusherchat

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.pusher.client.Pusher
import com.pusher.client.PusherOptions
import org.json.JSONObject
import java.util.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat

private const val TAG = "ChatActivity"

class ChatActivity : AppCompatActivity() {
    private lateinit var adapter: MessageAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        findViewById<RecyclerView>(R.id.messageList).layoutManager = LinearLayoutManager(this)
        adapter = MessageAdapter(this)
        findViewById<RecyclerView>(R.id.messageList).adapter = adapter

        findViewById<Button>(R.id.btnSend).setOnClickListener {
            if (findViewById<TextView>(R.id.txtMessage).text.isNotEmpty()) {
                val message = Message(
                    App.user,
                    findViewById<TextView>(R.id.txtMessage).text.toString(),
                    Calendar.getInstance().timeInMillis
                )

                val call = ChatService.create().postMessage(message)
                Log.i("Test", "$message + $call");
                call.enqueue(object : Callback<Void> {
                    override fun onResponse(call: Call<Void>, response: Response<Void>) {
                        resetInput()
                        if (!response.isSuccessful) {
                            Log.e(TAG,response.code().toString() + response.message().toString());
                            Toast.makeText(
                                applicationContext,
                                "Response was not successful",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        else {
                            Log.d(TAG, response.code().toString());
                            Toast.makeText(
                                applicationContext,
                                "Response was successful",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    override fun onFailure(call: Call<Void>, t: Throwable) {
                        resetInput()
                        Log.e(TAG, t.toString());
                        Toast.makeText(
                            applicationContext,
                            "Error when calling the service",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
            } else {
                Toast.makeText(
                    applicationContext,
                    "Message should not be empty",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        setupPusher()
    }

    private fun setupPusher() {
        val options = PusherOptions()
        options.setCluster("ap1")

        val pusher = Pusher("680ab392c38199159854", options)
        val channel = pusher.subscribe("chat")
        channel.bind("App\\Events\\MessageSent") { channelName, eventName, data ->
            val jsonObject = JSONObject(data)
            Log.d("View", jsonObject.toString())
            val messageObject = jsonObject.getJSONObject("message")
            Log.d("View", messageObject.toString())
            Log.d("View", messageObject.getString("user"))
            val timeDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(messageObject.getString("time"))
            val timeMillis = timeDate?.time ?: 0L
            val message = Message(
                messageObject.getString("user"),
                messageObject.getString("message"),
                timeMillis
            )

            runOnUiThread {
                adapter.addMessage(message)
                // scroll the RecyclerView to the last added element
                findViewById<RecyclerView>(R.id.messageList).scrollToPosition(adapter.itemCount - 1);
            }

        }

        pusher.connect()
    }

    private fun resetInput() {
        // Clean text box
        //txtMessage.text.clear()

        // Hide keyboard
        val inputManager =
            getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.hideSoftInputFromWindow(
            currentFocus!!.windowToken, InputMethodManager.HIDE_NOT_ALWAYS
        )
    }
}