package com.example.driverbooking

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.storage.StorageManager
import android.text.TextUtils
import android.view.Menu
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.bumptech.glide.Glide
import com.example.driverbooking.Utils.UserUtils
import com.firebase.ui.auth.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.lang.StringBuilder
import java.util.*
import kotlin.collections.HashMap

class DriverHomeActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var navView:NavigationView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navController:NavController
    private lateinit var img_avatar:ImageView
    private lateinit var waittingDialog:AlertDialog
    private lateinit var storageReference:StorageReference
    private var imageUri: Uri? =null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_home)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)


        drawerLayout = findViewById(R.id.drawer_layout) as DrawerLayout
        navView = findViewById(R.id.nav_view)
        navController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        init()
    }

    private fun init() {

        storageReference =FirebaseStorage.getInstance().getReference()

        waittingDialog = AlertDialog.Builder(this)
            .setMessage("Wating...")
            .setCancelable(false).create()

        navView.setNavigationItemSelectedListener {it->
            if(it.itemId == R.id.nav_sign_out)
            {
                val builder = AlertDialog.Builder(this@DriverHomeActivity)
                builder.setTitle("Sign out")
                    .setMessage("Do you really want to sign out?")
                    .setNegativeButton("CANCEL",{dialogInterface, _ -> dialogInterface.dismiss() })
                    .setPositiveButton("SIGN OUT"){dialogInterface,  _->
                        FirebaseAuth.getInstance().signOut()
                        val intent = Intent(this@DriverHomeActivity,SplashScreenActivity::class.java)
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        startActivity(intent)
                        finish()

                    }.setCancelable(false)

                val dialog = builder.create()
                dialog.setOnShowListener{
                    //Fix
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setTextColor(ContextCompat.getColor(this@DriverHomeActivity,android.R.color.holo_red_dark))
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                        .setTextColor(ContextCompat.getColor(this@DriverHomeActivity,R.color.colorAccent))
                }

                dialog.show()
            }
            true
        }

        val headerView = navView.getHeaderView(0)
        val txt_name= headerView.findViewById<View>(R.id.txt_name) as TextView
        val txt_phone= headerView.findViewById<View>(R.id.txt_phone) as TextView
        val txt_star= headerView.findViewById<View>(R.id.txt_star) as TextView
        img_avatar = headerView.findViewById<View>(R.id.img_avatar) as ImageView
        txt_name.setText(Common.buildWelcomeMessage())
        txt_phone.setText(Common.currentUser!!.phoneNumber)
        txt_star.setText(StringBuilder().append(Common.currentUser!!.rating))

        if(Common.currentUser !=null && Common.currentUser!!.avatar != null && !TextUtils.isEmpty(Common.currentUser!!.avatar))
        {
            Glide.with(this)
                .load(Common.currentUser!!.avatar)
                .into(img_avatar)
        }

        img_avatar.setOnClickListener{
            val intent = Intent()
            intent.setType("image/*")
            intent.setAction(Intent.ACTION_GET_CONTENT)
            startActivityForResult(Intent.createChooser(intent,"Select Picture"),PICK_IMAGE_REQUEST)

        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK)
        {
            if(data !=null &&data.data !=null)
            {
                imageUri=data.data
                img_avatar.setImageURI(imageUri)

                showDialogUpload()
            }
        }
    }

    private fun showDialogUpload() {
        val builder = AlertDialog.Builder(this@DriverHomeActivity)
        builder.setTitle("Change Avatar")
            .setMessage("Do you really want to change Avatar?")
            .setNegativeButton("CANCEL",{dialogInterface, _ -> dialogInterface.dismiss() })

            .setPositiveButton("CHANGE"){dialogInterface,  _->
                if(imageUri !=null)
                {
                    waittingDialog.show()
                    val avatarFolder = storageReference.child("avatars/"+FirebaseAuth
                        .getInstance().currentUser!!.uid)

                    avatarFolder.putFile(imageUri!!)
                        .addOnFailureListener{e->
                            Snackbar.make(drawerLayout,e.message!!,Snackbar.LENGTH_LONG).show()
                        }.addOnCompleteListener{task ->
                            if(task.isSuccessful)
                            {
                                avatarFolder.downloadUrl.addOnSuccessListener { uri->
                                    val update_data = HashMap<String,Any>()
                                    update_data.put("avatar",uri.toString())

                                    UserUtils.updateUser(drawerLayout,update_data)

                                }
                            }
                            waittingDialog.dismiss()
                        }.addOnProgressListener { taskSnapshot ->
                            val progress=(100.0*taskSnapshot.bytesTransferred/taskSnapshot.totalByteCount)
                            waittingDialog.setMessage(StringBuilder("Uploading: ").append(progress).append("%"))
                        }
                }

            }.setCancelable(false)

        val dialog = builder.create()
        dialog.setOnShowListener{
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setTextColor(ContextCompat.getColor(this@DriverHomeActivity,android.R.color.holo_red_dark))
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                .setTextColor(ContextCompat.getColor(this@DriverHomeActivity,R.color.colorAccent))
        }

        dialog.show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.driver_home, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    companion object{
        val PICK_IMAGE_REQUEST=7272
    }
}

