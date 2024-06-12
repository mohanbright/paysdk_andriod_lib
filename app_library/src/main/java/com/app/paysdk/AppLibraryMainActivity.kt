package com.app.paysdk

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.denovo.app.top.uilayer.sdk.ToPService
import com.denovo.app.top.uilayer.sdk.ToPService.OnBatchViewListener
import com.denovo.app.top.uilayer.sdk.ToPService.OnTipAdjustListener
import com.denovo.app.top.uilayer.sdk.ToPService.OnTransactionListener
import com.denovo.app.top.uilayer.sdk.exceptions.TransactionException
import com.denovo.app.top.uilayer.sdk.utils.TOPParams
import com.denovo.app.top.utility.CommonAppPreference
import com.denovo.app.top.utility.Constants
import com.denovo.app.top.utility.Utils
import com.google.android.material.tabs.TabLayout
import org.json.JSONException
import org.json.JSONObject
import java.util.UUID

class AppLibraryMainActivity : AppCompatActivity(), View.OnClickListener {

    private var onBackPress = true
    private val TAG = "AppLibraryMainActivity"
    private val SESSION_KEY = "SESSION_KEY"
    private lateinit var preference: CommonAppPreference
    private var selectedTabPosition = 0
    private lateinit var toPService: ToPService

    private var btnRegister:Button? = null
    private var btnPay:Button? = null

    private var tabLayout: TabLayout? = null

    private var etTpn:EditText? = null
    private var etMerchantCode:EditText? = null
    private var maAmountEditText:EditText? = null

    private var llPayment:LinearLayout? = null
    private var llRegistration:LinearLayout? = null

    private var tvResult:TextView? = null
    private var tvResultPay:TextView? = null

