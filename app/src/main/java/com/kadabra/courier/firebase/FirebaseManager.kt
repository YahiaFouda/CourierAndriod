package com.kadabra.courier.firebase

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.kadabra.courier.model.Courier
import com.kadabra.courier.model.location
import com.kadabra.courier.utilities.AppConstants
import com.kadabra.courier.model.Task
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.ValueEventListener
import com.google.firebase.iid.FirebaseInstanceId
import com.kadabra.courier.location.LocationHelper


object FirebaseManager {

    //region Members
    private var TAG = "FirebaseManager"
    private lateinit var dbCourier: DatabaseReference
    private lateinit var dbCourierTaskHistory: DatabaseReference

    private lateinit var firebaseDatabase: FirebaseDatabase
    private lateinit var auth: FirebaseAuth
    private var firebaseAuthListener: FirebaseAuth.AuthStateListener? = null
    private var fireBaseUser: FirebaseUser? = null
    private var dbNameCourier = "courier"
    private var dbNameTaskHistory = "task_history"
    private var courier = Courier()
    var exception = ""
    var token = ""


    //endregion

    //region Helper Functions
    interface IFbOperation {
        fun onSuccess(code: Int)
        fun onFailure(message: String)
    }

    fun setUpFirebase() {

        FirebaseInstanceId.getInstance().instanceId.addOnSuccessListener { instanceIdResult ->
            token = instanceIdResult.token
            Log.d("Token", token)
        }
        auth = FirebaseAuth.getInstance()
        firebaseDatabase = FirebaseDatabase.getInstance()
        dbCourier = firebaseDatabase.getReference(dbNameCourier)
        dbCourierTaskHistory = firebaseDatabase.getReference(dbNameTaskHistory)

    }

    fun getCurrentUser(): FirebaseUser? {
        return fireBaseUser
    }

    fun auth(): FirebaseAuth {
        return auth
    }

    fun setCurrentUser(fUser: FirebaseUser) {
        fireBaseUser = fUser
    }

    //auth courier for deal with db or not
    fun createAccount(
        userName: String,
        password: String,
        listener: (user: FirebaseUser?, error: String?) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(userName, password).addOnCompleteListener {
            if (it.isSuccessful) {// user created in auth and ready to insert to db
                // setCurrentUser(auth.currentUser!!)
                listener(auth.currentUser, null)
                //  listener.onSuccess(1)
            } else { //failed to auth user so it must be register and prevent deal with him
                listener(null, it.exception!!.message)
                // exception = it.exception!!.message!!

            }
        }
    }

    fun logIn(
        userName: String, password: String, listener: (user: FirebaseUser?, error: String?) -> Unit
    ) {
        auth.signInWithEmailAndPassword(userName, password).addOnCompleteListener {
            if (it.isSuccessful) {
                fireBaseUser = auth.currentUser!!
                listener(auth.currentUser, null)
            } else {
                listener(null, it.exception!!.message!!)
            }
        }

    }

    fun logOut(): Boolean {
        FirebaseAuth.getInstance().signOut()
        return true
    }

    fun checkCourierLogInStatus(): Boolean {
        return auth.currentUser != null
    }

    fun updateCourierPassword(password: String): Boolean {
        var done = false
        auth.currentUser?.updatePassword(password)
            ?.addOnCompleteListener {
                done = it.isSuccessful
            }
        return done
    }

    fun createCourier(
        courier: Courier,
        completion: (success: Boolean) -> Unit
    ) {

        dbCourier.child(courier.CourierId.toString()).setValue(courier) { error, _ ->
            print(error)
            completion(error == null)


        }
        //.setValue(courier)

    }

    fun updateCourier(
        courier: Courier,
        completion: (success: Boolean) -> Unit
    ) {


        val map = mapOf(
            "token" to courier.token,
            "name" to courier.name,
            "city" to courier.city,
            "location" to courier.location,
            "isActive" to courier.isActive
        )
        dbCourier.child(courier.CourierId.toString()).updateChildren(map)
        { error, _ ->
            print(error)
            completion(error == null)
        }

    }

    fun createNewTask(task: Task) {
       if(AppConstants.CurrentLocation!=null)
       {
           dbCourierTaskHistory.child(task.TaskId).setValue(task)
           updateTaskLocation(task)
       }
    }

    fun updateTaskLocation(task: Task) {
        if(AppConstants.CurrentLocation!=null) {
            task.location.isGpsEnabled = LocationHelper.shared.isLocationEnabled()
            dbCourierTaskHistory.child(task.TaskId).child("locations").push()
                .setValue(task.location)
        }
    }

