package com.gorunning

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import kotlin.properties.Delegates

class LoginActivity : AppCompatActivity() {

    companion object{
        lateinit var useremail: String
        lateinit var providerSession: String
    }

    private var email by Delegates.notNull<String>()
    private var password by Delegates.notNull<String>()
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var lyTerms: LinearLayout

    private lateinit var mAuth: FirebaseAuth

    private var RESULT_CODE_GOOGLE_SIGN_IN = 100
    private val callbackManager = CallbackManager.Factory.create()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        useremail = ""

        //Ocultar linearLayout de terminos y condiciones
        lyTerms = findViewById(R.id.lyTerms)
        lyTerms.visibility = View.INVISIBLE

        //Asignar las variables
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        mAuth = FirebaseAuth.getInstance()

        //Habilitar boton de iniciar sesion
        manageButtonLogin()
        //Comprobacion cada vez que cambien los campos email y pass
        etEmail.doOnTextChanged { text, start, before, count ->  manageButtonLogin() }
        etPassword.doOnTextChanged { text, start, before, count ->  manageButtonLogin() }
    }

    //Si existe el usuario -> home
    public override fun onStart() {
        super.onStart()

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null)  goHome(currentUser.email.toString(), currentUser.providerId)

    }

    //Se elimina el super para que navege directamente al intent y no a la pantalla anterior
    override fun onBackPressed() {
        //super.onBackPressed()
        val startMain = Intent(Intent.ACTION_MAIN)
        startMain.addCategory(Intent.CATEGORY_HOME)
        startMain.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(startMain)
    }

    //Metodo para habilitar el boton de iniciar de sesion
    private fun manageButtonLogin(){
        var tvLogin = findViewById<TextView>(R.id.tvLogin)
        email = etEmail.text.toString()
        password = etPassword.text.toString()

        //Comprobar si pass esta vacio o email es invalido
        if (TextUtils.isEmpty(password) || ValidateEmail.isEmail(email) == false){

            tvLogin.setBackgroundColor(ContextCompat.getColor(this, R.color.gray))
            tvLogin.isEnabled = false
        }
        else{
            tvLogin.setBackgroundColor(ContextCompat.getColor(this, R.color.green))
            tvLogin.isEnabled = true
        }
    }

    //Login publico, y dentro el login privado
    fun login(view: View) {
        loginUser()
    }

    private fun loginUser(){
        //Asignar con un valor las variables
        email = etEmail.text.toString()
        password = etPassword.text.toString()

        //Inicio de sesion
        mAuth.signInWithEmailAndPassword(email, password)
                //Cuando se completo la tarea
            .addOnCompleteListener(this){ task ->
                //Inicio exitoso
                if (task.isSuccessful)  goHome(email, "email")
                //Si no
                else{
                    //Hacer visible el CheckBox
                    if (lyTerms.visibility == View.INVISIBLE) {
                        lyTerms.visibility = View.VISIBLE
                        //showDialogInLogIn()
                        Toast.makeText(this, "Aceptar los términos y reintentar", Toast.LENGTH_LONG).show()
                    }
                    else{
                        var cbAcept = findViewById<CheckBox>(R.id.cbAcept)
                        //Registrarse
                        if (cbAcept.isChecked) register()
                    }
                }
            }
    }

    //Navegar al home
    private fun goHome(email: String, provider: String){

        //Pasarle 2 parametros a la fun
        useremail = email
        providerSession = provider

        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

    //Registro de usuario
    private fun register(){
        //Capturar los valores del editText
        email = etEmail.text.toString()
        password = etPassword.text.toString()

        //Registro en Firebase
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener {
                //Si es exitoso -> ir al home
                if (it.isSuccessful){

                    //Guardar dato en Firestore
                    //fecha
                    var dateRegister = SimpleDateFormat("dd/MM/yyyy").format(Date())
                    //Instancia para acceder a la db
                    var dbRegister = FirebaseFirestore.getInstance()
                    //Acceder a las colecciones
                    dbRegister.collection("users").document(email).set(hashMapOf(
                        "user" to email,
                        "dateRegister" to dateRegister,
                        "provider" to "Email y Pass"
                    ))

                    goHome(email, "email")
                }
                //Si no, mostrar un toast
                else Toast.makeText(this, "Error, algo ha ido mal :(", Toast.LENGTH_SHORT).show()
            }
    }

    //Ir a la pagina de termYCond
    fun goTerms(v: View){
        val intent = Intent(this, TermsActivity::class.java)
        startActivity(intent)
    }

    //En caso de olvidar el pass
    fun forgotPassword(view: View) {
        //startActivity(Intent(this, ForgotPasswordActivity::class.java))
        resetPassword()
    }

    //Resetear el pass
    private fun resetPassword(){
        //Guardar el dato del email
        var e = etEmail.text.toString()
        //Verifica si el campo email tiene algo
        if (!TextUtils.isEmpty(e)){
            //Permite resetear el email
            mAuth.sendPasswordResetEmail(e)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) Toast.makeText(this, "Email Enviado a $e", Toast.LENGTH_SHORT).show()
                    else Toast.makeText(this, "No se encontró el usuario con este correo", Toast.LENGTH_SHORT).show()
                }
        }
        //Si el campo esta vacio
        else Toast.makeText(this, "Indica un email", Toast.LENGTH_SHORT).show()
    }

    fun callSignInGoogle (view:View){

        //Hacer visible el CheckBox
        if (lyTerms.visibility == View.INVISIBLE) {
            lyTerms.visibility = View.VISIBLE
            //showDialogInLogIn()
            Toast.makeText(this, "Aceptar los términos y reintentar", Toast.LENGTH_LONG).show()
        }
        else{
            var cbAcept = findViewById<CheckBox>(R.id.cbAcept)
            //Registrarse
            if (cbAcept.isChecked)
                signInGoogle()
        }
        //signInGoogle()
    }

    //Inicio sesion con google
    private fun signInGoogle(){
        // Configure Google Sign In
        //Hace la llamada al constructor del inicio de sesion de google
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            //solicita el token, email y construye
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        //se construye el cliente
        var googleSignInClient = GoogleSignIn.getClient(this, gso)
        googleSignInClient.signOut()

        //larga el intent con la ventana de google y un codigo
        startActivityForResult(googleSignInClient.signInIntent, RESULT_CODE_GOOGLE_SIGN_IN)

    }

    //Comprobacion de la respuesta luego del selector de google
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        //Hacer la llamada y pasar los mismos parametros para facebook
        callbackManager.onActivityResult(requestCode, resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        //Si la respuesta es que anduvo ok
        if (requestCode == RESULT_CODE_GOOGLE_SIGN_IN) {

            try {
                //Crear la tarea para llamar a la fun principal
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                // Google Sign In was successful, authenticate with Firebase
                val account = task.getResult(ApiException::class.java)!!

                //Una vez recibida la cuenta, verificamos si es nula
                if (account != null){
                    //Guardar el email
                    email = account.email!!
                    //Guardar las credenciales con el token
                    val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                    //Realizar el signing  con las creedenciales
                    mAuth.signInWithCredential(credential).addOnCompleteListener{
                        //Si es exitoso -> home
                        if (it.isSuccessful) {
                        //Guardar dato en Firestore
                        //fecha
                        var dateRegister = SimpleDateFormat("dd/MM/yyyy").format(Date())
                        //Instancia para acceder a la db
                        var dbRegister = FirebaseFirestore.getInstance()
                        //Acceder a las colecciones
                        dbRegister.collection("users").document(email).set(hashMapOf(
                            "user" to email,
                            "dateRegister" to dateRegister,
                            "provider" to "Google"
                        ))
                            goHome(email, "Google")
                        }
                        else showError("Google")
                    }
                }
            } catch (e: ApiException) {
                showError("Google")
            }
        }
    }

    fun callSignInFacebook (view:View){

        //Hacer visible el CheckBox
        if (lyTerms.visibility == View.INVISIBLE) {
            lyTerms.visibility = View.VISIBLE
            //showDialogInLogIn()
            Toast.makeText(this, "Aceptar los términos y reintentar", Toast.LENGTH_LONG).show()
        }
        else{
            var cbAcept = findViewById<CheckBox>(R.id.cbAcept)
            //Registrarse
            if (cbAcept.isChecked)
                signInFacebook()
        }
        //signInFacebook()
    }

    //Inicio sesion con facebook
    private fun signInFacebook(){
        //Abrir la ventana de facebook -> leer el email
        LoginManager.getInstance().logInWithReadPermissions(this, listOf("email"))

        //Lo que se recibe de vuelta cuando el usuario hace click
        LoginManager.getInstance().registerCallback(callbackManager, object :
            FacebookCallback<LoginResult> {
            //manejar los 3 eventos:
            override fun onSuccess(result: LoginResult) {
                result.let{
                    //Token
                    val token = it.accessToken
                    //Credenciales
                    val credential = FacebookAuthProvider.getCredential(token.token)
                    //Iniciar sesion en firebase
                    mAuth.signInWithCredential(credential).addOnCompleteListener {
                        if (it.isSuccessful){
                            //Guardar en la variable email el valor
                            email = it.result.user?.email.toString()

                            //Guardar dato en Firestore
                            //fecha
                            var dateRegister = SimpleDateFormat("dd/MM/yyyy").format(Date())
                            //Instancia para acceder a la db
                            var dbRegister = FirebaseFirestore.getInstance()
                            //Acceder a las colecciones
                            dbRegister.collection("users").document(email).set(hashMapOf(
                                "user" to email,
                                "dateRegister" to dateRegister,
                                "provider" to "Facebook"
                            ))

                            goHome(email, "Facebook")
                        }
                        else showError("Facebook")
                    }
                }
                //handleFacebookAccessToken(loginResult.accessToken)
            }
            override fun onCancel() { showError("Facebook")}
            override fun onError(error: FacebookException) { showError("Facebook") }
        })
    }

    private fun showError (provider: String){
        Toast.makeText(this, "Error en la conexión con $provider", Toast.LENGTH_SHORT).show()
    }

    private fun showDialogInLogIn(callback: (() -> Unit)? = null) {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.log_in_dialog))
            .setMessage(getString(R.string.accept_and_retry))
            .setPositiveButton(getString(R.string.ok)) { _, _ -> callback?.invoke() }
            .show()
    }
}