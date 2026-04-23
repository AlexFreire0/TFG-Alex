package com.example.tfg.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.tfg.databinding.FragmentReviewDialogBinding
import com.example.tfg.models.CalificacionRequest
import com.example.tfg.network.RetrofitClient
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReviewDialogFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentReviewDialogBinding? = null
    private val binding get() = _binding!!

    var intercambioId: Long = -1L
    var estadoIntercambio: String = ""
    var onReviewSubmitted: (() -> Unit)? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentReviewDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val isCancelado = estadoIntercambio.equals("Cancelado", ignoreCase = true) || estadoIntercambio.equals("Expirada", ignoreCase = true)
        
        if (isCancelado) {
            binding.tvReviewTitle.text = "¿Qué salió mal?"
            binding.tilObservaciones.hint = "Por favor, explica qué ocurrió *"
        } else {
            binding.tvReviewTitle.text = "¿Cómo fue el intercambio?"
            binding.tilObservaciones.hint = "Deja un comentario (opcional)"
        }

        binding.btnSubmitReview.setOnClickListener {
            val estrellas = binding.ratingBar.rating.toInt()
            val observacion = binding.etObservaciones.text.toString().trim()

            if (estrellas == 0) {
                Toast.makeText(requireContext(), "Por favor, selecciona una puntuación.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (isCancelado && observacion.isEmpty()) {
                binding.tilObservaciones.error = "Es obligatorio dejar un comentario."
                return@setOnClickListener
            } else {
                binding.tilObservaciones.error = null
            }

            if (intercambioId == -1L) return@setOnClickListener

            binding.btnSubmitReview.isEnabled = false
            enviarCalificacion(estrellas, observacion)
        }
    }

    private fun enviarCalificacion(estrellas: Int, observacion: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.getApiService().calificarIntercambio(
                    intercambioId,
                    CalificacionRequest(estrellas, observacion)
                )
                
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(requireContext(), "¡Feedback guardado con éxito!", Toast.LENGTH_SHORT).show()
                        dismiss()
                        onReviewSubmitted?.invoke()
                    } else {
                        binding.btnSubmitReview.isEnabled = true
                        Toast.makeText(requireContext(), "Error al guardar calificación.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.btnSubmitReview.isEnabled = true
                    Toast.makeText(requireContext(), "Error de red.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(intercambioId: Long, estadoIntercambio: String): ReviewDialogFragment {
            val fragment = ReviewDialogFragment()
            fragment.intercambioId = intercambioId
            fragment.estadoIntercambio = estadoIntercambio
            return fragment
        }
    }
}
