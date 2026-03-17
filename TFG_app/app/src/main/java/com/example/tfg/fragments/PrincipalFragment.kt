package com.example.tfg.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.tfg.activities.MapsActivity
import com.example.tfg.activities.OfrecerPlazaActivity
import com.example.tfg.databinding.FragmentPrincipalBinding
import com.example.tfg.models.Usuario

class PrincipalFragment : Fragment() {

    private var _binding: FragmentPrincipalBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPrincipalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Recuperamos el usuario
        val usuario = activity?.intent?.getParcelableExtra<Usuario>("USER_DATA")

        // 1. Click en la tarjeta de BUSCAR
        binding.cardBuscar.setOnClickListener {
            val intent = Intent(requireContext(), MapsActivity::class.java)
            intent.putExtra("USER_DATA", usuario)
            startActivity(intent)
        }

        // 2. Click en la tarjeta de OFRECER
        binding.cardOfrecer.setOnClickListener {
            val intent = Intent(requireContext(), OfrecerPlazaActivity::class.java)
            intent.putExtra("USER_DATA", usuario)
            startActivity(intent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}