    private var maApprovalScreenCheck:CheckBox? = null
    private var maReceiptCheck:CheckBox? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        preference = CommonAppPreference(this@AppLibraryMainActivity)
        toPService = ToPService(this@AppLibraryMainActivity)
        init()
    }

    private fun init(){
        btnRegister = findViewById(R.id.btn_register)
        btnPay = findViewById(R.id.btn_Pay)
        tabLayout = findViewById(R.id.tabLayout)
        etTpn = findViewById(R.id.et_tpn)
        etMerchantCode = findViewById(R.id.et_merchantCode)
        maAmountEditText = findViewById(R.id.ma_amountEditText)
        llPayment = findViewById(R.id.ll_payment)
        llRegistration = findViewById(R.id.ll_registration)
        tvResult = findViewById(R.id.tv_result)
        tvResultPay = findViewById(R.id.tv_resultPay)
        maApprovalScreenCheck = findViewById(R.id.ma_approvalScreenCheck)
        maReceiptCheck = findViewById(R.id.ma_receiptCheck)


        btnRegister!!.setOnClickListener(this)
        btnPay!!.setOnClickListener(this)

        tabLayout!!.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                selectedTabPosition = tab!!.position
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {

            }

            override fun onTabReselected(tab: TabLayout.Tab?) {

            }

        })
    }

    override fun onClick(p0: View?) {
        when(p0?.id){
            R.id.btn_register -> {


                //if (tpn.length() == 12 && merchantKey.length() == 12) {
                if (isPermissionGranted(
                        this@AppLibraryMainActivity,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Constants.LOCATION_PERMISSION
                    )
                ) {
                    registerDevice(toPService)
                }
            }

            R.id.btn_Pay -> {
                val type = getTxnTypeBasedOnSelection(selectedTabPosition)
                when (type) {
                    TOPParams.SALE -> startTransaction(TOPParams.SALE, toPService)
                    TOPParams.VOID -> startTransaction(TOPParams.VOID, toPService)
                    TOPParams.REFUND -> startTransaction(TOPParams.REFUND, toPService)
                    TOPParams.TIP_ADJUSTMENT -> showTipPage()
                    TOPParams.ADMINISTRATIVE_TXN -> startTransaction(
                        TOPParams.ADMINISTRATIVE_TXN,
                        toPService
                    )

                    TOPParams.PRE_AUTH -> startTransaction(TOPParams.PRE_AUTH, toPService)
                    TOPParams.TICKET -> setTransactionResultTxt("Not yet implemented")
                    TOPParams.BATCH -> showBatchPage()
                    else -> {}
                }
            }
        }
    }

    private fun registerDevice(toPService: ToPService) {
        val tpn: String = etTpn!!.text.toString().trim()
        val merchantKey: String = etMerchantCode!!.text.toString().trim()
        val progressDialog = ProgressDialog.show(this@AppLibraryMainActivity, " Register", "", true)
        toPService.registerDevice(tpn, merchantKey, object : ToPService.OnRegisterListener {
            override fun onRegisterSuccess(jsonObject: JSONObject) {
                Log.e(TAG, "onRegisterSuccess-$jsonObject")
                progressDialog.dismiss()
                setRegisterResultTxt("" + jsonObject.toString())
                val sessionKey = jsonObject.optString("session_key")
                if (preference != null) {
                    preference!!.putString(SESSION_KEY, sessionKey)
                }

                Toast.makeText(this@AppLibraryMainActivity, "Registration done successfully", Toast.LENGTH_SHORT).show()
                llPayment!!.visibility = View.VISIBLE
                llRegistration!!.visibility = View.GONE

                /*startTransaction("",toPService);*/
            }

            override fun onProcess(s: String?) {
                progressDialog.setMessage(s)
                if (!progressDialog.isShowing) {
                    progressDialog.show()
                }

                llPayment!!.visibility = View.GONE
                llRegistration!!.visibility = View.VISIBLE
            }

            /* @Override
            public void onProcess(String s) {
                progressDialog.setMessage(s);
            }*/
            /*   @Override
            public void onProcess(String s) {
                progressDialog.setMessage(s);
                if (!progressDialog.isShowing()) {
                    progressDialog.show();
                }
            }*/
            override fun onRegisterError(jsonObject: JSONObject) {
                Log.e(TAG, "onRegisterError-$jsonObject")
                Log.d("errorrrr", "onRegisterError: $jsonObject")
                progressDialog.dismiss()
                setRegisterResultTxt("" + jsonObject.toString())

                llPayment!!.visibility = View.GONE
                llRegistration!!.visibility = View.VISIBLE
                Toast.makeText(this@AppLibraryMainActivity, "Something went wrong", Toast.LENGTH_SHORT).show()

            }
        })
    }

    private fun setRegisterResultTxt(value: String){
        runOnUiThread {
            tvResult!!.text = value
        }

    }

    private fun getTxnTypeBasedOnSelection(selectedTabPosition: Int): String? {
        return when (selectedTabPosition) {
            0 -> TOPParams.SALE
            1 -> TOPParams.VOID
            2 -> TOPParams.REFUND
            3 -> TOPParams.PRE_AUTH
            4 -> TOPParams.TICKET
            5 -> TOPParams.BATCH
            6 -> TOPParams.TIP_ADJUSTMENT
            7 -> TOPParams.ADMINISTRATIVE_TXN
            else -> TOPParams.SALE
        }
    }

    private fun startTransaction(type: String, toPService: ToPService) {
        try {
            val amount: String = maAmountEditText!!.text.toString().trim()


            //if (type.equalsIgnoreCase(TOPParams.ADMINISTRATIVE_TXN)  || !amount.equals("0.00")) {
            var jsonRequest: JSONObject? = null
            val jsonObject = JSONObject()
            if (type.equals(TOPParams.SALE, ignoreCase = true)) {
                jsonObject.put("type", TOPParams.SALE)
                /*jsonObject.put("lang", TOPParams.HEBREW);*/jsonObject.put("amount", amount)
                /*jsonObject.put(TOPParams.UN_MASKED_PAN_NEEDED, isFullCardNumberNeeded());*/jsonObject.put(
                    TOPParams.IS_RECEIPTS_NEEDED,
                    isReceiptNeededInResponse()
                )
                jsonObject.put(TOPParams.SHOW_APPROVAL_SCREEN, showApprovalScreen())
                jsonRequest = getCustomObject(jsonObject)
            } else if (type.equals(TOPParams.VOID, ignoreCase = true)) {
                jsonObject.put("type", TOPParams.VOID)
                /*jsonObject.put(TOPParams.UN_MASKED_PAN_NEEDED, isFullCardNumberNeeded());*/jsonRequest =
                    getCustomObject(jsonObject)
            } else if (type.equals(TOPParams.REFUND, ignoreCase = true)) {
                jsonObject.put("type", TOPParams.REFUND)
                jsonObject.put("amount", amount)
                /*jsonObject.put(TOPParams.UN_MASKED_PAN_NEEDED, isFullCardNumberNeeded());*/jsonObject.put(
                    TOPParams.IS_RECEIPTS_NEEDED,
                    isReceiptNeededInResponse()
                )
                jsonObject.put(TOPParams.SHOW_APPROVAL_SCREEN, showApprovalScreen())
                jsonRequest = getCustomObject(jsonObject)
            } else if (type.equals(TOPParams.ADMINISTRATIVE_TXN, ignoreCase = true)) {
                jsonObject.put("type", TOPParams.ADMINISTRATIVE_TXN)
                /*jsonObject.put("lang", TOPParams.HEBREW);*/
                /*jsonObject.put("amount", amount);*/
                /*jsonObject.put(TOPParams.UN_MASKED_PAN_NEEDED, isFullCardNumberNeeded());*/jsonObject.put(
                    TOPParams.IS_RECEIPTS_NEEDED,
                    isReceiptNeededInResponse()
                )
                jsonObject.put(TOPParams.SHOW_APPROVAL_SCREEN, showApprovalScreen())
                jsonRequest = getCustomObject(jsonObject)
            } else if (type.equals(TOPParams.PRE_AUTH, ignoreCase = true)) {
                jsonObject.put("type", TOPParams.PRE_AUTH)
                /*jsonObject.put("lang", TOPParams.HEBREW);*/jsonObject.put("amount", amount)
                /*jsonObject.put(TOPParams.UN_MASKED_PAN_NEEDED, isFullCardNumberNeeded());*/jsonObject.put(
                    TOPParams.IS_RECEIPTS_NEEDED,
                    isReceiptNeededInResponse()
                )
                jsonObject.put(TOPParams.SHOW_APPROVAL_SCREEN, showApprovalScreen())
                jsonRequest = getCustomObject(jsonObject)
            }
            /*
                ProgressDialog progressDialog = ProgressDialog.show(context, " Processing Transaction", "", true);
*/val progressDialog = ProgressDialog.show(this@AppLibraryMainActivity, " Please wait...", "", true)
            val sessionKey = preference.getString(SESSION_KEY, "")
            toPService.performTransaction(jsonRequest, sessionKey, object : OnTransactionListener {

                /*@Override
                public void onTransactionResponse(JSONObject jsonObject) {

                    setTransactionResultTxt("" + jsonObject.toString());
                    Log.d("mResponse",jsonObject.toString());

                }*/
                override fun onTransactionSuccess(jsonObject: JSONObject) {
                    Utils.logPrint('E', "onTransactionSuccess--$jsonObject")
                    setTransactionResultTxt("" + jsonObject.toString())
                    Toast.makeText(this@AppLibraryMainActivity, "Payment Done", Toast.LENGTH_SHORT).show()
                    llPayment!!.visibility = View.GONE
                    llRegistration!!.visibility = View.VISIBLE
                    progressDialog.dismiss()

                }

                override fun onTransactionError(jsonObject: JSONObject) {
                    Utils.logPrint('E', "onTransactionError--$jsonObject")
                    setTransactionResultTxt("" + jsonObject.toString())
                    llPayment!!.visibility = View.GONE
                    llRegistration!!.visibility = View.VISIBLE
                    Toast.makeText(this@AppLibraryMainActivity, "Something went wrong", Toast.LENGTH_SHORT).show()
                    progressDialog.dismiss()
                }

                override fun onRegisterNeeded(jsonObject: JSONObject) {
                    Utils.logPrint('E', "onRegisterNeeded--$jsonObject")
                    setTransactionResultTxt("" + jsonObject.toString())
                    Toast.makeText(this@AppLibraryMainActivity, "Please register first", Toast.LENGTH_SHORT).show()
                    /*registerDevice(toPService);*/
                    llPayment!!.visibility = View.GONE
                    llRegistration!!.visibility = View.VISIBLE
                    progressDialog.dismiss()
                }
            })

            /*} else {
                setTransactionResultTxt("Enter the Amount");
            }*/
        } catch (e: JSONException) {
            e.printStackTrace()
            setTransactionResultTxt("Exp:" + e.message)
        } catch (e: TransactionException) {
            e.printStackTrace()
            setTransactionResultTxt("Exp:" + e.message)
        }
    }

    @Throws(JSONException::class)
    private fun getCustomObject(jsonObject: JSONObject): JSONObject? {
        val customObject = JSONObject()
        customObject.put("CustomerEmail", "graghu@denovosystem.com")
        customObject.put("PhoneNumber", "919840720372")
        customObject.put("CreditType", "1")
        customObject.put("NumberOfPayments", "1")
        customObject.put("ExtraData", "[]")
        customObject.put("HolderID", "1")
        customObject.put("TransactionUniqueIdForQuery", UUID.randomUUID().toString())
        jsonObject.put("CustomObject", customObject)
        return jsonObject
    }

    private fun setTransactionResultTxt(value: String){
        tvResultPay!!.text = value
    }

    private fun showApprovalScreen(): Boolean{
        return maApprovalScreenCheck!!.isChecked
    }

    private fun isReceiptNeededInResponse(): Boolean{
        return maReceiptCheck!!.isChecked
    }

    private fun showTipPage() {
        val Topservicespp = ToPService(this@AppLibraryMainActivity)
        val sessionKey = preference.getString(SESSION_KEY, "")
        Topservicespp.tipAdjustments(sessionKey, object : OnTipAdjustListener {
            override fun onTipViewSuccess(jsonObject: JSONObject) {
                Utils.logPrint('E', "onTipViewSuccess--$jsonObject")
                setTransactionResultTxt("" + jsonObject.toString())
            }

            override fun onTipViewFailed(jsonObject: JSONObject) {
                Utils.logPrint('E', "onTipViewFailed--$jsonObject")
                setTransactionResultTxt("" + jsonObject.toString())
            }

            override fun onRegisterNeeded(jsonObject: JSONObject) {
                Utils.logPrint('E', "onRegisterNeeded--$jsonObject")
                setTransactionResultTxt("" + jsonObject.toString())
            }
        })
    }

    private fun showBatchPage() {
        val toPServicess = ToPService(this@AppLibraryMainActivity)
        val sessionKey = preference.getString(SESSION_KEY, "")
        toPServicess.showBatch(sessionKey, object : OnBatchViewListener {
            override fun onBatchViewSuccess(jsonObject: JSONObject) {
                Utils.logPrint('E', "onBatchViewSuccess--$jsonObject")
                setTransactionResultTxt("" + jsonObject.toString())
            }

            override fun onBatchViewFailed(jsonObject: JSONObject) {
                Utils.logPrint('E', "onBatchViewFailed--$jsonObject")
                setTransactionResultTxt("" + jsonObject.toString())
            }

            override fun onRegisterNeeded(jsonObject: JSONObject) {
                Utils.logPrint('E', "onRegisterNeeded--$jsonObject")
                setTransactionResultTxt("" + jsonObject.toString())
            }
        })
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        Utils.logPrint('E', "SPLASH-PERMISSION-requestCode-$requestCode")
        when (requestCode) {
            Constants.LOCATION_PERMISSION -> if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (toPService != null) {
                    registerDevice(toPService)
                }
            } else {
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        this@AppLibraryMainActivity,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                ) {
                    // now, user has denied permission (but not permanently!)
                    finish()
                } else {
                    // now, user has denied permission permanently!
                    Utils.showPermissionSnackBar(
                        this@AppLibraryMainActivity,
                        resources.getString(com.denovo.app.top.R.string.LOCATION_PERMISSION_CONTENT)
                    )
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions!!, grantResults)
    }

    fun isPermissionGranted(activity: Activity?, permission: String, requestCode: Int?): Boolean {
        return if (ContextCompat.checkSelfPermission(
                activity!!,
                permission
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {

                //This is called if user has denied the permission before
                //In this case I am just asking the permission again
                ActivityCompat.requestPermissions(
                    activity, arrayOf(permission),
                    requestCode!!
                )
            } else {
                ActivityCompat.requestPermissions(
                    activity, arrayOf(permission),
                    requestCode!!
                )
            }
            false
        } else {
            true
        }
    }
}