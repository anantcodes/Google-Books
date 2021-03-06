package com.example.googlebooks

import android.app.AlertDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.core.app.ActivityCompat
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONException

class MainActivity : AppCompatActivity() {

    lateinit var searchButton : ImageButton
    lateinit var searchQuery : EditText
    lateinit var listView: ListView
    var bookList:ArrayList<Model> = ArrayList();

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        searchQuery = findViewById(R.id.search_query)
        searchButton = findViewById(R.id.search_button)
        if (ConnectivityManager().checkConnectivity(this)) {
            var query: String = ""
            val queue = Volley.newRequestQueue(this)
            searchButton.setOnClickListener {
                query = getQuery()
                searchRequest(query, queue)
            }
            listView = findViewById<ListView>(R.id.listView) as ListView

            listView.setOnItemClickListener { parent, view, position, id ->
                Toast.makeText(
                    this@MainActivity,
                    "item clicked " + position.toString(),
                    Toast.LENGTH_SHORT
                ).show()
                bookRequest(bookList.get(position).id, queue)
            }
        }
        else
        {
            val dialog = AlertDialog.Builder(this)
            dialog.setTitle("Error")
            dialog.setMessage("Internet Connection is not Found")
            dialog.setPositiveButton("Open Settings") { text, listener ->

                val settingsIntent= Intent(Settings.ACTION_WIRELESS_SETTINGS)
                startActivity(settingsIntent)
                this@MainActivity.finish()


            }

            dialog.setNegativeButton("Exit") { text, listener ->

                ActivityCompat.finishAffinity(this)


            }
            dialog.create()
            dialog.show()
        }
    }

    fun searchRequest(query : String, queue : RequestQueue) {
        val url = "https://www.googleapis.com/books/v1/volumes?q=" + query
        val stringRequest = object:StringRequest(Request.Method.GET, url,
            Response.Listener<String> { response ->
                // Display the first 500 characters of the response string.
//                textView.text = response
                val base:JSONObject = JSONObject(response)
                val arr : JSONArray = base.getJSONArray("items")
                setModels(arr)
            },
            Response.ErrorListener { /*textView.text = "That didn't work!" */}){
            override fun getHeaders(): MutableMap<String, String> {
                val headers=HashMap<String,String>()
                headers["Content-type"]="application/json"
                return headers
            }
        }
        queue.add(stringRequest)
    }

    fun getQuery(): String {
        val query : String = searchQuery.getText().toString()
        query.replace(' ', '+')
        return query
    }

    fun setModels(items:JSONArray) {
        bookList=ArrayList()
        for(i in 0..(items.length()-1)){
            var obj : JSONObject = items.get(i) as JSONObject
            var model:Model=Model()
            model.id=obj.getString("id")
            model.selfLink=obj.getString("selfLink")
            val volumeInfo:JSONObject = obj.getJSONObject("volumeInfo")
            model.title=volumeInfo.getString("title")
            try {
                model.subtitle=volumeInfo.getString("subtitle")
            }
            catch (e:JSONException){
                model.subtitle=null
            }
            try {
                var authors: JSONArray = volumeInfo.getJSONArray("authors")
                model.authors = ArrayList()
                for (i in 0..authors.length() - 1)
                    model.authors?.add(authors[i].toString())
            }
            catch (e:JSONException){
                model.authors=null
            }
            try {
                val imageLinks: JSONObject = volumeInfo.getJSONObject("imageLinks")
                model.smallThumbnail=imageLinks.getString("smallThumbnail")
            }
            catch (e:JSONException){
                model.smallThumbnail=null
            }
            bookList.add(model)
        }
        runOnUiThread {
            //stuff that updates ui
            val obj_adapter : CustomAdapter
            obj_adapter = CustomAdapter(applicationContext,bookList)
            listView.adapter=obj_adapter
        }
    }

    fun bookRequest(id:String, queue:RequestQueue){
        val url = "https://www.googleapis.com/books/v1/volumes/" + id
        val stringRequest = object:StringRequest(Request.Method.GET, url,
            Response.Listener<String> { response ->
                val intent = Intent(this@MainActivity, BookActivity::class.java)
                intent.putExtra("JSONString", response)
                startActivity(intent)
            },
            Response.ErrorListener {
                Toast.makeText(this@MainActivity, "error", Toast.LENGTH_SHORT).show()
            }){
            override fun getHeaders(): MutableMap<String, String> {
                val headers=HashMap<String,String>()
                headers["Content-type"]="application/json"
                return headers
            }
        }
        queue.add(stringRequest)
    }
}