    fun endTask(task: Task) {
        dbCourierTaskHistory.child(task.TaskId).child("active")
            .setValue(false)
    }


    fun getCurrentActiveTask(courieId: String, listener: IFbOperation) {
        var task = Task()
        val query = dbCourierTaskHistory
        dbCourierTaskHistory.keepSynced(true)
        var valueEventListener = query.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                listener.onFailure(p0.message)
            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (currentTask in dataSnapshot.children) {
                    task = currentTask.getValue(Task::class.java)!!
                    if (task.isActive && task.CourierID == courieId.toInt()) {
                        task.TaskId = currentTask.key!!
                        AppConstants.CurrentAcceptedTask = task
                        listener.onSuccess(1)
                    }
                }
            }

        })

        dbCourierTaskHistory.addListenerForSingleValueEvent(valueEventListener)

    }

    fun getCurrentCourierLocation(courieId: String, listener: IFbOperation) {

        val query = dbCourier.child(courieId).child("location")
        dbCourier.keepSynced(true)

        var valueListener = query.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                listener.onFailure(p0.message)
            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (currentTask in dataSnapshot.children) {
                    try {
                        var location = dataSnapshot.getValue(location::class.java)!!
                        if (!location.lat.isNullOrEmpty() && !location.long.isNullOrEmpty()) {
                            AppConstants.CurrentCourierLocation = location!!
                            listener.onSuccess(1)
                        }
                    } catch (ex: Exception) {
                        Log.d("FIreBase Manager", ex.message)
                    }
                }
            }

        })

        dbCourier.addListenerForSingleValueEvent(valueListener)
    }

    fun updateCourierCity(courierId: Int, value: Any) {
        dbCourier.child(courierId.toString()).child(AppConstants.FIREBASE_CITY)
            .setValue(value)

    }

    fun updateCourierLocation(courierId: String, location: location) {
        dbCourier.child(courierId)
            .child("location").setValue(location)
            .addOnFailureListener {
                Log.d("Location", it.toString())
            }.addOnSuccessListener {
                var t = 120
                var s = t
            }


    }

    //setValue(location)


    fun updateCourierActive(courierId: Int, value: Object) {
        dbCourier.child(courierId.toString()).child(AppConstants.FIREBASE_IS_ACTIVE)
            .setValue(value)

    }

    fun listenOnTaskHistory(taskId: String): Courier {
        // User data change listener

        dbCourierTaskHistory.child(taskId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                var updatedCourier = dataSnapshot.getValue(Courier::class.java)!!

                // Check for null
                if (updatedCourier == null) {
                    return
                }


                // Display newly updated data
                courier.name = updatedCourier.name
                courier.token = updatedCourier.token
                courier.city = updatedCourier.city
                courier.location = updatedCourier.location
                courier.isActive = updatedCourier.isActive

            }

            override fun onCancelled(error: DatabaseError) {
                // Failed to read value
            }
        })

        return courier
    }

    //start listen
    fun onstartAuthListener() {
        auth.addAuthStateListener(firebaseAuthListener!!)
    }

    //stop listen
    fun stopAuthListener() {
        auth.removeAuthStateListener(firebaseAuthListener!!)
    }

    //check after logIn if the user is exit or not on auth
//if exist navigate to main or logIn
    fun getCurrentUser(listener: (user: FirebaseUser?) -> Unit) {
//        firebaseAuthListener = FirebaseAuth.AuthStateListener {
//            fireBaseUser = FirebaseAuth.getInstance().currentUser
//            listener(fireBaseUser)
//        }

        val user = FirebaseAuth.getInstance().currentUser
        user?.let {
            // Name, email address, and profile photo Url
            val name = user.displayName
            val email = user.email
            val photoUrl = user.photoUrl

            // Check if user's email is verified
            val emailVerified = user.isEmailVerified

            // The user's ID, unique to the Firebase project. Do NOT use this value to
            // authenticate with your backend server, if you have one. Use
            // FirebaseUser.getToken() instead.
            val uid = user.uid

            listener(user)
        }

    }

    private fun checkAccountEmailExistInFirebase(email: String): Boolean {
        val mAuth = FirebaseAuth.getInstance()
        var isExist = false
        auth.fetchSignInMethodsForEmail(email)
            .addOnCompleteListener {
                isExist = !it.result.toString().isEmpty()
            }
        return isExist
    }

    private fun clearDb() {
        var applesQuery = dbCourierTaskHistory.setValue(null)
    }
//endregion


}