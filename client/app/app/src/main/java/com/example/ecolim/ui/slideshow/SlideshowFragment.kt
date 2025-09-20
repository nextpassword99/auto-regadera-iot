package com.example.ecolim.ui.slideshow

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.ecolim.EcolimApplication
import com.example.ecolim.databinding.FragmentSlideshowBinding
import com.example.ecolim.ui.ViewModelFactory
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog
import java.text.SimpleDateFormat
import java.util.*

class SlideshowFragment : Fragment(), DatePickerDialog.OnDateSetListener {

    private var _binding: FragmentSlideshowBinding? = null
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private var isStartDatePicker = true
    private lateinit var slideshowViewModel: SlideshowViewModel

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val application = requireActivity().application as EcolimApplication
        val factory = ViewModelFactory(application.serverConfigManager)
        slideshowViewModel = ViewModelProvider(this, factory)[SlideshowViewModel::class.java]
        
        _binding = FragmentSlideshowBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setupDatePickers()

        return root
    }

    private fun setupDatePickers() {
        // Configurar click listeners para los campos de fecha
        binding.editFilterStartDate.setOnClickListener {
            isStartDatePicker = true
            showDatePicker()
        }

        binding.editFilterEndDate.setOnClickListener {
            isStartDatePicker = false
            showDatePicker()
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog.newInstance(
            this,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        
        // Configurar tema
        datePickerDialog.accentColor = resources.getColor(android.R.color.holo_blue_dark, null)
        datePickerDialog.show(parentFragmentManager, "DatePickerDialog")
    }

    override fun onDateSet(view: DatePickerDialog?, year: Int, monthOfYear: Int, dayOfMonth: Int) {
        val calendar = Calendar.getInstance()
        calendar.set(year, monthOfYear, dayOfMonth)
        val selectedDate = dateFormat.format(calendar.time)

        if (isStartDatePicker) {
            binding.editFilterStartDate.setText(selectedDate)
        } else {
            binding.editFilterEndDate.setText(selectedDate)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}