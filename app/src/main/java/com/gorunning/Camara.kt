package com.gorunning

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toFile
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.ktx.storageMetadata
import com.gorunning.LoginActivity.Companion.useremail
import com.gorunning.MainActivity.Companion.countPhotos
import com.gorunning.MainActivity.Companion.lastimage
import com.gorunning.databinding.ActivityCamaraBinding
import java.io.File
import java.lang.Exception
import kotlin.math.max
import kotlin.math.min
import kotlin.math.abs
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class Camara : AppCompatActivity() {

    companion object{
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
        private val REQUEST_CODE_PERMISSIONS = 10

        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0
    }

    private var FILENAME : String = ""
    lateinit var binding : ActivityCamaraBinding

    private var preview: Preview? = null

    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK
    private var cameraProvider: ProcessCameraProvider? = null

    private var imageCapture: ImageCapture? = null
    private lateinit var outputDirectory: File

    private lateinit var cameraExecutor: ExecutorService

    private lateinit var dateRun: String
    private lateinit var startTimeRun: String

    private lateinit var metadata: StorageMetadata


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCamaraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Recibir los keys de la Main
        val bundle = intent.extras
        dateRun = bundle?.getString("dateRun").toString()
        startTimeRun = bundle?.getString("startTimeRun").toString()

        //Ejecutor de la camara -> crearlo con un nuevo hilo
        cameraExecutor = Executors.newSingleThreadExecutor()

        //Configurar la carpeta para guardar las fotos
        outputDirectory = getOutputDirectory()

        //Capturar foto
        binding.cameraCaptureButton.setOnClickListener { takePhoto() }

        //Cambiar entre camara frontal o trasera
        binding.cameraSwitchButton.setOnClickListener {
            lensFacing = if (CameraSelector.LENS_FACING_FRONT == lensFacing){
                CameraSelector.LENS_FACING_BACK
            }else{
                CameraSelector.LENS_FACING_FRONT
            }
            //Vincular la camara elegida
            bindCamera()
        }

        //Si estan todos los permisos aprobados -> iniciar camara
        if (allPermissionsGranted()) startCamera()
        //Si no, solicitar permisos
        else ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
    }

    //Sobreescribir el resultado de los permisos
    override fun onRequestPermissionsResult(requestCode: Int,permissions: Array<out String>,grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        //Si regresa con el codigo OK
        if (requestCode == REQUEST_CODE_PERMISSIONS){
            //Si estan todos aprobados -> iniciar camara
            if (allPermissionsGranted()) startCamera()
            //Toast solicitando permisos
            else{
                Toast.makeText(this, "Debes proporcionar permisos si quieres tomar fotos", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }
    //Permisos aprobados
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all{
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    //Vincular la camara elegida
    private fun bindCamera(){
        //Valores de pantalla
        val metrics = DisplayMetrics().also { binding.viewFinder.display.getRealMetrics(it) }
        val screenAspectRatio = aspectRadio(metrics.widthPixels, metrics.heightPixels)
        val rotation = binding.viewFinder.display.rotation

        //Proveedor
        val cameraProvider = cameraProvider ?: throw IllegalStateException("Fallo al iniciar la camara")

        //Selector de la camara -> la esa lente (lensFacing) tiene que construir la camara
        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        //Preview -> construir con los datos reunidos
        preview = Preview.Builder()
            .setTargetAspectRatio(screenAspectRatio)
            .setTargetRotation(rotation)
            .build()

        //Camara -> construir con los datos reunidos
        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setTargetAspectRatio(screenAspectRatio)
            .setTargetRotation(rotation)
            .build()

        //Desvincular a cualqueir camara que este vinculada
        cameraProvider.unbindAll()

        try{
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            //Cargar todos los elementos en el xml
            preview?.setSurfaceProvider(binding.viewFinder.surfaceProvider)
        }catch(exc: Exception){
            Log.e("CameraWildRunning", "Fallo al vincular la camara", exc)
        }
    }
    //Calcular el ratio del preview
    private fun aspectRadio(width: Int, height: Int): Int{
        //Dividir el mas grande por el mas chico
        val previewRatio = max(width, height).toDouble() / min(width, height)

        //Saber al ratio
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)){
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9


    }
    //Inicializar la camara
    private fun startCamera(){
        val cameraProviderFinnaly = ProcessCameraProvider.getInstance(this)
        cameraProviderFinnaly.addListener(Runnable {

            //Capturar la variable auxiliar creada
            cameraProvider = cameraProviderFinnaly.get()

            //Asignar la lente para reconocer el hardware (segun le codigo)
            lensFacing = when{
                //Consultar que camaras hay disponibles
                hasBackCamera() -> CameraSelector.LENS_FACING_BACK
                hasFrontCamera() -> CameraSelector.LENS_FACING_FRONT
                else -> throw IllegalStateException("No tenemos camara")
            }

            //Administrar que camara usar
            manageSwitchButton()
            //Hacer la conexion
            bindCamera()

        }, ContextCompat.getMainExecutor(this))  //Capturar el hilo principal
    }

    //Tiene camara trasera ?
    private fun hasBackCamera(): Boolean{
        return cameraProvider?.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) ?: false
    }
    //Tiene camara frontal ?
    private fun hasFrontCamera(): Boolean{
        return cameraProvider?.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) ?: false
    }
    //Administrar que camara usar
    private fun manageSwitchButton(){
        val switchButton = binding.cameraSwitchButton
        try {
            //enable si tiene 2 camaras
            switchButton.isEnabled = hasBackCamera() && hasFrontCamera()

        }
        //En caso de no tener informacion sobre si hay camaras -> desabilitar boton
        catch (exc: CameraInfoUnavailableException){
            //false si tiene solo una camara
            switchButton.isEnabled = false
        }
    }

    //Configurar la carpeta para guardar las fotos
    private fun getOutputDirectory(): File{
        //Administrar ubicacion para guardar. Si existe OK, si no, se crea mkdirs()
        val mediaDir = externalMediaDirs.firstOrNull()?.let{
            File(it, "wildRunning").apply {  mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists()) mediaDir else filesDir

    }
    //Capturar foto
    private fun takePhoto(){
        FILENAME = getString(R.string.app_name) + useremail + dateRun + startTimeRun
        FILENAME = FILENAME.replace(":", "")
        FILENAME = FILENAME.replace("/", "")

        //Antes de generar la foto -> se guardan los metadatas a firestore
        //Que orientacion tiene actualmente ?
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
            //Generamos una variable con la informacion de la orientacion
            metadata = storageMetadata {
                //Aca guardar los metadatos que quiera en clave/valor
                contentType = "image/jpg"
                //Para establecer metadatas personalizados (clave/valor)
                setCustomMetadata("orientation", "horizontal")
            }
        else
            metadata = storageMetadata {
                contentType = "image/jpg"
                setCustomMetadata("orientation", "vertical")
            }


        //Crear el archivo = directorio de salida, nombre y extencion
        val photoFile = File (outputDirectory, FILENAME + ".jpg")
        //Opciones de salida
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        //Capturar la imagen
        imageCapture?.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            //La llamada que se tiene que hacerse cuando la imagen sea guardada
            object:ImageCapture.OnImageSavedCallback{
                //Manejar ambos eventos
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    //Que hacer cuando se guarde la imagen -> actualizar la galeria
                    val  savedUri = Uri.fromFile(photoFile)  //uri = identificador unico de recurso

                    //version del SDK -> API 23 +
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                        setGalleryThumbnail (savedUri)
                    }

                    //Actualizar la galeria para que aparezca ese archivo
                    val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(savedUri.toFile().extension)
                    MediaScannerConnection.scanFile(
                        baseContext,
                        arrayOf(savedUri.toFile().absolutePath),
                        arrayOf(mimeType)
                    ){ _, uri ->

                    }

                    /*
                    var clMain = findViewById<ConstraintLayout>(R.id.clMain)
                    Snackbar.make(clMain, "Imagen guardada con éxito", Snackbar.LENGTH_LONG).setAction("OK"){
                        clMain.setBackgroundColor(Color.CYAN)
                    }.show()
                    */

                    //Firebase Storage
                    //Subida del archivo
                    upLoadFile(photoFile)

                }
                //Cuando ocurra un error -> Snackbar
                override fun onError(exception: ImageCaptureException) {
                    var clMain = findViewById<ConstraintLayout>(R.id.clMain)
                    Snackbar.make(clMain, "Error al guardar la imagen", Snackbar.LENGTH_LONG).setAction("OK"){
                        clMain.setBackgroundColor(Color.CYAN)
                    }.show()
                }
            })
    }
    //Cual queremos que sea la miniatura
    private fun setGalleryThumbnail(uri: Uri){
        var thumbnail = binding.photoViewButton
        thumbnail.post {
            Glide.with (thumbnail)
                .load(uri)
                .apply(RequestOptions.circleCropTransform())
                .into(thumbnail)
        }
    }
    //Subir el archivo al Storage
    private fun upLoadFile(image: File){
        //Nombre del directorio
        var dirName = dateRun + startTimeRun
        dirName = dirName.replace(":", "")
        dirName = dirName.replace("/", "")

        //Nombre del archivo
        var fileName = dirName + "-" + countPhotos

        //Instancia de Storage
        val storageReference = FirebaseStorage.getInstance().getReference("images/$useremail/$dirName/$fileName")

        //Publicar el archivo
        storageReference.putFile(Uri.fromFile(image))
            .addOnSuccessListener {
                //Referenciar la ruta de la "lastimage"
                lastimage = "images/$useremail/$dirName/$fileName"
                //Aumentar el condador de fotos
                countPhotos++

                //Borrar el archivo del dispositivo una vez que se sube a la nube
                val myFile = File(image.absolutePath)
                myFile.delete()

                //Asociar el metadata generado
                val metaRef = FirebaseStorage.getInstance().getReference("images/$useremail/$dirName/$fileName")
                //Actualizamos el metadata
                metaRef.updateMetadata(metadata)
                    .addOnSuccessListener {
                    }
                    .addOnFailureListener {
                    }

                //Mensaje de confirmacion de subida -> se hace luego de subir y borrar la imagen del dispositivo
                var clMain = findViewById<ConstraintLayout>(R.id.clMain)
                Snackbar.make(clMain, "Imagen Subida a la nube", Snackbar.LENGTH_LONG).setAction("OK") {
                    clMain.setBackgroundColor(Color.CYAN)
                }.show()

            }
            .addOnFailureListener{
                Toast.makeText(this, "Tu imagen se guardó en el tfno, pero no en la nube :(",Toast.LENGTH_LONG).show()
            }
    }


    //TODO confirmar si va o no
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
