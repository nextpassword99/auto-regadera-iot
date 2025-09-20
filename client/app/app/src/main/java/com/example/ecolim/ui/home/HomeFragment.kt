package com.example.ecolim.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.ecolim.EcolimApplication
import com.example.ecolim.R
import com.example.ecolim.databinding.FragmentHomeBinding
import com.example.ecolim.ui.ViewModelFactory
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var homeViewModel: HomeViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val application = requireActivity().application as EcolimApplication
        val factory = ViewModelFactory(application.serverConfigManager)
        homeViewModel = ViewModelProvider(this, factory)[HomeViewModel::class.java]
        
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        
        setupUI()
        observeViewModel()
        
        return binding.root
    }

    private fun setupUI() {
        // Configuración inicial de la UI
        updateInitialState()
        
        // Configurar listeners
        binding.btnSettings.setOnClickListener {
            findNavController().navigate(R.id.nav_settings)
        }
        
        // binding.btnRefresh.setOnClickListener {
        //     homeViewModel.refreshData()
        // }
    }

    private fun updateInitialState() {
        // Configuración inicial de la UI
        binding.apply {
            try {
                // textHumidity.text = "---"
                // textLight.text = "---"
                // textMode.text = "---"
                // textSoilType.text = "Tipo de suelo: ---"
                // textPumpStatus.text = "Inactiva"
                // connectionStatusText.text = "Desconectado"
                
                // TODO: Uncomment when layout is properly generated
            } catch (e: Exception) {
                // Manejar errores de binding temporalmente
            }
        }
    }

    private fun observeViewModel() {
        // Observar el texto básico por ahora
        homeViewModel.text.observe(viewLifecycleOwner) { _ ->
            // binding.textHome.text = text
        }
        
        // TODO: Agregar observadores de StateFlow cuando el layout esté listo
        /*
        viewLifecycleOwner.lifecycleScope.launch {
            homeViewModel.currentSensorData.collect { sensorData ->
                if (sensorData != null) {
                    updateSensorData(sensorData)
                }
            }
        }
        */
    }

    // Métodos comentados temporalmente hasta que el layout esté listo
    /*
    private fun updateSensorData(sensorData: com.example.ecolim.data.models.SensorReading) {
        binding.apply {
            textHumidity.text = sensorData.humidity.toInt().toString()
            textLight.text = sensorData.light.toInt().toString()
            textMode.text = sensorData.mode.replaceFirstChar { it.uppercaseChar() }
            textSoilType.text = "Tipo de suelo: ${sensorData.soilType.replaceFirstChar { it.uppercaseChar() }}"
            
            if (sensorData.pumpStatus) {
                textPumpStatus.text = "Activa"
                pumpIndicator.setBackgroundResource(R.drawable.circle_blue)
            } else {
                textPumpStatus.text = "Inactiva"
                pumpIndicator.setBackgroundResource(R.drawable.circle_gray)
            }
        }
    }

    private fun updateConnectionStatus(isConnected: Boolean) {
        binding.apply {
            if (isConnected) {
                connectionStatusText.text = "Conectado"
                connectionIndicator.setBackgroundResource(R.drawable.circle_green)
            } else {
                connectionStatusText.text = "Desconectado"
                connectionIndicator.setBackgroundResource(R.drawable.circle_red)
            }
        }
    }
    */

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}