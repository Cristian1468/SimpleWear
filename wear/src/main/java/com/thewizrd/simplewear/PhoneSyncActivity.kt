package com.thewizrd.simplewear

import android.bluetooth.BluetoothAdapter
import android.content.*
import android.os.Bundle
import android.support.wearable.view.ConfirmationOverlay
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.wearable.intent.RemoteIntent
import com.thewizrd.shared_resources.helpers.WearConnectionStatus
import com.thewizrd.shared_resources.helpers.WearableHelper
import com.thewizrd.simplewear.databinding.ActivitySetupSyncBinding
import kotlinx.coroutines.launch

class PhoneSyncActivity : WearableListenerActivity() {
    override lateinit var broadcastReceiver: BroadcastReceiver
        private set
    override lateinit var intentFilter: IntentFilter
        private set

    private lateinit var binding: ActivitySetupSyncBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create your application here
        binding = ActivitySetupSyncBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bluetoothButton.setOnClickListener {
            runCatching {
                startActivity(
                    Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }

        startProgressBar()

        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (ACTION_UPDATECONNECTIONSTATUS == intent.action) {
                    when (WearConnectionStatus.valueOf(
                        intent.getIntExtra(
                            EXTRA_CONNECTIONSTATUS,
                            0
                        )
                    )) {
                        WearConnectionStatus.DISCONNECTED -> {
                            binding.message.setText(R.string.status_disconnected)
                            binding.button.setImageDrawable(
                                ContextCompat.getDrawable(
                                    context,
                                    R.drawable.ic_phonelink_erase_white_24dp
                                )
                            )
                            binding.circularProgress.setOnClickListener {
                                lifecycleScope.launch {
                                    startProgressBar()
                                    updateConnectionStatus()
                                }
                            }
                            checkBluetoothStatus()
                            stopProgressBar()
                        }
                        WearConnectionStatus.CONNECTING -> {
                            binding.message.setText(R.string.status_connecting)
                            binding.button.setImageDrawable(
                                ContextCompat.getDrawable(
                                    context,
                                    android.R.drawable.ic_popup_sync
                                )
                            )
                            binding.bluetoothButton.visibility = View.GONE
                        }
                        WearConnectionStatus.APPNOTINSTALLED -> {
                            binding.message.setText(R.string.error_notinstalled)

                            binding.circularProgress.setOnClickListener {
                                // Open store on remote device
                                val intentAndroid = Intent(Intent.ACTION_VIEW)
                                    .addCategory(Intent.CATEGORY_BROWSABLE)
                                    .setData(WearableHelper.getPlayStoreURI())

                                RemoteIntent.startRemoteActivity(
                                    this@PhoneSyncActivity,
                                    intentAndroid,
                                    null
                                )

                                // Show open on phone animation
                                ConfirmationOverlay().setType(ConfirmationOverlay.OPEN_ON_PHONE_ANIMATION)
                                    .setMessage(this@PhoneSyncActivity.getString(R.string.message_openedonphone))
                                    .showOn(this@PhoneSyncActivity)
                            }
                            binding.button.setImageDrawable(
                                ContextCompat.getDrawable(
                                    context,
                                    R.drawable.open_on_phone
                                )
                            )
                            binding.bluetoothButton.visibility = View.GONE

                            stopProgressBar()
                        }
                        WearConnectionStatus.CONNECTED -> {
                            binding.message.setText(R.string.status_connected)
                            binding.button.setImageDrawable(ContextCompat.getDrawable(context, android.R.drawable.ic_popup_sync))
                            binding.bluetoothButton.visibility = View.GONE

                            // Continue operation
                            startActivity(Intent(this@PhoneSyncActivity, DashboardActivity::class.java)
                                    .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
                            stopProgressBar()
                        }
                    }
                } else if (ACTION_OPENONPHONE == intent.action) {
                    val success = intent.getBooleanExtra(EXTRA_SUCCESS, false)

                    ConfirmationOverlay()
                            .setType(if (success) ConfirmationOverlay.OPEN_ON_PHONE_ANIMATION else ConfirmationOverlay.FAILURE_ANIMATION)
                            .showOn(this@PhoneSyncActivity)

                    if (!success) {
                        binding.message.setText(R.string.error_syncing)
                    }
                }
            }
        }

        binding.message.setText(R.string.message_gettingstatus)

        intentFilter = IntentFilter(ACTION_UPDATECONNECTIONSTATUS)
    }

    private fun stopProgressBar() {
        binding.circularProgress.isIndeterminate = false
        binding.circularProgress.totalTime = 1
    }

    private fun startProgressBar() {
        binding.circularProgress.isIndeterminate = true
    }

    override fun onResume() {
        super.onResume()

        // Update statuses
        lifecycleScope.launch {
            updateConnectionStatus()
        }
    }

    private fun checkBluetoothStatus() {
        val btAdapter = BluetoothAdapter.getDefaultAdapter()
        if (btAdapter != null) {
            binding.bluetoothButton.visibility = View.VISIBLE
            if (!btAdapter.isEnabled) {
                Toast.makeText(this, R.string.message_enablebt, Toast.LENGTH_SHORT).show()
            }
        } else {
            binding.bluetoothButton.visibility = View.GONE
        }
    }
}