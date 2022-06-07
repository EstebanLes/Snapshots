package com.example.snapshots

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.snapshots.databinding.FragmentAddBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class AddFragment : Fragment() {

    private val RC_GALLERY = 18// variable creada para el codigo de la galeria
    private val PATH_SNAPSHOT = "snapshots" //variable creada para almacenar la ruta de la imagen

    private lateinit var mBinding: FragmentAddBinding
    private lateinit var mStorageReference: StorageReference
    private lateinit var mDataBaseReference: DatabaseReference

    private var mPhotoSelectedUri: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        mBinding = FragmentAddBinding.inflate(inflater, container, false)
        return mBinding.root
    }

    //este metodo se ejecuta cuando el fragmento se ha creado y siempre se debe implementar
    // las vistas y los listeners de los botones y demas elementos de la vista
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mBinding.btnPost.setOnClickListener { postSnapshot() }
        mBinding.btnSelect.setOnClickListener { openGallery() }

        mStorageReference = FirebaseStorage.getInstance().reference
        mDataBaseReference = FirebaseDatabase.getInstance().reference.child(PATH_SNAPSHOT)
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, RC_GALLERY)
    }

    //este metodo se ejecuta cuando se selecciona una imagen de la galeria
    private fun postSnapshot() {
        mBinding.progressBar.visibility = View.VISIBLE
        val key = mDataBaseReference.push().key!!
        val storageReference = mStorageReference.child(PATH_SNAPSHOT)
            .child(FirebaseAuth.getInstance().currentUser!!.uid)
            .child(key)
        if (mPhotoSelectedUri != null) {
            storageReference.putFile(mPhotoSelectedUri!!)
                .addOnProgressListener { it ->
                    val progress = 100 * it.bytesTransferred / it.totalByteCount.toDouble()//barra de progreso al subir la imagen

                    mBinding.progressBar.progress = progress.toInt()
                    mBinding.tvMessage.text = "$progress %"
                }
                .addOnCompleteListener {
                    mBinding.progressBar.visibility = View.INVISIBLE
                }
                .addOnSuccessListener { it ->
                    Snackbar.make(mBinding.root, "Instantanea Publicada.",
                        Snackbar.LENGTH_SHORT).show()
                    it.storage.downloadUrl.addOnSuccessListener {
                        saveSnapshot(key,it.toString(),mBinding.etTitle.text.toString().trim())
                        mBinding.tilTitle.visibility = View.GONE
                        mBinding.tvMessage.text = getString(R.string.post_message_title)
                    }
                }
                .addOnFailureListener {
                Snackbar.make(mBinding.root, "Error al publicar la instantanea." , Snackbar.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun saveSnapshot(key: String, url: String, title: String) {

        val snapshot = Snapshot( title=title, photoUrl =url)
        mDataBaseReference.child(key).setValue(snapshot)
    }

    @Deprecated("accion deprecada por java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) { //aca me equivoque y puse el requestCode y no me dejaba agregar la foto a la galeria
            if (requestCode == RC_GALLERY) {
                mPhotoSelectedUri = data?.data
                mBinding.imgPhoto.setImageURI(mPhotoSelectedUri)
                mBinding.tilTitle.visibility = View.VISIBLE
                mBinding.tvMessage.text = getText(R.string.post_message_valid_title)
            }
        }
    }
}