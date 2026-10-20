package com.dermify.dermify.ui.camera

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.dermify.dermify.databinding.FragmentCameraBinding
import java.io.File
import androidx.camera.core.ImageCaptureException
import androidx.navigation.fragment.findNavController
import com.dermify.dermify.R
import android.net.Uri
import androidx.activity.result.PickVisualMediaRequest
import android.graphics.BitmapFactory
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection

class CameraFragment : Fragment() {

    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!
    private lateinit var imageCapture: ImageCapture

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startCamera()
        else Toast.makeText(requireContext(), "Camera permission needed", Toast.LENGTH_SHORT).show()
    }
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) handleGalleryImage(uri)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }

        binding.btnCapture.setOnClickListener { takePhoto() }
        binding.btnGallery.setOnClickListener {
            galleryLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }
            imageCapture = ImageCapture.Builder().build()
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                viewLifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview, imageCapture
            )
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun takePhoto() {
        val file = File(requireContext().cacheDir, "skin_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()
        imageCapture.takePicture(outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    requireActivity().runOnUiThread {
                        detectFaceThenNavigate(file.absolutePath)
                    }
                }
                override fun onError(exc: ImageCaptureException) {
                    Toast.makeText(requireContext(), "Capture failed", Toast.LENGTH_SHORT).show()
                }
            })
    }
    private fun handleGalleryImage(uri: Uri) {
        try {
            val file = File(requireContext().cacheDir, "skin_${System.currentTimeMillis()}.jpg")
            requireContext().contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }

            detectFaceThenNavigate(file.absolutePath)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Failed to load image", Toast.LENGTH_SHORT).show()
        }
    }
    private fun detectFaceThenNavigate(imagePath: String) {
        val switchChecked = binding.switchFaceScan.isChecked

        // ML Kit detection is a best-effort backstop. If anything goes wrong,
        // we fall back to the toggle value and navigate normally — never crash.
        try {
            val bitmap = BitmapFactory.decodeFile(imagePath)
            if (bitmap == null) {
                navigateToResult(imagePath, switchChecked)
                return
            }
            // --- Two-layer capture gate ---
            val captureGate = com.dermify.dermify.ml.CaptureGate(requireContext())
            val verdict = captureGate.check(bitmap)
            if (verdict is com.dermify.dermify.ml.CaptureGate.Verdict.Rejected) {
                com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                    .setTitle("No skin detected")
                    .setMessage(verdict.userMessage)
                    .setPositiveButton("Retake") { d, _ -> d.dismiss() }
                    .setCancelable(false)
                    .show()
                return
            }
            // --- end gate ---

            val image = InputImage.fromBitmap(bitmap, 0)
            val detector = FaceDetection.getClient()
            detector.process(image)
                .addOnSuccessListener { faces ->
                    val isFaceScan = switchChecked || faces.isNotEmpty()
                    navigateToResult(imagePath, isFaceScan)
                }
                .addOnFailureListener {
                    navigateToResult(imagePath, switchChecked)
                }
        } catch (e: Exception) {
            android.util.Log.e("FaceDetect", "Detection skipped", e)
            navigateToResult(imagePath, switchChecked)
        }
    }

    private fun navigateToResult(imagePath: String, isFaceScan: Boolean) {
        // Guard against the fragment being gone by the time an async callback fires.
        if (!isAdded || _binding == null) return
        requireActivity().runOnUiThread {
            if (!isAdded || _binding == null) return@runOnUiThread
            try {
                val bundle = Bundle().apply {
                    putString("imagePath", imagePath)
                    putBoolean("isFaceScan", isFaceScan)
                }
                findNavController().navigate(R.id.action_camera_to_result, bundle)
            } catch (e: Exception) {
                android.util.Log.e("FaceDetect", "Navigation skipped", e)
            }
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
