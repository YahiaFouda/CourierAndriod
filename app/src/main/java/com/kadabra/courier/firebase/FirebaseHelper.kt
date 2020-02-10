package com.kadabra.courier.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.kadabra.courier.model.Courier
import com.kadabra.courier.model.location
import com.kadabra.courier.utilities.AppConstants
import com.kadabra.courier.model.Task
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.ValueEventListener


object FirebaseHelper {

    //region Members
    private var TAG = "FirebaseHelper"
    private lateinit var dbCourier: DatabaseReference
    private lateinit var dbCourierTaskHistory: DatabaseReference

    private lateinit var firebaseDatabase: FirebaseDatabase
    private lateinit var auth: FirebaseAuth
    private var firebaseAuthListener: FirebaseAuth.AuthStateListener? = null
    private var fireBaseUser: FirebaseUser? = null
    private var dbNameCourier = "courierTest"
    private var dbNameTaskHistory = "taskHistory"
    private var courier = Courier()
    var exception = ""


    //endregion

    //region Helper Functions
    interface IFbOperation {
        fun onSuccess(code: Int)
        fun onFailure(message: String)
    }

    fun setUpFirebase() {

        auth = FirebaseAuth.getInstance()
        firebaseDatabase = FirebaseDatabase.getInstance()
        dbCourier = firebaseDatabase.getReference(dbNameCourier)
        dbCourierTaskHistory = firebaseDatabase.getReference(dbNameTaskHistory)

//        checkCourierExist()
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
    fun createAccount(userName: String, password: String, listener: IFbOperation): Boolean {
        var done = false
        auth.createUserWithEmailAndPassword(userName, password).addOnCompleteListener {
            if (it.isSuccessful) {// user created in auth and ready to insert to db
                fireBaseUser = auth.currentUser!!
                listener.onSuccess(1)
                done = true
            } else { //failed to auth user so it must be register and prevent deal with him
                exception = it.exception!!.message!!
                listener.onFailure(exception)
                done = false
            }
        }
        return done
    }

    fun logIn(
        userName: String,
        password: String, listener: IFbOperation
    ): Boolean {
        var done = false
        auth.signInWithEmailAndPassword(userName, password).addOnCompleteListener {
            if (it.isSuccessful) {
                fireBaseUser = auth.currentUser!!
                listener.onSuccess(2)
                done = true
            } else {
                listener.onFailure(it.exception!!.message!!)
                fireBaseUser = null
            }
        }
        return done
    }

    fun logOut(): Boolean {
        FirebaseAuth.getInstance().signOut()
        var user = checkCourierExist()
        return user == null
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
        courier: Courier
    ) {

        dbCourier.child(courier.CourierId.toString()).setValue(courier)

    }

    fun updateCourier(
        courier: Courier
    ) {


        val map = mapOf(
            "token" to courier.token,
            "name" to courier.name,
            "city" to courier.city,
            "location" to courier.location,
            "isActive" to courier.isActive,

            "" to "Geeks"
        )
        dbCourier.child(courier.CourierId.toString()).updateChildren(map)

    }

    fun createNewTask(task: Task) {
        dbCourierTaskHistory.child(task.TaskId).setValue(task)
       updateTaskLocation(task)
    }

    fun updateTaskLocation(task: Task) {
        dbCourierTaskHistory.child(task.TaskId).child("locations").push().setValue(task.location)

    }

    fun endTask(task: Task) {
        dbCourierTaskHistory.child(task.TaskId).child("active")
            .setValue(false)
    }

    fun getActiveTask(task: Task): Task {
        dbCourierTaskHistory.child(task.courierId.toString())
            .limitToFirst(1)
        return Task()
    }

    fun getCurrentActiveTask(courieId: String, listener: IFbOperation) {
        // active =true and courierid=current
        var task = Task()
        val query = dbCourierTaskHistory.orderByChild("courieId").equalTo(courieId)

        query.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                listener.onFailure(p0.message)
            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (currentTask in dataSnapshot.children) {
                    task = dataSnapshot.getValue(Task::class.java)!!
                    if(task.isActive)
                    {
                        AppConstants.CurrentAcceptedTask = task
                        listener.onSuccess(1)
                    }


                }
            }

        })

    }


    fun updateCourierCity(courierId: Int, value: Any) {
        dbCourier.child(courierId.toString()).child(AppConstants.FIREBASE_CITY)
            .setValue(value)

    }

    fun updateCourierLocation(location: location) {
        dbCourier.child(courier.CourierId.toString())
            .child(AppConstants.FIREBASE_LOCATION).setValue(location)


    }

    fun updateCourierActive(courierId: Int, value: Object) {
        dbCourier.child(courierId.toString()).child(AppConstants.FIREBASE_IS_ACTIVE)
            .setValue(value)

    }

    fun addUserChangeListener(): Courier {
        // User data change listener
        dbCourier.child(dbNameCourier).addValueEventListener(object : ValueEventListener {
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
    fun startAuthListener() {
        auth.addAuthStateListener(firebaseAuthListener!!)
    }

    //stop listen
    fun stopAuthListener() {
        auth.removeAuthStateListener(firebaseAuthListener!!)
    }

    //check after logIn if the user is exit or not on auth
    //if exist navigate tio main or logIn
    fun checkCourierExist(): FirebaseUser? {

        firebaseAuthListener = FirebaseAuth.AuthStateListener {
            fireBaseUser = FirebaseAuth.getInstance().currentUser
        }
        if (fireBaseUser != null)
            fireBaseUser = FirebaseAuth.getInstance().currentUser
        else
            fireBaseUser = null

        return fireBaseUser
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

    //endregion


}