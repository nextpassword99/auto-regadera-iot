package com.example.ecolim.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.ecolim.R
import com.example.ecolim.data.network.ApiClient
import com.example.ecolim.data.preferences.ServerConfigManager
import com.example.ecolim.databinding.FragmentSettingsBinding
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var serverConfigManager: ServerConfigManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        serverConfigManager = ServerConfigManager(requireContext())
        
        setupUI()
        loadCurrentConfig()
        setupListeners()
    }

    private fun setupUI() {
        // Configurar listeners para actualizar URL en tiempo real
        binding.editServerIp.addTextChangedListener { updateResultingUrl() }
        binding.editServerPort.addTextChangedListener { updateResultingUrl() }
    }

    private fun loadCurrentConfig() {
        binding.editServerIp.setText(serverConfigManager.serverIp)
        binding.editServerPort.setText(serverConfigManager.serverPort)
        updateResultingUrl()
        updateConnectionStatus()
    }

    private fun setupListeners() {
        // Botón guardar configuración
        binding.btnSaveConfig.setOnClickListener {
            saveConfiguration()
        }

        // Botón probar conexión
        binding.btnTestConnection.setOnClickListener {
            testConnection()
        }

        // Configuraciones rápidas
        binding.btnLocalhost.setOnClickListener {
            setQuickConfig("127.0.0.1", "8000")
        }

        binding.btnLocalNetwork.setOnClickListener {
            setQuickConfig("192.168.18.21", "8000")
        }

        binding.btnResetDefaults.setOnClickListener {
            resetToDefaults()
        }
    }

    private fun updateResultingUrl() {
        val ip = binding.editServerIp.text.toString().trim()
        val port = binding.editServerPort.text.toString().trim()
        
        if (ip.isNotEmpty() && port.isNotEmpty()) {
            val url = "http://$ip:$port"
            binding.textResultingUrl.text = url
        } else {
            binding.textResultingUrl.text = "URL incompleta"
        }
    }

    private fun saveConfiguration() {
        val ip = binding.editServerIp.text.toString().trim()
        val port = binding.editServerPort.text.toString().trim()

        if (ip.isEmpty()) {
            binding.editServerIp.error = "La IP es requerida"
            return
        }

        if (port.isEmpty()) {
            binding.editServerPort.error = "El puerto es requerido"
            return
        }

        if (port.toIntOrNull() == null) {
            binding.editServerPort.error = "Puerto inválido"
            return
        }

        // Guardar configuración
        serverConfigManager.serverIp = ip
        serverConfigManager.serverPort = port

        // Actualizar cliente API
        ApiClient.updateServerConfig(serverConfigManager)

        Toast.makeText(requireContext(), "✅ Configuración guardada", Toast.LENGTH_SHORT).show()
        updateConnectionStatus()
    }

    private fun testConnection() {
        if (!serverConfigManager.isValidConfig()) {
            Toast.makeText(requireContext(), "❌ Configuración inválida", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.textConnectionStatus.text = "Probando conexión..."
        binding.connectionIndicator.setBackgroundResource(R.drawable.circle_yellow)

        lifecycleScope.launch {
            try {
                // Probar conexión con el endpoint de latest reading
                val response = ApiClient.apiService.getLatestSensorReading()
                
                if (response.isSuccessful) {
                    binding.textConnectionStatus.text = "✅ Conexión exitosa"
                    binding.textConnectionDetails.text = "Servidor responde correctamente"
                    binding.connectionIndicator.setBackgroundResource(R.drawable.circle_green)
                } else {
                    binding.textConnectionStatus.text = "⚠️ Servidor responde con error"
                    binding.textConnectionDetails.text = "Código: ${response.code()}"
                    binding.connectionIndicator.setBackgroundResource(R.drawable.circle_red)
                }
            } catch (e: Exception) {
                binding.textConnectionStatus.text = "❌ Error de conexión"
                binding.textConnectionDetails.text = "Error: ${e.message}"
                binding.connectionIndicator.setBackgroundResource(R.drawable.circle_red)
            }
            
            binding.progressBar.visibility = View.GONE
        }
    }

    private fun setQuickConfig(ip: String, port: String) {
        binding.editServerIp.setText(ip)
        binding.editServerPort.setText(port)
        updateResultingUrl()
    }

    private fun resetToDefaults() {
        serverConfigManager.resetToDefaults()
        loadCurrentConfig()
        Toast.makeText(requireContext(), "🔄 Configuración restablecida", Toast.LENGTH_SHORT).show()
    }

    private fun updateConnectionStatus() {
        if (serverConfigManager.isValidConfig()) {
            binding.textConnectionStatus.text = "Configuración lista"
            binding.textConnectionDetails.text = "Conectando a: ${serverConfigManager.getServerUrl()}"
            binding.connectionIndicator.setBackgroundResource(R.drawable.circle_yellow)
        } else {
            binding.textConnectionStatus.text = "Configuración incompleta"
            binding.textConnectionDetails.text = "Complete la IP y puerto del servidor"
            binding.connectionIndicator.setBackgroundResource(R.drawable.circle_red)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}