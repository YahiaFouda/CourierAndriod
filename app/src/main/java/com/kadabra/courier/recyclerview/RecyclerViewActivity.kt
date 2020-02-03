package com.kadabra.courier.recyclerview

import androidx.appcompat.app.AppCompatActivity
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.kadabra.courier.R
import com.kadabra.courier.base.BaseActivity
import com.kadabra.courier.databinding.ActivityRecyclerViewBinding
import com.kadabra.courier.model.Point

class RecyclerViewActivity : BaseActivity() {

    private val TAG = javaClass.name

    // ViewModel, DataBinding
    private lateinit var mViewModel: RecyclerViewModel
    private lateinit var mBinding: ActivityRecyclerViewBinding

    // List
    private lateinit var points: List<Point>
    private var actionBar: ActionBar? = null

    override fun before() {
        setupActionBar()
        setupRecyclerViewBindings()
        setupDataOnRecyclerView()
    }

    override fun setupObserving() {
        mViewModel.posMessageLiveData.observe(this, Observer {
            Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
        })
    }

    override fun after() {
    }

    override fun initToolbar() {
    }

    private fun setupActionBar(){
        if(actionBar == null){
            actionBar = supportActionBar
        }
        actionBar!!.setDisplayHomeAsUpEnabled(true)
        actionBar!!.setDisplayShowHomeEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean{
        when(item.itemId){
            (android.R.id.home)->
                //Intent intent = new Intent(this, MainActivity.class)
                //startActivity(intent)
                onBackPressed()
            else -> Exception("???")
        }

        return super.onOptionsItemSelected(item)
    }

    private fun setupRecyclerViewBindings(){
        mViewModel = ViewModelProviders.of(this).get(RecyclerViewModel::class.java)
        mViewModel.init()

        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_recycler_view)
        mBinding.viewmodel = mViewModel
        mBinding.lifecycleOwner = this
    }

    private fun setupDataOnRecyclerView(){
        val intent = intent
        points = intent.getSerializableExtra("LIST") as List<Point>

        if(!points.isNullOrEmpty()){

            mViewModel.setRecyclerViewAdapter(points)
        }
    }
}
