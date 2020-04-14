package com.kadabra.courier.firebase

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.iid.FirebaseInstanceId
import com.kadabra.courier.location.LocationHelper
import com.kadabra.courier.model.Courier
import com.kadabra.courier.model.Task
import com.kadabra.courier.model.location
import com.kadabra.courier.utilities.AppConstants


object FirebaseManager {

    //region Members
    private var TAG = "FirebaseManager"
    private lateinit var dbCourier: DatabaseReference
    private lateinit var dbCourierTaskHistory: DatabaseReference
    private lateinit var dbCourierFeesTaskHistory: DatabaseReference


    private lateinit var firebaseDatabase: FirebaseDatabase
    private lateinit var auth: FirebaseAuth
    private var firebaseAuthListener: FirebaseAuth.AuthStateListener? = null
    private var fireBaseUser: FirebaseUser? = null
    private var dbNameCourier = "courier"
    private var dbNameTaskHistory = "task_history"
    private var dbNameCourierFeesTaskHistory = "courier_fees_task_history"

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
        dbCourierFeesTaskHistory = firebaseDatabase.getReference(dbNameCourierFeesTaskHistory)

//        clearDb()

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
                fireBaseUser = auth.currentUser!!
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
            "isActive" to courier.isActive,
            "haveTask" to courier.haveTask,
            "startTask" to courier.startTask
        )
        dbCourier.child(courier.CourierId.toString()).updateChildren(map)
        { error, _ ->
            print(error)
            completion(error == null)
        }

    }

    fun createNewTask(task: Task, courierId: Int) {
        if (AppConstants.CurrentLocation != null) {
            dbCourierTaskHistory.child(task.TaskId).setValue(task)
            updateTaskLocation(task)
            updateCourierFeesTaskLocation(task)
        }
    }

    fun updateTaskLocation(task: Task) {
        if (AppConstants.CurrentLocation != null) {
            task.location.isGpsEnabled = LocationHelper.shared.isGPSEnabled()
            dbCourierTaskHistory.child(task.TaskId).child("locations").push()
                .setValue(task.location)
        }
    }

    fun createCourierFeesTaskLocation(task: Task) {
        var current = Task()
        current.TaskId = task.TaskId
        current.CourierID = task.CourierID
        current.isActive = true

        dbCourierFeesTaskHistory.child(current.TaskId).setValue(current)
    }

    fun updateCourierFeesTaskLocation(task: Task) {
        if (AppConstants.CurrentLocation != null) {
            task.isActive=true
            task.location.isGpsEnabled = LocationHelper.shared.isGPSEnabled()
            dbCourierFeesTaskHistory.child(task.TaskId).child("locations").push()
                .setValue(location())
        }
    }

    fun endTask(task: Task, courierId: Int) {
        updateCourierHaveTask(courierId, false)
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
                    try {
                        task = currentTask.getValue(Task::class.java)!!
                        if (task.isActive && task.CourierID == courieId.toInt() && !task.TaskId.isNullOrEmpty()) {
                            task.TaskId = currentTask.key!!
                            AppConstants.CurrentAcceptedTask = task
                            listener.onSuccess(1)
                        }
                    } catch (ex: java.lang.Exception) {
                        Log.e(TAG, ex.message)
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


    fun isCourierStartTask(courieId: String, listener: IFbOperation) {


        val query =dbCourier
        dbCourier.keepSynced(true)


        var valueListener = query.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                listener.onFailure(p0.message)
            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (currentTask in dataSnapshot.children) {
                    try {
                        var curreentCourier=dataSnapshot.getValue(Courier::class.java)!!
                        if(curreentCourier.CourierId.toString()==courieId)
                        {
//                            var startTask = dataSnapshot.getValue(Boolean::class.java)!!
                            if (curreentCourier.startTask) {
                                AppConstants.COURIERSTARTTASK = true
                                listener.onSuccess(1)
                            }
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


    fun updateCourierActive(courierId: Int, value: Boolean) {
        dbCourier.child(courierId.toString()).child(AppConstants.FIREBASE_IS_ACTIVE)
            .setValue(value)

    }

    fun updateCourierHaveTask(courierId: Int, value: Boolean) {
        dbCourier.child(courierId.toString()).child(AppConstants.FIREBASE_HAVE_TASK)
            .setValue(value)

    }

    fun updateCourierStartTask(courierId: Int, value: Boolean) {
        dbCourier.child(courierId.toString()).child(AppConstants.FIREBASE_START_TASK)
            .setValue(value)

    }


    private fun clearDb() {
        var applesQuery = dbCourierTaskHistory.setValue(null)
        var applesQuery1 = dbCourierFeesTaskHistory.setValue(null)
    }


//endregion


}