package com.example.ecolim.ui.gallery

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.ecolim.EcolimApplication
import com.example.ecolim.R
import com.example.ecolim.data.models.ChartSensorReading
import com.example.ecolim.databinding.FragmentGalleryBinding
import com.example.ecolim.ui.ViewModelFactory
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class GalleryFragment : Fragment(), DatePickerDialog.OnDateSetListener {

    private var _binding: FragmentGalleryBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var galleryViewModel: GalleryViewModel
    private lateinit var lineChart: LineChart

    private var startDate: Date = Date()
    private var endDate: Date = Date()
    private var isSelectingStartDate = true // Para saber cuál fecha estamos seleccionando
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGalleryBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Initialize ViewModel with factory
        val application = requireActivity().application as EcolimApplication
        val viewModelFactory = ViewModelFactory(application.serverConfigManager)
        galleryViewModel = ViewModelProvider(this, viewModelFactory)[GalleryViewModel::class.java]

        // Initialize chart
        lineChart = binding.sensorChart
        setupChart()

        // Initialize date range - por defecto últimos 7 días
        val calendar = Calendar.getInstance()
        endDate = Date(calendar.timeInMillis)
        calendar.add(Calendar.DAY_OF_MONTH, -7)
        startDate = Date(calendar.timeInMillis)

        // Initialize date selector
        updateDateDisplay()
        
        binding.buttonSelectDate.setOnClickListener {
            showStartDatePicker()
        }

        binding.buttonLoadData.setOnClickListener {
            loadSensorData()
        }

        // Configurar observadores para el ViewModel
        setupObservers()

        // Load initial data
        loadSensorData()

        return root
    }

    private fun updateDateDisplay() {
        binding.textSelectedDate.setText("Rango: ${dateFormat.format(startDate)} - ${dateFormat.format(endDate)}")
    }

    private fun setupObservers() {
        // Observar los datos del chart
        lifecycleScope.launch {
            galleryViewModel.chartData.collect { sensorData ->
                if (sensorData.isNotEmpty()) {
                    updateChart(sensorData)
                } else {
                    Toast.makeText(
                        requireContext(),
                        "No hay datos disponibles para el rango seleccionado",
                        Toast.LENGTH_SHORT
                    ).show()
                    clearChart()
                }
            }
        }

        // Observar estado de carga
        lifecycleScope.launch {
            galleryViewModel.isLoading.collect { isLoading ->
                binding.buttonLoadData.isEnabled = !isLoading
                if (isLoading) {
                    binding.buttonLoadData.setText("Cargando...")
                } else {
                    binding.buttonLoadData.setText("Cargar Datos")
                }
            }
        }

        // Observar errores
        lifecycleScope.launch {
            galleryViewModel.errorMessage.collect { errorMessage ->
                errorMessage?.let {
                    Toast.makeText(
                        requireContext(),
                        "Error: $it",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun showStartDatePicker() {
        isSelectingStartDate = true
        val calendar = Calendar.getInstance()
        calendar.time = startDate
        
        val datePickerDialog = DatePickerDialog.newInstance(
            this,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        
        datePickerDialog.show(parentFragmentManager, "StartDatePicker")
    }

    private fun showEndDatePicker() {
        isSelectingStartDate = false
        val calendar = Calendar.getInstance()
        calendar.time = endDate
        
        val datePickerDialog = DatePickerDialog.newInstance(
            this,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        
        datePickerDialog.show(parentFragmentManager, "EndDatePicker")
    }

    private fun setupChart() {
        lineChart.apply {
            description = Description().apply {
                text = "Datos de Sensores"
                textSize = 12f
                textColor = Color.GRAY
            }
            
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            
            // Configure X axis
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                textSize = 10f
                textColor = Color.GRAY
                setDrawGridLines(true)
                granularity = 1f
            }
            
            // Configure Y axis
            axisLeft.apply {
                textColor = Color.GRAY
                textSize = 10f
                setDrawGridLines(true)
            }
            
            axisRight.isEnabled = false
            
            legend.apply {
                textSize = 12f
                textColor = Color.GRAY
            }
            
            // Set background
            setBackgroundColor(Color.WHITE)
        }
    }

    private fun loadSensorData() {
        // Formatear las fechas para el ViewModel
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val startDateStr = sdf.format(startDate)
        val endDateStr = sdf.format(endDate)
        
        // Llamar al método del ViewModel que carga los datos
        galleryViewModel.getSensorDataByDateRange(startDateStr, endDateStr)
    }

    private fun updateChart(sensorData: List<ChartSensorReading>) {
        val humidityEntries = mutableListOf<Entry>()
        val lightEntries = mutableListOf<Entry>()
        
        val timeLabels = mutableListOf<String>()
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())

        sensorData.forEachIndexed { index, reading ->
            val x = index.toFloat()
            
            humidityEntries.add(Entry(x, reading.humidity))
            lightEntries.add(Entry(x, reading.lightLevel))
            
            // Parsear el timestamp string y formatear para mostrar
            try {
                val date = isoFormat.parse(reading.timestamp)
                timeLabels.add(if (date != null) timeFormat.format(date) else "00:00")
            } catch (e: Exception) {
                timeLabels.add("00:00")
            }
        }

        val dataSets = mutableListOf<LineDataSet>()

        // Humidity dataset
        if (humidityEntries.isNotEmpty()) {
            val humidityDataSet = LineDataSet(humidityEntries, "Humedad (%)").apply {
                color = ContextCompat.getColor(requireContext(), R.color.humidity_color)
                setCircleColor(ContextCompat.getColor(requireContext(), R.color.humidity_color))
                lineWidth = 3f
                circleRadius = 4f
                setDrawCircleHole(false)
                valueTextSize = 9f
                setDrawValues(false)
            }
            dataSets.add(humidityDataSet)
        }

        // Light level dataset
        if (lightEntries.isNotEmpty()) {
            val lightDataSet = LineDataSet(lightEntries, "Nivel de Luz").apply {
                color = ContextCompat.getColor(requireContext(), R.color.light_color)
                setCircleColor(ContextCompat.getColor(requireContext(), R.color.light_color))
                lineWidth = 3f
                circleRadius = 4f
                setDrawCircleHole(false)
                valueTextSize = 9f
                setDrawValues(false)
            }
            dataSets.add(lightDataSet)
        }

        val lineData = LineData(dataSets as List<com.github.mikephil.charting.interfaces.datasets.ILineDataSet>)
        lineChart.data = lineData
        
        // Set X axis labels
        lineChart.xAxis.valueFormatter = IndexAxisValueFormatter(timeLabels)
        
        lineChart.invalidate() // Refresh chart
    }

    private fun clearChart() {
        lineChart.clear()
        lineChart.invalidate()
    }

    override fun onDateSet(view: DatePickerDialog?, year: Int, monthOfYear: Int, dayOfMonth: Int) {
        val calendar = Calendar.getInstance()
        calendar.set(year, monthOfYear, dayOfMonth)
        
        if (isSelectingStartDate) {
            startDate = calendar.time
            // Si la fecha de inicio es posterior a la fecha de fin, ajustar la fecha de fin
            if (startDate.after(endDate)) {
                endDate = Date(startDate.time)
            }
            // Después de seleccionar fecha de inicio, mostrar selector de fecha de fin
            updateDateDisplay()
            showEndDatePicker()
        } else {
            endDate = calendar.time
            // Si la fecha de fin es anterior a la fecha de inicio, ajustar la fecha de inicio
            if (endDate.before(startDate)) {
                startDate = Date(endDate.time)
            }
            updateDateDisplay()
            // Automatically load data for the new date range
            loadSensorData()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}