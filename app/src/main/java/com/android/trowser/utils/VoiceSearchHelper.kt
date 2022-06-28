package com.android.trowser.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.android.trowser.R
import com.android.trowser.databinding.ViewSpeechRecognizerResultsBinding


class VoiceSearchHelper(private val activity: Activity, private val requestCode: Int, private val permissionRequestCode: Int) {

    private var recognitionResultsRendererView: RecognitionResultsRendererView? = null
    private lateinit var languageModel: String
    private lateinit var callback: Callback

    interface Callback {
        fun onResult(text: String?)
        /*
        fun onFallbackPartialResult(text: String)
        fun onFallbackStartedRecognizing()
        fun onFallbackError(error: Int)
         */
    }

    fun initiateVoiceSearch(callback: Callback,
                            languageModel: String = RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH) {
        this.callback = callback
        this.languageModel = languageModel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            initiateVoiceSearchAndroid11AndNext()
        } else {
            val pm = activity.packageManager
            val activities = pm.queryIntentActivities(
                Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0
            )
            if (activities.size == 0) {
                showInstallVoiceEnginePrompt(activity)
            } else {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, languageModel)
                intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1000)
                //intent.putExtra(RecognizerIntent.EXTRA_PROMPT, activity.getString(R.string.speak))
                //intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1500);
                try {
                    activity.startActivityForResult(intent, requestCode)
                } catch (e: Exception) {
                    Toast.makeText(activity, R.string.error, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showInstallVoiceEnginePrompt(activity: Activity) {
        val dialogBuilder = AlertDialog.Builder(activity)
            .setTitle(R.string.app_name)
            .setMessage(R.string.voice_search_not_found)
            .setNeutralButton(android.R.string.ok) { _, _ -> }
        val appPackageName =
            if (Utils.isTV(activity)) "com.google.android.katniss" else "com.google.android.googlequicksearchbox"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$appPackageName"))
        val activities = activity.packageManager.queryIntentActivities(intent, 0)
        if (activities.size > 0) {
            dialogBuilder.setPositiveButton(R.string.find_in_apps_store) { _, _ ->
                try {
                    activity.startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(activity, R.string.error, Toast.LENGTH_SHORT).show()
                }
            }
        }
        dialogBuilder.show()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun initiateVoiceSearchAndroid11AndNext() {
        if (activity.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            activity.requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), permissionRequestCode)
            return
        }

        val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(activity)
        recognitionResultsRendererView = RecognitionResultsRendererView(activity)
        recognitionResultsRendererView?.setSpeechRecognizer(speechRecognizer)
        recognitionResultsRendererView?.setRecognitionListener(callback, ::disposeResultsView)



        val speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, languageModel)
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, activity.packageName)
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000)

        val lp = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        activity.addContentView(recognitionResultsRendererView, lp)
        speechRecognizer.startListening(speechRecognizerIntent)

    }

    // Moving this function inside RecognitionResultsRendererView does not work, why?
    private fun disposeResultsView() {
        (recognitionResultsRendererView?.parent as? ViewGroup)?.apply {
            removeView(recognitionResultsRendererView)
            recognitionResultsRendererView = null
        }
    }

    fun processActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCode != this.requestCode) return false

        if (resultCode == Activity.RESULT_OK) {
            val matches = data?.getStringArrayListExtra(
                RecognizerIntent.EXTRA_RESULTS)

            callback.onResult(matches?.firstOrNull())
        }
        return true
    }

    fun processPermissionsResult(requestCode: Int, permissions: Array<String>,
                                 grantResults: IntArray): Boolean {
        if (requestCode != permissionRequestCode) return false
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initiateVoiceSearch(callback)
        }
        return true
    }

    open class RecognitionListenerAdapter: RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
        }

        override fun onBeginningOfSpeech() {
        }

        override fun onRmsChanged(rmsdB: Float) {
        }

        override fun onBufferReceived(buffer: ByteArray?) {
        }

        override fun onEndOfSpeech() {
        }

        override fun onError(error: Int) {
        }

        override fun onResults(results: Bundle?) {
        }

        override fun onPartialResults(partialResults: Bundle?) {
        }

        override fun onEvent(eventType: Int, params: Bundle?) {
        }
    }

    class RecognitionResultsRendererView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
    ) : FrameLayout(context, attrs, defStyleAttr) {

        private var vb = ViewSpeechRecognizerResultsBinding.inflate(LayoutInflater.from(activity), this)
        private val recognitionProgressView = vb.ivMic

        var resultText: String = ""
        set(value) {
            field = value
            vb.tvResults.text = value
        }

        init {
            setBackgroundResource(R.color.top_bar_background)
            val h = (resources.getDimension(R.dimen.voice_recognition_results_renderer_height)/2).toInt() - 2
            val heights = intArrayOf((0.83*h).toInt(), h, (0.75*h).toInt(), (0.95*h).toInt(), (0.66*h).toInt())
            recognitionProgressView.setBarMaxHeightsInDp(heights)

            val colors = intArrayOf(
                ContextCompat.getColor(context, R.color.GBlue),
                ContextCompat.getColor(context, R.color.GRed),
                ContextCompat.getColor(context, R.color.GYellow),
                ContextCompat.getColor(context, R.color.GBlue),
                ContextCompat.getColor(context, R.color.GGreen)
            )
            recognitionProgressView.setColors(colors)
            elevation = Utils.D2P(context, 5f)
            // unable to start animation from onReadyForSpeech or onBeginningOfSpeech
            recognitionProgressView.setCircleRadiusInDp(3)
            recognitionProgressView.setSpacingInDp(3)
            recognitionProgressView.play()
        }

        fun setSpeechRecognizer(speechRecognizer: SpeechRecognizer){
            recognitionProgressView.setSpeechRecognizer(speechRecognizer)
        }

        fun setRecognitionListener(callback: Callback, dispose: () -> (Unit)){
            recognitionProgressView.setRecognitionListener(object : RecognitionListenerAdapter() {
                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (matches != null && matches.isNotEmpty()) {
                            resultText = matches.first()
                    }
                }

                override fun onEndOfSpeech() {
                    recognitionProgressView.stop()
                    dispose()
                    super.onEndOfSpeech()
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    callback.onResult(matches?.firstOrNull())
                }

                override fun onError(error: Int) {
                    Toast.makeText(activity, R.string.error, Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
}