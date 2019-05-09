package mx.itson.miscomidas

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.android.synthetic.main.activity_agregar_comida.*
import java.io.File
import java.net.URI
import java.text.SimpleDateFormat
import java.util.*
import java.util.jar.Manifest
import android.support.annotation.NonNull
import android.support.v4.app.FragmentActivity
import android.util.Log
import com.google.android.gms.tasks.OnFailureListener
import com.google.firebase.storage.UploadTask
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.database.FirebaseDatabase


class AgregarComidaActivity : AppCompatActivity() {

    private val CODIGO_PERMISOS = 123
    private val CODIGO_IMAGE = 55
    private val CODIGO_LEER = 20
    private val CODIGO_SELECCION = 23
    var img_uri: Uri? = null
    var nombreImg: String = "nombre.jpg"
    private var mStorageRef: StorageReference? = null
    var database = FirebaseDatabase.getInstance()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_agregar_comida)

        mStorageRef = FirebaseStorage.getInstance().getReference()

        btn_tomar_foto.setOnClickListener{
            //Verifica los permisos
            //A partir de la versión de Android de Marshmallow
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                if(checkSelfPermission(android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED ||
                    checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED){
                    //no tiene los permisos

                    val permisos = arrayOf(android.Manifest.permission.CAMERA,
                                            android.Manifest.permission.WRITE_EXTERNAL_STORAGE)

                    //muestra ventana para pedir los permisos al usuario
                    requestPermissions(permisos, CODIGO_PERMISOS)
                }else{
                    //tiene los permisos
                    tomar_foto()
                }
            }else{
                //No es necesario pedir los permisos directamente
                tomar_foto()
            }
        }

        btn_agregar_imagen.setOnClickListener{
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                if(checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) ==
                        PackageManager.PERMISSION_DENIED){
                    val permiso = arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)

                    requestPermissions(permiso, CODIGO_LEER)
                }else{
                    subir_foto();
                }
            }else{
                subir_foto()
            }
        }

        btn_guardar.setOnClickListener{
            guardar_firebase_storage();
            guardar_firebase_bd();
        }
    }

//
    private fun guardar_firebase_bd(){
        var myRef = database.getReference("usuario")

        var desc = et_desc.text.toString()

        myRef.child("desc").setValue(desc)



        myRef.child("img").setValue(nombreImg)

    }

    private fun guardar_firebase_storage(){

        val referencia: StorageReference? = mStorageRef?.child("fotos/$nombreImg")

        referencia?.putFile(img_uri!!)
            ?.addOnSuccessListener(OnSuccessListener<UploadTask.TaskSnapshot> { taskSnapshot ->

                var downloadURL = taskSnapshot.storage.downloadUrl.toString()

                taskSnapshot.storage.downloadUrl.addOnSuccessListener { uri ->
                    downloadURL = uri.toString()
                }.addOnFailureListener{
                    Toast.makeText(this,"Failed upload 1", Toast.LENGTH_SHORT).show()
                }

            })
            ?.addOnFailureListener(OnFailureListener {
                var msj = it.message
                Toast.makeText(this, msj, Toast.LENGTH_SHORT).show()
            })
    }

    private fun ubicacion(): String{
        val carpeta = File(Environment.getExternalStorageDirectory(), "prueba_imgs")
        if(!carpeta.exists()){
            carpeta.mkdir()
        }

        return carpeta.absolutePath
    }

    private fun subir_foto(){
        var intentSelect = Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intentSelect, CODIGO_SELECCION)
    }

    private fun tomar_foto(){
        val date = Date()
        val df = SimpleDateFormat("yyyy-MM-dd_HH:mm:ss")
        nombreImg = df.format(date) + ".jpg"
        val outFile = File(ubicacion(), nombreImg)
        img_uri = Uri.fromFile(outFile)

        val camara_intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        camara_intent.putExtra(MediaStore.EXTRA_OUTPUT, img_uri)
        startActivityForResult(camara_intent, CODIGO_IMAGE)

    }



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when(requestCode){

            CODIGO_IMAGE ->{
                // Toast.makeText(this,"AQUI 1", Toast.LENGTH_SHORT).show();
                if(resultCode == Activity.RESULT_OK){
                    //var img_bitmap = data.extras.get("data") as Bitmap
                    //iv_comida.setImageBitmap(img_bitmap)

                    //var img = data.data
//                    Toast.makeText(this,"esperando", Toast.LENGTH_SHORT).show()
//                    Thread.sleep(3000);
                    iv_comida.setImageURI(img_uri)
                   // Toast.makeText(this,"IMG URI", Toast.LENGTH_LONG).show()
                }
            }

            CODIGO_SELECCION -> {
                if(resultCode == Activity.RESULT_OK && data != null){
                    val selectedImage = data.data
                    val filePathColumn = arrayOf(MediaStore.Images.Media.DATA)
                    val cursor = contentResolver.query(selectedImage, filePathColumn,null,null,null)
                    cursor.moveToFirst()

                    val columnIndex = cursor.getColumnIndex(filePathColumn[0])
                    val picturePath = cursor.getString(columnIndex)
                    cursor.close()
                    iv_comida.setImageBitmap(BitmapFactory.decodeFile(picturePath))
                }
            }
        }


    }

    //Maneja el resultado de los permisos
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when(requestCode){
            CODIGO_PERMISOS -> {
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    tomar_foto()
                }else{
                    Toast.makeText(this,"Permisos negados", Toast.LENGTH_SHORT).show()
                }

            }

            CODIGO_LEER -> {
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    subir_foto()
                }else{
                    Toast.makeText(this,"Permiso negado", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
