package com.gorunning

import android.content.ContentValues
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.gorunning.LoginActivity.Companion.useremail

class RecordActivity : AppCompatActivity() {

    private var sportSelected : String = "Running"

    private lateinit var ivBike : ImageView
    private lateinit var ivRollerSkate: ImageView
    private lateinit var ivRunning: ImageView

    private lateinit var recyclerView: RecyclerView
    private lateinit var runsArrayList : ArrayList<Runs>
    private lateinit var myAdapter: RunsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_record)

        //Variable para administrar el toolbar
        val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.toolbar_record)
        setSupportActionBar(toolbar)

        //Modificar le titutlo
        toolbar.title = getString(R.string.bar_title_record)
        //Administrar los colores
        toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.gray_dark))
        toolbar.setBackgroundColor(ContextCompat.getColor(this, R.color.gray_light))

        //Visualizacion del boton (flecha hacia atras) para poder desplazar al home (volver)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)

        ivBike = findViewById(R.id.ivBike)
        ivRollerSkate = findViewById(R.id.ivRollerSkate)
        ivRunning = findViewById(R.id.ivRunning)

        recyclerView = findViewById(R.id.rvRecords)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.setHasFixedSize(true)  //Establecer un tamañp fijo a cada uno de los items

        runsArrayList = arrayListOf()
        //Conectar el adaptador con el arrayList
        myAdapter = RunsAdapter(runsArrayList)
        //Conectar el rv con el adaptador
        recyclerView.adapter = myAdapter


    }
    //Aqui se hace la llamada del rv
    override fun onResume() {
        super.onResume()
        loadRecyclerView("date", Query.Direction.DESCENDING)
    }
    //Si cambia de ventana o se va a otra app -> se limpia el rv
    override fun onPause() {
        super.onPause()
        runsArrayList.clear()
    }
    //Administra la flecha hacia atras para volver a la pantalla anterior
    override fun onSupportNavigateUp(): Boolean {
        //onBackPressed()  //TODO se reemplaza por goHome() para que se actualice el drawer al volver a consultar
        goHome()
        return true
    }
    //Inflar el menu de los 3 puntitos
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.order_records_by, menu)
        return true //super.onCreateOptionsMenu(menu)
    }
    //Administrar la funcionalidad de los items del menu de los 3 puntitos
    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        var order: Query.Direction = Query.Direction.DESCENDING

        when (item.itemId) {
            R.id.orderby_date -> {
                if (item.title == getString(R.string.orderby_dateZA)) {
                    item.title = getString(R.string.orderby_dateAZ)
                    order = Query.Direction.DESCENDING
                } else {
                    item.title = getString(R.string.orderby_dateZA)
                    order = Query.Direction.ASCENDING
                }
                loadRecyclerView("date", order)
                return true
            }
            R.id.orderby_duration -> {
                var option = getString(R.string.orderby_durationZA)
                if (item.title == getString(R.string.orderby_durationZA)) {
                    item.title = getString(R.string.orderby_durationAZ)
                    order = Query.Direction.DESCENDING
                } else {
                    item.title = getString(R.string.orderby_durationZA)
                    order = Query.Direction.ASCENDING
                }
                loadRecyclerView("duration", order)
                return true
            }

            R.id.orderby_distance -> {
                var option = getString(R.string.orderby_distanceZA)
                if (item.title == option) {
                    item.title = getString(R.string.orderby_distanceAZ)
                    order = Query.Direction.ASCENDING
                } else {
                    item.title = getString(R.string.orderby_distanceZA)
                    order = Query.Direction.DESCENDING
                }
                loadRecyclerView("distance", order)
                return true
            }

            R.id.orderby_avgspeed -> {
                var option = getString(R.string.orderby_avgspeedZA)
                if (item.title == getString(R.string.orderby_avgspeedZA)) {
                    item.title = getString(R.string.orderby_avgspeedAZ)
                    order = Query.Direction.ASCENDING
                } else {
                    item.title = getString(R.string.orderby_avgspeedZA)
                    order = Query.Direction.DESCENDING
                }
                loadRecyclerView("avgSpeed", order)
                return true
            }

            R.id.orderby_maxspeed -> {
                var option = getString(R.string.orderby_maxspeedZA)
                if (item.title == getString(R.string.orderby_maxspeedZA)) {
                    item.title = getString(R.string.orderby_maxspeedAZ)
                    order = Query.Direction.ASCENDING
                } else {
                    item.title = getString(R.string.orderby_maxspeedZA)
                    order = Query.Direction.DESCENDING
                }
                loadRecyclerView("maxSpeed", order)
                return true
            }

        }
        return super.onOptionsItemSelected(item)
    }
    //Carga de datos
    private fun loadRecyclerView(field: String, order: Query.Direction){
        //Limpiar el rv
        runsArrayList.clear()

        //Instancia en firestore
        var dbRuns = FirebaseFirestore.getInstance()
        dbRuns.collection("runs$sportSelected").orderBy(field, order)
            .whereEqualTo("user", useremail)
            .get()
            .addOnSuccessListener { documents ->
                for (run in documents)
                    //Añadir el elemento
                    runsArrayList.add(run.toObject(Runs::class.java))
                //Notificar al adaptador que se cambiaron los datos -> cambia la vista
                myAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                Log.w(ContentValues.TAG, "Error getting documents WHERE EQUAL TO: ", exception)
            }
    }
    fun loadRunsBike(v: View){
        sportSelected = "Bike"
        ivBike.setBackgroundColor(ContextCompat.getColor(MainActivity.mainContext, R.color.blue_trans))
        ivRollerSkate.setBackgroundColor(ContextCompat.getColor(MainActivity.mainContext, R.color.gray_medium))
        ivRunning.setBackgroundColor(ContextCompat.getColor(MainActivity.mainContext, R.color.gray_medium))

        loadRecyclerView("date", Query.Direction.DESCENDING)
    }
    fun loadRunsRollerSkate(v: View){
        sportSelected = "RollerSkate"
        ivBike.setBackgroundColor(ContextCompat.getColor(MainActivity.mainContext, R.color.gray_medium))
        ivRollerSkate.setBackgroundColor(ContextCompat.getColor(MainActivity.mainContext, R.color.blue_trans))
        ivRunning.setBackgroundColor(ContextCompat.getColor(MainActivity.mainContext, R.color.gray_medium))

        loadRecyclerView("date", Query.Direction.DESCENDING)
    }
    fun loadRunsRunning(v: View){
        sportSelected = "Running"
        ivBike.setBackgroundColor(ContextCompat.getColor(MainActivity.mainContext, R.color.gray_medium))
        ivRollerSkate.setBackgroundColor(ContextCompat.getColor(MainActivity.mainContext, R.color.gray_medium))
        ivRunning.setBackgroundColor(ContextCompat.getColor(MainActivity.mainContext, R.color.blue_trans))

        loadRecyclerView("date", Query.Direction.DESCENDING)
    }
    //Llamada al home (publica)
    fun callHome(v: View) {
        goHome ()
    }
    //Llamada al home
    private fun goHome() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }
}